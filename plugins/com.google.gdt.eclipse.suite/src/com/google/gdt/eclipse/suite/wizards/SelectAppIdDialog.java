/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.suite.wizards;

import com.google.gdt.eclipse.appengine.api.AppengineApiWrapper;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import java.io.IOException;

/**
 * This is the Select App Id dialog. It allows the user to select an existing app id or create a new
 * app id using the App Engine Api.
 */
public class SelectAppIdDialog extends TitleAreaDialog {

  private ListViewer appIdViewer;
  private String selectedAppId;
  private Link loginLink;

  /**
   * Constructor
   */
  public SelectAppIdDialog(Shell parentShell) {
    super(parentShell);
  }

  /**
   * Returns the app id that was selected last.
   */
  public String getSelectedAppId() {
    return selectedAppId;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Select App Id");
    setHelpAvailable(false);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    getButton(IDialogConstants.OK_ID).setEnabled(false);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    parent = (Composite) super.createDialogArea(parent);

    // Create group control
    Group listComposite = new Group(parent, SWT.NONE);
    listComposite.setText("App Ids");
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    layout.makeColumnsEqualWidth = false;
    layout.marginHeight = 10;
    layout.marginWidth = 10;
    listComposite.setLayout(layout);

    listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL
        | GridData.FILL_BOTH));

    // Create list of existing app ids
    appIdViewer =
        new ListViewer(listComposite, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.BORDER);
    appIdViewer.setContentProvider(new IStructuredContentProvider() {
      public void dispose() {
        // Do nothing
      }

      public Object[] getElements(Object inputElement) {
        String[] v = (String[]) inputElement;
        return v;
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // Do nothing
      }
    });

    appIdViewer.setInput(getAppIds());
    appIdViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
          IStructuredSelection sselection = (IStructuredSelection) selection;
          selectedAppId = (String) sselection.getFirstElement();
        }
        setCompletionStatus();
      }
    });
    GridDataFactory.fillDefaults().grab(true, true).applyTo(appIdViewer.getControl());

    // NOTE: Disabled for now. See comment in CreateAppIdDialog.
    // Create "Create" button
    // createButton = new Button(listComposite, SWT.PUSH);
    // createButton.setText("Create App Id");
    // final GridData gd1 = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    // createButton.setLayoutData(gd1);
    // createButton.setToolTipText("Create a new App Id");
    // createButton.addSelectionListener(new SelectionAdapter() {
    // @Override
    // public void widgetSelected(SelectionEvent e) {
    // CreateAppIdDialog dialog = new CreateAppIdDialog(getShell());
    // if (dialog.open() == Window.OK) {
    // String newAppId = dialog.getAppId();
    // addNewAppID(newAppId);
    // }
    // }
    // });

    loginLink = new Link(parent, SWT.NONE);
    final GridData loginLinkLayout = new GridData();
    loginLinkLayout.horizontalAlignment = GridData.BEGINNING;
    loginLinkLayout.grabExcessHorizontalSpace = true;
    loginLinkLayout.horizontalSpan = 3;
    loginLinkLayout.horizontalIndent = 5;
    loginLink.setLayoutData(loginLinkLayout);
    loginLink.setToolTipText(AppengineApiWrapper.APPENGINE_CREATE_APP);
    loginLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event ev) {
        userLogInRequest();
      }
    });
    updateLoginMessage();

    updateUserAccessControls();
    return parent;
  }

  // NOTE: Disabled for now. See comment in CreateAppIdDialog.
  // private void addNewAppID(String newAppId) {
  // appIdViewer.add(newAppId);
  //
  // // Select last element
  // Object element = appIdViewer.getElementAt(appIdViewer.getList().getItemCount() - 1);
  // if (element != null) {
  // appIdViewer.setSelection(new StructuredSelection(element), true);
  // }
  // setCompletionStatus();
  // }

  private String[] getAppIds() {
    String[] appIds = {};

    if (!GoogleLogin.getInstance().isLoggedIn()) {
      return appIds;
    }

    AppengineApiWrapper appengineApiWrapper = new AppengineApiWrapper();
    try {
      appIds = appengineApiWrapper.getApplications(false);
    } catch (IOException e) {
      MessageDialog.openError(Display.getDefault().getActiveShell(),
          "Error get list of existing App Ids", e.getMessage());
    }
    return appIds;
  }

  private void setCompletionStatus() {
    if (appIdViewer.getSelection() == null) {
      getButton(IDialogConstants.OK_ID).setEnabled(false);
    } else {
      getButton(IDialogConstants.OK_ID).setEnabled(true);
    }
  }

  private void updateLoginMessage() {
    loginLink.redraw();
    if (GoogleLogin.getInstance().isLoggedIn()) {
      loginLink.setText("You are currently  logged in as " + GoogleLogin.getInstance().getEmail()
          + ". Click " + "<a href=\"\">here</a>" + " to change that.");
    } else {
      loginLink.setText("Click " + "<a href=\"\">here</a>" + " to log in.");
    }
    loginLink.setSize(loginLink.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  private void updateUserAccessControls() {
    if (GoogleLogin.getInstance().isLoggedIn()) {
      // createButton.setEnabled(true);
      if (appIdViewer.getList().getItemCount() == 0) {
        setMessage("You currently have no existing app ids.");
      } else {
        setMessage("Select an app id for a new web application project.");
      }

    } else {
      setMessage("Click the link below to log in to view app ids.");
      // createButton.setEnabled(false);
    }
  }

  private void userLogInRequest() {
    if (GoogleLogin.getInstance().isLoggedIn()) {
      GoogleLogin.getInstance().logOut(false);
    }

    if (GoogleLogin.getInstance().logIn()) {
      appIdViewer.setInput(getAppIds());
      Object element = appIdViewer.getElementAt(0);
      if (element != null) {
        appIdViewer.setSelection(new StructuredSelection(element), true);
      }
    } else {
      appIdViewer.setInput(null);
    }

    updateLoginMessage();
    updateUserAccessControls();
  }
}
