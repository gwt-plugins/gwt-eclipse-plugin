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
package com.google.gdt.eclipse.core;

import com.google.gdt.eclipse.core.jobs.JobsUtilities;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.PreferenceConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods to assist in working with IJavaProjects.
 */
@SuppressWarnings("restriction")
public class JavaProjectUtilities {

  private static final IClasspathEntry[] NO_ICLASSPATH_ENTRIES = new IClasspathEntry[0];

  /**
   * Adds the specified classpath to the Java project's existing classpath.
   * <p>
   * To instantiate classpath entries, see the helper methods in JavaCore (e.g.
   * {@link JavaCore#newSourceEntry(org.eclipse.core.runtime.IPath)}).
   * 
   * @param javaProject project to update
   * @param rawClasspathEntry new raw classpath entry
   * @throws JavaModelException
   */
  public static void addRawClassPathEntry(IJavaProject javaProject,
      IClasspathEntry rawClasspathEntry) throws JavaModelException {
    IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
  
    IProgressMonitor monitor = new NullProgressMonitor();
    List<IClasspathEntry> newEntries = new ArrayList<IClasspathEntry>(
        Arrays.asList(oldEntries));
    newEntries.add(rawClasspathEntry);
    javaProject.setRawClasspath(newEntries.toArray(NO_ICLASSPATH_ENTRIES),
        monitor);
    try {
      ClasspathUtilities.waitUntilEntriesAreOnClasspath(javaProject, newEntries);
    } catch (InterruptedException e) {
      // Continue, with a note
      CorePluginLog.logWarning(e,
          "Interrupted while waiting to ensure classpath entries were added");
    }
  }
  
  /**
   * Creates an {@link ICompilationUnit} with the given fully qualified name and
   * code in the <code>javaProject</code>.
   * 
   * @param javaProject java project to host the new class
   * @param fullyQualifiedClassName fully qualified name for the class
   * @param source code for the classs
   * @return newly created {@link ICompilationUnit}
   * @throws JavaModelException
   */
  public static ICompilationUnit createCompilationUnit(
      IJavaProject javaProject, String fullyQualifiedClassName, String source)
      throws JavaModelException {
    IPackageFragmentRoot root = javaProject.findPackageFragmentRoot(javaProject.getPath());
    if (root == null) {
      addRawClassPathEntry(javaProject,
          JavaCore.newSourceEntry(javaProject.getPath()));
      root = javaProject.findPackageFragmentRoot(javaProject.getPath());
    }
  
    String qualifier = Signature.getQualifier(fullyQualifiedClassName);
    IProgressMonitor monitor = new NullProgressMonitor();
    IPackageFragment packageFragment = root.createPackageFragment(qualifier,
        true, monitor);
    String name = Signature.getSimpleName(fullyQualifiedClassName);
    ICompilationUnit cu = packageFragment.createCompilationUnit(name + ".java",
        source, false, monitor);
    JobsUtilities.waitForIdle();
    return cu;
  }

  /**
   * Creates a Java project using the default JRE library.
   * 
   * @param projectName
   * @return a new project
   * @throws CoreException
   */
  public static IJavaProject createJavaProject(String projectName)
      throws CoreException {
    return createProject(projectName,
        PreferenceConstants.getDefaultJRELibrary());
  }

  /**
   * a Finds a Java project with the given name.
   * 
   * @param projectName The name of the Java project
   * @return The Java project, or null if it cannot be found or if it does not
   *         have the Java nature
   */
  public static IJavaProject findJavaProject(String projectName) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(projectName);
    
    if (!project.exists()) {
      return null;
    }
    
    try {
      if (!NatureUtils.hasNature(project, JavaCore.NATURE_ID)) {
        return null;
      }
    } catch (CoreException e) {
      // Thrown if the project doesn't exist or is not open
      return null;
    }
    
    return JavaCore.create(project);
  }

  /**
   * Gets the transitive closure of required Java projects for the given Java
   * project (including the project itself). The ordering is depth-first
   * preordered (i.e. a Java project is earlier than its dependencies).
   * 
   * @param javaProject The Java project whose dependencies will be returned
   * @return An ordered set of the Java project and its transitively required
   *         projects
   * @throws JavaModelException
   */
  public static List<IJavaProject> getTransitivelyRequiredProjects(
      IJavaProject javaProject) throws JavaModelException {
    List<IJavaProject> requiredProjects = new ArrayList<IJavaProject>();
    fillTransitivelyRequiredProjects(javaProject, requiredProjects);
    return requiredProjects;
  }

  /**
   * Returns <code>true</code> if the given IJavaProject is not null, and
   * <code>javaProject.exists()</code> is true.
   */
  public static boolean isJavaProjectNonNullAndExists(IJavaProject javaProject) {
    return (javaProject != null && javaProject.exists());
  }

  /**
   * Creates a Java project with the specified name and raw classpath.
   * 
   * @param projectName
   * @param rawClasspaths
   * @return a new project
   * @throws CoreException
   */
  private static IJavaProject createProject(String projectName,
      IClasspathEntry[] rawClasspaths) throws CoreException {
    IProject project = ProjectUtilities.createProject(projectName);
    NullProgressMonitor monitor = new NullProgressMonitor();
    BuildPathsBlock.addJavaNature(project, monitor);
  
    IJavaProject javaProject = JavaCore.create(project);
    javaProject.setRawClasspath(rawClasspaths, monitor);
    javaProject.open(monitor);
  
    /*
     * Should only need a single waitForIdle() call, even though we make 3
     * separate calls to asynchronous JDT methods above. The reason is that each
     * operation creates a Java workspace job with the same scheduling rule, so
     * they should end up running in FIFO order, meaning that each operation
     * will have its prerequisites met before it begins execution.
     */
    JobsUtilities.waitForIdle();
  
    return javaProject;
  }

  private static void fillTransitivelyRequiredProjects(IJavaProject javaProject,
      List<? super IJavaProject> requiredProjects)
      throws JavaModelException {
    
    if (requiredProjects.contains(javaProject)) {
      return;
    }
    
    requiredProjects.add(javaProject);
    
    for (String projectName : javaProject.getRequiredProjectNames()) {
      IJavaProject curJavaProject = JavaProjectUtilities.findJavaProject(
          projectName);
      if (curJavaProject == null) {
        continue;
      }
      
      fillTransitivelyRequiredProjects(curJavaProject, requiredProjects);
    }
  }
  
}
