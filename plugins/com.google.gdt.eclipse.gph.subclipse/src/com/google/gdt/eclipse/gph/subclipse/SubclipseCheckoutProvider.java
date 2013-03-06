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

import com.google.gdt.eclipse.core.pde.BundleUtilities;
import com.google.gdt.eclipse.gph.install.P2InstallationFeature;
import com.google.gdt.eclipse.gph.install.P2InstallationUnit;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.providers.ICheckoutProvider;
import com.google.gdt.eclipse.gph.wizards.ErrorWizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IWorkbenchWizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link ICheckoutProvider} for subclipse.
 */
public class SubclipseCheckoutProvider implements ICheckoutProvider {

  /**
   * The id of the Project Hosting Subclipse plug-in (value
   * <code>"com.google.gdt.eclipse.gph.subclipse"</code>).
   */
  public static final String PLUGIN_ID = SubclipseCheckoutProvider.class.getPackage().getName();

  /**
   * Bundle ids for required subclipse bundles.
   */
  static final String[] SUBCLIPSE_BUNDLE_IDS = new String[] {
      "org.tigris.subversion.clientadapter",
      "org.tigris.subversion.clientadapter.svnkit",
      "org.tigris.subversion.subclipse.core",
      "org.tigris.subversion.subclipse.ui", "org.tmatesoft.svnkit"};

  private static final String ERROR_MSG = "Unable to configure Subclipse.";

  public IWorkbenchWizard createWizard(final IShellProvider shellProvider,
      final GPHProject project) {
    try {
      configure();
      return new SubclipseCheckoutWizard(shellProvider, project);
    } catch (Throwable e) {
      return new ErrorWizard(new Status(IStatus.ERROR, PLUGIN_ID, ERROR_MSG, e));
    }
  }

  public P2InstallationUnit getP2InstallationUnit() {
    List<P2InstallationFeature> features = new ArrayList<P2InstallationFeature>();

    features.add(new P2InstallationFeature("Subclipse",
        "org.tigris.subversion.subclipse"));
    features.add(new P2InstallationFeature(
        "Subclipse client adapter framework",
        "org.tigris.subversion.clientadapter.feature"));
    features.add(new P2InstallationFeature("Subclipse SVNKit client adapter",
        "org.tigris.subversion.clientadapter.svnkit.feature"));
    features.add(new P2InstallationFeature("SVNKit library",
        "org.tmatesoft.svnkit"));

    return new P2InstallationUnit("Subclipse",
        "http://subclipse.tigris.org/update_1.6.x", features);
  }

  public boolean isFullyInstalled() {
    return BundleUtilities.areBundlesInstalled(SUBCLIPSE_BUNDLE_IDS);
  }

  private void configure() throws IOException {
    if (!SubclipseProduct.isConfigured()) {
      SubclipseProduct.configureSVNKit();
    }
  }

}
