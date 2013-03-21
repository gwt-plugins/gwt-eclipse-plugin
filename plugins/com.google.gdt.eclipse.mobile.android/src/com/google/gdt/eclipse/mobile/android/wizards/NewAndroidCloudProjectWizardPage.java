/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.gdt.eclipse.mobile.android.wizards;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.preferences.ui.GaePreferencePage;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.appengine.swarm_backend.impl.BackendGenerator;
import com.google.gdt.eclipse.core.sdk.Sdk;

import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.io.File;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * Wizard page where the user specifies the parameters for a new Android and App
 * Engine projects.
 */
@SuppressWarnings("restriction")
public class NewAndroidCloudProjectWizardPage extends WizardPage {

  private static final String ANDROID_PREFERENCE_ID = "com.android.ide.eclipse.preferences.main"; //$NON-NLS-1$

  /**
   * Initial value for all name fields (project, activity, application,
   * package). Used whenever a value is requested before controls are created.
   */
  private static final String INITIAL_NAME = ""; //$NON-NLS-1$
  // Initial value for the Use Default Location check box.
  private static final boolean INITIAL_USE_DEFAULT_LOCATION = true;
  /**
   * Pattern for characters accepted in a project name. Since this will be used
   * as a directory name, we're being a bit conservative on purpose. It cannot
   * start with a space.
   */
  private static final Pattern sProjectNamePattern = Pattern.compile("^[\\w][\\w. -]*$"); //$NON-NLS-1$

  private static final int MSG_ERROR = 2;

  private static final int MSG_NONE = 0;
  private static final int MSG_WARNING = 1;

  public static boolean isAndroidSdkInstalled() {
    return com.android.ide.eclipse.adt.internal.sdk.Sdk.getCurrent() != null;
  }

  // widgets - Android project
  private Text androidProjectNameText;
  private Button browseButton;

  private Link configureAndroidSdkLink;
  private Link configureOrDownloadLink;
  private Label locationLabel;
  private Text locationPathText;

  private Text packageNameText;
  private IAndroidTarget target = null;

  private boolean useDefaultLocation = true;
  private Button useDefaultLocationButton;
  
  private Text gcmProjectNumberText;
  private Text gcmApiKeyText;

  /**
   * Create the wizard.
   */
  public NewAndroidCloudProjectWizardPage() {
    super("New App Engine Connected Android Project");
    setTitle("App Engine Connected Android Project");
    setDescription("Create a sample Android project and a Google App Engine backend project");
  }

  @Override
  public boolean canFlipToNextPage() {
    return false;
  }

  /**
   * Create contents of the wizard.
   * 
   * @param parent
   */
  public void createControl(Composite parent) {

    initializeDialogUnits(parent);

    final Composite container = new Composite(parent, SWT.NONE);
    container.setFont(parent.getFont());
    container.setLayout(new GridLayout());

    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    createInfoSection(container);
    new Label(container, SWT.NONE); // filler
    createProjectNameLocationSection(container);
    new Label(container, SWT.NONE); // filler
    createBackendConfigSection(container);

    createGoogleSdkSection(container);

    validateSdkTarget();
    validatePageComplete();
    setControl(container);
  }

  public String getActivityName() {
    String name = getAndroidProjectName();
    return name.substring(0, 1).toUpperCase() + name.substring(1) + "Activity";
  }

  public String getAndroidApplicationName() {
    String name = getAndroidProjectName();
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  public String getAndroidPackageName() {
    return packageNameText.getText().trim();
  }

  public String getAndroidProjectName() {
    return androidProjectNameText == null ? INITIAL_NAME : androidProjectNameText.getText().trim();
  }

  /**
   * Returns the most recent version of the target.
   */
  public IAndroidTarget getAndroidSdkTarget() {
    if (target != null) {
      return target;
    }
    IAndroidTarget[] targets = null;
    targets = getAndroidSdks();
    if (targets == null) {
      return null;
    }
    target = targets[0];
    if (targets.length > 1) {
      for (int i = 1; i < targets.length; i++) {
        if (targets[i].isPlatform() && targets[i].getVersion().getApiLevel() > 7) {
          if (target.getVersion().getApiLevel() < 8
              || target.getVersion().compareTo(targets[i].getVersion()) > 0) {
            target = targets[i];
          }
        }
      }
    }
    return target;
  }

  /**
   * Returns the current project location path as entered by the user, or its
   * anticipated initial value. Note that if the default has been returned the
   * path in a project description used to create a project should not be set.
   * 
   * @return the project location path or its anticipated initial value.
   */
  public IPath getLocationPath() {
    return new Path(getProjectLocation());
  }
  
  public String getAPIKey() {
    return gcmApiKeyText.getText().trim();
  }
  
  public String getProjectNumber() {
    return gcmProjectNumberText.getText().trim();
  }

  public String getMinSdkVersion() {
    // GCM is supported by api 8 and above
    return "8";
  }

  private void createConfigureAdtSdkLink(Composite parent) {
    configureAndroidSdkLink = new Link(parent, SWT.NONE);
    configureAndroidSdkLink.setText("<a href=\"#\">" + "Configure Android SDK ..." + "</a>");
    configureAndroidSdkLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent e) {
        if (Window.OK == PreferencesUtil.createPreferenceDialogOn(getShell(),
            ANDROID_PREFERENCE_ID, new String[] {ANDROID_PREFERENCE_ID}, null).open()) {
          updateControls();
          validatePageComplete();
        }
      }
    });
    configureAndroidSdkLink.setEnabled(true);
  }

  /**
   * Create link to configure App Engine SDK, if there is no default
   */
  private void createConfigureGaeSdkLink(Composite parent) {
    configureOrDownloadLink = new Link(parent, SWT.NONE);
    configureOrDownloadLink.setText("<a href=\"#\">" + "Configure App Engine SDK ..." + "</a>");
    configureOrDownloadLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent e) {
        if (Window.OK == PreferencesUtil.createPreferenceDialogOn(getShell(), GaePreferencePage.ID,
            new String[] {GaePreferencePage.ID}, null).open()) {
          updateControls();
          validatePageComplete();
        }
      }
    });
    configureOrDownloadLink.setEnabled(true);
  }

  /**
   * Creates the group for Gae/ADT SDK Selection
   */
  private void createGoogleSdkSection(Composite container) {
    boolean gaeSdkAvailable = (getSelectedGaeSdk() != null);
    boolean androidSdkAvailable = isAndroidSdkInstalled();

    if (!gaeSdkAvailable || !androidSdkAvailable) {
      Label filler = new Label(container, SWT.NONE);
    }

    if (!gaeSdkAvailable) {
      createConfigureGaeSdkLink(container);
    }

    if (!androidSdkAvailable) {
      createConfigureAdtSdkLink(container);
    }
  }

  private void createInfoSection(final Composite container) {
    Label infoLabel = new Label(container, SWT.WRAP);
    infoLabel.setText("Creates a sample application that includes an Android mobile client"
        + " connected to a backend service. The backend service runs on App Engine, and it communicates with"
        + " the mobile app via Google Cloud Messaging."
        + "\n\n"
        + "A sample web application that allows you to send a notification to your mobile devices via your backend is also created.");

    infoLabel.setText("This wizard creates a sample application that provides an Android app with an App Engine backend.\n\n"
        + "The Android App registers with the backend via an Endpoints API and receives messages from it via Google Cloud Messaging.\n\n"
        + "The App Engine backend provides an Endpoints API to register Android clients and a Web UI to broadcast messages to clients.");
    
    GridData infoData = new GridData(GridData.FILL_HORIZONTAL);
    infoData.widthHint = convertWidthInCharsToPixels(80);
    infoLabel.setLayoutData(infoData);
  }

  /**
   * Creates the group for the Android project name: [label: "Project Name"]
   * [text field]
   * 
   * @param parent the parent composite
   */
  private final void createProjectNameLocationSection(Composite parent) {
    Composite group = new Composite(parent, SWT.NONE);
    group.setLayout(new GridLayout(2, false));
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    // new project label
    Label label = new Label(group, SWT.NONE);
    label.setText("Project name:");
    label.setFont(parent.getFont());
    label.setToolTipText("Name of the Eclipse project to create. It cannot be empty.");
    // new project name entry field
    androidProjectNameText = new Text(group, SWT.BORDER);
    androidProjectNameText.setToolTipText("Name of the Eclipse project to create. It cannot be empty.");
    androidProjectNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    androidProjectNameText.setFont(parent.getFont());
    androidProjectNameText.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        updateFields(androidProjectNameText.getText().trim());
        validatePageComplete();
      }
    });

    label = new Label(group, SWT.NONE);
    label.setText("Package name:");
    label.setFont(parent.getFont());
    label.setToolTipText("Namespace of the Package to create. This must be a Java namespace with at least two components.");

    // new package name entry field
    packageNameText = new Text(group, SWT.BORDER);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    packageNameText.setToolTipText("Namespace of the Package to create. This must be a Java namespace with at least two components.");
    packageNameText.setLayoutData(data);
    packageNameText.setFont(parent.getFont());
    packageNameText.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        validatePageComplete();
      }
    });

    useDefaultLocationButton = new Button(group, SWT.CHECK);
    useDefaultLocationButton.setText("Use default location");
    useDefaultLocationButton.setSelection(INITIAL_USE_DEFAULT_LOCATION);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1);
    gd.verticalIndent = convertHeightInCharsToPixels(1);
    useDefaultLocationButton.setLayoutData(gd);

    SelectionListener locationListener = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        super.widgetSelected(e);
        useDefaultLocation = useDefaultLocationButton.getSelection();
        updateControls();
        validatePageComplete();
      }
    };

    useDefaultLocationButton.addSelectionListener(locationListener);

    Composite locationGroup = new Composite(group, SWT.NONE);
    locationGroup.setLayout(new GridLayout(3, /* num columns */
    false /* columns of not equal size */));
    locationGroup.setLayoutData(new GridData(SWT.FILL, GridData.BEGINNING, true, false, 2, 1));
    locationGroup.setFont(parent.getFont());

    locationLabel = new Label(locationGroup, SWT.NONE);
    locationLabel.setText("Location:");

    locationPathText = new Text(locationGroup, SWT.BORDER);/* verticalSpan */
    locationPathText.setLayoutData(new GridData(SWT.FILL, GridData.BEGINNING, true, false, 1, 1));
    locationPathText.setFont(parent.getFont());
    String outDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
    locationPathText.setText(outDir);
    locationPathText.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        validatePageComplete();
      }
    });
    locationPathText.setEnabled(false);
    browseButton = new Button(locationGroup, SWT.PUSH);
    browseButton.setText("Browse...");
    setButtonLayoutData(browseButton);
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String dd = onOpenDirectoryBrowser();
        if (dd != null) {
          locationPathText.setText(dd);
          validatePageComplete();
        }
      }
    });
    browseButton.setEnabled(false);
  }
  
  /**
   * Creates a group that allows entering project number and api key
   * @param parent
   */
  private void createBackendConfigSection(Composite parent) {
    Group configGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
    configGroup.setText("Configuration Parameters");
    configGroup.setLayout(new GridLayout(1, false));
    configGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    GridData textGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    textGridData.widthHint = convertWidthInCharsToPixels(80);
    Text configText = new Text(configGroup, SWT.WRAP);
    configText.setLayoutData(textGridData);
    // TODO(appu): this current doesn't show a link and is not selectable, it
    // merely provides the information of the link to go to. Same goes for
    // the content in GenerateBackendDialog.
    configText.setText("These are required for a working example.  " +
        "They can be obtained from the Google API console for your project.  " +
        "If entered now they will be injected into the project. If " +
        "left blank, they can manually entered into code after the " +
        "project is generated. For more information visit " +
        "https://developers.google.com/eclipse/docs/cloud_endpoints.");
    
    Composite formGroup = new Composite(configGroup, SWT.NONE); 
    formGroup.setLayout(new GridLayout(2, false));
    formGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    // api key label
    final String gcmApiTip = "API Key for Google Cloud Messaging. " +
        "This will be injected into the App Engine project. " +
        "Can be left blank and manually entered later";
    Label apiLabel = new Label(formGroup, SWT.NONE);
    apiLabel.setText("API Key:");
    apiLabel.setFont(parent.getFont());
    apiLabel.setToolTipText(gcmApiTip);
    // api key entry field
    gcmApiKeyText = new Text(formGroup, SWT.BORDER);
    gcmApiKeyText.setToolTipText(gcmApiTip);
    gcmApiKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    gcmApiKeyText.setFont(parent.getFont());
    
    // project number
    final String gcmProjectNumberTip = "Project Number for Google Cloud Messaging. " +
        "This will be injected into the Android project. " +
        "Can be left blank and manually entered later";
    Label pnLabel = new Label(formGroup, SWT.NONE);
    pnLabel.setText("Project Number:");
    pnLabel.setFont(parent.getFont());
    pnLabel.setToolTipText(gcmProjectNumberTip);
    // project number entry field
    gcmProjectNumberText = new Text(formGroup, SWT.BORDER);
    gcmProjectNumberText.setToolTipText(gcmProjectNumberTip);
    gcmProjectNumberText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    gcmProjectNumberText.setFont(parent.getFont());
  }

  /**
   * @param targets
   * @return
   */
  private IAndroidTarget[] getAndroidSdks() {
    if (isAndroidSdkInstalled()) {
      return com.android.ide.eclipse.adt.internal.sdk.Sdk.getCurrent().getTargets();
    }
    return null;
  }

  /**
   * Creates a project resource handle for the current project name field value.
   * 
   * @param projectName
   * @return the new project resource handle
   */
  private IProject getProjectHandle(String projectName) {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
  }

  /**
   * Returns the current project location, depending on the Use Default Location
   * check box or the Create From Sample check box.
   */
  private String getProjectLocation() {

    if (useDefaultLocation) {
      return Platform.getLocation().toString();
    } else {
      return locationPathText.getText().trim();
    }
  }

  private GaeSdk getSelectedGaeSdk() {
    return GaePreferences.getDefaultSdk();
  }
  
  /**
   * Display a directory browser and update the location path field with the
   * selected path
   */
  private String onOpenDirectoryBrowser() {

    String existingDir = locationPathText == null ? "" : locationPathText.getText().trim();

    // Disable the path if it doesn't exist
    if (existingDir.length() == 0) {
      existingDir = null;
    } else {
      File f = new File(existingDir);
      if (!f.exists()) {
        existingDir = null;
      }
    }

    DirectoryDialog dd = new DirectoryDialog(locationPathText.getShell());
    dd.setMessage("Browse for folder");
    dd.setFilterPath(existingDir);
    String absDir = dd.open();

    if (absDir != null) {
      return absDir;
    }
    return null;
  }

  private boolean projectExists(String projectName) {
    return getProjectHandle(projectName).exists();
  }

  /**
   * Sets the error message for the wizard with the given message icon.
   * 
   * @param message The wizard message type, one of MSG_ERROR or MSG_WARNING.
   * @return As a convenience, always returns messageType so that the caller can
   *         return immediately.
   */
  private int setStatus(String message, int messageType) {
    if (message == null) {
      setErrorMessage(null);
      setMessage(null);
    } else if (!message.equals(getMessage())) {
      if (messageType == MSG_NONE) {
        setMessage(message, IMessageProvider.NONE);
      } else if (messageType == MSG_ERROR) {
        setMessage(message, IMessageProvider.ERROR);
      } else {
        setMessage(message, IMessageProvider.WARNING);
      }
    }
    return messageType;
  }

  /**
   * Update the various controls
   */
  private void updateControls() {
    // Set the output directory to the workspace
    if (useDefaultLocationButton.getSelection()) {
      locationLabel.setEnabled(false);
      locationPathText.setEnabled(false);
      browseButton.setEnabled(false);
    } else {
      locationLabel.setEnabled(true);
      locationPathText.setEnabled(true);
      browseButton.setEnabled(true);
    }
  }

  /**
   * Updates the fields in the wizard based on project name
   */
  private void updateFields(String name) {
    packageNameText.setText("com." + name.toLowerCase().replaceAll(" ", ""));
    if (useDefaultLocation) {
      String outDir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
      if (getAndroidProjectName().length() > 0) {
        outDir += (System.getProperty("file.separator") + getAndroidProjectName());
      }
      locationPathText.setText(outDir);
    }
  }

  /**
   * Validates the location path field.
   * 
   * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
   */
  private int validateLocationPath(IWorkspace workspace) {
    Path path = new Path(getProjectLocation());

    if (!useDefaultLocation) {
      // If not using the default value validate the location.
      URI uri = URIUtil.toURI(path.toOSString());
      IStatus locationStatus = workspace.validateProjectLocationURI(
          getProjectHandle(getAndroidProjectName()), uri);
      if (!locationStatus.isOK()) {
        return setStatus(locationStatus.getMessage(), MSG_ERROR);
      } else {
        // The location is valid as far as Eclipse is concerned (i.e. mostly not
        // an existing workspace project.) Check it either doesn't exist or is
        // a directory that is empty.
        File f = path.toFile();
        if (f.exists() && !f.isDirectory()) {
          return setStatus("A directory name must be specified.", MSG_ERROR);
        } else if (f.isDirectory()) {
          // However if the directory exists, we should put a warning if it is
          // not
          // empty. We don't put an error (we'll ask the user again for
          // confirmation
          // before using the directory.)
          String[] l = f.list();
          if (l.length != 0) {
            return setStatus("The selected output directory is not empty.", MSG_WARNING);
          }
        }
      }
    } else {
      // Otherwise validate the path string is not empty
      if (getProjectLocation().length() == 0) {
        return setStatus("A directory name must be specified.", MSG_ERROR);
      }
      if (getAndroidProjectName().length() > 0) {
        File dest = path.append(getAndroidProjectName()).toFile();
        if (dest.exists()) {
          return setStatus(String.format(
              "There is already a file or directory named \"%1$s\" in the selected location.",
              getAndroidProjectName()), MSG_ERROR);
        }
      }
    }

    return MSG_NONE;
  }

  /**
   * Validates the package name field.
   * 
   * @param packageFieldContents
   * 
   * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
   */
  private int validatePackageField(String packageFieldContents) {
    // Validate package field
    if (packageFieldContents.length() == 0) {
      return setStatus("Package name must be specified.", MSG_ERROR);
    }

    // Check it's a valid package string
    int result = MSG_NONE;
    IStatus status = JavaConventions.validatePackageName(packageFieldContents, "1.5", "1.5"); //$NON-NLS-1$ $NON-NLS-2$
    if (!status.isOK()) {
      result = setStatus(status.getMessage(), status.getSeverity() == IStatus.ERROR ? MSG_ERROR
          : MSG_WARNING);
    }

    // The Android Activity Manager does not accept packages names with only one
    // identifier. Check the package name has at least one dot in them (the
    // previous rule
    // validated that if such a dot exist, it's not the first nor last
    // characters of the
    // string.)
    if (result != MSG_ERROR && packageFieldContents.indexOf('.') == -1) {
      return setStatus("Package name must have at least two identifiers.", MSG_ERROR);
    }

    return result;
  }

  /**
   * Returns whether this page's controls currently all contain valid values.
   * 
   * @return <code>true</code> if all controls are valid, and <code>false</code>
   *         if at least one is invalid
   */
  private boolean validatePage() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();

    int status = validateProjectField(workspace, getAndroidProjectName());

    if (getAndroidProjectName().length() == 0) {
      return false;
    }

    if ((status & MSG_ERROR) == 0) {
      status |= validateLocationPath(workspace);
    }
    if ((status & MSG_ERROR) == 0) {
      status |= validatePackageField(getAndroidPackageName());
    }

    if ((status & MSG_ERROR) == 0) {
      status |= validateSdks();
    }

    if (status == MSG_NONE) {
      setStatus(null, MSG_NONE);
    }
    // Return false if there's an error so that the finish button be disabled.
    return (status & MSG_ERROR) == 0;
  }

  /**
   * Validates the page and updates the Next/Finish buttons
   */
  private void validatePageComplete() {
    setPageComplete(validatePage());
  }

  /**
   * Validates the project name field.
   * 
   * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
   */
  private int validateProjectField(IWorkspace workspace, String projectName) {

    if (projectName.length() == 0) {
      return setStatus("Enter a project name", MSG_NONE);
    }

    // Limit the project name to shell-agnostic characters since it will be used
    // to generate the final package
    if (!sProjectNamePattern.matcher(projectName).matches()) {
      return setStatus(
          "The project name must start with an alphanumeric characters, followed by one or more alphanumerics, digits, dots, dashes, underscores or spaces.",
          MSG_ERROR);
    }

    IStatus nameStatus = workspace.validateName(projectName, IResource.PROJECT);
    if (!nameStatus.isOK()) {
      return setStatus(nameStatus.getMessage(), MSG_ERROR);
    }

    if (projectExists(projectName)) {
      return setStatus("A project with the name " + projectName
          + " already exists in the workspace", MSG_ERROR);
    }

    String gaeProjectName = BackendGenerator.getAndroidBackendProjectName(projectName);
    if (projectExists(gaeProjectName)) {
      return setStatus("A project with the name " + gaeProjectName
          + " already exists in the workspace", MSG_ERROR);
    }
    return MSG_NONE;
  }

  /**
   * Validates the Gae and Android Sdk selections
   * 
   * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
   */
  private int validateSdks() {
    Sdk selectedGaeSdk = getSelectedGaeSdk();
    if (selectedGaeSdk == null) {
      return setStatus("Please configure an App Engine SDK.", MSG_ERROR);
    }

    IStatus gaeSdkValidationStatus = selectedGaeSdk.validate();
    if (!gaeSdkValidationStatus.isOK()) {
      return setStatus(
          "The selected App Engine SDK is not valid: " + gaeSdkValidationStatus.getMessage(),
          MSG_ERROR);
    }

    if (!isAndroidSdkInstalled()) {
      return setStatus("Please configure an Android SDK.", MSG_ERROR);
    }

    return MSG_NONE;
  }

  /**
   * Validates the sdk target choice.
   * 
   * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
   */
  private int validateSdkTarget() {
    if (isAndroidSdkInstalled()) {
      return MSG_NONE;
    }
    return setStatus("An Android SDK Target must be specified.", MSG_ERROR);
  }

}
