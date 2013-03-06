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
package com.google.gdt.eclipse.gph.hge;

import com.google.gdt.eclipse.core.pde.BundleUtilities;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.wizards.ImportHostedProjectsWizard;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.wizards.HgWizardPage;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * The import wizard for Mercurial projects.
 */
public class HgImportWizard extends Wizard implements IWorkbenchWizard {
  private GPHProject project;

  private IHgWizardHelper wizardHelper;

  public HgImportWizard(GPHProject project) {
    this.project = project;

    setWindowTitle(ImportHostedProjectsWizard.getDefaultPageTitle());
    setDefaultPageImageDescriptor(ImportHostedProjectsWizard.getDefaultPageImageDescriptor());

    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    IStatus status = configureMercurial();

    if (status.isOK()) {
      addMercurialPages();
    } else {
      addPage(new HgInstallStatusWizardPage(this));
    }
  }

  @Override
  public IWizardPage getNextPage(IWizardPage currentPage) {
    if (super.getNextPage(currentPage) == null) {
      addNextPages(currentPage);
    }

    return super.getNextPage(currentPage);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

  @Override
  public boolean performCancel() {
    if (wizardHelper != null) {
      getWizardHelper().performCancel();
    }
    
    return super.performCancel();
  }

  @Override
  public boolean performFinish() {
    if (wizardHelper != null) {
      return getWizardHelper().performFinish();
    } else {
      return true;
    }
  }

  protected IHgWizardHelper getWizardHelper() {
    if (wizardHelper == null) {
      try {
        wizardHelper = (IHgWizardHelper) Class.forName(IHgWizardHelper.IMPL).newInstance();
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    return wizardHelper;
  }

  protected void initPage(String description, HgWizardPage wizardPage) {
    wizardPage.setDescription(description);

    wizardPage.setDialogSettings(getDialogSettings());
  }

  private void addMercurialPages() {
    getWizardHelper().addMercurialPages(this, project);
  }

  private void addNextPages(IWizardPage currentPage) {
    if (currentPage instanceof HgInstallStatusWizardPage) {
      if (isMercurialInstalled() && configureMercurial().isOK()) {
        addMercurialPages();
      }
    }
  }

  private IStatus configureMercurial() {
    try {
      MercurialEclipsePlugin.getDefault().checkHgInstallation();

      if (MercurialEclipsePlugin.getDefault().isHgUsable()) {
        return Status.OK_STATUS;
      } else {
        return new Status(IStatus.ERROR, HgCheckoutProvider.PLUGIN_ID,
            "MercurialEclipse is not installed correctly,");
        // + HgDebugInstallClient.debugInstall());
      }
    } catch (Throwable t) {
      return new Status(IStatus.ERROR, HgCheckoutProvider.PLUGIN_ID,
          "MercurialEclipse is not installed correctly.");
    }
  }

  private boolean isMercurialInstalled() {
    return BundleUtilities.areBundlesDependenciesSatisfied(HgCheckoutProvider.PLUGIN_ID);
  }

}
