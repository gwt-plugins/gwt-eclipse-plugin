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

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateProjectSdkCommand;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateWebInfFolderCommand;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand.UpdateType;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;

/**
 * 
 */
@SuppressWarnings("restriction")
public class PluginTestUtils {
  public static GaeProject createGaeProject(String projectName)
      throws CoreException {

    IJavaProject javaProject = createProject(projectName);
    IProject project = javaProject.getProject();

    GaeNature.addNatureToProject(project);
    GaeProject gaeProject = GaeProject.create(project);

    try {
      WebAppUtilities.setDefaultWarSettings(project);

      UpdateWebInfFolderCommand webInfLibUpdateCommand = new AppEngineUpdateWebInfFolderCommand(
          javaProject, GaePreferences.getDefaultSdk());
      AppEngineUpdateProjectSdkCommand command = new AppEngineUpdateProjectSdkCommand(
          javaProject, null, GaePreferences.getDefaultSdk(),
          UpdateType.DEFAULT_CONTAINER, webInfLibUpdateCommand);
      command.execute();
    } catch (FileNotFoundException e) {
      throw new CoreException(new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    } catch (BackingStoreException e) {
      throw new CoreException(new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    }

    // FIXME: Need to refactor all of this; it is also present in
    // AddNatureToProjectAction.perform
    // setOutputLocationToWarDir(gaeProject);

    return gaeProject;
  }

  /**
   * Creates a Java project with the given name and the default JRE library for
   * its classpath.
   */
  public static IJavaProject createProject(String projectName)
      throws CoreException {
    return createProject(projectName,
        PreferenceConstants.getDefaultJRELibrary());
  }

  /**
   * Creates a Java project with the given name and the specified raw classpath.
   */
  public static IJavaProject createProject(String projectName,
      IClasspathEntry[] rawClasspaths) throws CoreException {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(projectName);
    if (project.exists()) {
      throw new IllegalStateException("Project " + projectName
          + " already exists in this workspace");
    }

    NullProgressMonitor monitor = new NullProgressMonitor();

    BuildPathsBlock.createProject(project, project.getLocationURI(), monitor);
    BuildPathsBlock.addJavaNature(project, monitor);

    IJavaProject javaProject = JavaCore.create(project);
    javaProject.setRawClasspath(rawClasspaths, monitor);

    return javaProject;
  }

  public static void delay(long waitTimeMillis) {
    Display display = Display.getCurrent();

    // If this is the UI thread, then process input.
    if (display != null) {

      /*
       * We set up a timer on the UI thread that fires after the desired wait
       * time. We do this because we want to make sure that the UI thread wakes
       * up from a display.sleep() call. We set a flag in the runnable so that
       * we can terminate the wait loop.
       */
      final boolean[] hasDeadlineTimerFiredPtr = {false};

      display.timerExec((int) waitTimeMillis, new Runnable() {
        public void run() {

          /*
           * We don't have to worry about putting a lock around the update/read
           * of this variable. It is only accessed by the UI thread, and there
           * is only one UI thread.
           */
          hasDeadlineTimerFiredPtr[0] = true;
        }
      });

      while (!hasDeadlineTimerFiredPtr[0]) {

        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }

      display.update();
    } else {
      try {
        // Otherwise, perform a simple sleep.
        Thread.sleep(waitTimeMillis);
      } catch (InterruptedException e) {
        // Ignored
      }
    }
  }

  public static void removeDefaultGaeSdk() {
    SdkSet<GaeSdk> sdkSet = GaePreferences.getSdks();
    sdkSet.remove(sdkSet.getDefault());
    GaePreferences.setSdks(sdkSet);
  }

  /**
   * Wait until all background tasks are complete.
   */
  public static void waitForIdle() {
    while (!Job.getJobManager().isIdle()) {
      delay(1000);
    }
  }
}
