/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.appengine.eclipse.core.properties.ui;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.datatools.SqlConnectionExtensionPopulator;
import com.google.appengine.eclipse.core.properties.GoogleCloudSqlProperties;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Dialog box to configure the Google Cloud SQL Service for test or prod.
 */
@SuppressWarnings("restriction") // org.eclipse.debug.internal.ui.SWTFactory
public class GoogleCloudSqlConfigure extends StatusDialog {

  private static final int TEXT_WIDTH = 220;

  private boolean isProd;
  private IProject project;
  private IJavaProject javaProject;
  private Text instanceName;
  private Text databaseName;
  private Text databaseUser;
  private Text databasePassword;

  public GoogleCloudSqlConfigure(Shell parent, IJavaProject javaProject, boolean isProd) {
    super(parent);
    this.project = javaProject.getProject();
    this.javaProject = javaProject;
    this.isProd = isProd;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.jface.dialogs.Dialog#create()
   */
  @Override
  public void create() {
    super.create();
    getShell().setText("Configure Google Cloud SQL instance");
  }

  private void addControls(Composite composite) {
    Label instanceNameLabel = new Label(composite, SWT.NONE);
    instanceNameLabel.setText("Instance name");
    instanceNameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    instanceName = new Text(composite, SWT.BORDER);
    instanceName.setLayoutData(new GridData(TEXT_WIDTH, SWT.DEFAULT));
    Label databaseNameLabel = new Label(composite, SWT.NONE);
    databaseNameLabel.setText("Database name");
    databaseNameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    databaseName = new Text(composite, SWT.BORDER);
    databaseName.setLayoutData(new GridData(TEXT_WIDTH, SWT.DEFAULT));
    Label databaseUserLabel = new Label(composite, SWT.NONE);
    databaseUserLabel.setText("Database username");
    databaseUserLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    databaseUser = new Text(composite, SWT.BORDER);
    databaseUser.setLayoutData(new GridData(TEXT_WIDTH, SWT.DEFAULT));
    Label databasePasswordLabel = new Label(composite, SWT.NONE);
    databasePasswordLabel.setText("Database password");
    databasePasswordLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    databasePassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
    databasePassword.setLayoutData(new GridData(TEXT_WIDTH, SWT.DEFAULT));
  }

  private void addEventHandlers() {
    instanceName.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validateFields();
      }
    });
    databaseName.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validateFields();
      }
    });
    databaseUser.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validateFields();
      }
    });
    databasePassword.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validateFields();
      }
    });
  }

  private void initializeControls() {
    if (isProd) {
      instanceName.setText(GoogleCloudSqlProperties.getProdInstanceName(project));
      databaseName.setText(GoogleCloudSqlProperties.getProdDatabaseName(project));
      databaseUser.setText(GoogleCloudSqlProperties.getProdDatabaseUser(project));
      databasePassword.setText(GoogleCloudSqlProperties.getProdDatabasePassword(project));
    } else {
      instanceName.setText(GoogleCloudSqlProperties.getTestInstanceName(project));
      databaseName.setText(GoogleCloudSqlProperties.getTestDatabaseName(project));
      databaseUser.setText(GoogleCloudSqlProperties.getTestDatabaseUser(project));
      databasePassword.setText(GoogleCloudSqlProperties.getTestDatabasePassword(project));
    }
  }

  private void validateFields() {
    IStatus status = StatusUtilities.OK_STATUS;
    if (instanceName.getText().trim().equals("")) {
      status =
          StatusUtilities.newErrorStatus("Enter instance name.", AppEngineCorePlugin.PLUGIN_ID);
    } else if (databaseName.getText().trim().equals("")) {
      status =
          StatusUtilities.newErrorStatus("Enter database name.", AppEngineCorePlugin.PLUGIN_ID);
    }
    updateStatus(status);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets .Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = SWTFactory.createComposite(
        (Composite) super.createDialogArea(parent), 2, 1, SWT.HORIZONTAL);
    addControls(composite);
    addEventHandlers();
    initializeControls();
    updateStatus(StatusUtilities.newInfoStatus(
        "Please enter database details", AppEngineCorePlugin.PLUGIN_ID));
    return composite;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {
    validateFields();
    try {
      if (isProd) {
        GoogleCloudSqlProperties.setProdDatabaseName(project, databaseName.getText().trim());
        GoogleCloudSqlProperties.setProdDatabasePassword(
            project, databasePassword.getText().trim());
        GoogleCloudSqlProperties.setProdDatabaseUser(project, databaseUser.getText().trim());
        GoogleCloudSqlProperties.setProdInstanceName(project, instanceName.getText().trim());
        SqlConnectionExtensionPopulator.populateCloudSQLBridgeExtender(
            javaProject, SqlConnectionExtensionPopulator.ConnectionType.CONNECTION_TYPE_PROD);
        GoogleCloudSqlProperties.setProdIsConfigured(project, true);
      } else {
        GoogleCloudSqlProperties.setTestDatabaseName(project, databaseName.getText().trim());
        GoogleCloudSqlProperties.setTestDatabasePassword(
            project, databasePassword.getText().trim());
        GoogleCloudSqlProperties.setTestDatabaseUser(project, databaseUser.getText().trim());
        GoogleCloudSqlProperties.setTestInstanceName(project, instanceName.getText().trim());
        SqlConnectionExtensionPopulator.populateCloudSQLBridgeExtender(
            javaProject, SqlConnectionExtensionPopulator.ConnectionType.CONNECTION_TYPE_TEST);
        GoogleCloudSqlProperties.setTestIsConfigured(project, true);
      }
    } catch (BackingStoreException e) {
      AppEngineCorePluginLog.logError(e, "Unable to store Google Cloud SQL configurations");
    }
    super.okPressed();
  }
}
