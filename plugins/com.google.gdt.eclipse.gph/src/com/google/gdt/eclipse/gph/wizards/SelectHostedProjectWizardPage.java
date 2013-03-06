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
package com.google.gdt.eclipse.gph.wizards;

import com.google.api.client.http.HttpRequestFactory;
import com.google.gdt.eclipse.gph.ProjectHostingService;
import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.providers.ScmProvider;
import com.google.gdt.eclipse.gph.util.ModelLabelProvider;
import com.google.gdt.eclipse.gph.util.ProjectViewer;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A wizard page to browse and select GPH projects.
 * 
 * @see ImportHostedProjectsWizard
 */
public class SelectHostedProjectWizardPage extends WizardSelectionPage
implements IShellProvider {

  private final ProjectHostingService hostingService = new ProjectHostingService();

  private ProjectViewer projectControl;
  private TableViewer projectViewer;

  /**
   * Creates an instance.
   * 
   * @param pageName the page name.
   * @param selection the current workbench selection.
   */
  public SelectHostedProjectWizardPage(String pageName,
      IStructuredSelection selection) {
    super(pageName);

    setTitle(pageName);
    setDescription("Select project for import.");
    setPageComplete(false);
  }

  @Override
  public boolean canFlipToNextPage() {
    return !projectViewer.getSelection().isEmpty();
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 10;
    layout.marginHeight = 10;
    composite.setLayout(layout);

    SashForm sash = new SashForm(composite, SWT.HORIZONTAL);
    GridData data = new GridData(GridData.FILL_BOTH);
    data.widthHint = 400;
    data.heightHint = 300;
    sash.setLayoutData(data);

    projectViewer = new TableViewer(sash, SWT.SINGLE | SWT.V_SCROLL
        | SWT.BORDER);
    projectViewer.setContentProvider(new ArrayContentProvider());
    projectViewer.setLabelProvider(new ModelLabelProvider());
    projectViewer.setComparator(new ViewerComparator());
    projectViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateProjectBrowser();
        getContainer().updateButtons();
      }
    });
    projectViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        handleDoubleClick();
      }
    });

    projectControl = new ProjectViewer(sash);

    sash.setWeights(new int[] {35, 65});

    showProjectInBrowser(null);

    setControl(composite);
  }

  @Override
  public IWizardPage getNextPage() {
    // we're updating the node lazily since this will ensure that we don't
    // over-eagerly (and hence perhaps unnecessarily) load checkout provider
    // plugins
    loadProviderForSelection();
    return super.getNextPage();
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible && getWizard().getContainer() instanceof WizardDialog) {
      WizardDialog dialog = (WizardDialog) getWizard().getContainer();

      dialog.setMinimumPageSize(700, 300);
    }

    super.setVisible(visible);

    if (visible && projectViewer.getInput() == null) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          populateProjectViewer();
        }
      });
    }
  }

  private void displayProjects(final List<GPHProject> projects) {
    getShell().getDisplay().syncExec(new Runnable() {
      public void run() {
        projectViewer.setInput(projects);

        Object element = projectViewer.getElementAt(0);

        if (element != null) {
          projectViewer.setSelection(new StructuredSelection(element), true);
        }
      }
    });
  }

  private HttpRequestFactory getAuthenticatedRequestFactory() {
    final HttpRequestFactory[] transport = new HttpRequestFactory[1];

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        transport[0] = GoogleLogin.getInstance().createRequestFactory(
            "Importing projects from Google Project Hosting requires authentication.");
      }
    });

    return transport[0];
  }

  private GPHProject getSelectedProject() {
    IStructuredSelection selection = (IStructuredSelection) projectViewer.getSelection();

    return (GPHProject) selection.getFirstElement();
  }

  private void handleDoubleClick() {
    GPHProject project = getSelectedProject();

    if (project != null) {
      if (getContainer() instanceof WizardDialog) {
        WizardDialog dialog = (WizardDialog) getContainer();

        // dialog.nextPressed();
        try {
          Method nextPressedMethod = WizardDialog.class.getDeclaredMethod("nextPressed");
          nextPressedMethod.setAccessible(true);
          nextPressedMethod.invoke(dialog);
        } catch (Throwable t) {
          // If anything at all comes out of this, we don't care.
        }
      }
    }
  }

  private void loadProviderForSelection() {
    try {
      updateWizardNode();
    } catch (Throwable e) {
      ErrorDialog.openError(getShell(), "Checkout Error",
          "Unable to load SCM checkout provider.",
          ProjectHostingUIPlugin.createStatus(e));
    }
  }

  private void populateProjectViewer() {
    final List<GPHProject> projects = new ArrayList<GPHProject>();

    try {
      getWizard().getContainer().run(true, false, new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
          monitor.beginTask("Connecting to Project Hosting...",
              IProgressMonitor.UNKNOWN);
          try {
            retrieveProjects(projects);
          } catch (Exception e) {
            throw new InvocationTargetException(e);
          }
        }
      });

      displayProjects(projects);
    } catch (InterruptedException ie) {
      // The user canceled.

    } catch (InvocationTargetException e) {
      Throwable target = e.getTargetException();

      if (target instanceof OperationCanceledException) {
        // ignore
      } else if (target instanceof CoreException) {
        ProjectHostingUIPlugin.logError(target);
        ErrorDialog.openError(getShell(), "Connection Error",
            "Unable to connect to Project Hosting.",
            ((CoreException) target).getStatus());
      } else {
        ProjectHostingUIPlugin.logError(target);
        ErrorDialog.openError(getShell(), "Connection Error",
            "Unable to connect to Project Hosting.",
            ProjectHostingUIPlugin.createStatus(target));
      }
    }
  }

  private void retrieveProjects(List<GPHProject> projects)
      throws CoreException, IOException {
    // Fetch an authenticated transport object to pass to the GPH service API.
    // note: this will initiate the login process if there's no user logged in
    HttpRequestFactory requestFactory = getAuthenticatedRequestFactory();

    if (requestFactory == null) {
      // The user canceled the login process.
      throw new OperationCanceledException();
    }

    List<GPHProject> results = hostingService.getProjects(requestFactory);

    projects.addAll(results);
  }

  private void showProjectInBrowser(GPHProject project) {
    projectControl.setProject(project);
  }

  private void updateProjectBrowser() {
    showProjectInBrowser(getSelectedProject());
  }

  private void updateWizardNode() {
    GPHProject project = getSelectedProject();

    if (project != null) {
      ScmProvider provider = ProjectHostingUIPlugin.getScmProvider(project.getScmType());

      if (provider == null || !provider.isFullyInstalled()) {
        setSelectedNode(new ProvisionSCMProviderNode(provider, project));
      } else {
        setSelectedNode(new CheckoutWizardNode(provider.getCheckoutProvider(),
            project, this));
      }
    }
  }
}
