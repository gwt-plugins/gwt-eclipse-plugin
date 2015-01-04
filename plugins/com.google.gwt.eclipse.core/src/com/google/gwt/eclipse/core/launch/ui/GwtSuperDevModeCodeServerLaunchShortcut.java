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
package com.google.gwt.eclipse.core.launch.ui;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gwt.eclipse.core.launch.GwtSuperDevModeLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.util.GwtSuperDevModeCodeServerLaunchUtil;
import com.google.gwt.eclipse.core.propertytesters.GwtLaunchTargetTester;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Launch shortcut for SDM Code server.
 *
 * Set SDM on by default.
 */
public class GwtSuperDevModeCodeServerLaunchShortcut implements ILaunchShortcut {

  private static String calculateLaunchConfigName(IResource resource) {
    return resource.getProject().getName();
  }

  @Override
  public void launch(IEditorPart editor, String mode) {
    IResource resource = ResourceUtils.getEditorInput(editor);
    if (resource != null) {
      launch(resource, mode);
    }
  }

  @Override
  public void launch(ISelection selection, String mode) {
    IResource resource = ResourceUtils.getSelectionResource(selection);
    if (resource != null) {
      launch(resource, mode);
    }
  }

  /**
   * Finds and returns an <b>existing</b> configuration to re-launch for the given URL, or
   * <code>null</code> if there is no existing configuration.
   *
   * @return a configuration to use for launching the given type or <code>null
   *         </code> if none
   * @throws CoreException
   */
  protected ILaunchConfiguration findLaunchConfiguration(IResource resource)
      throws CoreException {

    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType typeid =
        launchManager.getLaunchConfigurationType(GwtSuperDevModeLaunchConfiguration.TYPE_ID);
    ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(typeid);

    return searchMatchingUrlAndProject(resource.getProject(), configs);
  }

  /**
   * Given a resource, infer the startup URL that the resource points at, then look for an existing
   * launch configuration that points at this URL. If none exists, we'll create a new one.
   *
   * @return the found or newly created launch configuration
   * @throws CoreException
   * @throws OperationCanceledException
   */
  protected ILaunchConfiguration findOrCreateLaunchConfiguration(IResource resource) throws CoreException, OperationCanceledException {
    ILaunchConfiguration config = findLaunchConfiguration(resource);

    if (config == null) {
      config = createNewLaunchConfiguration(resource);
    }

    return config;
  }

  /**
   * Given a specific resource, launch for that resource. This will involve either finding an
   * existing launch configuration, or making a new one.
   */
  protected void launch(IResource resource, String mode) {
    // assert that by the time we're in here, the PropertyTester agrees that we
    // should be here.
    assert (new GwtLaunchTargetTester().test(resource, null, new Object[0], null));

    // Resolve to the actual resource (if it is linked)
    resource = ResourceUtils.resolveTargetResource(resource);

    try {
      String startupUrl = GwtSuperDevModeCodeServerLaunchUtil.determineStartupURL(resource, false);
      if (startupUrl != null) {
        ILaunchConfiguration config = findOrCreateLaunchConfiguration(resource);

        assert (config != null);

        DebugUITools.launch(config, mode);
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    } catch (OperationCanceledException e) {
      // Abort launch
    }
  }

  protected ILaunchConfiguration searchMatchingUrlAndProject(IProject project,
      ILaunchConfiguration[] configs) throws CoreException {
    List<ILaunchConfiguration> candidates = new ArrayList<ILaunchConfiguration>();

    for (ILaunchConfiguration config : configs) {
      if (LaunchConfigurationUtilities.getProjectName(config).equals(project.getName())) {
        candidates.add(config);
      }
    }

    if (candidates.isEmpty()) {
      return null;
    } else if (candidates.size() == 1) {
      return candidates.get(0);
    } else {
      return LaunchConfigurationUtilities.chooseConfiguration(candidates, getShell());
    }
  }

  /**
   * Create a new launch configuration.
   */
  private ILaunchConfiguration createNewLaunchConfiguration(IResource resource)
      throws CoreException, OperationCanceledException {
    String initialName = calculateLaunchConfigName(resource);

    IProject project = resource.getProject();
    ILaunchConfigurationWorkingCopy wc =
        GwtSuperDevModeCodeServerLaunchUtil.createLaunchConfigWorkingCopy(initialName, project);

    // save the new launch configuration
    ILaunchConfiguration toReturn = wc.doSave();

    return toReturn;
  }

  private Shell getShell() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

}
