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
package com.google.gdt.eclipse.appengine.rpc.validators;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;
import com.google.gdt.eclipse.appengine.rpc.util.RequestFactoryUtils;
import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.compiler.ReconcileContext;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Integrates into the JDT reconcile and build process to flag Request Factory
 * specific errors.
 */
public class JavaCompilationParticipant extends CompilationParticipant {

  private static final CategorizedProblem[] EMPTY_PROBLEMS = new CategorizedProblem[0];

  private static IJavaProject javaProject;
  private static List<IType> projectEntities = new ArrayList<IType>();
  private static List<IType> proxies = new ArrayList<IType>();
  private static HashMap<String, IType> requestMap = new HashMap<String, IType>();
  private static String cuName;

  @Override
  public void buildStarting(final BuildContext[] files, boolean isBatch) {

    for (BuildContext context : files) {
      IFile file = context.getFile();

      try {
        ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file);
        ASTNode ast = null;
        IType requestContext = null;

        // find the requestfactory classes, once per project
        if (javaProject == null
            || javaProject.getProject() != cu.getJavaProject().getProject()) {

          javaProject = cu.getJavaProject();
          projectEntities.clear();
          proxies.clear();
          requestMap.clear();
          RequestFactoryUtils.findAllTypes(javaProject, projectEntities,
              proxies, requestMap);
        }

        // if there is no requestfactory implementation, no need to validate
        String name = cu.findPrimaryType().getFullyQualifiedName();
        if (requestMap.containsKey(name)) {
          requestContext = requestMap.get(name);
        }

        if (requestContext != null) {
          try {
            /*
             * Generally, the compilation unit will be consistent (the Java
             * Model matches the .java file on disk). However, in certain cases,
             * such as when the user undos a member rename refactoring, the two
             * are out of sync when the build starts. In these cases, we have to
             * explicitly reconcile the compilation unit with its underlying
             * resource and use the AST we get back for validation.
             */
            if (!cu.isConsistent()) {
              ast = cu.reconcile(AST.JLS3, true, null, null);
              assert (cu.isConsistent());
            } else {
              // Have JDT parse the compilation unit
              ASTParser parser = ASTParser.newParser(AST.JLS3);
              parser.setResolveBindings(true);

              parser.setSource(cu);
              ast = parser.createAST(null);
            }
          } catch (JavaModelException e) {
            AppEngineRPCPlugin.log(e);
            continue;
          }
          // Validate the Java AST and record any problems we find
          RequestFactoryValidator validator = new RequestFactoryValidator(
              requestContext, projectEntities, proxies);
          List<? extends CategorizedProblem> problems = validator.validate(ast);
          context.recordNewProblems(problems.toArray(EMPTY_PROBLEMS));
        }
      } catch (OperationCanceledException e) {
        // Thrown by Eclipse to abort long-running processes
        throw e;
      } catch (Exception e) {
        AppEngineRPCPlugin.getLogger().logError(e,
            "Unexpected error while validating {0}", file.getName());
      }
    }
  }

  @Override
  public boolean isActive(IJavaProject project) {
    try {
      boolean active = project.exists()
          && (project.getProject().hasNature(GaeNature.NATURE_ID) || project.getProject().hasNature(
              GWTNature.NATURE_ID));
      return active;
    } catch (CoreException e) {
      AppEngineRPCPlugin.log(e);
      return false;
    }
  }

  @Override
  public void reconcile(ReconcileContext context) {

    ICompilationUnit cu = context.getWorkingCopy();

    IType requestContext = null;

    try {
      if (!cu.isConsistent()) {
        cu.reconcile(AST.JLS3, true, null, null);
        assert (cu.isConsistent());
      }

      // find the requestfactory classes, once per project
      if (javaProject == null
          || javaProject.getProject() != cu.getJavaProject().getProject()) {

        javaProject = cu.getJavaProject();
        projectEntities.clear();
        proxies.clear();
        requestMap.clear();
        RequestFactoryUtils.findAllTypes(javaProject, projectEntities, proxies,
            requestMap);
      }

      if (cu.findPrimaryType() == null) {
        return;
      }
      if (cuName == null
          || !cuName.equals(cu.findPrimaryType().getFullyQualifiedName())) {
        cuName = cu.findPrimaryType().getFullyQualifiedName();

        if (requestMap.containsKey(cuName)) {
          requestContext = requestMap.get(cuName);
        } else {
          requestContext = null;
        }
        // if there is no requestfactory implementation, no need to validate
      }
      if (requestContext != null) {
        CompilationUnit ast = context.getAST3();
        ArrayList<CategorizedProblem> finalProblemSet = new ArrayList<CategorizedProblem>();
        CategorizedProblem[] currentProblems = context.getProblems(GWTJavaProblem.MARKER_ID);
        if (currentProblems != null) {
          finalProblemSet.addAll(Arrays.asList(currentProblems));
        }
        RequestFactoryValidator validator = new RequestFactoryValidator(
            requestContext, projectEntities, proxies);
        List<CategorizedProblem> reqFactoryProblems = validator.validate(ast);
        finalProblemSet.addAll(reqFactoryProblems);
        context.putProblems(
            GWTJavaProblem.MARKER_ID,
            (finalProblemSet.size() > 0
                ? finalProblemSet.toArray(EMPTY_PROBLEMS) : null));
      }
    } catch (JavaModelException e) {
      AppEngineRPCPlugin.log(e);
    }
  }

}
