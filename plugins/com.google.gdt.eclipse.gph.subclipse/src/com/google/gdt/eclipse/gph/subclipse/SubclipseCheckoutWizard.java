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
package com.google.gdt.eclipse.gph.subclipse;

import com.google.gdt.eclipse.gph.model.GPHProject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.repo.SVNRepositoryLocation;
import org.tigris.subversion.subclipse.ui.wizards.CheckoutWizard;
import org.tigris.subversion.subclipse.ui.wizards.CheckoutWizardSelectionPage;

import java.util.Properties;

/**
 * A wizard to check out projects from a given svn repo.
 */
public class SubclipseCheckoutWizard extends CheckoutWizard {

  private static ISVNRepositoryLocation getRepositoryLocation(
      IShellProvider shellProvider, GPHProject project) throws CoreException {

    String url = project.getRepoUrls().get(0);

    Properties properties = new Properties();
    properties.setProperty("url", url); //$NON-NLS-1$
    properties.setProperty("user", project.getUser().getUserName()); //$NON-NLS-1$
    properties.setProperty("password", project.getUser().getRepoPassword()); //$NON-NLS-1$

    ISVNRepositoryLocation repo = SVNRepositoryLocation.fromProperties(properties);

    SVNProviderPlugin provider = SVNProviderPlugin.getPlugin(); 
    provider.getRepositories().addOrUpdateRepository(repo);

    return repo;
  }

  private CheckoutWizardSelectionPage folderSelectionPage;

  private final ISVNRepositoryLocation repositoryLocation;

  public SubclipseCheckoutWizard(IShellProvider shellProvider,
      GPHProject project) throws CoreException {
    this(getRepositoryLocation(shellProvider, project));
  }

  public SubclipseCheckoutWizard(ISVNRepositoryLocation repositoryLocation)
      throws CoreException {
    this.repositoryLocation = repositoryLocation;
  }

  @Override
  public IWizardPage getStartingPage() {
    configureFolderSelectionPage();

    return folderSelectionPage;
  }

  private void configureFolderSelectionPage() {
    IWizardPage[] pages = getPages();

    for (IWizardPage page : pages) {
      if (page instanceof CheckoutWizardSelectionPage) {
        folderSelectionPage = (CheckoutWizardSelectionPage) page;
        folderSelectionPage.setLocation(repositoryLocation);
      }
    }
  }

}
