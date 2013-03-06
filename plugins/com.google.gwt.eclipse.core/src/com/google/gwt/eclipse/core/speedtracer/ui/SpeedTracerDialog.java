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

import com.google.gdt.eclipse.core.ui.AbstractTitleAreaDialog;
import com.google.gdt.eclipse.core.ui.ProjectSelectionBlock;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.resources.GWTImages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import java.util.Collections;

/**
 * Dialog to launch a Speed Tracer session.
 */
public class SpeedTracerDialog extends AbstractTitleAreaDialog {

  protected final ProjectSelectionBlock projectSelectionBlock;

  public SpeedTracerDialog(Shell parentShell, IProject project) {
    super(parentShell, "Speed Tracer", "Profile Using Speed Tracer", "",
        GWTPlugin.getDefault().getImage(GWTImages.SPEED_TRACER_LARGE),
        "Profile");

    projectSelectionBlock = new ProjectSelectionBlock(project,
        Collections.singletonMap(GWTNature.NATURE_ID,
            "The project must be a GWT project"),
        new ProjectSelectionBlock.Listener() {
          public void projectSelected(IProject project, IStatus status) {
            validate();
            updateControls();
          }
        });
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    parent = (Composite) super.createDialogArea(parent);

    projectSelectionBlock.createContents(parent);

    return parent;
  }

  @Override
  protected void okPressed() {
    super.okPressed();
    
    new SpeedTracerLaunchShortcut().launch(projectSelectionBlock.getProject(),
        ILaunchManager.PROFILE_MODE);
  }

  /**
   * Provided for subclasses.
   */
  protected void updateControls() {
  }
  
  @Override
  protected void validate() {
    updateStatus(projectSelectionBlock.validateProject());
  }

}
