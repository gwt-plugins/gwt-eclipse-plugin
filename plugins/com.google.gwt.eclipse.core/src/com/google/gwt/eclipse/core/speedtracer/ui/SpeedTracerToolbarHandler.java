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

import com.google.gdt.eclipse.core.ActiveProjectFinder;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.SpeedTracerLaunchListener;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.util.List;

/**
 * Simple action to open the Speed Tracer dialog.
 */
public class SpeedTracerToolbarHandler extends AbstractHandler {

  /**
   * Interface for opening Speed Tracer dialogs other than the default one in
   * this package.
   */
  public interface AlternateSpeedTracerDialog {
    /**
     * Opens the alternative speed tracer dialog.
     * 
     * @return the dialog's return status, as defined in Window (Window.OK and
     *         Window.CANCEL)
     */
    int open(Shell shell, IProject project);
  }

  public Object execute(ExecutionEvent event) {

    if (!SpeedTracerLaunchListener.INSTANCE.isSpeedTracerRunning()) {

      ExtensionQuery<AlternateSpeedTracerDialog> extQuery = new ExtensionQuery<AlternateSpeedTracerDialog>(
          GWTPlugin.PLUGIN_ID, "alternateSpeedTracerDialog", "class");
      List<ExtensionQuery.Data<AlternateSpeedTracerDialog>> dialogs = extQuery.getData();

      for (ExtensionQuery.Data<AlternateSpeedTracerDialog> d : dialogs) {
        AlternateSpeedTracerDialog dialog = d.getExtensionPointData();
        dialog.open(Display.getDefault().getActiveShell(), ActiveProjectFinder.getInstance().getProject());
        return null; // take the first dialog
      }

      new SpeedTracerDialog(Display.getDefault().getActiveShell(), ActiveProjectFinder.getInstance().getProject()).open();

    } else {
      MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Speed Tracer",
          "Speed Tracer is already running. Please stop the current session through"
              + " the Console View before starting a new one.");
    }
    
    return null;
  }

}
