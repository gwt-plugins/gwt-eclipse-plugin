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
package com.google.gdt.eclipse.gph.subversive;

import com.google.gdt.eclipse.gph.model.GPHProject;

import org.eclipse.team.svn.core.resource.IRepositoryLocation;
import org.eclipse.team.svn.core.svnstorage.SVNRemoteStorage;
import org.eclipse.team.svn.core.utility.SVNUtility;
import org.eclipse.team.svn.ui.wizard.ImportFromSVNWizard;
import org.eclipse.team.svn.ui.wizard.checkoutas.SelectCheckoutResourcePage;
import org.eclipse.team.svn.ui.wizard.shareproject.AddRepositoryLocationPage;
import org.eclipse.team.svn.ui.wizard.shareproject.SelectRepositoryLocationPage;

/**
 * TODO: doc me.
 */
public class SubversiveCheckoutWizard extends ImportFromSVNWizard {

  private final IRepositoryLocation location;

  public SubversiveCheckoutWizard(GPHProject project) {
    String url = project.getRepoUrls().get(0);
    location = SVNRemoteStorage.instance().newRepositoryLocation();
    SVNUtility.initializeRepositoryLocation(location, url);
    location.setAuthorName(project.getUser().getUserName());
    location.setPassword(project.getUser().getRepoPassword());
    location.setStructureEnabled(false);
    location.setUrl(url);
    SVNRemoteStorage.instance().addRepositoryLocation(location);
  }
  
  public void addPages() {
    this.addPage(this.selectLocation = new SelectRepositoryLocationPage(new IRepositoryLocation[]{location}));
    this.addPage(this.addLocation = new AddRepositoryLocationPage());
    this.addPage(this.selectResource = new SelectCheckoutResourcePage());
  }
  
}
