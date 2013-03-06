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
import com.google.gdt.eclipse.gph.install.P2InstallationUnit;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.providers.ScmProvider;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * A wizard page to tell the user where and how to install a needed SCM team
 * provider.
 */
public class ManualInstallWizardPage extends WizardPage {
  private P2InstallationUnit installationUnit;
  private GPHProject project;
  private ScmProvider provider;

  /**
   * Create a new ManualInstallWizardPage.
   * 
   * @param p2InstallationUnit
   * @deprecated
   */
  @Deprecated
  public ManualInstallWizardPage(GPHProject project,
      P2InstallationUnit installationUnit) {
    super("provisionSCMProviderPage");

    setTitle("Install " + installationUnit.getInstallationUnitName());
    setMessage("In order to import this project, you first need to install a "
        + project.getScmTypeLabel() + " team support provider.");

    this.project = project;
    this.installationUnit = installationUnit;
  }

  /**
   * Create a new ProvisionSCMProviderPage.
   */
  public ManualInstallWizardPage(ScmProvider provider, GPHProject project) {
    super("provisionSCMProviderPage");

    this.provider = provider;
    this.project = project;

    setTitle("Install " + provider.getProviderName() + " Team Support");
    setDescription("In order to import this project, you first need to install a "
        + provider.getScmTypeLabel() + " team support provider.");
  }

  public void createControl(Composite parent) {
    Text text = new Text(parent, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
    text.setBackground(parent.getBackground());

    // "Subversion support is provided by Subclipse."

    if (provider == null) {
      provider = ProjectHostingUIPlugin.getScmProvider(project.getScmType());
    }

    text.setText(provider.getScmTypeLabel()
        + " support is provided by "
        + provider.getProviderName()
        + ".\n\n"
        + "In order to install it, go to the Help > Install New Software... menu and enter the following update site:\n\n"
        + (installationUnit != null ? installationUnit.getUpdateSite()
            : provider.getInstallInfo().getUpdateSite()));

    setControl(text);
  }

}
