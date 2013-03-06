/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.Resources;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiListingSourceFactory;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiProjectImpl;
import com.google.gdt.eclipse.managedapis.platform.ManagedApiInstallJob;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Extends the Wizard interface to provide UI for the managed API import flow.
 */
public class ApiImportWizard extends Wizard implements IImportWizard {

  private ManagedApiListingSourceFactory managedApiListingSourceFactory;

  private IProject project;

  private Resources resources;

  private ProjectSelectionPage projectSelectionPage;

  private ApiListingPage apiListingPage;

  public ApiImportWizard() {
    super();
    setWindowTitle("Add Google APIs");
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    if (getContainer() instanceof WizardDialog) {
      WizardDialog dialog = (WizardDialog) getContainer();

      dialog.addPageChangingListener(new IPageChangingListener() {
        public void handlePageChanging(PageChangingEvent event) {
          if (event.getCurrentPage() == projectSelectionPage &&
              event.getTargetPage() == apiListingPage) {
            event.doit = updateFromProjectSelectionPage();
          }
        }
      });
    }

    ImageDescriptor wizardIcon = null;

    try {
      wizardIcon = resources.getManagedApiImportIcon();
    } catch (MalformedURLException e) {
      ManagedApiLogger.warn(
          "Unable to load Managed Api icon due to malformed URL");
    }

    // Add a project selection page.
    if (project == null) {
      projectSelectionPage = new ProjectSelectionPage(this, wizardIcon);

      addPage(projectSelectionPage);
    }

    // Add the API listings page.
    apiListingPage = new ApiListingPage(
        "GoogleApiPage", managedApiListingSourceFactory);
    apiListingPage.setImageDescriptor(wizardIcon);
    apiListingPage.setResources(resources);

    addPage(apiListingPage);
  }

  public Point getPreferredPageSize() {
    return new Point(700, 300);
  }

  public Resources getResources() {
    return resources;
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    setResources(ManagedApiPlugin.getDefault().getResources());

    // Get the project from the selection.
    IResource selectionResource = ResourceUtils.getSelectionResource(selection);

    if (selectionResource != null) {
      try {
        setProject(selectionResource.getProject());
      } catch (CoreException e) {
        ManagedApiLogger.warn(e);
      }
    }
  }

  @Override
  public boolean performFinish() {
    ApiListingPage page = (ApiListingPage) getPage("GoogleApiPage");
    List<ManagedApiEntry> apisToImport = page.getManagedApiEntrySelection();
    final ManagedApiInstallJob installJob = new ManagedApiInstallJob(
        apisToImport.size() > 1 ? "Add Google APIs" : "Add Google API",
        apisToImport, project, "Add Google API {0}");
    try {
      getContainer().run(true, true, new WorkspaceModifyOperation(
          ResourcesPlugin.getWorkspace().getRoot()) {
          @Override
        protected void execute(IProgressMonitor monitor)
            throws CoreException, InvocationTargetException,
            InterruptedException {
          installJob.run(monitor);
          // already display errors to the user, so fall through
        }
      });
    } catch (InvocationTargetException e) {
      // Error --fall through
      return false;
    } catch (InterruptedException e) {
      // Cancel -- fall through
    }
    return true;
  }

  public void setManagedApiListingSourceFactory(
      ManagedApiListingSourceFactory managedApiListingSourceFactory) {
    this.managedApiListingSourceFactory = managedApiListingSourceFactory;

    if (apiListingPage != null) {
      apiListingPage.setManagedApiListingSourceFactory(
          managedApiListingSourceFactory);
    }
  }

  public boolean setProject(IProject project) throws CoreException {
    this.project = project;

    if (project != null && AppEngineAndroidCheckDialog.isAppEngineAndroidProject(project)) {
      ManagedApiListingSourceFactory sourceFactory = new ManagedApiListingSourceFactory();
      sourceFactory.setApiDirectoryFactory(
          ManagedApiPlugin.getDefault().getApiDirectoryFactory());
      sourceFactory.setProject(
          ManagedApiProjectImpl.getManagedApiProject(JavaCore.create(project)));

      setManagedApiListingSourceFactory(sourceFactory);
      return true;
    }
    return false;
  }

  public void setResources(Resources resources) {
    this.resources = resources;
  }

  private boolean updateFromProjectSelectionPage() {
    try {
      return setProject(projectSelectionPage.getSelectedProject());
    } catch (CoreException ce) {
      ((WizardDialog) getContainer()).setErrorMessage(
          "Error with selected project: " + ce.getMessage());

      return false;
    }
  }

}
