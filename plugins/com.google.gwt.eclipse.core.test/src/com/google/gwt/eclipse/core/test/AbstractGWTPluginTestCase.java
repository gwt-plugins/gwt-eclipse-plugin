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
package com.google.gwt.eclipse.core.test;

import com.google.gdt.eclipse.core.TestUtilities;
import com.google.gdt.eclipse.core.jobs.JobsUtilities;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;
import com.google.gwt.eclipse.core.runtime.GwtRuntimeTestUtilities;
import com.google.gwt.eclipse.core.runtime.tools.NewProjectCreatorTool;
import com.google.gwt.eclipse.core.util.Util;

import junit.framework.TestCase;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

/**
 * Represents a test case for the GWT plug-in.
 * <p>
 * Using this as a base class is discouraged, try achieving the same goals with
 * the various helper methods available (for example, classes like
 * {@link com.google.gdt.eclipse.core.JavaProjectUtilities} provide similar functionality.)
 */
@SuppressWarnings("restriction")
public abstract class AbstractGWTPluginTestCase extends TestCase {

  /**
   * A class added to the test project for a particular test case.
   */
  public static class TestClass {

    private final String contents;

    private ICompilationUnit cu;

    private final String typeName;

    public TestClass(String code, String typeName) {
      this.contents = code;

      if (Signature.getQualifier(typeName).length() == 0) {
        typeName = TEST_PROJECT_SRC_PACKAGE + "." + typeName;
      }

      this.typeName = typeName;
    }

    public TestClass(String[] contents, String typeName) {
      this(createString(contents), typeName);
    }

    public void addToTestProject() throws Exception {
      IProject testProject = Util.getWorkspaceRoot().getProject(
          TEST_PROJECT_NAME);
      if (!testProject.exists()) {
        throw new Exception("The test project does not exist");
      }

      IJavaProject javaProject = JavaCore.create(testProject);

      IPath srcPath = new Path("/" + TEST_PROJECT_NAME + "/src");
      IPackageFragmentRoot pckgRoot = javaProject.findPackageFragmentRoot(srcPath);

      String packageName = Signature.getQualifier(typeName);
      String cuName = Signature.getSimpleName(typeName) + ".java";

      // If the package fragment already exists, this call does nothing
      IPackageFragment pckg = pckgRoot.createPackageFragment(packageName,
          false, null);

      cu = pckg.createCompilationUnit(cuName, contents, true, null);
      JobsUtilities.waitForIdle();
    }

    public ICompilationUnit getCompilationUnit() {
      return cu;
    }

    public String getContents() {
      return contents;
    }

    public String getTypeName() {
      return typeName;
    }
  }

  public static final String TEST_PROJECT_ENTRY_POINT = "Hello";

  public static final String TEST_PROJECT_MODULE_PACKAGE = "com.hello";

  public static final String TEST_PROJECT_NAME = "Hello";

  public static final String TEST_PROJECT_SRC_PACKAGE = "com.hello.client";

  private static final TestClass[] NO_TEST_CLASSES = new TestClass[0];

  private static int uniqueId = 0;

  public static String createString(String[] lines) {
    return Util.join(Arrays.asList(lines), System.getProperty("line.separator"));
  }

  public static String getUniqueFilePrefix() {
    return "Test" + (uniqueId++);
  }

  private static File createFile(IPath workspaceRelativeAbsPathOfFile,
      String contents) throws CoreException {
    // TODO: Convert to ResouceUtils.createFile
    Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
    File file = (File) workspace.newResource(workspaceRelativeAbsPathOfFile,
        IResource.FILE);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
        contents.getBytes());
    file.create(byteArrayInputStream, false, null);
    JobsUtilities.waitForIdle();
    return file;
  }

  public AbstractGWTPluginTestCase() {
    this(null);
  }

  public AbstractGWTPluginTestCase(String name) {
    super(name);
  }

  public File addFileToTestProjSrcPkg(String fileName, String[] contentArray)
      throws CoreException {
    IPath pathToFile = getTestProject().getPath().append("src").append(fileName);
    File file = createFile(pathToFile, createString(contentArray));
    return file;
  }

  protected TestClass[] getTestClasses() {
    return NO_TEST_CLASSES;
  }

  protected IJavaProject getTestProject() {
    IProject testProject = Util.getWorkspaceRoot().getProject(TEST_PROJECT_NAME);
    if (!testProject.exists()) {
      return null;
    }

    return JavaCore.create(testProject);
  }

  protected void log(String message) {
    // Do nothing by default; subclasses may override this to receive messages
  }

  protected void rebuildTestProject() throws CoreException {
    IProject project = getTestProject().getProject();
    log("Building project " + project.getName());
    project.build(IncrementalProjectBuilder.FULL_BUILD, null);
    JobsUtilities.waitForIdle();
    log("Build complete");
  }

  /**
   * Allows a test case to indicate that it requires that the GWT source
   * projects (gwt-user, gwt-dev-<platform>) be imported before its tests are
   * run. Once the GWT projects are imported into the test workspace, they will
   * remain during subsequent tests. However, unless those later tests also
   * require the GWT projects, they will be closed so they will not interfere
   * with the tests which do not need them.
   */
  protected boolean requiresGWTProjects() {
    return false;
  }

  protected boolean requiresTestProject() {
    return false;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    TestUtilities.setUp();

    if (requiresGWTProjects()) {
      importGWTProjects();
    } else {
      closeProject("gwt-user");
      closeProject("gwt-dev-" + Util.getPlatformName());
    }

    if (requiresTestProject()) {
      GwtRuntimeTestUtilities.addDefaultRuntime();
      createTestProject();
      addClassesToTestProject();
    }
  }

  private void addClassesToTestProject() throws Exception {
    TestClass[] classes = getTestClasses();

    // Now make sure all test classes are part of the project
    for (TestClass testClass : classes) {
      testClass.addToTestProject();
      log("Added " + testClass.getTypeName() + " to test project;");
    }

    rebuildTestProject();
  }

  private void closeProject(String projectName) throws CoreException {
    IProject project = Util.getWorkspaceRoot().getProject(projectName);
    if (project.exists() && project.isOpen()) {
      project.close(null);
      JobsUtilities.waitForIdle();
    }
  }

  private void createTestProject() throws Exception {
    // Delete the test project if it already exists
    IProject project = Util.getWorkspaceRoot().getProject(TEST_PROJECT_NAME);
    if (project.exists()) {
      project.delete(true, true, null);
      JobsUtilities.waitForIdle();
    }

    String workspaceDir = Util.getWorkspaceRoot().getLocation().toOSString();
    String outDir = workspaceDir + System.getProperty("file.separator")
        + TEST_PROJECT_NAME;

    // Get the default GWT runtime
    GWTRuntime runtime = GWTPreferences.getDefaultRuntime();
    if (runtime == null) {
      throw new Exception("No default GWT SDK");
    }
    IPath runtimePath = SdkClasspathContainer.computeQualifiedContainerPath(
        GWTRuntimeContainer.CONTAINER_ID, runtime);

    // Use the New GWT Project wizard to create a test project
    JobsUtilities.waitForIdle();
    log("Creating new GWT project " + TEST_PROJECT_NAME);
    IProgressMonitor monitor = new NullProgressMonitor();

    monitor.beginTask("Generating GWT project", 4);

    SdkManager<GWTRuntime> sdkManager = GWTPreferences.getSdkManager();
    GWTRuntime runtime1 = sdkManager.findSdkForPath(runtimePath);

    /*
     * FIXME: Use WebAppCreator here, or some portion of it (since WebAppCreator
     * lives in the GDT Plugin, we cannot reference it from this test plugin).
     * We need to get rid of NewProjectCreatorTool.
     */
    NewProjectCreatorTool.createProject(monitor, runtime1,
        SdkClasspathContainer.Type.NAMED, TEST_PROJECT_NAME,
        TEST_PROJECT_MODULE_PACKAGE, outDir);
    log("Created project " + TEST_PROJECT_NAME);
  }

  private void importGWTProjects() throws Exception {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    String platform = Util.getPlatformName();
    String gwtDevProjectName = "gwt-dev-" + platform;

    // Get the path to the Eclipse project files for GWT
    IPath gwtProjectsDir = workspace.getPathVariableManager().getValue(
        "GWT_ROOT").append("eclipse");

    // Import gwt-dev
    IPath gwtDevDir = gwtProjectsDir.append("dev").append(platform);
    importProject(gwtDevProjectName, gwtDevDir);

    // Import gwt-user
    IPath gwtUserDir = gwtProjectsDir.append("user");
    importProject("gwt-user", gwtUserDir);
  }

  private void importProject(String projectName, IPath directory)
      throws CoreException {
    IProject project = Util.getWorkspaceRoot().getProject(projectName);
    if (!project.exists()) {
      log("Importing project " + projectName);

      IPath path = directory.append(IProjectDescription.DESCRIPTION_FILE_NAME);
      IProjectDescription projectFile = ResourcesPlugin.getWorkspace().loadProjectDescription(
          path);
      IProjectDescription projectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(
          projectName);
      projectDescription.setLocation(path);

      project.create(projectFile, null);
    } else {
      log("Project " + projectName + " already exists");
    }

    project.open(null);
    JobsUtilities.waitForIdle();
  }

}
