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
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMarkerResolution;
import org.osgi.service.prefs.BackingStoreException;

import java.util.List;

/**
 * Resolution to the problem of a file on the build classpath which is not
 * present on the server classpath. The resolution will add the build classpath
 * jar to the list of jars that the validator should not warn about.
 * 
 * TODO: Consider extending WorkbenchMarkerResolution so that multiple
 * quick-fixes can be applied at the same time.
 */
public class AddToWarningExclusionListMarkerResolution implements
    IMarkerResolution {

  private IPath buildClasspathFilePath;

  public AddToWarningExclusionListMarkerResolution(IPath buildClasspathFilePath) {
    this.buildClasspathFilePath = buildClasspathFilePath;
  }

  public String getLabel() {
    return "Do not warn me about " + buildClasspathFilePath.lastSegment()
        + " not being present on the server's classpath.";
  }

  public void run(IMarker marker) {
    IProject project = marker.getResource().getProject();

    try {
      WebAppUtilities.verifyHasManagedWarOut(project);

      if (!buildClasspathFilePath.toFile().exists()) {
        MessageDialog.openError(
            CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
            "Error While Attempting to Update Warnings List", "Unable to add "
                + buildClasspathFilePath.toString()
                + " to the warnings exclusion list. The file does not exist.");
        return;
      }

      List<IPath> excludedJars = WebAppProjectProperties.getJarsExcludedFromWebInfLib(project);
      excludedJars.add(buildClasspathFilePath);

      WebAppProjectProperties.setJarsExcludedFromWebInfLib(project,
          excludedJars);
    } catch (BackingStoreException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Update Warnings List", "Unable to add "
              + buildClasspathFilePath.toString()
              + " to the warnings exclusion list."
              + " See the Error Log for more details.");
    } catch (CoreException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Update Warnings List", "Unable to add "
              + buildClasspathFilePath.toString()
              + " to the warnings exclusion list."
              + " See the Error Log for more details.");
    }
  }

}
