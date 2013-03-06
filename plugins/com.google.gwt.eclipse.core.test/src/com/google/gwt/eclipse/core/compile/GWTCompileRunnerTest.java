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
package com.google.gwt.eclipse.core.compile;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tests the GWTCompileRunner.
 */
@SuppressWarnings("restriction")
public class GWTCompileRunnerTest extends TestCase {

  private static final String JAVA_PROJECT_A_NAME = "projectA";
  private static final String JAVA_PROJECT_B_NAME = "projectB";
  private static final String SECONDARY_SRC_DIR_NAME = "secondary-src-dir";
  private static final String SRC_DIR_NAME = "src";

  /**
   * Create a library entry and add it to the raw classpath.
   */
  private static void addAndCreateFolderLibraryEntry(IJavaProject javaProject,
      String folderName) throws CoreException, UnsupportedEncodingException {
    IFolder projLibFolder = javaProject.getProject().getFolder(folderName);
    ResourceUtils.createFolderStructure(javaProject.getProject(),
        projLibFolder.getProjectRelativePath());
    JavaProjectUtilities.addRawClassPathEntry(
        javaProject,
        JavaCore.newLibraryEntry(projLibFolder.getFullPath(), null, null,
            true /* exported */));
  }

  /**
   * Create a JAR library entry and add it to the raw classpath.
   * <p>
   * This ensures a file exist at the target library path, but no guarantees on
   * its contents or its validity as a JAR.
   */
  private static void addAndCreateJarLibraryEntry(IJavaProject javaProject,
      IPath projectRelativeLibraryPath) throws CoreException,
      UnsupportedEncodingException {
    IFile lib = javaProject.getProject().getFile(projectRelativeLibraryPath);
    // Create the parent dirs and a dummy file for the library
    ResourceUtils.createFolderStructure(javaProject.getProject(),
        projectRelativeLibraryPath.removeLastSegments(1));
    ResourceUtils.createFile(lib.getFullPath(), "");
    JavaProjectUtilities.addRawClassPathEntry(
        javaProject,
        JavaCore.newLibraryEntry(lib.getFullPath(), null, null,
            true /* exported */));
  }

  /**
   * Create a source entry (including the dir structure) and add it to the raw
   * classpath.
   * 
   * @param javaProject The Java project that receives the source entry
   * @param directoryName The source directory name
   * @param outputDirectoryName The optional output location of this source
   *          directory. Pass null for the default output location.
   */
  private static void addAndCreateSourceEntry(IJavaProject javaProject,
      String directoryName, String outputDirectoryName) throws CoreException {
    IFolder srcFolder = javaProject.getProject().getFolder(directoryName);
    ResourceUtils.createFolderStructure(javaProject.getProject(),
        srcFolder.getProjectRelativePath());
    
    IPath workspaceRelOutPath = null;
    if (outputDirectoryName != null) {
      // Ensure output directory exists
      IFolder outFolder = javaProject.getProject().getFolder(outputDirectoryName);
      ResourceUtils.createFolderStructure(javaProject.getProject(),
          outFolder.getProjectRelativePath());
      workspaceRelOutPath = outFolder.getFullPath();
    }
    
    JavaProjectUtilities.addRawClassPathEntry(javaProject,
        JavaCore.newSourceEntry(srcFolder.getFullPath(),
            ClasspathEntry.EXCLUDE_NONE, workspaceRelOutPath));
  }

  /**
   * Gets a File instance for the specified path.
   */
  private static File getFile(IProject project, String projectRelativePath) {
    return project.getLocation().append(projectRelativePath).toFile();
  }

  /**
   * Gets a list of File from a list of resolved IRuntimeClasspathEntry.
   * 
   */
  private static List<File> getListOfFiles(
      List<IRuntimeClasspathEntry> classpathEntries) {
    List<File> files = new ArrayList<File>();
    for (IRuntimeClasspathEntry classpathEntry : classpathEntries) {
      files.add(new File(classpathEntry.getLocation()));
    }

    return files;
  }

  /**
   * Gets a File pointing to the output of the given Java project.
   */
  private static File getOutputOfProject(IJavaProject javaProject)
      throws CoreException {
    return ResourceUtils.resolveToAbsoluteFileSystemPath(
        javaProject.getOutputLocation()).toFile();
  }
  
  /** A simple project with only the JRE in the classpath (by default). */
  private IJavaProject javaProjectA;

  /** A simple project with only the JRE in the classpath (by default). */
  private IJavaProject javaProjectB;

  /**
   * Tests computing the classpath for dependent projects.
   */
  public void testComputeClasspathForDependentProject() throws CoreException {
    addAndCreateSourceEntry(javaProjectA, SRC_DIR_NAME, null);
    addAndCreateSourceEntry(javaProjectB, SRC_DIR_NAME, null);

    // Make project A dependent on project B
    JavaProjectUtilities.addRawClassPathEntry(javaProjectA,
        JavaCore.newProjectEntry(javaProjectB.getProject().getFullPath(),
            true));

    // Get the computed classpath
    List<File> actualCp = getListOfFiles(
        GWTCompileRunner.computeClasspath(javaProjectA));

    // Ensure the paths and ordering are all the same
    List<File> expectedCp = new ArrayList<File>();

    // Source of project A
    expectedCp.add(getFile(javaProjectA.getProject(), SRC_DIR_NAME));

    // Source of project B
    expectedCp.add(getFile(javaProjectB.getProject(), SRC_DIR_NAME));

    // Output of project A
    expectedCp.add(getOutputOfProject(javaProjectA));

    // Output of project B
    expectedCp.add(getOutputOfProject(javaProjectB));

    assertEquals(expectedCp, actualCp);
  }

  /**
   * Tests that the computed classpath does not contain the JRE.
   */
  public void testComputeClasspathForJre() throws CoreException {
    // The raw classpath includes the JRE
    assertEquals(1, javaProjectA.getRawClasspath().length);

    // Get the computed classpath
    List<File> actualCp = getListOfFiles(
        GWTCompileRunner.computeClasspath(javaProjectA));

    // The GWT compiler classpath should not contain the JRE
    assertEquals(0, actualCp.size());
  }

  /**
   * Tests computing the classpath for libraries (both a folder library and a
   * JAR).
   */
  public void testComputeClasspathForLibrary() throws CoreException,
      UnsupportedEncodingException {
    final IPath projectRelativeJarPath = new Path("lib/test.jar");
    // Tests the CPE_LIBRARY for folders (that usually contain classes)
    final String folderName = "folder-library";

    addAndCreateJarLibraryEntry(javaProjectA, projectRelativeJarPath);
    addAndCreateFolderLibraryEntry(javaProjectA, folderName);

    // Get the computed classpath
    List<File> actualCp = getListOfFiles(
        GWTCompileRunner.computeClasspath(javaProjectA));

    // Ensure the paths and ordering are all the same
    List<File> expectedCp = new ArrayList<File>();

    // JAR
    expectedCp.add(getFile(javaProjectA.getProject(),
        projectRelativeJarPath.toOSString()));

    // Folder
    expectedCp.add(getFile(javaProjectA.getProject(), folderName));

    assertEquals(expectedCp, actualCp);
  }

  /**
   * Tests computing the classpath for a project with multiple source
   * directories.
   */
  public void testComputeClasspathForMultipleSources() throws CoreException {
    addAndCreateSourceEntry(javaProjectA, SRC_DIR_NAME, null);
    addAndCreateSourceEntry(javaProjectA, SECONDARY_SRC_DIR_NAME, null);

    // Get the computed classpath
    List<File> actualCp = getListOfFiles(
        GWTCompileRunner.computeClasspath(javaProjectA));

    // Ensure the paths and ordering are all the same
    List<File> expectedCp = new ArrayList<File>();

    // Check that it contains both source dirs
    expectedCp.add(getFile(javaProjectA.getProject(), SRC_DIR_NAME));
    expectedCp.add(getFile(javaProjectA.getProject(), SECONDARY_SRC_DIR_NAME));
    
    // Check that it contains the output dir of A
    expectedCp.add(getOutputOfProject(javaProjectA));

    assertEquals(expectedCp, actualCp);
  }

  /**
   * Tests computing the classpath for a project with a dependency project that
   * all have multiple source directories and each having a specific output
   * directory.
   */
  public void testComputeClasspathForProjectsWithMultipleSourcesAndSpecificOutputs() throws
      CoreException {
    final String sourceOutDirName = "srcOut";
    final String secondarySourceOutDirName = "secondarySrcOut";
    
    // Create source dirs and specific outputs for A
    addAndCreateSourceEntry(javaProjectA, SRC_DIR_NAME, sourceOutDirName);
    addAndCreateSourceEntry(javaProjectA, SECONDARY_SRC_DIR_NAME,
        secondarySourceOutDirName);

    // Create source dirs and specific outputs for B
    addAndCreateSourceEntry(javaProjectB, SRC_DIR_NAME, sourceOutDirName);
    addAndCreateSourceEntry(javaProjectB, SECONDARY_SRC_DIR_NAME,
        secondarySourceOutDirName);
    
    // Add A depends on B
    JavaProjectUtilities.addRawClassPathEntry(javaProjectA,
        JavaCore.newProjectEntry(javaProjectB.getProject().getFullPath(),
            true));

    // Get the computed classpath
    List<File> actualCp = getListOfFiles(
        GWTCompileRunner.computeClasspath(javaProjectA));

    // Ensure the paths and ordering are all the same
    List<File> expectedCp = new ArrayList<File>();

    // Check that it contains both source dirs for A
    expectedCp.add(getFile(javaProjectA.getProject(), SRC_DIR_NAME));
    expectedCp.add(getFile(javaProjectA.getProject(), SECONDARY_SRC_DIR_NAME));
    
    // Check that it contains both source dirs for B
    expectedCp.add(getFile(javaProjectB.getProject(), SRC_DIR_NAME));
    expectedCp.add(getFile(javaProjectB.getProject(), SECONDARY_SRC_DIR_NAME));
    
    // Check that it contains both output dirs for A
    IPath projPath = javaProjectA.getProject().getFullPath();
    expectedCp.add(ResourceUtils.resolveToAbsoluteFileSystemPath(
        projPath.append(sourceOutDirName)).toFile());
    expectedCp.add(ResourceUtils.resolveToAbsoluteFileSystemPath(
        projPath.append(secondarySourceOutDirName)).toFile());
    
    // Check that the default output directory for A is there
    expectedCp.add(getOutputOfProject(javaProjectA));

    // Check that it contains both output dirs for B
    IPath projBPath = javaProjectB.getProject().getFullPath();
    expectedCp.add(ResourceUtils.resolveToAbsoluteFileSystemPath(
        projBPath.append(sourceOutDirName)).toFile());
    expectedCp.add(ResourceUtils.resolveToAbsoluteFileSystemPath(
        projBPath.append(secondarySourceOutDirName)).toFile());
    
    // Check that the default output directory for B is there
    expectedCp.add(getOutputOfProject(javaProjectB));

    assertEquals(expectedCp, actualCp);
  }

  /**
   * Tests computing the classpath for a simple project (one source, one
   * output).
   */
  public void testComputeClasspathForSimpleProject() throws CoreException {
    addAndCreateSourceEntry(javaProjectA, SRC_DIR_NAME, null);

    // Get the computed classpath
    List<File> actualCp = getListOfFiles(
        GWTCompileRunner.computeClasspath(javaProjectA));

    // Ensure the paths and ordering are all the same
    List<File> expectedCp = new ArrayList<File>();

    // Check that it contains the source dir
    expectedCp.add(getFile(javaProjectA.getProject(), SRC_DIR_NAME));

    // Check that it contains the output dir
    expectedCp.add(getOutputOfProject(javaProjectA));
    
    assertEquals(expectedCp, actualCp);
  }

  /**
   * Tests variable support when computing classpaths. 
   */
  public void testComputeClasspathForVariables() throws CoreException,
      IOException {
    // Create the classpath variable
    Random rand = new Random();
    String varName = null;
    while (varName == null) {
      String curVarName = this.getName() + rand.nextInt();
      if (JavaCore.getClasspathVariable(curVarName) == null) {
        varName = curVarName;
      }
    }

    File systemTempFile = File.createTempFile(this.getName(), ".temp");
    JavaCore.setClasspathVariable(varName,
        Path.fromOSString(systemTempFile.getAbsolutePath()),
        new NullProgressMonitor());

    try {
      // Create a variable entry and add it to the raw classpath
      JavaProjectUtilities.addRawClassPathEntry(javaProjectA,
          JavaCore.newVariableEntry(new Path(varName), null, null, true));
    
      // Get the computed classpath
      List<File> actualCp = getListOfFiles(
          GWTCompileRunner.computeClasspath(javaProjectA));
    
      // Ensure the paths and ordering are all the same
      List<File> expectedCp = new ArrayList<File>();
      expectedCp.add(systemTempFile);

      assertEquals(expectedCp, actualCp);
    } finally {
      JavaCore.removeClasspathVariable(varName, new NullProgressMonitor());
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    javaProjectA = JavaProjectUtilities.createJavaProject(JAVA_PROJECT_A_NAME);
    GWTNature.addNatureToProject(javaProjectA.getProject());

    javaProjectB = JavaProjectUtilities.createJavaProject(JAVA_PROJECT_B_NAME);
    GWTNature.addNatureToProject(javaProjectB.getProject());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    ProjectUtilities.deleteProject(javaProjectA.getElementName());
    ProjectUtilities.deleteProject(javaProjectB.getElementName());
  }

}
