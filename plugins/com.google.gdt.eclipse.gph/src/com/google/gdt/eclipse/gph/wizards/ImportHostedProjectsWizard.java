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
package com.google.gdt.eclipse.gph.wizards;

import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;
import com.google.gdt.eclipse.gph.extensions.IImportCallback;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Wizard that imports hosted projects into the workspace.
 */
public class ImportHostedProjectsWizard extends Wizard implements IImportWizard {
  public static ImageDescriptor getDefaultPageImageDescriptor() {
    return ProjectHostingUIPlugin.getImageDescriptor("wizban/projecthosting_wiz.png");
  }

  public static String getDefaultPageTitle() {
    return "Import Hosted Projects";
  }

  private SelectHostedProjectWizardPage projectSelectionPage;

  public ImportHostedProjectsWizard() {
  }

  @Override
  public void addPages() {
    addPage(projectSelectionPage);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    setNeedsProgressMonitor(true);
    setForcePreviousAndNextButtons(true);

    setDefaultPageImageDescriptor(getDefaultPageImageDescriptor());
    setWindowTitle(getDefaultPageTitle());

    projectSelectionPage = new SelectHostedProjectWizardPage(
        "Select Google Hosted Project", selection);
  }

  @Override
  public boolean performFinish() {

    ExtensionQuery<IImportCallback> extensionQuery = new ExtensionQuery<IImportCallback>(
        ProjectHostingUIPlugin.PLUGIN_ID, "importcallback", "class");

    for (ExtensionQuery.Data<IImportCallback> extensionData : extensionQuery.getData()) {
      extensionData.getExtensionPointData().onFinish();
    }

    return true;
  }

}
