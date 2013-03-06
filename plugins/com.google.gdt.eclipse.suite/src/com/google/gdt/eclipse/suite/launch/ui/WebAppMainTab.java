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
package com.google.gdt.eclipse.suite.launch.ui;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.launch.UpdateLaunchConfigurationDialogBatcher;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.launch.processors.LaunchConfigurationUpdater;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;

import java.text.MessageFormat;

/**
 * A launch configuration tab based off of JDT's main tab that provides
 * GPE-specific validation of the project and main class.
 */
@SuppressWarnings("restriction")
public class WebAppMainTab extends JavaMainTab implements
    UpdateLaunchConfigurationDialogBatcher.Listener {

  /**
   * See javadoc on
   * {@link com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor}
   * for information about why this is required.
   */
  private boolean blockUpdateLaunchConfigurationDialog;

  private final UpdateLaunchConfigurationDialogBatcher updateLaunchConfigurationDialogBatcher = new UpdateLaunchConfigurationDialogBatcher(
      this);

  public void callSuperUpdateLaunchConfigurationDialog() {
    super.updateLaunchConfigurationDialog();
  }

  @Override
  public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
    updateLaunchConfigurationDialogBatcher.deactivatedCalled(workingCopy);

    super.deactivated(workingCopy);
  }

  @Override
  public void dispose() {
    updateLaunchConfigurationDialogBatcher.disposeCalled();

    super.dispose();
  }

  public void doPerformApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);

    // Link the launch configuration to the project. This will cause the
    // launch config to be deleted automatically if the project is deleted.
    IProject project = getProjectNamed(getEnteredProjectName());
    if (project != null) {
      configuration.setMappedResources(new IResource[] {project});
    }

    IJavaProject javaProject = getJavaProject();
    if (javaProject != null && javaProject.exists()) {
      try {
        new LaunchConfigurationUpdater(configuration, javaProject).update();
      } catch (CoreException e) {
        CorePluginLog.logError(e,
            "Could not update arguments to reflect main tab changes");
      }
    }
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    blockUpdateLaunchConfigurationDialog = true;

    try {
      super.initializeFrom(config);
    } finally {
      blockUpdateLaunchConfigurationDialog = false;
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    setErrorMessage(null);
    setMessage(null);

    if (!super.isValid(launchConfig)
        || !isValidProject(getEnteredProjectName())) {
      return false;
    }

    return true;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    if (!this.equals(getLaunchConfigurationDialog().getActiveTab())) {
      return;
    }

    doPerformApply(configuration);
  }

  @Override
  protected void updateLaunchConfigurationDialog() {
    if (!blockUpdateLaunchConfigurationDialog) {
      updateLaunchConfigurationDialogBatcher.updateLaunchConfigurationDialogCalled();
    }
  }

  private String getEnteredProjectName() {
    return fProjText.getText().trim();
  }

  /**
   * If the named project exists, return it as an IProject. Otherwise, return
   * null.
   */
  private IProject getProjectNamed(String projectName) {
    if (projectName.length() == 0) {
      return null;
    }

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
        projectName);
    return project.exists() ? project : null;
  }

  /**
   * Returns <code>true</code> if the project exists and it uses GWT or GAE.
   */
  private boolean isValidProject(String projectName) {
    if (projectName.length() == 0) {
      setErrorMessage("Project was not specified");
      return false;
    }

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IStatus status = workspace.validateName(projectName, IResource.PROJECT);
    if (status.isOK()) {
      IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
          projectName);
      if (!project.exists()) {
        setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_20,
            projectName));
        return false;
      }

      if (!project.isOpen()) {
        setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_21,
            projectName));
        return false;
      }

      boolean isGwtOrGaeProject;
      try {
        isGwtOrGaeProject = NatureUtils.hasNature(project, GWTNature.NATURE_ID)
            || NatureUtils.hasNature(project, GaeNature.NATURE_ID);
      } catch (CoreException e) {
        GdtPlugin.getLogger().logError(e);
        isGwtOrGaeProject = false;
      }

      if (!isGwtOrGaeProject) {
        setErrorMessage("Project does not use GWT or GAE");
        return false;
      }
    } else {
      setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_19,
          status.getMessage()));
      return false;
    }

    return true;
  }

}
