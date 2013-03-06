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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.ui.controls.SelectionChangeListener;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.Resources;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiListingSourceFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Represents a (currently the only) page in the ApiImportWizard. This page
 * wraps the ApiViewer and provides event handling glue to the containing
 * wizard.
 * 
 * suggestion: tabs to include search, recent popular, installed. These were
 * part of this view originally.
 */
public class ApiListingPage extends WizardPage implements IShellProvider {
  private static final String WILL_BE_IMPORTED = "Click Finish to add {0} to your project.";
  private static final String DEFAULT_DESCRIPTION = "Choose a Google Managed Api to add.";

  private SelectionListener doubleClickListener = new SelectionAdapter() {
    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
      if (getWizard().canFinish()) {
        if (getWizard().performFinish()) {
          ((WizardDialog) getContainer()).close();
        }
      }
    }
  };
  private boolean initialized = false;
  private ManagedApiListingSourceFactory managedApiListingSourceFactory;
  private Resources resources;

  private List<ManagedApiEntry> selected;
  private SelectionChangeListener<ManagedApiEntry> selectionChangeListener = new SelectionChangeListener<ManagedApiEntry>() {
    public void selectionChanged(List<ManagedApiEntry> selection) {
      selected = selection;
      setPageComplete(!selection.isEmpty());
      updateInstallMessage();
      updateApiEntryViewer(selection);
    }
  };
  private boolean updated;
  private ApiViewer viewer;
  private ManagedApiEntryViewer apiEntryViewer;

  protected ApiListingPage(String pageName,
      ManagedApiListingSourceFactory managedApiListingSourceFactory) {
    super(pageName);

    this.managedApiListingSourceFactory = managedApiListingSourceFactory;

    setDescription(DEFAULT_DESCRIPTION);
    setPageComplete(false);
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(composite);

    Link apiConsoleLink = new Link(composite, SWT.NONE);
    apiConsoleLink.setText("Visit the <a>API Console</a> to manage your APIs");
    apiConsoleLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtilities.launchBrowserAndHandleExceptions("http://code.google.com/apis/console");
      }
    });

    SashForm sash = new SashForm(composite, SWT.HORIZONTAL);
    GridDataFactory.fillDefaults().grab(true, true).hint(500, 300).applyTo(sash);

    viewer = new ApiViewer(getContainer(), managedApiListingSourceFactory);
    viewer.setResources(resources);
    viewer.createControl(sash);
    viewer.registerSelectionChangeListener(selectionChangeListener);
    viewer.registerSelectionListener(doubleClickListener);

    Composite apiEntryContainer = new Composite(sash, SWT.NONE);
    GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(apiEntryContainer);

    apiEntryViewer = new ManagedApiEntryViewer(apiEntryContainer);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(
        apiEntryViewer.getControl());

    sash.setWeights(new int[] {45, 55});

    setControl(composite);
  }

  public List<ManagedApiEntry> getManagedApiEntrySelection() {
    return selected;
  }

  @Override
  public ApiImportWizard getWizard() {
    return (ApiImportWizard) super.getWizard();
  }

  public void setResources(Resources resources) {
    this.resources = resources;
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      setTitle("Google Managed APIs");

      if (!initialized) {
        initialized = true;
        updated = false;
      }
    }

    super.setVisible(visible);

    if (visible) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          doUpdateListing();
        }
      });
    }
  }

  protected void setManagedApiListingSourceFactory(
      ManagedApiListingSourceFactory managedApiListingSourceFactory) {
    this.managedApiListingSourceFactory = managedApiListingSourceFactory;

    if (viewer != null) {
      viewer.setManagedApiListingSourceFactory(managedApiListingSourceFactory);

      doUpdateListing();
    }
  }

  private void doUpdateListing() {
    if (!updated) {
      updated = true;

      final IStatus[] statusResult = new IStatus[1];

      try {
        getContainer().run(true, true, new IRunnableWithProgress() {
          public void run(IProgressMonitor monitor)
              throws InvocationTargetException, InterruptedException {

            monitor.beginTask("Retrieving API listings...",
                IProgressMonitor.UNKNOWN);

            // Can also pass the monitor into this method -
            statusResult[0] = viewer.updateListing(new NullProgressMonitor());

            monitor.done();
          }
        });

        if (statusResult[0] != null && !statusResult[0].isOK()) {
          ManagedApiPlugin.getDefault().getLog().log(statusResult[0]);

          setErrorMessage("Error contacting the API service");
        }
      } catch (InvocationTargetException e) {
        String message = e.getMessage();

        if (e.getCause() != null) {
          message = e.getCause().getMessage();
        }

        if (message == null) {
          setErrorMessage("Error contacting the API service");
        } else {
          setErrorMessage("Error contacting the API service: " + message);
        }
      } catch (InterruptedException e) {
        // The user canceled - close the dialog.
        ((WizardDialog) getContainer()).close();
      }
    }
  }

  private void updateApiEntryViewer(List<ManagedApiEntry> selection) {
    if (selection.size() == 0) {
      apiEntryViewer.setManagedApiEntry(null);
    } else {
      apiEntryViewer.setManagedApiEntry(selection.get(selection.size() - 1));
    }
  }

  private void updateInstallMessage() {
    if (selected.size() == 0) {
      setMessage(null);
    } else if (selected.size() <= 3) {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < selected.size(); i++) {
        // foo
        // foo and bar
        // foo, bar, and baz
        ManagedApiEntry api = selected.get(i);

        if (i == 1 && selected.size() == 2) {
          builder.append(" and ");
        } else if (i > 0 && i == selected.size() - 1) {
          builder.append(", and ");
        } else if (i > 0) {
          builder.append(", ");
        }

        builder.append("\"" + api.getDisplayName() + "\"");
      }

      setMessage(NLS.bind(WILL_BE_IMPORTED, builder.toString()));
    } else {
      setMessage(NLS.bind(WILL_BE_IMPORTED, selected.size() + " APIs"));
    }
  }

}
