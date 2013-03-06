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
package com.google.appengine.eclipse.datatools.ui;

import com.google.appengine.eclipse.datatools.utils.DatatoolsUtils;

import org.eclipse.datatools.connectivity.ConnectionProfileConstants;
import org.eclipse.datatools.connectivity.drivers.DriverInstance;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCDriverDefinitionConstants;
import org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributor;
import org.eclipse.datatools.connectivity.ui.wizards.IDriverUIContributorInformation;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Defines a Google Cloud Sql driver contribution UI to datatools platform.
 */
public class GoogleSqlDriverUIContributer implements IDriverUIContributor,
    Listener, ModifyListener {

  private Label databaseLabel;
  private Text databaseText;

  private Label instanceNameLabel;
  private Text instanceNameText;

  private Label usernameLabel;
  private Text usernameText;

  private Label passwordLabel;

  private Text passwordText;

  private DialogPage parentPage;

  private Properties properties;

  private IDriverUIContributorInformation contributorInformation;

  Composite generalComposite = null;

  public void addListeners() {
    databaseText.addListener(SWT.Modify, this);
    instanceNameText.addListener(SWT.Modify, this);
    usernameText.addListener(SWT.Modify, this);
    passwordText.addListener(SWT.Modify, this);
  }

  public boolean determineContributorCompletion() {
    boolean complete = true;

    if (generalComposite == null) {
      return true;
    }
    if (generalComposite.isDisposed()) {
      return true;
    }

    if (databaseText.getText().trim().length() < 1) {
      parentPage.setErrorMessage("Enter valid database name");
      return false;
    } else if (instanceNameText.getText().trim().isEmpty()) {
      parentPage.setErrorMessage("Enter valid instance name");
      complete = false;
    }
    if (complete) {
      parentPage.setErrorMessage(null);
    }
    return complete;
  }

  public Composite getContributedDriverUI(Composite parent, boolean isReadOnly) {

    if (generalComposite != null && !(generalComposite.isDisposed())) {
      return generalComposite;
    }

    generalComposite = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.makeColumnsEqualWidth = true;
    generalComposite.setLayout(layout);

    GridData gridData;

    databaseLabel = new Label(generalComposite, SWT.NONE);
    databaseLabel.setText("Database name");
    gridData = new GridData();
    gridData.verticalAlignment = GridData.BEGINNING;
    databaseLabel.setLayoutData(gridData);

    databaseText = new Text(generalComposite, SWT.SINGLE | SWT.BORDER);
    gridData = new GridData();
    gridData.verticalAlignment = GridData.BEGINNING;
    gridData.horizontalAlignment = GridData.FILL;
    databaseText.setLayoutData(gridData);

    instanceNameLabel = new Label(generalComposite, SWT.NONE);
    instanceNameLabel.setText("Instance name");
    gridData = new GridData();
    gridData.verticalAlignment = GridData.BEGINNING;
    instanceNameLabel.setLayoutData(gridData);

    instanceNameText = new Text(generalComposite, SWT.SINGLE | SWT.BORDER);
    gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.verticalAlignment = GridData.BEGINNING;
    instanceNameText.setLayoutData(gridData);

    usernameLabel = new Label(generalComposite, SWT.NONE);
    usernameLabel.setText("Username");
    gridData = new GridData();
    gridData.verticalAlignment = GridData.BEGINNING;
    usernameLabel.setLayoutData(gridData);

    usernameText = new Text(generalComposite, SWT.SINGLE | SWT.BORDER);
    gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.verticalAlignment = GridData.BEGINNING;
    usernameText.setLayoutData(gridData);

    passwordLabel = new Label(generalComposite, SWT.NONE);
    passwordLabel.setText("Password");
    gridData = new GridData();
    gridData.verticalAlignment = GridData.BEGINNING;
    passwordLabel.setLayoutData(gridData);

    passwordText = new Text(generalComposite, SWT.SINGLE | SWT.BORDER
        | SWT.PASSWORD);
    gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.verticalAlignment = GridData.BEGINNING;
    passwordText.setLayoutData(gridData);

    loadProperties();
    return generalComposite;
  }

  public List<String[]> getSummaryData() {
    List<String[]> summaryData = new ArrayList<String[]>();
    String driverID = this.properties.getProperty(
      ConnectionProfileConstants.PROP_DRIVER_DEFINITION_ID);
    DriverInstance driverInstance = DriverManager.getInstance().getDriverInstanceByID(
        driverID);

    summaryData.add(new String[] {
        "database name", this.databaseText.getText().trim()});
    summaryData.add(new String[] {
        "instance name", this.instanceNameText.getText().trim()});
    summaryData.add(new String[] {
        "username", this.usernameText.getText().trim()});
    summaryData.add(new String[] {
        "password", this.passwordText.getText().trim()});

    return summaryData;
  }

  public void handleEvent(Event event) {
    determineContributorCompletion();
    setProperties();
  }

  public void loadProperties() {

    if (generalComposite == null) {
      return;
    }
    if (generalComposite.isDisposed()) {
      return;
    }
    if (this.properties == null) {
      return;
    }

    removeListeners();

    String instanceName = this.properties.getProperty(DatatoolsUtils.GOOGLESQL_INSTANCENAME_PROP_ID);
    instanceNameText.setText(instanceName);

    String databaseName = this.properties.getProperty(
      IJDBCDriverDefinitionConstants.DATABASE_NAME_PROP_ID);
    if (databaseName != null) {
      databaseText.setText(databaseName);
    }
    String username = this.properties.getProperty(
      IJDBCDriverDefinitionConstants.USERNAME_PROP_ID);
    if (username != null) {
      usernameText.setText(username);
    }
    String password = this.properties.getProperty(
      IJDBCDriverDefinitionConstants.PASSWORD_PROP_ID);
    if (password != null) {
      passwordText.setText(password);
    }

    String connectionProperties = this.properties.getProperty(
      IJDBCConnectionProfileConstants.CONNECTION_PROPERTIES_PROP_ID);

    addListeners();
  }

  public void modifyText(ModifyEvent e) {
    handleEvent(null);
  }

  public void setDialogPage(DialogPage parentPage) {
    this.parentPage = parentPage;
  }

  public void setDriverUIContributorInformation(
      IDriverUIContributorInformation contributorInformation) {
    this.contributorInformation = contributorInformation;
    this.properties = contributorInformation.getProperties();
  }

  public void setProperties() {

    if (generalComposite == null) {
      return;
    }
    if (generalComposite.isDisposed()) {
      return;
    }

    properties.setProperty(
        IJDBCDriverDefinitionConstants.DATABASE_NAME_PROP_ID,
        this.databaseText.getText().trim());
    properties.setProperty(IJDBCDriverDefinitionConstants.PASSWORD_PROP_ID,
        this.passwordText.getText());
    properties.setProperty(IJDBCDriverDefinitionConstants.USERNAME_PROP_ID,
        this.usernameText.getText());
    properties.setProperty(DatatoolsUtils.GOOGLESQL_INSTANCENAME_PROP_ID,
        this.instanceNameText.getText());
    this.contributorInformation.setProperties(properties);
  }

  protected void removeListeners() {
    databaseText.removeListener(SWT.Modify, this);
    instanceNameText.removeListener(SWT.Modify, this);
    usernameText.removeListener(SWT.Modify, this);
    passwordText.removeListener(SWT.Modify, this);
  }
}
