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
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gdt.eclipse.suite.launch.WebAppLaunchUtil;
import com.google.gdt.eclipse.suite.propertytesters.LaunchTargetTester;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Launch shortcut for Web Applications.
 */
public class WebAppLaunchShortcut implements ILaunchShortcut {

  private static String calculateLaunchConfigName(
      String startupUrl, boolean isExternal, IResource resource) {
    String launchConfigName = "";
    if ("".equals(startupUrl)) {
      launchConfigName = resource.getProject().getName();
    } else {
      try {
        URL url = new URL(startupUrl);
        String path = url.getPath();
        String hostPageName = new Path(path).lastSegment();
        if (hostPageName != null) {
          launchConfigName = hostPageName;
        } else {
          // No path was specified, use the host name
          launchConfigName = url.getHost();
        }
        
      } catch (MalformedURLException e) {
        // If the startup URL is not a true URL, just a path plus a file name,
        // then use the file name which is the last segment of the path.
        // Eclipse doesn't correctly handle slashes in launch config names, 
        // which occur in legacy GWT cases.
        launchConfigName = new Path(startupUrl).lastSegment();
      }
    }

    if (isExternal) {
      return launchConfigName + "-external";
    }

    return launchConfigName;
  }

  public void launch(IEditorPart editor, String mode) {
    IResource resource = ResourceUtils.getEditorInput(editor);

    if (resource != null) {
      launch(resource, mode);
    }
  }

  public void launch(ISelection selection, String mode) {
    IResource resource = ResourceUtils.getSelectionResource(selection);

    if (resource != null) {
      launch(resource, mode);
    }
  }

  /**
   * Finds and returns an <b>existing</b> configuration to re-launch for the
   * given URL, or <code>null</code> if there is no existing configuration.
   *
   * @return a configuration to use for launching the given type or <code>null
   *         </code> if none
   * @throws CoreException
   */
  protected ILaunchConfiguration findLaunchConfiguration(
      IResource resource, String startupUrl, boolean isExternal) throws CoreException {

    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType typeid =
        launchManager.getLaunchConfigurationType(WebAppLaunchConfiguration.TYPE_ID);
    ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(typeid);

    return searchMatchingUrlAndProject(startupUrl, resource.getProject(), isExternal, configs);
  }

  /**
   * Given a resource, infer the startup URL that the resource points at, then
   * look for an existing launch configuration that points at this URL. If none
   * exists, we'll create a new one.
   *
   * @return the found or newly created launch configuration
   * @throws CoreException
   * @throws OperationCanceledException
   */
  protected ILaunchConfiguration findOrCreateLaunchConfiguration(
      IResource resource, String startupUrl, boolean isExternal)
      throws CoreException, OperationCanceledException {
    ILaunchConfiguration config = findLaunchConfiguration(resource, startupUrl, isExternal);

    if (config == null) {
      config = createNewLaunchConfiguration(resource, startupUrl, isExternal);
    }

    return config;
  }

  /**
   * Given a specific resource, launch for that resource. This will involve
   * either finding an existing launch configuration, or making a new one.
   */
  protected void launch(IResource resource, String mode) {

    // assert that by the time we're in here, the PropertyTester agrees that we
    // should be here.
    assert (new LaunchTargetTester().test(resource, null, new Object[0], null));

    // Resolve to the actual resource (if it is linked)
    resource = ResourceUtils.resolveTargetResource(resource);

    try {
      String startupUrl = WebAppLaunchUtil.determineStartupURL(resource, false);
      if (startupUrl != null) {
        ILaunchConfiguration config = findOrCreateLaunchConfiguration(resource, startupUrl, false);

        assert (config != null);

        DebugUITools.launch(config, mode);
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    } catch (OperationCanceledException e) {
      // Abort launch
    }
  }

  private ILaunchConfiguration createNewLaunchConfiguration(
      IResource resource, String startupUrl, boolean isExternal)
      throws CoreException, OperationCanceledException {
    String initialName = calculateLaunchConfigName(startupUrl, isExternal, resource);
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    String launchConfigName = manager.generateUniqueLaunchConfigurationNameFrom(initialName);

    IProject project = resource.getProject();
    ILaunchConfigurationWorkingCopy wc = WebAppLaunchUtil.createLaunchConfigWorkingCopy(
        launchConfigName, project, startupUrl, isExternal);

    ILaunchConfiguration toReturn = wc.doSave();

    return toReturn;
  }

  private Shell getShell() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

  private ILaunchConfiguration searchMatchingUrlAndProject(
      String startupUrl, IProject project, boolean isExternal, ILaunchConfiguration[] configs)
      throws CoreException {
    List<ILaunchConfiguration> candidates = new ArrayList<ILaunchConfiguration>();

    for (ILaunchConfiguration config : configs) {
      String configUrl = GWTLaunchConfiguration.getStartupUrl(config);

      if (configUrl.equals(startupUrl)
          && LaunchConfigurationUtilities.getProjectName(config).equals(project.getName())
          && WebAppLaunchConfiguration.getRunServer(config) == !isExternal) {
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
}
