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

import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.wizards.ShowErrorPage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.wizards.ClonePage;
import com.vectrace.MercurialEclipse.wizards.ProjectsImportPage;
import com.vectrace.MercurialEclipse.wizards.SelectRevisionPage;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * An implementation of the IHgWizardHelper interface. This class is created via reflection
 * from the {@link HgImportWizard} class.
 */
public class HgWizardHelperImpl implements IHgWizardHelper {

  private ClonePage clonePage;
  private ProjectsImportPage importPage;

  /**
   * Create a new HgWizardHelperImpl.
   */
  public HgWizardHelperImpl() {
  }

  public void addMercurialPages(Wizard wizard, GPHProject project) {
    clonePage = new ClonePage(null, "CreateRepoPage", "Clone repository", null);
    clonePage.setDescription("Create a clone from another hg repository (local or remote).");
    (clonePage).setDialogSettings(wizard.getDialogSettings());

    // also, use MercurialEclipsePlugin.isHgUsable()
    HgRepositoryLocationManager locationManager = MercurialEclipsePlugin.getRepoManager();

    try {
      // TODO: hardcoded to the first repo url
      IHgRepositoryLocation location = locationManager.getRepoLocation(
          project.getRepoUrls().get(0), project.getUser().getUserName(),
          project.getUser().getRepoPassword());

      (clonePage).setInitialRepo(location);
    } catch (Exception hge) {
      wizard.addPage(new ShowErrorPage(wizard, hge));
    }

    IWizardPage selectRevisionPage = new SelectRevisionPage(
        "SelectRevisionPage");

    importPage = new ProjectsImportPage("ProjectsImportPage");

    wizard.addPage(clonePage);
    wizard.addPage(selectRevisionPage);
    wizard.addPage(importPage);
  }

  public void performCancel() {
    if (clonePage != null) {
      clonePage.performCleanup();
    }
  }

  public boolean performFinish() {
    if (importPage != null) {
      return importPage.createProjects();
    } else {
      return true;
    }
  }

}
