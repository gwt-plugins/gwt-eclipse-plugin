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
import com.google.gdt.eclipse.gph.wizards.AbstractWizardPage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgDebugInstallClient;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * Display the current install status of the MercurialEclipse and hg tools.
 */
public class HgInstallStatusWizardPage extends AbstractWizardPage {

  /**
   * Create a new HgInstallStatusWizardPage.
   */
  public HgInstallStatusWizardPage(IWizard wizard) {
    super("HgInstallStatusWizardPage");

    setTitle(wizard.getWindowTitle());
  }

  @Override
  protected Control createPageContents(Composite parent) {
    Text text = new Text(parent, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
    text.setBackground(parent.getBackground());

    IStatus isInstalled = getMercurialInstalled();

    if (isInstalled.isOK()) {
      IStatus configStatus = getConfigurationStatus();

      if (configStatus.isOK()) {
        setMessage("MercurialEclipse successfully installed.");

        text.setText("MercurialEclipse was successfully installed.");
      } else {
        setMessage("Problems with the MercurialEclipse installation.");

        text.setText(configStatus.getMessage());
      }
    } else {
      setMessage("Unable to install MercurialEclipse.");

      text.setText("Unable to install MercurialEclipse.\n\nTo perform a manual installation, "
          + "go to the Help > Install New Software... menu and enter the following update site:\n\n"
          + "http://cbes.javaforge.com/update");
    }

    return text;
  }

  private IStatus getConfigurationStatus() {
    try {
      if (MercurialEclipsePlugin.getDefault().isHgUsable()) {
        return Status.OK_STATUS;
      } else {
        return new Status(IStatus.ERROR, HgCheckoutProvider.PLUGIN_ID,
            "There are problems with the MercurialEclipse installation. Details: \n"
                + HgDebugInstallClient.debugInstall());
      }
    } catch (Throwable t) {
      return new Status(IStatus.ERROR, HgCheckoutProvider.PLUGIN_ID,
          "MercurialEclipse configuration error: " + t.getMessage(), t);
    }
  }

  private IStatus getMercurialInstalled() {
    if (BundleUtilities.areBundlesDependenciesSatisfied(HgCheckoutProvider.PLUGIN_ID)) {
      return Status.OK_STATUS;
    } else {
      return new Status(IStatus.ERROR, HgCheckoutProvider.PLUGIN_ID,
          "MercurialEclipse was not installed properly.");
    }
  }

}
