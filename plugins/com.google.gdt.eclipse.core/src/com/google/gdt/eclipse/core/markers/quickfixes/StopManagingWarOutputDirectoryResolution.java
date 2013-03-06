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
package com.google.gdt.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMarkerResolution;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Disables management of the WAR directory as an output directory. This is
 * equivalent to unchecking the box under the WAR directory field in the Web
 * Application project property page.
 */
public class StopManagingWarOutputDirectoryResolution implements
    IMarkerResolution {

  private final IProject project;

  public StopManagingWarOutputDirectoryResolution(IProject project) {
    this.project = project;
  }

  public String getLabel() {
    return "Do not use the WAR directory for " + project.getName()
        + " launching and deploying (this disables management of WEB-INF).";
  }

  public void run(IMarker marker) {
    Exception error = null;

    try {
      WebAppUtilities.verifyHasManagedWarOut(project);
      WebAppProjectProperties.setWarSrcDirIsOutput(project, false);
    } catch (BackingStoreException e) {
      error = e;
    } catch (CoreException e) {
      error = e;
    }

    if (error != null) {
      CorePluginLog.logError(error);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Updating Project Properties",
          "Unable to update setting for using the WAR directory for launching and deploying."
              + " See the Error Log for more details.");
    }
  }

}
