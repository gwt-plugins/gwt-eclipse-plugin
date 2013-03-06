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
package com.google.gwt.eclipse.core.runtime.tools;

import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer.Type;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand.UpdateType;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.sdk.GWTUpdateProjectSdkCommand;
import com.google.gwt.eclipse.core.sdk.GWTUpdateWebInfFolderCommand;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;

/**
 * FIXME: This file is only used by AbstractGWTPluginTestCase. We really need to
 * re-work our testing infrastructure, and get rid of this class.
 * 
 * TODO: Once we get rid of this class, we can remove the
 * UpdateWebInfFolderCommand argument from the AppEngineUpdateProjectSdkCommand
 * and GWTUpdateProjectSdkCommand constructors. Also, the
 * XUpdateWebInfFolderCommand classes should be turned into jobs themselves.
 */
public class NewProjectCreatorTool {

  // FIXME: Why are we throwing Exception?
  public static IJavaProject createProject(IProgressMonitor progressMonitor,
      GWTRuntime runtime, SdkClasspathContainer.Type containerType,
      String projectName, String packageName, String outDirPath)
      throws Exception {

    /*
     * The project name will be used as the entry point name. When invoking
     * GWT's WebAppCreator, the entry point name will be passed in as the last
     * component in the module name. The WebAppCreator will use the last
     * component of the module name as the generated Eclipse project name, which
     * gives us the desired effect.
     */
    final String entryPoint = projectName;
    final String qualifiedModuleName = packageName + "." + entryPoint;

    // Get a reference to the gwt-dev-<platform>.jar
    File gwtDevJar = runtime.getDevJar();

    // Need to set gwt.devjar property before calling projectCreator and
    // applicationCreator
    System.setProperty("gwt.devjar", gwtDevJar.toString());

    File outDir = new File(outDirPath);

    IPath startupUrl = new Path(entryPoint + ".html");

    try {
      if (!runtime.containsSCL()) {
        URLClassLoader cl = runtime.createClassLoader();
        Class<?> projectCreatorClass = cl.loadClass("com.google.gwt.user.tools.ProjectCreator");
        Method m = projectCreatorClass.getDeclaredMethod("createProject",
            String.class, String.class, File.class, boolean.class,
            boolean.class);
        m.setAccessible(true);
        m.invoke(null, projectName, null, outDir, false, false);
        progressMonitor.worked(1);

        Class<?> applicationCreatorClass = cl.loadClass("com.google.gwt.user.tools.ApplicationCreator");
        m = applicationCreatorClass.getDeclaredMethod("createApplication",
            String.class, File.class, String.class, boolean.class,
            boolean.class);
        m.setAccessible(true);

        m.invoke(null, packageName + "." + entryPoint, outDir, projectName,
            false, false);

        startupUrl = new Path(qualifiedModuleName).append(startupUrl);
      } else {
        WebAppProjectCreatorRunner.createProject(qualifiedModuleName,
            outDir.getAbsolutePath(), runtime, progressMonitor, null, "sample", "eclipse");
      }

    } catch (NoSuchMethodException e) {
      GWTPluginLog.logError(e);
      throw new SdkException(
          "Unable to invoke methods for project creation or application creation. using runtime "
              + runtime.getName() + " " + e);
    } catch (IllegalAccessException e) {
      throw new SdkException(
          "Unable to access methods for project creation or application creation. using runtime "
              + runtime.getName() + " " + e);
    } catch (InvocationTargetException e) {
      // Rethrow exception thrown by creator class
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof Exception) {
        GWTPluginLog.logError(cause);
      } else {
        GWTPluginLog.logError(e);
      }

      throw new SdkException(
          "Exception occured while attempting to create project and application "
              + runtime.getName() + " " + e);
    }
    progressMonitor.worked(1);

    // Import the newly-created project into the workspace
    IProject project = importProject(projectName, outDirPath);
    progressMonitor.worked(1);

    // Replace GWT jar classpath entries with the GWT runtime library
    IJavaProject javaProject = JavaCore.create(project);
    GWTRuntime oldSdk = GWTRuntime.findSdkFor(javaProject);

    GWTUpdateWebInfFolderCommand updateWebInfCommand = new GWTUpdateWebInfFolderCommand(
        javaProject, runtime);

    GWTUpdateProjectSdkCommand updateProjectSdkCommand = new GWTUpdateProjectSdkCommand(
        javaProject, oldSdk, runtime, containerType == Type.DEFAULT
            ? UpdateType.DEFAULT_CONTAINER : UpdateType.NAMED_CONTAINER,
        updateWebInfCommand);
    updateProjectSdkCommand.execute();

    progressMonitor.worked(1);

    // remove existing Java launch configuration
    removeLaunchConfig(entryPoint);

    progressMonitor.done();

    return javaProject;
  }

  private static IProject importProject(String projectName, String directory)
      throws Exception {

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IPath path = new Path(directory).append(IProjectDescription.DESCRIPTION_FILE_NAME);

    try {
      IProjectDescription projectFile = workspace.loadProjectDescription(path);
      IProjectDescription projectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(
          projectName);
      projectDescription.setLocation(path);

      IProject project = workspace.getRoot().getProject(projectName);
      project.create(projectFile, null);
      project.open(null);

      GWTNature.addNatureToProject(project);

      return project;

    } catch (CoreException e) {
      GWTPluginLog.logError(e);
      throw new Exception("Could not import new GWT project");
    }
  }

  private static void removeLaunchConfig(String launchConfigName)
      throws Exception {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    try {
      ILaunchConfiguration[] launchConfigs = manager.getLaunchConfigurations();
      for (ILaunchConfiguration launchConfig : launchConfigs) {
        if (launchConfig.getName().equals(launchConfigName)) {
          launchConfig.delete();
          break;
        }
      }

    } catch (CoreException e) {
      GWTPluginLog.logError(e);
      throw new Exception("Could not remove existing Java launch configuration");
    }
  }

}
