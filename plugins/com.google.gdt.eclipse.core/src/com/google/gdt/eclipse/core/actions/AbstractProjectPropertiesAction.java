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
package com.google.gdt.eclipse.core.actions;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.natures.NatureUtils;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Action that opens up a project's properties dialog to a specific page, which
 * is specified by subclasses.
 */
public abstract class AbstractProjectPropertiesAction extends Action implements
    IActionDelegate {

  private final String propertiesPageID;

  private IResource resource;

  protected AbstractProjectPropertiesAction(String propertiesPageID) {
    this.propertiesPageID = propertiesPageID;
  }

  public void run(IAction action) {
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

    if (resource != null && resource.getType() == IResource.PROJECT) {
      PreferenceDialog page = PreferencesUtil.createPropertyDialogOn(shell,
          resource, propertiesPageID, null, null);
      if (page != null) {
        page.open();
        return;
      }
    }

    CorePluginLog.logError("Could not create project properties dialog for resource "
        + resource.toString());
  }

  public final void selectionChanged(IAction action, ISelection selection) {
    this.resource = ResourceUtils.getSelectionResource(selection);

    boolean isJavaProjectSelected = false;

    try {
      isJavaProjectSelected = (this.resource != null && NatureUtils.hasNature(
          this.resource.getProject(), JavaCore.NATURE_ID));
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }

    action.setEnabled(isJavaProjectSelected);
  }
}
