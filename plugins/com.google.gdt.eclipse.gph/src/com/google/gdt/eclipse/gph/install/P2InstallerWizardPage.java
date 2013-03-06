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
package com.google.gdt.eclipse.gph.install;

import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;
import com.google.gdt.eclipse.gph.providers.ScmProvider;
import com.google.gdt.eclipse.gph.wizards.AbstractWizardPage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

/**
 * A wizard page to install a set of {@link P2InstallationUnit}s.
 */
public class P2InstallerWizardPage extends AbstractWizardPage {

  private static Font BOLD_FONT;

  private P2InstallManager installManager;

  private ScmProvider provider;
  
  private List<P2InstallationFeature> featuresToInstall;

  public P2InstallerWizardPage(ScmProvider provider) {
    super("installIUWizardPage");

    this.provider = provider;

    installManager = P2InstallManagerFactory.createInstallManager(Arrays.asList(provider.getInstallInfo()));

    featuresToInstall = installManager.resolveInstalledStatus();
    
    setTitle("Install " + provider.getProviderName());
    setMessage("It is necessary to install a " + provider.getScmTypeLabel()
        + " team provider in order to import this project.");

    setPageComplete(false);
  }

  @Override
  public boolean performFinish() {
    final IStatus[] status = new IStatus[1];

    try {
      getContainer().run(true, true, new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
          status[0] = installManager.resolveP2Information(monitor);
        }
      });
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof OperationCanceledException) {
        return false;
      } else {
        ProjectHostingUIPlugin.logError(e);
        tellTheUserTheyreWedged(ProjectHostingUIPlugin.createStatus(
            "Error during installation.", e));
        return false;
      }
    } catch (InterruptedException e) {
      return false;
    }

    if (status[0] != null) {
      if (status[0].getSeverity() == IStatus.CANCEL) {
        return false;
      } else if (!status[0].isOK()) {
        ProjectHostingUIPlugin.logStatus(status[0]);

        tellTheUserTheyreWedged(status[0]);

        return false;
      }
    }

    installManager.runP2Install();

    return true;
  }
  
  @Override
  protected Control createPageContents(Composite parent) {
    Composite c1 = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).applyTo(
        c1);

    Label iconLabel = new Label(c1, SWT.NONE);
    if (provider.getProviderImageDescriptor() != null) {
      iconLabel.setImage(ProjectHostingUIPlugin.createImage(
          "scm-" + provider.getProviderName(),
          provider.getProviderImageDescriptor()));
    }
    GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(iconLabel);
    
    Composite composite = new Composite(c1, SWT.NONE);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
    GridLayoutFactory.fillDefaults().applyTo(composite);
    
    Label providerDescriptionLabel = new Label(composite, SWT.WRAP);
    providerDescriptionLabel.setText(provider.getProviderDescription());
    GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).applyTo(
        providerDescriptionLabel);

    Label instructionsLabel = new Label(composite, SWT.WRAP);
    instructionsLabel.setText("Installing "
        + provider.getProviderName()
        + " will require a restart of Eclipse. "
        + "After Eclipse restarts, you will need to run this wizard again to import your project.");
    GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).applyTo(
        instructionsLabel);
    
    Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);
    
    Label toBeInstalledLabel = new Label(composite, SWT.WRAP);
    toBeInstalledLabel.setText("Features to install:");
    toBeInstalledLabel.setFont(createBoldFont(toBeInstalledLabel.getFont()));
    
    Composite featuresComposite = new Composite(composite, SWT.NONE);
    GridDataFactory.fillDefaults().indent(15, 0).applyTo(featuresComposite);
    GridLayoutFactory.fillDefaults().spacing(0, 2).applyTo(featuresComposite);
    
    for (P2InstallationFeature feature : featuresToInstall) {
      CLabel label = new CLabel(featuresComposite, SWT.NONE);
      label.setText(feature.getFeatureLabel());
      
      if (feature.isInstalled()) {
        label.setText(label.getText() + " (already installed)");
        label.setImage(ProjectHostingUIPlugin.getImage("feature_obj_d.gif"));
      } else {
        label.setImage(ProjectHostingUIPlugin.getImage("feature_obj.gif"));
      }
    }
    
    Button installButton = new Button(composite, SWT.PUSH);
    installButton.setText("Install " + provider.getProviderName());
    installButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        performInstall();
      }
    });
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.BEGINNING).applyTo(installButton);
    
    separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);
    
    return composite;
  }
  
  protected void performInstall() {
    if (performFinish()) {
      if (getContainer() instanceof WizardDialog) {
        WizardDialog dialog = (WizardDialog) getContainer();

        dialog.close();
      }
    }
  }

  private Font createBoldFont(Font font) {
    if (BOLD_FONT == null) {
      FontData data = font.getFontData()[0];
      
      BOLD_FONT = new Font(font.getDevice(), data.getName(), data.getHeight(), SWT.BOLD);
    }
    
    return BOLD_FONT;
  }

  private void tellTheUserTheyreWedged(IStatus status) {
    ErrorDialog.openError(getShell(), "Error Installing Features",
        "The installation was unable to complete.", status);
  }

}
