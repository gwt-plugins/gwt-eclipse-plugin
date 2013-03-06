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
package com.google.gdt.eclipse.appsmarketplace.ui;

import com.google.api.client.http.HttpRequestFactory;
import com.google.gdt.eclipse.appsmarketplace.AppsMarketplacePlugin;
import com.google.gdt.eclipse.appsmarketplace.AppsMarketplacePluginLog;
import com.google.gdt.eclipse.appsmarketplace.data.DataStorage;
import com.google.gdt.eclipse.appsmarketplace.job.BackendJob;
import com.google.gdt.eclipse.appsmarketplace.resources.AppsMarketplaceImages;
import com.google.gdt.eclipse.appsmarketplace.resources.AppsMarketplaceProject;
import com.google.gdt.eclipse.core.ActiveProjectFinder;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Deploys a Apps Marketplace project.
 */
public abstract class AbstractMarketplaceHandler extends AbstractHandler {

  /**
   * Exception used internally by AbstractMarketplaceHandler and derived
   * classes.
   */
  protected class AbstractMarketplaceHandlerException extends Exception {
  }

  private IWorkbenchWindow window;

  protected HttpRequestFactory requestFactory;

  protected IProject project;

  protected void executeAddMarketplaceSupport(
      ExecutionEvent event, Boolean defaultSupport)
      throws AbstractMarketplaceHandlerException {
    PreferenceDialog page = PreferencesUtil.createPropertyDialogOn(getShell(),
        project, AppsMarketplaceProjectPropertyPage.ID,
        new String[] {AppsMarketplaceProjectPropertyPage.ID}, defaultSupport);

    if (page.open() != Window.OK) {
      throw new AbstractMarketplaceHandlerException();
    }
  }

  protected void executeListOnMarketplace(ExecutionEvent event)
      throws AbstractMarketplaceHandlerException {
    DataStorage.clearData();
    AppsMarketplaceProject appsMktProj = AppsMarketplaceProject.create(project);

    BackendJob job = new BackendJob("Getting Vendor Profile",
        BackendJob.Type.GetVendorProfile, requestFactory, appsMktProj);
    ProgressMonitorDialog pdlg = BackendJob.launchBackendJob(job, getShell());

    if (pdlg.open() != Window.OK) {
      throw new AbstractMarketplaceHandlerException();
    }

    if (job.getOperationStatus() == true) {
      job = new BackendJob("Getting Application Listing",
          BackendJob.Type.GetAppListing, requestFactory, appsMktProj);
      pdlg = BackendJob.launchBackendJob(job, getShell());

      if (pdlg.open() != Window.OK) {
        throw new AbstractMarketplaceHandlerException();
      }
    }

    // Gather deployment parameters
    ListOnMarketplaceDialog dlg = new ListOnMarketplaceDialog(
        getShell(), requestFactory, appsMktProj);
    if (dlg.open() != Window.OK) {
      throw new AbstractMarketplaceHandlerException();
    }
  }

  protected void executeLogin(ExecutionEvent event)
      throws AbstractMarketplaceHandlerException {
    if (!GoogleLogin.getInstance().isLoggedIn()) {
      boolean signedIn = GoogleLogin.getInstance().logIn(
          "Listing on AppMarket place requires authentication.");
      if (!signedIn) {
        // user canceled signing in
        throw new AbstractMarketplaceHandlerException();
      }
    }
    requestFactory = GoogleLogin.getInstance().createRequestFactory();

    if (requestFactory == null) {
      throw new AbstractMarketplaceHandlerException();
    }
  }

  protected void executeProjectSelection(ExecutionEvent event)
      throws AbstractMarketplaceHandlerException {
    // Check for dirty editors and prompt to save
    if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
      throw new AbstractMarketplaceHandlerException();
    }
    // Get initial project selection
    project = ActiveProjectFinder.getInstance().getProject();
    if (project == null) {
      project = chooseProject().getProject();
    }
  }

  private IJavaProject chooseProject()
      throws AbstractMarketplaceHandlerException {
    IJavaProject[] javaProjects;
    try {
      javaProjects = JavaCore.create(
          ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
    } catch (JavaModelException e) {
      AppsMarketplacePluginLog.logError(e);
      javaProjects = new IJavaProject[0];
    }
    ILabelProvider labelProvider = new JavaElementLabelProvider(
        JavaElementLabelProvider.SHOW_DEFAULT);
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(), labelProvider);
    dialog.setTitle("Project Selection");
    dialog.setMessage("Choose a project to Deploy to Google Apps Marketplace");
    dialog.setImage(AppsMarketplacePlugin.getDefault().getImage(
        AppsMarketplaceImages.APPS_MARKETPLACE_LIST_SMALL));
    dialog.setElements(javaProjects);
    dialog.setInitialSelections(new Object[] {JavaCore.create(project)});

    dialog.setHelpAvailable(false);
    if (dialog.open() != Window.OK || dialog.getFirstResult() == null) {
      throw new AbstractMarketplaceHandlerException();
    }
    return (IJavaProject) dialog.getFirstResult();
  }

  private Shell getShell() {
    if (window != null) {
      return window.getShell();
    }
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

}
