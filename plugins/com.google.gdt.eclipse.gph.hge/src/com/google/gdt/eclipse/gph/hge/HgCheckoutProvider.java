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

import com.google.gdt.eclipse.core.OSUtilities;
import com.google.gdt.eclipse.core.pde.BundleUtilities;
import com.google.gdt.eclipse.gph.install.P2InstallationFeature;
import com.google.gdt.eclipse.gph.install.P2InstallationUnit;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.providers.ICheckoutProvider;

import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IWorkbenchWizard;

import java.util.ArrayList;
import java.util.List;

// TODO: if the wizard is canceled, we should clean up any repo locations
// that were created

/**
 * This checkout provider supports the MercurialEclipse SCM provider. It enables hg support for GPH.
 */
public class HgCheckoutProvider implements ICheckoutProvider {
  public static final String PLUGIN_ID = "com.google.gdt.eclipse.gph.hge";

  private static final String MAIN_FEATURE_ID = "mercurialeclipse";

  private static final String MAIN_FEATURE_NAME = "MercurialEclipse";

  private static final String UPDATE_SITE = "http://cbes.javaforge.com/update";
  private static final String WINDOWS_FEATURE_ID = "com.intland.hgbinary.win32";

  /**
   * Create a new MercurialEclipseCheckoutProvider.
   */
  public HgCheckoutProvider() {}

  @Override
  public IWorkbenchWizard createWizard(IShellProvider shellProvider, GPHProject project) {
    return new HgImportWizard(project);
  }

  @Override
  public P2InstallationUnit getP2InstallationUnit() {
    // TODO: when MercurialEclipse 1.8 comes out, it will require Eclipse 3.5.
    // We need to return the URL for MercurialEclipse 1.7.1 if running on
    // Eclipse 3.4.

    List<P2InstallationFeature> features = new ArrayList<P2InstallationFeature>();

    features.add(new P2InstallationFeature(MAIN_FEATURE_NAME, MAIN_FEATURE_ID));

    if (OSUtilities.isWindows()) {
      features.add(new P2InstallationFeature("Mercurial Windows binary", WINDOWS_FEATURE_ID));
    }

    return new P2InstallationUnit(MAIN_FEATURE_NAME, UPDATE_SITE, features);
  }

  @Override
  public boolean isFullyInstalled() {
    return BundleUtilities.areBundlesDependenciesSatisfied(PLUGIN_ID);
  }
}
