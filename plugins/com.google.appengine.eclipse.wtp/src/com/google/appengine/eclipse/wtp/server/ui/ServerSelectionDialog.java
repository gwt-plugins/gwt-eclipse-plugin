/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.server.ui;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.resources.GaeImages;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.ServerUIPlugin;

/**
 * A dialog showing a list of Servers for user to select.
 */
@SuppressWarnings("restriction")
public final class ServerSelectionDialog extends TitleAreaDialog {

  private IServer[] servers;
  private ListViewer serverViewer;
  private IServer selectedServer;

  public ServerSelectionDialog(IServer[] servers, Shell shell) {
    super(shell);
    this.servers = servers;
  }

  public IServer getSelectedServer() {
    return selectedServer;
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (IDialogConstants.DETAILS_ID == buttonId) {
      showServersView();
      cancelPressed();
    } else {
      super.buttonPressed(buttonId);
    }
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Select server to deploy to Google App Engine");
    setHelpAvailable(false);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    createButton(parent, IDialogConstants.DETAILS_ID, "Open Servers View", false);
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);
    setTitle("Deploy to Google App Engine");
    setTitleImage(AppEngineCorePlugin.getDefault().getImage(GaeImages.APP_ENGINE_DEPLOY_LARGE));
    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(dialogArea, SWT.NONE);
    GridData containerGridData = new GridData(GridData.FILL_BOTH);
    container.setLayoutData(containerGridData);
    container.setLayout(new GridLayout());
    {
      Label serverLabel = new Label(container, SWT.NONE);
      serverLabel.setText("Select server:");
    }
    {
      serverViewer = new ListViewer(container);
      serverViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
      serverViewer.setContentProvider(ArrayContentProvider.getInstance());
      serverViewer.setInput(servers);
    }
    return container;
  }

  @Override
  protected void okPressed() {
    IStructuredSelection selection = (IStructuredSelection) serverViewer.getSelection();
    selectedServer = (IServer) selection.getFirstElement();
    if (selectedServer == null) {
      MessageDialog.openInformation(getShell(), "Selection required", "Please select a server.");
      return;
    }
    super.okPressed();
  }

  /**
   * Show the Servers view.
   */
  private void showServersView() {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        try {
          IWorkbenchWindow workbenchWindow = ServerUIPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow();
          if (workbenchWindow == null) {
            return;
          }
          IWorkbenchPage page = workbenchWindow.getActivePage();
          page.showView("org.eclipse.wst.server.ui.ServersView");
        } catch (Throwable e) {
          AppEnginePlugin.logMessage(e);
        }
      }
    });
  }
}
