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

import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;
import com.google.gdt.eclipse.gph.install.P2InstallManagerFactory;
import com.google.gdt.eclipse.gph.install.P2InstallerWizardPage;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.providers.ScmProvider;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * A container for the ProvisionSCMProviderPage wizard page.
 */
public class ProvisionSCMProviderWizard extends Wizard {
  private GPHProject project;
  private ScmProvider provider;

  public ProvisionSCMProviderWizard(ScmProvider provider, GPHProject project) {
    this.provider = provider;
    this.project = project;

    setDefaultPageImageDescriptor(ProjectHostingUIPlugin.getImageDescriptor("wizban/projecthosting_wiz.png")); //$NON-NLS-1$    
    setWindowTitle("Import Hosted Projects");
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    if (provider == null) {
      addPage(new ShowErrorPage(this,
          "No team support provider found for source control type: "
              + project.getScmTypeLabel() + "."));
    } else if (!P2InstallManagerFactory.isInstallManagerAvailable()) {
      addPage(new ManualInstallWizardPage(provider, project));
    } else {
      addPage(new P2InstallerWizardPage(provider));
    }
  }

  @Override
  public boolean performFinish() {
    IWizardPage page = getContainer().getCurrentPage();
    
    if (page instanceof IFinishablePage) {
      return ((IFinishablePage)page).performFinish();
    }
    
    return true;
  }

}
