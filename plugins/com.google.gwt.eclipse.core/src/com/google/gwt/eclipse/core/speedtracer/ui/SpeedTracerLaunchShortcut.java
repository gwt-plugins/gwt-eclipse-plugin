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
package com.google.gwt.eclipse.core.speedtracer.ui;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerBrowserUtilities;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerLaunchConfiguration;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

/**
 * Launches a Speed Tracer launch configuration relating to the selected/editing
 * resource.
 */
public class SpeedTracerLaunchShortcut implements ILaunchShortcut {

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

  void launch(IResource resource, String mode) {
    ILaunchConfiguration config;
    try {
      config = SpeedTracerLaunchConfiguration.findOrCreateLaunchConfiguration(resource);

      // Make sure this launch config's specific browser exists
      if (!SpeedTracerBrowserUtilities.ensureChromeConfiguredOrPrompt(
          SWTUtilities.getShell(), config)) {
        return;
      }

    } catch (CoreException e) {
      Shell shell = Display.getDefault().getActiveShell();
      if (shell != null) {
        ErrorDialog.openError(shell, "Speed Tracer",
            "Could not launch Speed Tracer, see log for details",
            StatusUtilities.newErrorStatus(e, GWTPlugin.PLUGIN_ID));
      }

      GWTPluginLog.logError(e, "Could not launch Speed Tracer");

      return;
    } catch (OperationCanceledException e) {
      // User canceled one of the dialogs, cancel the launch
      return;
    }

    DebugUITools.launch(config, mode);
  }

}
