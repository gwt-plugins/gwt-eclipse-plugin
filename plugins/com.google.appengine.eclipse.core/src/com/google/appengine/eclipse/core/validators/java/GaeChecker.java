/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.appengine.eclipse.core.validators.java;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.markers.AppEngineJavaProblem;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.GaeSdk;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.launching.JavaRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checks that only Google App Engine supported JRE types are used by a
 * {@link CompilationUnit}, and no imports from com.google.appengine.repackaged
 * are used.
 * 
 * TODO: This class requires {@link ITypeBinding}s in order to determine the
 * fully qualified name of the type referenced by a {@link SimpleName} node.
 * However, these are expensive; it seems like there should be a lighter weight
 * way of getting fully qualified names.
 */
public class GaeChecker {
  /**
   * Validates that the {@link CompilationUnit} uses only JRE type that are
   * supported by the Google App Engine. A {@link CategorizedProblem} will be
   * added to the {@link #problems} for each use of an unsupported JRE type.
   * 
   * Also validates that there are no imports from
   * com.google.appengine.repackaged. A {@link CategorizedProblem} will be added
   * to the {@link #problems} for each such import.
   * 
   * TODO: We maybe able to refactor this validation into a visitor that
   * collects all references to JRE members and a utility class that iterates
   * over the references and creates the {@link CategorizedProblem}s.
   */
  private static class GaeValidationVisitor extends ASTVisitor {

    /**
     * Returns <code>true</code> if the type is part of the JRE,
     * <code>false</code> otherwise.
     * 
     * @throws JavaModelException
     */
    private static boolean isJreType(IType type) throws JavaModelException {
      IPackageFragmentRoot packageFragmentRoot = getPackageFragmentRoot(type);
      if (packageFragmentRoot != null) {
        IClasspathEntry cpEntry = packageFragmentRoot.getRawClasspathEntry();
        if (cpEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
            && cpEntry.getPath().segment(0).equals(JavaRuntime.JRE_CONTAINER)) {
          return true;
        }
      }

      return false;
    }

    private final IJavaProject javaProject;

    private final List<CategorizedProblem> problems;

    private final Set<String> whiteList;

    GaeValidationVisitor(GaeProject gaeProject,
        List<CategorizedProblem> problems) {
      this.javaProject = gaeProject.getJavaProject();
      this.problems = problems;
      GaeSdk sdk = gaeProject.getSdk();
      assert (sdk != null);
      this.whiteList = sdk.getWhiteList();
    }

    @Override
    public void endVisit(SimpleName node) {
      IBinding binding = node.resolveBinding();
      if (binding == null) {
        // Ignore nodes for which there is no binding
        return;
      }

      if (binding.getKind() == IBinding.TYPE) {
        validateTypeReference(node, (ITypeBinding) binding);
      }
    }

    @Override
    public boolean visit(ImportDeclaration node) {
      if (node.getName().getFullyQualifiedName().startsWith(
          "com.google.appengine.repackaged")) {
        CategorizedProblem problem = AppEngineJavaProblem.createRepackagedImportError(node);
        if (problem != null) {
          problems.add(problem);
        }
      }
      return false;
    }

    @Override
    public boolean visit(PackageDeclaration node) {
      // Ignore the package declaration.
      return false;
    }

    /**
     * Validate that the <code>resolvedBinding</code> Record the
     * <code>node</code> if it references a JRE type.
     * 
     * @param typeNode type node to inspect
     * @TODO: javadoc out of whack.
     */
    private void validateTypeReference(ASTNode node,
        ITypeBinding resolvedBinding) {
      assert (resolvedBinding != null);

      if (resolvedBinding.isPrimitive()) {
        // Primitive types cannot cause problems for GAE
        return;
      }

      try {
        ITypeBinding erasureBinding = resolvedBinding.getErasure();
        if (erasureBinding == null) {
          // Ignore unknown types (e.g. getErasure returns null when it
          // encounters: <T extends UnknownClass>)
          return;
        }

        IType type = javaProject.findType(erasureBinding.getQualifiedName());
        if (type == null) {
          // Ignore types that cannot be found
          return;
        }

        if (!type.isBinary()) {
          // Ignore source types
          return;
        }

        if (!isJreType(type)) {
          // Ignore types that are not part of the JRE.
          return;
        }

        if (!whiteList.contains(type.getFullyQualifiedName())) {
          // It is an error to use a JRE type that is not included in the
          // white-list.
          CategorizedProblem problem = AppEngineJavaProblem.createUnsupportedTypeError(
              node, erasureBinding.getQualifiedName());
          if (problem != null) {
            problems.add(problem);
          }
        }
      } catch (JavaModelException e) {
        // Log the error and continue
        AppEngineCorePluginLog.logError(e);
      }
    }
  }

  /**
   * Returns a {@link CategorizedProblem} for each use of a JRE type that is not
   * supported by the Google App Engine, and for each import from
   * com.google.appengine.repackaged. Package declarations are ignored.
   * 
   * @param compilationUnit {@link CompilationUnit} to check
   * @param javaProject project that owns the {@link CompilationUnit}
   * @return {@link CategorizedProblem} for each use of a JRE type that is not
   *         supported by the Google App Engine and for each import from
   *         com.google.appengine.repackaged
   */
  public static List<CategorizedProblem> check(
      CompilationUnit compilationUnit, IJavaProject javaProject) {
    List<CategorizedProblem> problems = new ArrayList<CategorizedProblem>();
    IProject project = javaProject.getProject();
    GaeProject gaeProject = GaeProject.create(project);
    GaeSdk sdk = gaeProject.getSdk();
    if (sdk != null && sdk.validate().isOK()) {
      GaeValidationVisitor visitor = new GaeValidationVisitor(gaeProject,
          problems);
      compilationUnit.accept(visitor);
    } else {
      // This will be caught by GAE project validation
    }
    return problems;
  }

  private static IPackageFragmentRoot getPackageFragmentRoot(
      IJavaElement element) {
    return (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
  }
}
