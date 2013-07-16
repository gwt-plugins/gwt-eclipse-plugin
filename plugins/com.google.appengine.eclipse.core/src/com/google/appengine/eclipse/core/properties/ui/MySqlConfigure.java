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
package com.google.appengine.eclipse.core.properties.ui;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.datatools.SqlConnectionExtensionPopulator;
import com.google.appengine.eclipse.core.properties.GoogleCloudSqlProperties;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Dialog box to configure MySQL instance for local development.
 */
@SuppressWarnings("restriction") // org.eclipse.debug.internal.ui.SWTFactory
public class MySqlConfigure extends StatusDialog {

  private static final String[] JDBC_FILE_FILTER_EXTENSIONS = new String[] {"*.jar"};
  private static final int TEXT_WIDTH = 220;
  private static final String MYSQL_JDBC_JAR_URL = "http://dev.mysql.com/downloads/connector/j/";

  private Text hostName;
  private Text databaseName;
  private Text port;
  private Text databaseUser;
  private Text databasePassword;
  private Text jdbcJar;
  private Link jdbcJarLink;
  private IProject project;
  private IJavaProject javaProject;

  public MySqlConfigure(Shell shell, IJavaProject javaProject) {
    super(shell);
    this.project = javaProject.getProject();
    this.javaProject = javaProject;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.jface.dialogs.Dialog#create()
   */
  @Override
  public void create() {
    super.create();
    getShell().setText("Configure MySQL instance");
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
   * .Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = SWTFactory.createComposite(
        (Composite) super.createDialogArea(parent), 2, 1, SWT.HORIZONTAL);
    addControls(composite);
    initializeControls();
    addEventHandlers();
    updateStatus(StatusUtilities.newInfoStatus("Please enter database details",
        AppEngineCorePlugin.PLUGIN_ID));
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
      GoogleCloudSqlProperties.setMySqlHostName(project,
          hostName.getText().trim());
      GoogleCloudSqlProperties.setMySqlDatabaseName(project,
          databaseName.getText().trim());
      GoogleCloudSqlProperties.setMySqlPort(project,
          Integer.parseInt(port.getText().trim()));
      GoogleCloudSqlProperties.setMySqlDatabaseUser(project,
          databaseUser.getText().trim());
      GoogleCloudSqlProperties.setMySqlDatabasePassword(project,
          databasePassword.getText().trim());
      GoogleCloudSqlProperties.setMySqlJdbcJar(project,
          jdbcJar.getText().trim());
      SqlConnectionExtensionPopulator.populateCloudSQLBridgeExtender(javaProject,
          SqlConnectionExtensionPopulator.ConnectionType.CONNECTION_TYPE_LOCAL_MYSQL);
      GoogleCloudSqlProperties.setMySqlIsConfigured(project, true);
    } catch (NumberFormatException e) {
      // This case is taken care by validateFields().
      AppEngineCorePluginLog.logError(e,
          "Incorrect port. Should have been taken care by validateFields()");
    } catch (BackingStoreException e) {
      AppEngineCorePluginLog.logError(e,
          "Unable to save MySQL Configurations for local development Instance.");
    }

    super.okPressed();
  }

  /**
   * @param composite The composite to add controls to.
   */
  private void addControls(Composite composite) {
    Label hostNameLabel = new Label(composite, SWT.NONE);
    hostNameLabel.setText("Hostname");
    hostNameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    hostName = new Text(composite, SWT.BORDER);
    hostName.setLayoutData(new GridData(TEXT_WIDTH, SWT.DEFAULT));
    Label databaseNameLabel = new Label(composite, SWT.NONE);
    databaseNameLabel.setText("Database name");
    databaseNameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    databaseName = new Text(composite, SWT.BORDER);
    databaseName.setLayoutData(new GridData(TEXT_WIDTH, SWT.DEFAULT));
    Label portLabel = new Label(composite, SWT.NONE);
    portLabel.setText("Port number");
    portLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    port = new Text(composite, SWT.BORDER);
    port.setLayoutData(new GridData(TEXT_WIDTH, SWT.DEFAULT));
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
    jdbcJarLink = new Link(composite, SWT.NONE);
    jdbcJarLink.setText("Path to <a href=\"" + MYSQL_JDBC_JAR_URL
        + "\">MySQL JDBC JAR</a>");
    jdbcJarLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    Composite jdbcJarComposite = SWTFactory.createComposite(composite, 2, 1,
        SWT.HORIZONTAL);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.horizontalSpacing = 0;
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    jdbcJarComposite.setLayout(gridLayout);
    jdbcJar = new Text(jdbcJarComposite, SWT.BORDER);
    jdbcJar.setLayoutData(new GridData(170, SWT.DEFAULT));
    Button browseButton = new Button(jdbcJarComposite, SWT.NONE);
    browseButton.setLayoutData(new GridData());
    browseButton.setText("&Browse...");
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String jarPath = browseForJar();
        if (jarPath != null) {
          jdbcJar.setText(jarPath);
        }
      }
    });
  }

  private void addEventHandlers() {
    hostName.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validateFields();
      }
    });
    databaseName.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validateFields();
      }
    });
    port.addModifyListener(new ModifyListener() {
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
    jdbcJar.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validateFields();
      }
    });
    jdbcJarLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event ev) {
        BrowserUtilities.launchBrowserAndHandleExceptions(ev.text);
      }
    });
  }

  private String browseForJar() {
    FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
    fileDialog.setFilterPath(jdbcJar.getText().trim());
    fileDialog.setFilterExtensions(JDBC_FILE_FILTER_EXTENSIONS);

    return fileDialog.open();
  }

  private void initializeControls() {
    hostName.setText(GoogleCloudSqlProperties.getMySqlHostName(project));
    databaseName.setText(GoogleCloudSqlProperties.getMySqlDatabaseName(project));
    port.setText(Integer.toString(GoogleCloudSqlProperties.getMySqlPort(project)));
    databaseUser.setText(GoogleCloudSqlProperties.getMySqlDatabaseUser(project));
    databasePassword.setText(GoogleCloudSqlProperties.getMySqlDatabasePassword(project));
    jdbcJar.setText(GoogleCloudSqlProperties.getMySqlJdbcJar(project));
  }

  private void validateFields() {
    IStatus status = StatusUtilities.OK_STATUS;
    try {
      if (hostName.getText().trim().equals("")) {
        status = StatusUtilities.newErrorStatus("Enter hostname.", AppEngineCorePlugin.PLUGIN_ID);
      } else if (databaseName.getText().trim().equals("")) {
        status = StatusUtilities.newErrorStatus(
            "Enter database name.", AppEngineCorePlugin.PLUGIN_ID);
      } else if (port.getText().trim().equals("")) {
        status = StatusUtilities.newErrorStatus(
            "Enter port number.", AppEngineCorePlugin.PLUGIN_ID);
      } else {
        int x = Integer.parseInt(port.getText().trim());
        if (x >= 65536 || x < 0) {
          status = StatusUtilities.newErrorStatus("Enter a correct port number.",
              AppEngineCorePlugin.PLUGIN_ID);
        } else if (jdbcJar.getText().trim().isEmpty()) {
          status = StatusUtilities.newErrorStatus(
              "Enter JDBC Jar path.", AppEngineCorePlugin.PLUGIN_ID);
        } else {
          IPath jdbcPath = Path.fromOSString(jdbcJar.getText().trim());
          if (!jdbcPath.toFile().exists()) {
            status = StatusUtilities.newErrorStatus("JDBC Jar path does not exist.",
                AppEngineCorePlugin.PLUGIN_ID);
          }
        }
      }
    } catch (NumberFormatException e) {
      status = StatusUtilities.newErrorStatus("Enter a correct port number.",
          AppEngineCorePlugin.PLUGIN_ID);
    }
    updateStatus(status);
  }
}
