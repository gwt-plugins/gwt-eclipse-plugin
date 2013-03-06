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

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.launch.ILaunchArgumentsContainer;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.UpdateLaunchConfigurationDialogBatcher;
import com.google.gdt.eclipse.platform.debug.ui.ArgumentsTab;
import com.google.gdt.eclipse.platform.debug.ui.VMArgumentsBlock;
import com.google.gdt.eclipse.platform.debug.ui.WorkingDirectoryBlock;
import com.google.gdt.eclipse.suite.launch.WebAppLaunchUtil;
import com.google.gdt.eclipse.suite.launch.processors.LaunchConfigurationUpdater;
import com.google.gdt.eclipse.suite.launch.processors.WarArgumentProcessor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.JavaCore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Arguments tab for Web App projects.
 */
public class WebAppArgumentsTab extends ArgumentsTab implements
    ILaunchArgumentsContainer, UpdateLaunchConfigurationDialogBatcher.Listener {

  private final Set<ArgumentsListener> programArgsListeners = new HashSet<ArgumentsListener>();

  private boolean blockUpdateLaunchConfigurationDialog;

  private final UpdateLaunchConfigurationDialogBatcher updateLaunchConfigurationDialogBatcher = new UpdateLaunchConfigurationDialogBatcher(
      this);

  @Override
  public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
    // JavaArgumentsTab does not call through to AbstractLCTab, which normally
    // does this. We need this to update the arguments text boxes from the
    // launch config.
    initializeFrom(workingCopy);

    super.activated(workingCopy);
  }

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

    List<String> programArgs = getProgramArgs();
    for (ArgumentsListener listener : programArgsListeners) {
      try {
        listener.persistFromArguments(programArgs, configuration);
      } catch (Exception e) {
        CorePluginLog.logError(e);
      }
    }

    // Run the WAR updater so it can record when the user manually-sets a WAR
    // dir
    WarArgumentProcessor warArgProcessor = new WarArgumentProcessor();
    warArgProcessor.setUserUpdate(true);
    LaunchConfigurationProcessorUtilities.updateViaProcessor(warArgProcessor,
        configuration);
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    blockUpdateLaunchConfigurationDialog = true;
    try {
      super.initializeFrom(configuration);
    } finally {
      blockUpdateLaunchConfigurationDialog = false;
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration config) {
    if (!super.isValid(config)) {
      return false;
    }

    setErrorMessage(null);

    try {
      IProject project = WebAppLaunchUtil.getProject(config);
      if (project == null) {
        return true;
      }

      String msg = new LaunchConfigurationUpdater(config,
          JavaCore.create(project)).validate();
      if (msg != null) {
        setErrorMessage(msg);
      }

    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }

    // Even though there may be an error message, we don't return false here,
    // so the user can still save the launch in case we're wrong.
    return true;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    if (!this.equals(getLaunchConfigurationDialog().getActiveTab())) {
      return;
    }

    doPerformApply(configuration);
  }

  public void registerProgramArgsListener(ArgumentsListener listener) {
    programArgsListeners.add(listener);
  }

  @Override
  protected VMArgumentsBlock createVMArgsBlock() {
    return new VMArgumentsBlock();
  }

  @Override
  protected WorkingDirectoryBlock createWorkingDirBlock() {
    return new WebAppWorkingDirectoryBlock();
  }

  @Override
  protected void updateLaunchConfigurationDialog() {
    if (!blockUpdateLaunchConfigurationDialog) {
      updateLaunchConfigurationDialogBatcher.updateLaunchConfigurationDialogCalled();
    }
  }

  private List<String> getProgramArgs() {
    String args = fPrgmArgumentsText.getText();
    return LaunchConfigurationProcessorUtilities.parseArgs(args);
  }
}