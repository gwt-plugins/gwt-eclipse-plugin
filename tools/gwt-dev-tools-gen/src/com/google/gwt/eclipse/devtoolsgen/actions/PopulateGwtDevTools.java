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
package com.google.gwt.eclipse.devtoolsgen.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Computes the Google Plugin for Eclipse's transitive dependencies on GWT.
 */
// Steps for generating gwt-dev-tools.jar
//
// 1. Run this plugin using an Eclipse Application launch configuration.
// 
// In the Eclipse you just launched:
// 2. Import the GWT source projects (gwt-dev, gwt-user) and the gwt-dev-tools
// project.
// 3. Set the GWT_TOOLS classpath variable appropriately.
// 4. Add import statements to Deps.java in gwt-dev-tools for all GWT classes
// directly referenced by the GWT plugin.
// 4a. If the new GWT classes have dependencies on new external JAR files, add
// them to the build path of the gwt-dev-tools project (use GWT_TOOLS variable).
// 5. Click the purple Eclipse button in the toolbar. Watch the Console of the
// development workbench as the plugin searches for dependencies.
// 6. When the job finishes, the gwt-dev-tools project should contain exactly
// the transitive set of compilation units needed by the GWT plugin.
// 
// 7. Create a staging directory for the new gwt-dev-tools JAR.
// 8. Copy the gwt-dev-tools bin directory (except Deps.class) into the staging
// directory.
// 9. Unjar the external GWT jars referenced by gwt-dev-tools project into the
// staging directory (make sure not to overwrite existing files).
// 10. Jar the staging directory: "jar -cf gwt-dev-tools.jar -C <staging-dir> ."
// 11. Copy the new gwt-dev-tool.jar to the GWT plugin's /libs directory, and
// then commit the updated gwt-dev-tools.jar and Deps.java.
// 
public class PopulateGwtDevTools implements IWorkbenchWindowActionDelegate {

  private class CopyDepsToGwtDevTools implements IWorkspaceRunnable {

    public void run(IProgressMonitor monitor) throws CoreException {
      IPackageFragmentRoot srcRoot = gwtDevTools.findPackageFragmentRoot(new Path(
          "/gwt-dev-tools/src/"));

      System.out.print("Copying files to gwt-dev-tools...");
      for (IFile dep : deps) {
        IPackageFragment srcPckg = (IPackageFragment) JavaCore.create(dep.getParent());
        IPackageFragment dstPckg = srcRoot.createPackageFragment(
            srcPckg.getElementName(), false, null);

        dep.copy(dstPckg.getPath().append(dep.getName()), false, null);
      }

      System.out.println("done");
    }
  }

  private class DependencyCollector extends ASTVisitor {

    private final int callGraphLevel;

    public DependencyCollector(int callGraphLevel) {
      this.callGraphLevel = callGraphLevel;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void endVisit(FieldDeclaration node) {
      if (!((CompilationUnit) node.getRoot()).getJavaElement().getElementName().equals(
          "Deps.java")) {
        return;
      }

      for (VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) node.fragments()) {
        if (fragment.getName().getIdentifier().equals("RESOURCES")) {
          ArrayInitializer array = (ArrayInitializer) fragment.getInitializer();
          for (StringLiteral arrayItem : (List<StringLiteral>) array.expressions()) {
            try {
              IFile resource = findResourceInGwtProjects(new Path(
                  arrayItem.getLiteralValue()));
              if (resource != null) {
                deps.add(resource);
                printDependency(resource);
              }
            } catch (JavaModelException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    @Override
    public void endVisit(SimpleName node) {
      IBinding binding = node.resolveBinding();
      if (binding == null) {
        // Ignore nodes for which there is no binding
        return;
      }

      if (binding.getKind() == IBinding.TYPE) {
        ITypeBinding typeBinding = (ITypeBinding) binding;

        try {
          if (typeBinding.isFromSource()) {
            String qualifiedTypeName = typeBinding.getQualifiedName();
            IType type = findTypeInGwtProjects(qualifiedTypeName);
            if (type != null) {
              ICompilationUnit cu = type.getCompilationUnit();
              IFile javaFile = (IFile) cu.getResource();

              // Ignore compilation units we've already seen
              if (deps.contains(javaFile)) {
                return;
              }

              deps.add(javaFile);
              printDependency(javaFile);

              // Find dependencies pulled in by this compilation unit
              findAllDependencies(cu, callGraphLevel + 1);
            }
          }
        } catch (JavaModelException e) {
          e.printStackTrace();
        }
      }
    }

    private void printDependency(IFile file) {
      for (int i = 0; i < callGraphLevel; i++) {
        System.out.print("  ");
      }
      System.out.println(file.getFullPath().toString());
    }
  }

  private static IFile findFileOnClasspath(IJavaProject javaProject,
      IPath classpathRelativePath) throws JavaModelException {
    for (IPackageFragmentRoot pckgFragmentRoot : javaProject.getPackageFragmentRoots()) {
      if (pckgFragmentRoot.isArchive()) {
        continue;
      }
      IPath filepath = pckgFragmentRoot.getPath().append(classpathRelativePath);
      IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(
          filepath);
      if (res instanceof IFile) {
        return (IFile) res;
      }
    }
    return null;
  }

  private static IJavaProject findJavaProject(String name) {
    return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(
        name));
  }

  private Set<IFile> deps = new HashSet<IFile>();

  private final IJavaProject gwtDev = findJavaProject("gwt-dev");

  private final IJavaProject gwtDevTools = findJavaProject("gwt-dev-tools");

  private final IJavaProject gwtUser = findJavaProject("gwt-user");

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

  public void run(IAction action) {
    try {
      if (gwtDev == null) {
        System.err.println("Must import the gwt-dev project");
        return;
      }

      if (gwtUser == null) {
        System.err.println("Must import the gwt-user project");
        return;
      }

      if (gwtDevTools == null) {
        System.err.println("Must import the gwt-dev-tools project");
        return;
      }

      System.out.println("Searching for dependecies in GWT source projects...");

      deps.clear();
      IType searchRoot = gwtDevTools.findType("Deps");

      // Find compilation units and resources we depend on
      findAllDependencies(searchRoot.getCompilationUnit(), 0);
      int totalDepsCount = deps.size();

      // Prune out files already in gwt-dev-tools. This eliminates most of the
      // bulk copy when you've already run this tool at least once.
      Iterator<IFile> iter = deps.iterator();
      while (iter.hasNext()) {
        IFile dep = iter.next();

        IJavaElement srcRoot = JavaCore.create(dep.getParent()).getAncestor(
            IJavaElement.PACKAGE_FRAGMENT_ROOT);
        IPath srcPathRelativePath = dep.getFullPath().removeFirstSegments(
            srcRoot.getPath().segmentCount());
        IFile existingFile = findFileOnClasspath(gwtDevTools,
            srcPathRelativePath);

        if (existingFile != null) {
          iter.remove();
        }
      }

      System.out.format(
          "Finished dependency search (%d files found; %d new)\n",
          totalDepsCount, deps.size());

      if (!deps.isEmpty()) {
        IWorkspaceRunnable op = new CopyDepsToGwtDevTools();
        ISchedulingRule lock = ResourcesPlugin.getWorkspace().getRoot();
        ResourcesPlugin.getWorkspace().run(op, lock, IWorkspace.AVOID_UPDATE,
            null);
      } else {
        System.out.println("Done");
      }
    } catch (CoreException e) {
      e.printStackTrace();
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
  }

  private void findAllDependencies(ICompilationUnit cu, int callGraphLevel) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setResolveBindings(true);
    parser.setSource(cu);
    CompilationUnit rootCu = (CompilationUnit) parser.createAST(null);
    rootCu.accept(new DependencyCollector(callGraphLevel));
  }

  private IFile findResourceInGwtProjects(IPath classpathRelativePath)
      throws JavaModelException {
    IFile res = findFileOnClasspath(gwtDev, classpathRelativePath);
    if (res != null) {
      return res;
    }
    return findFileOnClasspath(gwtUser, classpathRelativePath);
  }

  private IType findTypeInGwtProjects(String qualifiedTypeName)
      throws JavaModelException {
    IType type = gwtDev.findType(qualifiedTypeName);
    if (type != null) {
      return type;
    }

    return gwtUser.findType(qualifiedTypeName);
  }
}