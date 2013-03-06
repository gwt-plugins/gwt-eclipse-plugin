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
package com.google.gdt.eclipse.gph.egit.wizard;

import com.google.gdt.eclipse.gph.egit.EGitCheckoutProviderPlugin;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.wizards.ImportHostedProjectsWizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import java.io.File;

/**
 * The checkout wizard for GPH Git projects.
 */
public class EGitCheckoutWizard extends Wizard implements IWorkbenchWizard {

  private CloneRepositoryWizardPage cloneRepoPage;
  private ImportProjectsWizardPage importProjectsPage;

  private GPHProject project;

  /**
   * Create a new EGitCheckoutWizard.
   * 
   * @param shellProvider
   * @param project
   */
  public EGitCheckoutWizard(IShellProvider shellProvider, GPHProject project) {
    this.project = project;

    setWindowTitle(ImportHostedProjectsWizard.getDefaultPageTitle());
    setDefaultPageImageDescriptor(EGitCheckoutProviderPlugin.getImageDescriptor("import_wiz.png"));

    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    cloneRepoPage = new CloneRepositoryWizardPage(this);
    addPage(cloneRepoPage);

    importProjectsPage = new ImportProjectsWizardPage(this);
    addPage(importProjectsPage);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {

  }

  @Override
  public boolean performFinish() {
    return importProjectsPage.createProjects();
  }

  protected GPHProject getGPHProject() {
    return project;
  }

  protected File getRepoCloneDirectory() {
    return cloneRepoPage.getDestinationDirectory();
  }

}
