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
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMarkerResolution;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;

/**
 * Resolution for the problem of having SDK server classpath libraries
 * missing/differing from those in <WAR>/WEB-INF/lib. This resolution will cause
 * a refresh of the SDK server classpath libraries into <WAR>/WEB-INF/lib.
 * 
 * TODO: Consider extending WorkbenchMarkerResolution so that multiple
 * quick-fixes can be applied at the same time.
 */
public class SynchronizeSdkWebappClasspathMarkerResolution implements
    IMarkerResolution {

  private UpdateWebInfFolderCommand updateWebInfLibFolderCommand;

  /**
   * Constructs a new instance of this resolution.
   * 
   * @param updateWebInfLibFolderCommand Sdk-specific command to refresh the
   *          <WAR>/WEB-INF/lib folder of a project with the SDK server
   *          classpath jars
   */
  public SynchronizeSdkWebappClasspathMarkerResolution(
      UpdateWebInfFolderCommand updateWebInfLibFolderCommand) {
    this.updateWebInfLibFolderCommand = updateWebInfLibFolderCommand;
  }

  public String getLabel() {
    return "Synchronize <WAR>/WEB-INF/lib with SDK libraries";
  }

  public void run(IMarker marker) {
    try {
      updateWebInfLibFolderCommand.execute();
    } catch (FileNotFoundException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Synchronize Server Classpath",
          "Unable to find SDK file: " + e.getLocalizedMessage() + ".");
    } catch (CoreException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Synchronize Server Classpath",
          "Unable to copy the SDK's server classpath libraries to <WAR>/WEB-INF/lib. See the Error Log for more details.");
    } catch (BackingStoreException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Synchronize Server Classpath",
          "Unable to copy the SDK's server classpath libraries to <WAR>/WEB-INF/lib. See the Error Log for more details.");
    }
  }
}
