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

import com.google.gdt.eclipse.core.pde.BundleUtilities;
import com.google.gdt.eclipse.gph.install.P2InstallationUnit;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.providers.ICheckoutProvider;

import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * An {@link ICheckoutProvider} for subversive.
 */
public class SubversiveCheckoutProvider implements ICheckoutProvider {

  private static final String SUBVERSIVE_UI_PLUGIN_ID = "org.eclipse.team.svn.ui";

  public IWorkbenchWizard createWizard(IShellProvider shellProvider,
      GPHProject project) {
    return new SubversiveCheckoutWizard(project);
  }

  public P2InstallationUnit getP2InstallationUnit() {
    return null;
  }

  public boolean isFullyInstalled() {
    return BundleUtilities.isBundleInstalled(SUBVERSIVE_UI_PLUGIN_ID);
  }

}
