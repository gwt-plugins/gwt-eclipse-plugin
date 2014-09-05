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
package com.google.gdt.eclipse.gph.egit;

import com.google.gdt.eclipse.core.pde.BundleUtilities;
import com.google.gdt.eclipse.gph.egit.wizard.EGitCheckoutWizard;
import com.google.gdt.eclipse.gph.install.P2InstallationFeature;
import com.google.gdt.eclipse.gph.install.P2InstallationUnit;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.providers.ICheckoutProvider;
import com.google.gdt.eclipse.gph.wizards.ErrorWizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IWorkbenchWizard;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides EGit support for the GPH / GPE integration feature.
 */
public class EGitCheckoutProvider implements ICheckoutProvider {
  /** The EGit provider plugin id. */
  public static final String PLUGIN_ID = EGitCheckoutProvider.class.getPackage().getName();

  /**
   * Bundle ids for required egit bundles.
   */
  private static final String[] EGIT_BUNDLE_IDS = new String[] {"org.eclipse.egit.ui"};

  /**
   * Create a new EGitCheckoutProvider.
   */
  public EGitCheckoutProvider() {

  }

  @Override
  public IWorkbenchWizard createWizard(IShellProvider shellProvider, GPHProject project) {
    try {
      return new EGitCheckoutWizard(shellProvider, project);
    } catch (Throwable e) {
      return new ErrorWizard(new Status(IStatus.ERROR, PLUGIN_ID,
          "Error initializing Git team provider.", e));
    }
  }

  @Override
  public P2InstallationUnit getP2InstallationUnit() {
    List<P2InstallationFeature> features = new ArrayList<P2InstallationFeature>();

    features.add(new P2InstallationFeature("Eclipse Git Team Provider", "org.eclipse.egit"));
    features.add(new P2InstallationFeature("JGit Library", "org.eclipse.jgit"));

    return new P2InstallationUnit("EGit", "http://download.eclipse.org/egit/updates", features);
  }

  @Override
  public boolean isFullyInstalled() {
    return BundleUtilities.areBundlesInstalled(EGIT_BUNDLE_IDS);
  }
}
