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

import org.eclipse.core.resources.IProject;
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
 * Utility methods for manipulating Java projects.
 * 
 * TODO: Refactor the different java project creation code used by the test code
 * and WebAppProjectCreator into a single place.
 */
@SuppressWarnings("restriction")
public final class JavaProjectTestUtilities {
  private static final IClasspathEntry[] NO_ICLASSPATH_ENTRIES = new IClasspathEntry[0];

  /**
   * Adds the specified classpath to the Java project's existing classpath.
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
    return packageFragment.createCompilationUnit(name + ".java", source, false,
        monitor);
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
   * Creates a Java project with the specified name and raw classpath.
   * 
   * @param projectName
   * @param rawClasspaths
   * @return a new project
   * @throws CoreException
   */
  public static IJavaProject createProject(String projectName,
      IClasspathEntry[] rawClasspaths) throws CoreException {
    IProject project = ProjectTestUtilities.createProject(projectName);
    NullProgressMonitor monitor = new NullProgressMonitor();
    BuildPathsBlock.addJavaNature(project, monitor);

    IJavaProject javaProject = JavaCore.create(project);
    javaProject.setRawClasspath(rawClasspaths, monitor);
    javaProject.open(monitor);

    return javaProject;
  }

  private JavaProjectTestUtilities() {
  }
}
