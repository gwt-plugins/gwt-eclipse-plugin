/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.core.properties.ui;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.ILogGaeStats;
import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.preferences.ui.GaePreferencePage;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.properties.GoogleCloudSqlProperties;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.resources.GaeProjectResources;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateProjectSdkCommand;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateWebInfFolderCommand;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.appengine.eclipse.core.sdk.GaeSdkContainer;
import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand.UpdateType;
import com.google.gdt.eclipse.core.ui.AbstractProjectPropertyPage;
import com.google.gdt.eclipse.core.ui.ProjectSdkSelectionBlock;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock.SdkSelection;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock.SdkSelectionListener;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.service.prefs.BackingStoreException;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * App Engine project specific properties page.
 */
@SuppressWarnings("restriction")
public class GaeProjectPropertyPage extends AbstractProjectPropertyPage {

  /**
   * Interface to allow extension points to determine whether this project is
   * eligible for GAE SDK selection.
   */
  public interface GaeSdkSelectionEnablementFinder {
    boolean shouldEnableGaeSdkSelection(IProject project);
  }

  /**
   * We use this dummy subclass to read the application ID and version from a
   * project which may not actually have an App Engine nature. Normally, we can
   * only get a GaeProject by calling the static GaeProject.create(IProject),
   * which requires that the project already have the nature applied.
   */
  private static final class GaeProjectWithoutNature extends GaeProject {

    public GaeProjectWithoutNature(IJavaProject project) {
      super(project);
    }
  }

  public static final String APPENGINE_GOOGLE_CLOUD_SQL_URL =
      "https://code.google.com/apis/sql/docs/developers_guide_java.html";

  // TODO: read from .properties file
  public static final String APPENGINE_LOCAL_HRD_URL =
      "http://code.google.com/appengine/docs/java/datastore/hr/overview.html";

  public static final String ID = AppEngineCorePlugin.PLUGIN_ID + ".gaeProjectPropertyPage";

  private static final String APIS_CONSOLE_URL = "https://code.google.com/apis/console";

  // TODO: read from .properties file
  private static final String APPENGINE_APP_VERSIONS_URL = "http://appengine.google.com/deployment?&app_id=";

  // TODO: read from .properties file
  private static final String APPENGINE_CREATE_APP_URL = "http://appengine.google.com/";

  private static final String CONFIGURE_TEXT = "<a href=\"#\">Configure...</a>";

  private static final int INDENT_TAB = 35;

  private static final String RUN_GROUP_ID = "org.eclipse.debug.ui.launchGroup.run";

  private Link apisConsoleLink;

  private Link appengineCloudSqlConfigureLink;

  private boolean appEngineWebXmlExists;

  private String appId;

  private Text appIdText;

  private boolean configuredMySql;

  private boolean configuredProdCloudSql;

  private boolean configuredTestCloudSql;

  private Group deployGroup;

  private Link existingVersionsLink;

  private Button googleCloudSqlRadio;

  private boolean hrdSupport;

  private String initialAppId;

  private boolean initialUseDatanucleus;

  private boolean initialUseGae;

  private boolean initialUseGoogleCloudSql;

  private boolean initialUseHrd;

  private String initialDatanucleusVersion;

  private String initialVersion;

  private Link myApplicationsLink;

  private Link mySqlConfigureLink;

  private Button mySqlRadio;

  private ProjectSdkSelectionBlock<GaeSdk> sdkSelectionBlock;

  private Link testGoogleCloudSqlConfigureLink;

  private Button useDatanucleusCheckbox;

  private ComboViewer datanucleusVersionCombo;

  private Label useDatanucleusLabel;

  private Label datanucleusVersionLabel;

  private boolean useGae;

  private Button useGaeCheckbox;

  private Button useGoogleCloudSqlCheckbox;

  private Link useGoogleCloudSqlLink;

  private Button useHrdCheckbox;

  private Link useHrdLink;

  private String version;

  private Text versionText;

  public GaeProjectPropertyPage() {
    noDefaultAndApplyButton();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    panel.setLayout(new GridLayout());

    useGaeCheckbox = new Button(panel, SWT.CHECK);
    useGaeCheckbox.setText("Use Google App Engine");

    createSdkComponent(panel);
    createDeployComponent(panel);
    createDatastoreComponent(panel);
    createGoogleCloudSqlComponent(panel);

    recordInitialSettings();
    initializeControls();
    addEventHandlers();
    fieldChanged();

    return panel;
  }

  @Override
  protected void saveProjectProperties() throws BackingStoreException, CoreException, IOException {
    // Only add or remove App Engine if the nature or SDK actually changed
    if (hasNatureChanged() || hasSdkChanged()) {
      if (useGae) {
        addGae();
      } else {
        removeGae();
      }
    }

    if (useGae) {

      saveChangesToAppEngineWebXml();

      if (hasHrdChanged()) {
        GaeProjectProperties.jobSetGaeHrdEnabled(getProject(), useHrdCheckbox.getSelection());
      }

      if (hasDatanucleusChanged()) {
        if (!useDatanucleusCheckbox.getSelection()) {
          BuilderUtilities.removeBuilderFromProject(getProject(), GaeNature.CLASS_ENHANCER_BUILDER);
        } else {
          BuilderUtilities.addBuilderToProject(getProject(), GaeNature.CLASS_ENHANCER_BUILDER);
        }
        GaeProjectProperties.jobSetGaeDatanucleusEnabled(getProject(),
            useDatanucleusCheckbox.getSelection());

        // Will update WEB-INF/lib farther down..
      }

      boolean oldSdkSelected = !sdkSelectionBlock.getSdkSelection()
          .getSelectedSdk().getCapabilities().contains(GaeSdkCapability.OPTIONAL_USER_LIB);
      if (hasSdkChanged() && oldSdkSelected) {
        setDatanucleusVersionAndUpdateClasspath("", true);
        updateJdoConfig("v1");
        updatePersistenceXml("v1");
      } else if (hasDatanucleusVersionChanged() && useDatanucleusCheckbox.getSelection()) {
        String datanucleusVersion = getDatanucleusVersion();
        setDatanucleusVersionAndUpdateClasspath(datanucleusVersion, true);
        updateJdoConfig(datanucleusVersion);
        updatePersistenceXml(datanucleusVersion);
      } else if (hasDatanucleusChanged()) {
        /*
         * If we're here, it means that the user either disabled or enabled
         * datanucleus, and if they enabled it, they did not also modify the DN
         * version at the same time.
         */
        setDatanucleusVersionAndUpdateClasspath(getDatanucleusVersion(),
            useDatanucleusCheckbox.getSelection());
      }
    }

    GoogleCloudSqlProperties.jobSetGoogleCloudSqlEnabled(getProject(),
        useGoogleCloudSqlCheckbox.isEnabled() && useGoogleCloudSqlCheckbox.getSelection());
    GoogleCloudSqlProperties.jobSetLocalDevMySqlEnabled(getProject(), mySqlRadio.getSelection());
  }

  private void addAppengineDevelopmentControls(Composite composite) {
    Composite appengineDevelopmentSection = SWTFactory.createComposite(composite, 2, 1,
        GridData.FILL_HORIZONTAL);
    GridData indent = new GridData(GridData.FILL_HORIZONTAL);
    indent.horizontalIndent = INDENT_TAB;
    appengineDevelopmentSection.setLayoutData(indent);
    Label appengineSqlLabel = new Label(appengineDevelopmentSection, SWT.NONE);
    appengineSqlLabel.setText("App Engine SQL instance");
    appengineSqlLabel.setToolTipText("This is the SQL instance used by the "
        + "application deployed on App Engine");
    appengineSqlLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    appengineCloudSqlConfigureLink = new Link(appengineDevelopmentSection, SWT.NONE);
    GridData setRight = new GridData(SWT.RIGHT, SWT.NONE, false, false);
    appengineCloudSqlConfigureLink.setLayoutData(setRight);
    appengineCloudSqlConfigureLink.setText(CONFIGURE_TEXT);
    appengineCloudSqlConfigureLink.setToolTipText("Configure the Google Cloud "
        + "SQL instance to be used with App Engine");
  }

  private void addAppEngineWebXml(IProject project) {
    if (!WebAppUtilities.isWebApp(project)) {
      return;
    }
    IFile xmlFile = WebAppUtilities.getWebInfSrc(project).getFile("appengine-web.xml");
    appEngineWebXmlExists = xmlFile.exists();
    if (!appEngineWebXmlExists) {
      try {
        ResourceUtils.createFile(
            project.getFullPath().append(
                new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/appengine-web.xml")),
            GaeProjectResources.createAppEngineWebXmlSource(isGWTProject(project)));
        appEngineWebXmlExists = true;
      } catch (CoreException e) {
        AppEngineCorePluginLog.logError(e);
      }
    }
  }

  private void addEventHandlers() {
    useGaeCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fieldChanged();
      }
    });
    appIdText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        fieldChanged();
      }
    });
    versionText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        fieldChanged();
      }
    });

    myApplicationsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        openBrowser(APPENGINE_CREATE_APP_URL);
      }
    });

    existingVersionsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        assert (appId != null);
        openBrowser(APPENGINE_APP_VERSIONS_URL + appId);
      }
    });
    useGoogleCloudSqlLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event ev) {
        BrowserUtilities.launchBrowserAndHandleExceptions(ev.text);
      }
    });
    apisConsoleLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event ev) {
        BrowserUtilities.launchBrowserAndHandleExceptions(ev.text);
      }
    });
    useGoogleCloudSqlCheckbox.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        fieldChanged();
      }
    });
    appengineCloudSqlConfigureLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        GoogleCloudSqlConfigure googleSqlConfigure = new GoogleCloudSqlConfigure(getShell(),
            getJavaProject(), true);
        googleSqlConfigure.create();
        Boolean returnStatus = googleSqlConfigure.open() == Dialog.OK;
        if (!configuredProdCloudSql) {
          configuredProdCloudSql = returnStatus;
        }
        fieldChanged();
      }
    });
    mySqlConfigureLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        MySqlConfigure mySqlConfigure = new MySqlConfigure(getShell(), getJavaProject());
        mySqlConfigure.create();
        Boolean returnStatus = mySqlConfigure.open() == Dialog.OK;
        if (!configuredMySql) {
          configuredMySql = returnStatus;
        }
        fieldChanged();
      }
    });
    mySqlRadio.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fieldChanged();
      }
    });
    googleCloudSqlRadio.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fieldChanged();
      }
    });
    testGoogleCloudSqlConfigureLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        GoogleCloudSqlConfigure googleSqlConfigure = new GoogleCloudSqlConfigure(getShell(),
            getJavaProject(), false);
        googleSqlConfigure.create();
        Boolean returnStaus = googleSqlConfigure.open() == Dialog.OK;
        if (!configuredTestCloudSql) {
          configuredTestCloudSql = returnStaus;
        }
        fieldChanged();
      }
    });
    useDatanucleusCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fieldChanged();
      }
    });
  }

  /**
   * Removes all GAE SDK jars and replaces them with a single container entry,
   * adds GAE nature and optionally the web app nature.
   */
  private void addGae() throws CoreException, IOException, BackingStoreException {
    IProject project = getProject();
    /*
     * Set the appropriate web app project properties if this is a J2EE project.
     * 
     * There can be a collision between different property pages manipulating
     * the same web app properties, but the collision actually works itself out.
     * 
     * Both the GWT and GAE property pages make a call to this method. So, there
     * are no conflicting/differing settings of the web app project properties
     * in this case.
     * 
     * In the event that the GAE/GWT natures are enabled and the Web App
     * property page does not have the "This Project Has a War Directory"
     * setting selected, and that setting is enabled, then the settings on the
     * Web App project page will take precedence (over those settings that are
     * set by this method call).
     * 
     * The gory details as to why have to do with the order of application of
     * the properties for each page (App Engine, Web App, then GWT), and the
     * fact that this method will not make any changes to Web App properties if
     * the project is already a Web App.
     */
    WebAppProjectProperties.maybeSetWebAppPropertiesForDynamicWebProject(project);

    if (sdkSelectionBlock.hasSdkChanged()) {
      updateProjectSdk();
    }

    if (!GaeNature.isGaeProject(project)) {
      GaeNature.addNatureToProject(project);

      // Need to rebuild to get GAE errors to appear
      BuilderUtilities.scheduleRebuild(project);
    }
  }

  private void addLocalDevelopmentControls(Composite composite) {
    GridData indent;
    GridData setRight;
    Composite localDevelopmentSection = SWTFactory.createComposite(composite, 2, 1,
        GridData.FILL_HORIZONTAL);
    indent = new GridData(GridData.FILL_HORIZONTAL);
    indent.horizontalIndent = INDENT_TAB;
    localDevelopmentSection.setLayoutData(indent);

    Label localDevelopmentInstanceLabel = new Label(localDevelopmentSection, SWT.NONE);
    localDevelopmentInstanceLabel.setText(
        "Development SQL instance (used by local development server)");
    localDevelopmentInstanceLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    localDevelopmentInstanceLabel.setToolTipText("This is the SQL instance used"
        + " by the application running on local development server.");

    setRight = new GridData(SWT.RIGHT, SWT.NONE, true, false);
    // Empty label to correct the layout of the UI
    (new Label(localDevelopmentSection, SWT.NONE)).setLayoutData(setRight);

    mySqlRadio = new Button(localDevelopmentSection, SWT.RADIO);
    mySqlRadio.setText("Use MySQL instance");
    indent = new GridData();
    indent.horizontalIndent = INDENT_TAB;
    mySqlRadio.setLayoutData(indent);
    mySqlConfigureLink = new Link(localDevelopmentSection, SWT.NONE);
    setRight = new GridData(SWT.RIGHT, SWT.NONE, true, false);
    mySqlConfigureLink.setLayoutData(setRight);
    mySqlConfigureLink.setText(CONFIGURE_TEXT);
    mySqlConfigureLink.setToolTipText("Configure the local MySQL instance");
    googleCloudSqlRadio = new Button(localDevelopmentSection, SWT.RADIO);
    googleCloudSqlRadio.setText("Use Google Cloud SQL instance");
    indent = new GridData();
    indent.horizontalIndent = INDENT_TAB;
    googleCloudSqlRadio.setLayoutData(indent);
    testGoogleCloudSqlConfigureLink = new Link(localDevelopmentSection, SWT.NONE);
    setRight = new GridData(SWT.RIGHT, SWT.NONE, true, false);
    testGoogleCloudSqlConfigureLink.setLayoutData(setRight);
    testGoogleCloudSqlConfigureLink.setText(CONFIGURE_TEXT);
    testGoogleCloudSqlConfigureLink.setToolTipText("Configure the Google Cloud SQL"
        + " Instance to be used for local development");
  }

  private void createDatastoreComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "Datastore", 4, 1, GridData.FILL_HORIZONTAL);
    group.setLayout(new GridLayout(4, false));
    /*
     * The group is organised in the form of a table with 4 columns. It looks like the following.
     *
     * |-----|--------------------------|-----------|----------------------|
     * | [x] | Enable local HRD support (spans 2)   | Run Configurations...|
     * |-------------------------------------------------------------------|
     * | [ ] | Use Datanucleus JDO/JPA to access the datastore (spans 3)   |
     * |-------------------------------------------------------------------|
     * | => Datanucleus JDO/JPA version | [version] |       <empty>        |
     * |-----|--------------------------|-----------|----------------------|
     */
    useHrdCheckbox = new Button(group, SWT.CHECK);
    GridData useHrdLinkLayout = new GridData(SWT.NONE, SWT.NONE, true, false);
    useHrdLinkLayout.horizontalSpan = 2;
    useHrdLink = new Link(group, SWT.NONE);
    useHrdLink.setLayoutData(useHrdLinkLayout);
    useHrdLink.setText("Enable local <a href=\"" + APPENGINE_LOCAL_HRD_URL + "\">HRD</a> support");
    useHrdLink.setToolTipText(APPENGINE_LOCAL_HRD_URL);
    useHrdLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event ev) {
        BrowserUtilities.launchBrowserAndHandleExceptions(ev.text);
      }
    });

    Link hrdLink = new Link(group, SWT.NONE);
    hrdLink.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
    hrdLink.setText("<a href=\"#\">Run Configurations...</a>");
    hrdLink.setToolTipText("Runtime HRD parameters can be adjusted per run configuration, in the "
        + "App Engine options tab.");
    hrdLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        // Open the run configurations dialog and select the WebApp type.
        ILaunchConfigurationType webAppLaunchType = DebugPlugin.getDefault()
            .getLaunchManager().getLaunchConfigurationType(WebAppLaunchConfiguration.TYPE_ID);

        DebugUITools.openLaunchConfigurationDialogOnGroup(getShell(), new StructuredSelection(
            webAppLaunchType), RUN_GROUP_ID);
      }
    });

    useDatanucleusCheckbox = new Button(group, SWT.CHECK);
    GridData useDatanucleusLabelLayout = new GridData(SWT.NONE, SWT.NONE, true, false);
    useDatanucleusLabelLayout.horizontalSpan = 3;
    useDatanucleusLabel = new Label(group, SWT.NONE);
    useDatanucleusLabel.setLayoutData(useDatanucleusLabelLayout);
    useDatanucleusLabel.setText("Use Datanucleus JDO/JPA to access the datastore");
    useDatanucleusLabel.setToolTipText("Enabling this option imports the Datanucleus JAR files "
        + "into your project so that you can use them to access the datastore via JDO/JPA. It will "
        + "also enable the Datanucleus enhancer to be run automatically. Disable this option if "
        + "you are not using the datastore at all or if you are not accessing the datastore via "
        + "JDO/JPA.");

    GridData datanucleusVersionLabelLayout = new GridData();
    datanucleusVersionLabelLayout.horizontalIndent = INDENT_TAB;
    datanucleusVersionLabelLayout.horizontalSpan = 2;
    datanucleusVersionLabel = new Label(group, SWT.NONE);
    datanucleusVersionLabel.setLayoutData(datanucleusVersionLabelLayout);
    datanucleusVersionLabel.setText("Datanucleus JDO/JPA version:");
    datanucleusVersionCombo = new ComboViewer(group, SWT.READ_ONLY);
    updateDatanucleusVersionComboList();
  }

  private void createDeployComponent(Composite parent) {
    deployGroup = SWTFactory.createGroup(parent, "Deployment", 3, 1, GridData.FILL_HORIZONTAL);

    // Application ID field
    Label appIdLabel = new Label(deployGroup, SWT.NONE);
    appIdLabel.setText("Application ID:");
    appIdText = new Text(deployGroup, SWT.BORDER);
    appIdText.setLayoutData(new GridData(180, SWT.DEFAULT));

    // Link to applications
    myApplicationsLink = new Link(deployGroup, SWT.NONE);
    myApplicationsLink.setText("<a href=\"#\">My applications...</a>");
    GridData createAppLinkGridData = new GridData(SWT.END, SWT.TOP, true, false);
    myApplicationsLink.setLayoutData(createAppLinkGridData);

    // Version field
    Label versionLabel = new Label(deployGroup, SWT.NONE);
    versionLabel.setText("Version:");
    versionText = new Text(deployGroup, SWT.BORDER);
    GridData versionTextGridData = new GridData(60, SWT.DEFAULT);
    versionText.setLayoutData(versionTextGridData);

    // Link to existing versions
    existingVersionsLink = new Link(deployGroup, SWT.NONE);
    GridData seeVersionsLinkGridData = new GridData(SWT.END, SWT.TOP, true, false);
    existingVersionsLink.setLayoutData(seeVersionsLinkGridData);
    existingVersionsLink.setText("<a href=\"#\">Existing versions...</a>");

    // Set tab order to skip links
    deployGroup.setTabList(new Control[] {appIdText, versionText});
  }

  private void createGoogleCloudSqlComponent(Composite parent) {
    Composite panel = SWTFactory.createGroup(parent, "Google Cloud SQL", 1, 1,
        GridData.FILL_HORIZONTAL);

    Composite useGoogleCloudSqlGroup = SWTFactory.createComposite(panel, 3, 1,
        GridData.FILL_HORIZONTAL);
    useGoogleCloudSqlCheckbox = new Button(useGoogleCloudSqlGroup, SWT.CHECK);
    useGoogleCloudSqlLink = new Link(useGoogleCloudSqlGroup, SWT.NONE);
    useGoogleCloudSqlLink.setText("Enable <a href=\"" + APPENGINE_GOOGLE_CLOUD_SQL_URL
        + "\">Google Cloud SQL</a>");
    useGoogleCloudSqlLink.setToolTipText(APPENGINE_GOOGLE_CLOUD_SQL_URL);
    apisConsoleLink = new Link(useGoogleCloudSqlGroup, SWT.NONE);
    apisConsoleLink.setText("<a href=\"" + APIS_CONSOLE_URL + "\">My instances...</a>");
    apisConsoleLink.setToolTipText("Use the Google APIs Console to activate "
        + "Google Cloud SQL for your account and to manage your Google Cloud SQL instances.");
    apisConsoleLink.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, true, false));

    addLocalDevelopmentControls(panel);
    addAppengineDevelopmentControls(panel);
  }

  private void createSdkComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "App Engine SDK", 1, 1, GridData.FILL_HORIZONTAL);

    sdkSelectionBlock = new ProjectSdkSelectionBlock<GaeSdk>(group, SWT.NONE, getJavaProject()) {
      @Override
      protected void doConfigure() {
        if (Window.OK == PreferencesUtil.createPreferenceDialogOn(getShell(), GaePreferencePage.ID,
            new String[] {GaePreferencePage.ID}, null).open()) {
          GaeProjectPropertyPage.this.fieldChanged();
        }
      }

      @Override
      protected GaeSdk doFindSdkFor(IJavaProject javaProject) {
        try {
          return GaeSdk.findSdkFor(javaProject);
        } catch (JavaModelException e) {
          AppEngineCorePluginLog.logError(e);
          return null;
        }
      }

      @Override
      protected String doGetContainerId() {
        return GaeSdkContainer.CONTAINER_ID;
      }

      @Override
      protected SdkManager<GaeSdk> doGetSdkManager() {
        return GaePreferences.getSdkManager();
      }
    };

    sdkSelectionBlock.addSdkSelectionListener(new SdkSelectionListener() {
      public void onSdkSelection(SdkSelectionEvent ev) {
        validateFields();
        updateControls();
      }
    });
  }

  private void fieldChanged() {
    if (isAppEngineWebXmlNeeded()) {
      addAppEngineWebXml(getProject());
    }
    validateFields();
    updateControls();
  }

  private List<String> getDatanucleusLibVersions() {
    GaeSdk sdk = sdkSelectionBlock.getSdkSelection().getSelectedSdk();
    if (sdk != null) {
      // TODO(deepanshu): static final.
      return sdk.getLibVersions("datanucleus");
    } else {
      AppEngineCorePluginLog.logError("sdk is null for " + getJavaProject().toString());
    }
    return null;
  }

  private String getDatanucleusVersion() {
    return (String) ((IStructuredSelection) datanucleusVersionCombo.getSelection())
        .getFirstElement();
  }

  private static String getJdoConfigValue(boolean v1) {
    if (v1) {
      return "org.datanucleus.store.appengine.jdo.DatastoreJDOPersistenceManagerFactory";
    } else {
      return "org.datanucleus.api.jdo.JDOPersistenceManagerFactory";
    }
  }

  private static String getPersitenceValue(boolean v1) {
    if (v1) {
      return "org.datanucleus.store.appengine.jpa.DatastorePersistenceProvider";
    } else {
      return "org.datanucleus.api.jpa.PersistenceProviderImpl";
    }
  }

  private boolean hasDatanucleusChanged() {
    return useDatanucleusCheckbox.getSelection() ^ initialUseDatanucleus;
  }

  private boolean hasDatanucleusVersionChanged() {
    if (!useDatanucleusCheckbox.getSelection()) {
      return false;
    }
    String datanucleusVersion = getDatanucleusVersion();
    if (datanucleusVersion == null || datanucleusVersion.isEmpty()) {
      // This will happen if it is an old project with no version information
      // associated with it and
      // the user has not touched the version select box.
      return false;
    } else {
      return !datanucleusVersion.equals(initialDatanucleusVersion);
    }
  }

  private boolean hasHrdChanged() {
    return useHrdCheckbox.getSelection() ^ initialUseHrd;
  }

  private boolean hasNatureChanged() {
    return useGae ^ initialUseGae;
  }

  private boolean hasSdkChanged() {
    return sdkSelectionBlock.hasSdkChanged();
  }

  private void initializeControls() {
    useGaeCheckbox.setSelection(initialUseGae);
    appIdText.setText(initialAppId);
    versionText.setText(initialVersion);
    useHrdCheckbox.setSelection(initialUseHrd);
    useDatanucleusCheckbox.setSelection(initialUseDatanucleus);
    datanucleusVersionCombo.setSelection(new StructuredSelection(
        new String[] {initialDatanucleusVersion}), true);
    useGoogleCloudSqlCheckbox.setSelection(initialUseGoogleCloudSql);
    mySqlRadio.setEnabled(useGoogleCloudSqlCheckbox.getSelection());
    mySqlRadio.setSelection(GoogleCloudSqlProperties.getLocalDevMySqlEnabled(getProject()));
    googleCloudSqlRadio.setEnabled(useGoogleCloudSqlCheckbox.getSelection());
    googleCloudSqlRadio.setSelection(
        !GoogleCloudSqlProperties.getLocalDevMySqlEnabled(getProject()));
    mySqlConfigureLink.setEnabled(mySqlRadio.getSelection());
    testGoogleCloudSqlConfigureLink.setEnabled(googleCloudSqlRadio.getSelection());
    appengineCloudSqlConfigureLink.setEnabled(useGoogleCloudSqlCheckbox.getSelection());
  }

  private boolean isAppEngineWebXmlNeeded() {
    if (!useGaeCheckbox.getSelection()) {
      return false;
    }

    IConfigurationElement[] extensions = 
        Platform.getExtensionRegistry().getConfigurationElementsFor(
            "com.google.appengine.eclipse.core.appengineWebXml");
    if (extensions.length > 0) {
      // The plugin extending this extension point is responsible for adding the
      // appengine-web.xml. Hence, we don't need to add it.
      return false;
    }
    return true;
  }

  // TODO: This check should be extracted out to com.google.gdt.eclipse.core
  private static boolean isGWTProject(IProject project) {
    try {
      return project.isAccessible()
          && project.hasNature("com.google.gwt.eclipse.core.GWTPlugin.gwtNature");
    } catch (CoreException e) {
      AppEngineCorePluginLog.logError(e);
    }
    return false;
  }

  private void openBrowser(String url) {
    BrowserUtilities.launchBrowserAndHandleExceptions(url);
  }

  private static Document parseXML(String path) {
    Document document = null;
    try {
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      document = docBuilder.parse(path);
    } catch (IOException e) {
      AppEngineCorePluginLog.logError("Unable to read " + path);
    } catch (SAXException e) {
      AppEngineCorePluginLog.logError(path + " is not in the correct format. Left unaltered.");
    } catch (ParserConfigurationException e) {
      AppEngineCorePluginLog.logError(e);
    }
    return document;
  }

  private void recordInitialSettings() {
    initialUseGae = GaeNature.isGaeProject(getProject());
    initialUseHrd = GaeProjectProperties.getGaeHrdEnabled(getProject());
    initialUseDatanucleus = GaeProjectProperties.getGaeDatanucleusEnabled(getProject());
    initialDatanucleusVersion = GaeProjectProperties.getGaeDatanucleusVersion(getProject());
    initialUseGoogleCloudSql = GoogleCloudSqlProperties.getGoogleCloudSqlEnabled(getProject());

    if (WebAppUtilities.isWebApp(getProject())) {
      GaeProject gaeProject = new GaeProjectWithoutNature(getJavaProject());
      appEngineWebXmlExists = (gaeProject.getAppEngineWebXml() != null);
      initialAppId = gaeProject.getAppId();
      initialVersion = gaeProject.getAppVersion();
    } else {
      appEngineWebXmlExists = false;
      initialAppId = "";
      initialVersion = "";
    }

    configuredMySql = GoogleCloudSqlProperties.getMySqlIsConfigured(getProject());
    configuredProdCloudSql = GoogleCloudSqlProperties.getProdIsConfigured(getProject());
    configuredTestCloudSql = GoogleCloudSqlProperties.getTestIsConfigured(getProject());
  }

  private void removeGae() throws BackingStoreException, CoreException {
    GaeNature.removeNatureFromProject(getProject());
    ClasspathUtilities.replaceContainerWithClasspathEntries(getJavaProject(),
        GaeSdkContainer.CONTAINER_ID);
    GaeProjectProperties.setFileNamesCopiedToWebInfLib(getProject(),
        Collections.<String> emptyList());
  }

  private void saveChangesToAppEngineWebXml() throws IOException, CoreException {
    GaeProject gaeProject = GaeProject.create(getProject());

    if (!appId.equals(initialAppId)) {
      gaeProject.setAppId(appId, true);
      ExtensionQuery<GaeProjectChangeExtension> extQuery = new ExtensionQuery<
          GaeProjectChangeExtension>(AppEngineCorePlugin.PLUGIN_ID, "gaeProjectChange", "class");
      List<ExtensionQuery.Data<GaeProjectChangeExtension>> contributors = extQuery.getData();
      for (ExtensionQuery.Data<GaeProjectChangeExtension> c : contributors) {
        GaeProjectChangeExtension data = c.getExtensionPointData();
        data.gaeAppIdChanged(getProject());
      }
    }

    if (!version.equals(initialVersion)) {
      gaeProject.setAppVersion(version, true);
    }
  }

  private void setDatanucleusVersionAndUpdateClasspath(String datanucleusVersion,
      boolean isDatanucleusEnabled) throws BackingStoreException, CoreException, IOException {

    if (isDatanucleusEnabled) {
      GaeProjectProperties.setGaeDatanucleusVersion(getProject(), datanucleusVersion);
      // Ping the update server for logging the usage.
      ExtensionQuery<ILogGaeStats> extQuery = new ExtensionQuery<ILogGaeStats>(
          AppEngineCorePlugin.PLUGIN_ID, "logGaeStats", "class");
      List<ExtensionQuery.Data<ILogGaeStats>> contributors = extQuery.getData();
      for (ExtensionQuery.Data<ILogGaeStats> c : contributors) {
        ILogGaeStats optionalLibPingManager = c.getExtensionPointData();
        optionalLibPingManager.sendDatanucleusLibVersionChangedPing(datanucleusVersion);
      }
    }

    // Rebind any classpath containers that apply to this project
    IClasspathEntry containerEntry = ClasspathUtilities.findClasspathEntryContainer(
        getJavaProject().getRawClasspath(), GaeSdkContainer.CONTAINER_ID);

    if (containerEntry != null) {
      ClasspathContainerInitializer classpathContainerInitializer = 
          JavaCore.getClasspathContainerInitializer(GaeSdkContainer.CONTAINER_ID);
      classpathContainerInitializer.initialize(containerEntry.getPath(), getJavaProject());

    }

    // Update the GAE SDK
    updateProjectSdk();

    /*
     * Explicity force the update of the WEB-INF/lib folder, as changes to the
     * DN properties do not actually modify the .classpath file.
     */
    updateProjectWebInfLibFolder();
  }

  private void updateControls() {

    boolean shouldBeEnabled = useGae;
    ExtensionQuery<GaeProjectPropertyPage.GaeSdkSelectionEnablementFinder> extQuery = 
        new ExtensionQuery<GaeProjectPropertyPage.GaeSdkSelectionEnablementFinder>(
            AppEngineCorePlugin.PLUGIN_ID, "gaeSdkSelectionEnablementFinder", "class");
    List<ExtensionQuery.Data<GaeProjectPropertyPage.GaeSdkSelectionEnablementFinder>> 
      enablementFinders = extQuery.getData();
    for (ExtensionQuery.Data<GaeProjectPropertyPage.GaeSdkSelectionEnablementFinder> 
      enablementFinder : enablementFinders) {
        shouldBeEnabled = shouldBeEnabled
          && enablementFinder.getExtensionPointData().shouldEnableGaeSdkSelection(
              getProject().getProject());
    }

    sdkSelectionBlock.setEnabled(shouldBeEnabled);
    SWTUtilities.setEnabledRecursive(deployGroup, shouldBeEnabled);
    existingVersionsLink.setEnabled(shouldBeEnabled && appId != null && appId.length() > 0);

    // Only enable HRD selection for supported SDKs.
    useHrdCheckbox.setEnabled(useGae && hrdSupport);
    useHrdLink.setEnabled(useGae && hrdSupport);

    useDatanucleusCheckbox.setEnabled(shouldBeEnabled);
    useDatanucleusLabel.setEnabled(shouldBeEnabled);
    boolean datanucleusVersionShouldBeEnabled = shouldBeEnabled
        && useDatanucleusCheckbox.getSelection()
        && sdkSelectionBlock.getSdkSelection().getSelectedSdk() != null
        && sdkSelectionBlock.getSdkSelection().getSelectedSdk().getCapabilities().contains(
            GaeSdkCapability.OPTIONAL_USER_LIB);
    datanucleusVersionLabel.setEnabled(datanucleusVersionShouldBeEnabled);
    datanucleusVersionCombo.getCombo().setEnabled(datanucleusVersionShouldBeEnabled);

    if (hasSdkChanged()) {
      updateDatanucleusVersionComboList();
    }
    useGoogleCloudSqlCheckbox.setEnabled(shouldBeEnabled);
    useGoogleCloudSqlLink.setEnabled(shouldBeEnabled);
    apisConsoleLink.setEnabled(shouldBeEnabled);
    if (useGoogleCloudSqlCheckbox.getEnabled() && useGoogleCloudSqlCheckbox.getSelection()) {
      mySqlRadio.setEnabled(true);
      googleCloudSqlRadio.setEnabled(true);
      mySqlConfigureLink.setEnabled(mySqlRadio.getSelection());
      testGoogleCloudSqlConfigureLink.setEnabled(googleCloudSqlRadio.getSelection());
      appengineCloudSqlConfigureLink.setEnabled(true);
    } else {
      mySqlRadio.setEnabled(false);
      googleCloudSqlRadio.setEnabled(false);
      mySqlConfigureLink.setEnabled(false);
      testGoogleCloudSqlConfigureLink.setEnabled(false);
      appengineCloudSqlConfigureLink.setEnabled(false);
    }
  }

  private void updateDatanucleusVersionComboList() {
    Combo datanucleusCombo = datanucleusVersionCombo.getCombo();
    datanucleusCombo.removeAll();
    List<String> datanucleusLibVersions = getDatanucleusLibVersions();
    if (datanucleusLibVersions == null) {
      datanucleusCombo.setEnabled(false);
    } else {
      datanucleusVersionCombo.add(getDatanucleusLibVersions().toArray());
      datanucleusVersionCombo.setSelection(new StructuredSelection(
          new String[] {initialDatanucleusVersion}), true);
      // Update the size of the Combo. This is needed if the combo didn't have
      // any item and hence
      // was smaller than needed.
      Point newSize = datanucleusCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
      GridData datanucleusVersionComboLayout = new GridData(newSize.x, newSize.y);
      datanucleusCombo.setLayoutData(datanucleusVersionComboLayout);
      datanucleusCombo.redraw();
      datanucleusCombo.getParent().layout();
    }
  }

  private void updateProjectSdk() throws FileNotFoundException, CoreException,
      BackingStoreException {
    SdkSelection<GaeSdk> sdkSelection = sdkSelectionBlock.getSdkSelection();
    boolean isDefault = false;
    GaeSdk newSdk = null;
    if (sdkSelection != null) {
      newSdk = sdkSelection.getSelectedSdk();
      isDefault = sdkSelection.isDefault();
    }

    GaeSdk oldSdk = sdkSelectionBlock.getInitialSdk();

    UpdateType updateType = AppEngineUpdateProjectSdkCommand.computeUpdateType(
        sdkSelectionBlock.getInitialSdk(), newSdk, isDefault);

    /*
     * Update the project classpath which will trigger the <WAR>/WEB-INF/lib
     * jars to be updated, if the WAR output directory is managed
     */
    new AppEngineUpdateProjectSdkCommand(getJavaProject(), oldSdk, newSdk, updateType, null)
      .execute();
  }

  private void updateProjectWebInfLibFolder() throws CoreException, BackingStoreException,
      FileNotFoundException {
    IJavaProject javaProject = getJavaProject();
    GaeSdk sdk = GaeSdk.findSdkFor(javaProject);
    (new AppEngineUpdateWebInfFolderCommand(javaProject, sdk)).execute();
  }

  private void updateJdoConfig(String datanucleusVersion) {
    try {
      // TODO(rdayal): Don't hardcode this value. Look through the project's source paths. 
      String jdoconfigPath = getProject()
          .getLocation().append("src/META-INF/jdoconfig.xml").toOSString();
      Document document = parseXML(jdoconfigPath);
      if (document == null) {
        // The file doesn't exist. Do nothing.
        return;
      }
      NodeList nodes = document.getDocumentElement().getElementsByTagName("property");
      if (nodes == null || nodes.getLength() < 1) {
        AppEngineCorePluginLog.logError("jdoconfig.xml is invalid. left unaltered.");
        return;
      }
      boolean v1 = datanucleusVersion.isEmpty() || datanucleusVersion.compareTo("v1") == 0;
      for (int i = 0; i < nodes.getLength(); ++i) {
        Node node = nodes.item(i);
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
          // Ignore this element and proceed.
          continue;
        }
        Node attribute = attributes.getNamedItem("name");
        if (attribute == null) {
          // Ignore this element and proceed.
          continue;
        }
        String name = attribute.getNodeValue();
        if (name.compareTo("javax.jdo.PersistenceManagerFactoryClass") == 0) {
          Node value = attributes.getNamedItem("value");
          if (value != null) {
            value.setNodeValue(getJdoConfigValue(v1));
          } else {
            // This means that the xml file has been altered by hand and this
            // value has been removed. We add it back for the user.
            Attr attr = document.createAttribute("value");
            attr.setValue(getJdoConfigValue(v1));
            attributes.setNamedItem(attr);
          }
        }
      }
      writeXML(document, jdoconfigPath);
    } catch (DOMException e) {
      AppEngineCorePluginLog.logError(
          "jdoconfig.xml is not in the correct format. Left unaltered.");
    }
  }

  private void updatePersistenceXml(String datanucleusVersion) {
    try {
      // TODO(rdayal): Don't hardcode this value. Look through the project's source paths. 
      String persistenceXmlPath = getProject()
          .getLocation().append("src/META-INF/persistence.xml").toOSString();
      Document document = parseXML(persistenceXmlPath);
      if (document == null) {
        // The file doesn't exist. Do nothing.
        return;
      }
      NodeList nodes = document.getDocumentElement().getElementsByTagName("provider");
      if (nodes == null || nodes.getLength() < 1) {
        AppEngineCorePluginLog.logError("persistence.xml is invalid. left unaltered.");
        return;
      }
      boolean v1 = datanucleusVersion.isEmpty() || datanucleusVersion.compareTo("v1") == 0;
      nodes.item(0).setTextContent(getPersitenceValue(v1));
      writeXML(document, persistenceXmlPath);
    } catch (DOMException e) {
      AppEngineCorePluginLog.logError(
          "jdoconfig.xml is not in the correct format. Left unaltered.");
    }
  }

  private IStatus validateAppId() {
    appId = null;

    if (!useGae) {
      // Ignore this field since App Engine is not enabled
      return StatusUtilities.OK_STATUS;
    }

    String enteredAppId = appIdText.getText().trim();
    if (enteredAppId.length() > 0) {
      if (!appEngineWebXmlExists) {
        return StatusUtilities.newErrorStatus(
            "Cannot set application ID (appengine-web.xml is missing)",
            AppEngineCorePlugin.PLUGIN_ID);
      }
    }

    // Set the app ID now; any problems after this point are just warnings
    appId = enteredAppId;
    return StatusUtilities.OK_STATUS;
  }

  private void validateFields() {
    IStatus useGaeStatus = validateUseGae();
    IStatus sdkStatus = validateSdk();
    IStatus appIdStatus = validateAppId();
    IStatus versionStatus = validateVersion();
    IStatus hrdStatus = validateHrd();
    IStatus googleCloudSqlStatus = validateGoogleCloudSql();

    updateStatus(new IStatus[] {
        useGaeStatus, sdkStatus, appIdStatus, versionStatus, hrdStatus, googleCloudSqlStatus});
  }

  private IStatus validateGoogleCloudSql() {
    GaeSdk sdk = sdkSelectionBlock.getSdkSelection().getSelectedSdk();
    boolean googleCloudSqlSupport = sdk != null
        && sdk.getCapabilities().contains(GaeSdkCapability.GOOGLE_CLOUD_SQL);

    if (!useGoogleCloudSqlCheckbox.getSelection()) {
      // Ignore any configuration problems since Google Cloud SQL is not enabled
      return StatusUtilities.OK_STATUS;
    }

    if (!googleCloudSqlSupport) {
      return StatusUtilities.newErrorStatus("The selected SDK does not support Google Cloud SQL.",
          AppEngineCorePlugin.PLUGIN_ID);
    }
    if (mySqlRadio.getSelection() && !configuredMySql) {
      return StatusUtilities.newErrorStatus(
          "Configure MySQL instance for local development server.", AppEngineCorePlugin.PLUGIN_ID);
    }
    if (googleCloudSqlRadio.getSelection() && !configuredTestCloudSql) {
      return StatusUtilities.newErrorStatus(
          "Configure Google Cloud SQL instance for local development server.",
          AppEngineCorePlugin.PLUGIN_ID);
    }
    if (!configuredProdCloudSql) {
      return StatusUtilities.newErrorStatus("Configure Google Cloud SQL for App Engine.",
          AppEngineCorePlugin.PLUGIN_ID);
    }
    return StatusUtilities.OK_STATUS;
  }

  private IStatus validateHrd() {
    GaeSdk sdk = sdkSelectionBlock.getSdkSelection().getSelectedSdk();
    if (sdk != null) {
      hrdSupport = sdk.getCapabilities().contains(GaeSdkCapability.HRD);
      if (!hrdSupport) {
        return StatusUtilities.newInfoStatus("The selected SDK does not support HRD.",
            AppEngineCorePlugin.PLUGIN_ID);
      }
    }

    return StatusUtilities.OK_STATUS;
  }

  private IStatus validateSdk() {
    if (!useGae) {
      // Ignore any SDK problems since App Engine is not enabled
      return StatusUtilities.OK_STATUS;
    }

    if (sdkSelectionBlock.getSdks().isEmpty()) {
      return StatusUtilities.newErrorStatus("Please configure an SDK",
          AppEngineCorePlugin.PLUGIN_ID);
    }

    return StatusUtilities.OK_STATUS;
  }

  private IStatus validateUseGae() {
    useGae = useGaeCheckbox.getSelection();
    return StatusUtilities.OK_STATUS;
  }

  private IStatus validateVersion() {
    version = null;

    if (!useGae) {
      // Ignore this field since App Engine is not enabled
      return StatusUtilities.OK_STATUS;
    }

    String enteredVersion = versionText.getText().trim();

    if (!enteredVersion.matches("[a-zA-Z0-9-]*")) {
      return StatusUtilities.newErrorStatus(
          "Invalid version number. Only letters, digits and hyphen allowed.",
          AppEngineCorePlugin.PLUGIN_ID);
    }

    if (enteredVersion.length() > 0 && !appEngineWebXmlExists) {
      return StatusUtilities.newErrorStatus("Cannot set version (appengine-web.xml is missing)",
          AppEngineCorePlugin.PLUGIN_ID);
    }

    version = enteredVersion;
    return StatusUtilities.OK_STATUS;
  }

  private static void writeXML(Document document, String path) {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(new DOMSource(document), new StreamResult(new Path(path).toFile()));
    } catch (IllegalArgumentException e) {
      // Not possible but just in case.
      AppEngineCorePluginLog.logError(e);
    } catch (TransformerConfigurationException e) {
      AppEngineCorePluginLog.logError(e);
    } catch (TransformerException e) {
      AppEngineCorePluginLog.logError(e);
    } catch (TransformerFactoryConfigurationError e) {
      AppEngineCorePluginLog.logError(e);
    }
  }
}
