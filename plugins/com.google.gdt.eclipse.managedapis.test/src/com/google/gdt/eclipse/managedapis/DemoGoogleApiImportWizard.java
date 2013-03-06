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
package com.google.gdt.eclipse.managedapis;

import com.google.gdt.eclipse.managedapis.impl.ManagedApiListingSourceFactory;
import com.google.gdt.eclipse.managedapis.ui.ApiImportWizard;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.FileNotFoundException;


/**
 * TODO: doc me.
 */
public class DemoGoogleApiImportWizard {
  public static void main(String[] args) throws FileNotFoundException {
    Display display = new Display();
    final Shell shell = new Shell(display);

    GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 5).applyTo(shell);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(
        shell);
    final Resources rsrc = new LocalResources(shell.getDisplay());
    final ManagedApiListingSourceFactory managedApiListingSourceFactory = new ManagedApiListingSourceFactory();
    managedApiListingSourceFactory.setDirectorySourceFile("src/com/google/gdt/eclipse/managedapis/SampleAPIList.json");

    // final ApiDirectory ad = new MockDirectoryService();

    Button ok = new Button(shell, SWT.PUSH);
    ok.setText("OK");
    ok.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {

        ApiImportWizard wizard = new ApiImportWizard();
        wizard.setResources(rsrc);
        wizard.setManagedApiListingSourceFactory(managedApiListingSourceFactory);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.open();
      }
    });

    shell.pack();
    shell.open();
    shell.setSize(200, 200);

    ApiImportWizard wizard = new ApiImportWizard();
    wizard.setResources(rsrc);
    wizard.setManagedApiListingSourceFactory(managedApiListingSourceFactory);

    WizardDialog dialog = new WizardDialog(shell, wizard);
    dialog.open();

    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }

}
