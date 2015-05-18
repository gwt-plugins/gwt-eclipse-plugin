/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.update.importer;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateWebInfFolderCommand;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;
import com.google.gwt.eclipse.core.sdk.GWTUpdateWebInfFolderCommand;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.datatransfer.ExternalProjectImportWizard;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;

/**
 * Launch the wizard to import GAE demo apps.
 */
@SuppressWarnings("restriction") // org.eclipse.jdt.internal.ui.JavaPlugin
public class OpenImportGaeDemoAppWizardToolbarAction extends Action {

  private static final IPath DEMO_PATH = new Path("demos");
  private final Shell shell = JavaPlugin.getActiveWorkbenchShell();

  @Override
  public void run() {
    SdkSet<GaeSdk> sdks = GaePreferences.getSdks();
    String maxVersion = null;
    GaeSdk latestGaeSdk = null;
    for (GaeSdk gaeSdk : sdks) {
      if (maxVersion == null) {
        maxVersion = gaeSdk.getVersion();
        latestGaeSdk = gaeSdk;
      } else if (SdkUtils.compareVersionStrings(maxVersion, gaeSdk.getVersion()) < 0) {
        maxVersion = gaeSdk.getVersion();
        latestGaeSdk = gaeSdk;
      }
    }
    IPath sdkPath = latestGaeSdk.getInstallationPath();
    String sdkDemosPath = sdkPath.append(DEMO_PATH).toString();
    ExternalProjectImportWizard wizard = new ExternalProjectImportWizard(sdkDemosPath);
    wizard.init(PlatformUI.getWorkbench(), null);
    WizardDialog dialog = new WizardDialog(shell, wizard);
    dialog.open();
    GdtExtPlugin.getAnalyticsPingManager().sendGaeSampleAppsPing();
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    for (IProject project : workspaceRoot.getProjects()) {
      try {
        if (project.isAccessible() && project.hasNature(GaeNature.NATURE_ID)) {
          IJavaProject javaProject = JavaCore.create(project);
          if (GaeProjectProperties.getFileNamesCopiedToWebInfLib(project).isEmpty()) {
            // The project has just been imported. Add the jars in war/WEB-INF/lib
            GaeSdk sdk = GaeSdk.findSdkFor(javaProject);
            (new AppEngineUpdateWebInfFolderCommand(javaProject, sdk)).execute();
            // Also add GWT jars if the project has GWT nature.
            if (GWTNature.isGWTProject(project.getProject())) {
              // Check that no GWT jars had been added just in case.
              if (GWTProjectProperties.getFileNamesCopiedToWebInfLib(project).isEmpty()) {
                Sdk gwtSdk = GWTPreferences.getSdkManager()
                    .findSdkForPath(GWTRuntimeContainer.CONTAINER_PATH);
                (new GWTUpdateWebInfFolderCommand(javaProject, gwtSdk)).execute();
              }
            }
          }
        }
      } catch (JavaModelException e) {
        CorePluginLog.logError(e);
      } catch (FileNotFoundException e) {
        CorePluginLog.logError(e);
      } catch (CoreException e) {
        CorePluginLog.logError(e);
      } catch (BackingStoreException e) {
        CorePluginLog.logError(e);
      }
    }
  }
}

