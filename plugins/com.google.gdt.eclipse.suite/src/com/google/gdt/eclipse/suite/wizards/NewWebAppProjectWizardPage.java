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
package com.google.gdt.eclipse.suite.wizards;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.preferences.ui.GaePreferencePage;
import com.google.appengine.eclipse.core.properties.ui.GaeProjectPropertyPage;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkContainer;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock.SdkSelection;
import com.google.gdt.eclipse.platform.ui.PixelConverterFactory;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.preferences.ui.GwtPreferencePage;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard page where the user specifies the parameters for a new GWT project.
 */
public class NewWebAppProjectWizardPage extends WizardPage {
  /**
   * Select a {@link GaeSdk} from the set of {@link Sdk} known to the workspace.
   */
  private final class GaeWorkspaceSdkSelectionBlock extends
      SdkSelectionBlock<GaeSdk> {
    private GaeWorkspaceSdkSelectionBlock(Composite parent, int style) {
      super(parent, style);

      updateSdkBlockControls();
      initializeSdkComboBox();

      setSelection(-1);
    }

    @Override
    protected void doConfigure() {
      if (Window.OK == PreferencesUtil.createPreferenceDialogOn(getShell(),
          GaePreferencePage.ID, new String[] {GaePreferencePage.ID}, null).open()) {
        NewWebAppProjectWizardPage.this.updateControls();
      }
    }

    @Override
    protected GaeSdk doGetDefaultSdk() {
      return GaePreferences.getDefaultSdk();
    }

    @Override
    protected List<GaeSdk> doGetSpecificSdks() {
      return new ArrayList<GaeSdk>(GaePreferences.getSdks());
    }
  }

  /**
   * Select a GWT {@link Sdk} based on the set of {@link Sdk} known to the
   * workspace.
   */
  private final class GwtWorkspaceSdkSelectionBlock extends
      SdkSelectionBlock<GWTRuntime> {
    private GwtWorkspaceSdkSelectionBlock(Composite parent, int style) {
      super(parent, style);

      updateSdkBlockControls();
      initializeSdkComboBox();
      setSelection(-1);
    }

    @Override
    protected void doConfigure() {
      if (Window.OK == PreferencesUtil.createPreferenceDialogOn(getShell(),
          GwtPreferencePage.ID, new String[] {GwtPreferencePage.ID}, null).open()) {
        NewWebAppProjectWizardPage.this.updateControls();
      }
    }

    @Override
    protected GWTRuntime doGetDefaultSdk() {
      return GWTPreferences.getDefaultRuntime();
    }

    @Override
    protected List<GWTRuntime> doGetSpecificSdks() {
      return new ArrayList<GWTRuntime>(GWTPreferences.getSdks());
    }
  }

  private final List<String> existingProjectNames = new ArrayList<String>();

  private SdkSelectionBlock<GaeSdk> gaeSelectionBlock;

  private Link gaeHrdLink;

  private SdkSelectionBlock<GWTRuntime> gwtSelectionBlock;

  private Button outDirBrowseButton;

  private String outDirCustom = "";

  private Button outDirCustomButton;

  private Label outDirLabel;

  private Text outDirText;

  private Button outDirWorkspaceButton;

  private Text packageText;

  private Text projectNameText;

  private Button useGaeCheckbox;

  private Button useGwtCheckbox;

  private final String workspaceDirectory;

  private Button generateSampleCodeCheckbox;

  private Button appsMarketplaceCheckbox;

  public NewWebAppProjectWizardPage() {
    super("createProject");
    setTitle("Create a Web Application Project");
    setDescription("Create a Web Application project in the workspace or in an external location");

    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    workspaceDirectory = workspaceRoot.getLocation().toOSString();

    for (IProject project : workspaceRoot.getProjects()) {
      existingProjectNames.add(project.getName());
    }
  }

  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    final GridLayout gridLayout = new GridLayout();
    container.setLayout(gridLayout);
    setControl(container);

    // TODO: convert these fields to use StringDialogField instead?

    // Project name
    final Label projectNameLabel = new Label(container, SWT.NONE);
    projectNameLabel.setText("Project name:");

    projectNameText = new Text(container, SWT.BORDER);
    final GridData gd1 = new GridData(GridData.FILL_HORIZONTAL);
    gd1.horizontalSpan = 2;
    projectNameText.setLayoutData(gd1);
    projectNameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateControls();
      }
    });

    // Package
    final Label packageLabel = new Label(container, SWT.NONE);
    packageLabel.setText("Package: (e.g. com.example.myproject)");

    packageText = new Text(container, SWT.BORDER);
    final GridData gd2 = new GridData(GridData.FILL_HORIZONTAL);
    gd2.horizontalSpan = 2;
    packageText.setLayoutData(gd2);
    packageText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateControls();
      }
    });

    createLocationGroup(container);

    createGoogleSdkGroup(container);

    createAppsMarketplaceGroup(container);

    createOtherOptionsGroup(container);

    final ScrolledComposite scroller =
        new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
    scroller.setExpandHorizontal(true);
    scroller.setExpandVertical(true);
    scroller.setContent(container);
    scroller.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

    updateControls();
    projectNameText.forceFocus();
  }

  public String getCreationLocation() {
    if (outDirWorkspaceButton.getSelection()) {
      // Use the default location.
      return null;
    }
    return getOutputDirectory();
  }

  public URI getCreationLocationURI() {
    if (outDirWorkspaceButton.getSelection()) {
      // Default location is the workspace URI
      return ResourcesPlugin.getWorkspace().getRoot().getLocationURI();
    }

    return new Path(getOutputDirectory()).toFile().toURI();
  }

  public String getPackage() {
    return packageText.getText().trim();
  }

  public String getProjectName() {
    return projectNameText.getText().trim();
  }

  // TODO: Have this return the SDK object
  public GaeSdk getSelectedGaeSdk() {
    if (useGaeCheckbox.getSelection()) {
      SdkSelection<GaeSdk> sdkSelection = gaeSelectionBlock.getSdkSelection();
      if (sdkSelection != null) {
        return sdkSelection.getSelectedSdk();
      }
    }

    return null;
  }

  public GWTRuntime getSelectedGwtSdk() {
    if (useGwtCheckbox.getSelection()) {
      SdkSelection<GWTRuntime> sdkSelection = gwtSelectionBlock.getSdkSelection();
      if (sdkSelection != null) {
        return sdkSelection.getSelectedSdk();
      }
    }

    return null;
  }

  public boolean isAppsMarketplaceSupported() {
    return appsMarketplaceCheckbox.getSelection();
  }

  public boolean isDefaultGaeSdk() {
    return gaeSelectionBlock.isDefault();
  }

  public boolean isDefaultGwtSdk() {
    return gwtSelectionBlock.isDefault();
  }

  public boolean isGenerateEmptyProject() {
    return !generateSampleCodeCheckbox.getSelection();
  }

  IPath getGaeSdkContainerPath() {
    return getSdkContainerPath(gaeSelectionBlock.getSdkSelection(),
        GaeSdkContainer.CONTAINER_ID);
  }

  IPath getGWTSdkContainerPath() {
    return getSdkContainerPath(gwtSelectionBlock.getSdkSelection(),
        GWTRuntimeContainer.CONTAINER_ID);
  }

  IPath getSdkContainerPath(SdkSelection<? extends Sdk> sdkSelection,
      String containerId) {
    if (sdkSelection != null) {
      return SdkClasspathContainer.computeContainerPath(containerId,
          sdkSelection.getSelectedSdk(), sdkSelection.isDefault()
              ? SdkClasspathContainer.Type.DEFAULT
              : SdkClasspathContainer.Type.NAMED);
    }
    return null;
  }

  boolean useGae() {
    return useGaeCheckbox.getSelection();
  }

  boolean useGWT() {
    return useGwtCheckbox.getSelection();
  }

  private String browseForOutputDirectory() {
    DirectoryDialog dlg = new DirectoryDialog(getShell(), SWT.OPEN);
    dlg.setFilterPath(getOutputDirectory());
    dlg.setMessage("Choose a directory for the project contents:");

    return dlg.open();
  }

  private int convertValidationSeverity(int severity) {
    switch (severity) {
      case IStatus.ERROR:
        return ERROR;
      case IStatus.WARNING:
        return WARNING;
      case IStatus.INFO:
      default:
        return NONE;
    }
  }

  private void createAppsMarketplaceGroup(Composite container) {
    // Google Apps Marketplace options group
    final Group marketplaceGroup = new Group(container, SWT.NULL);
    marketplaceGroup.setText("Google Apps Marketplace");
    final GridData gd3 = new GridData(GridData.FILL_HORIZONTAL);
    gd3.horizontalSpan = 2;
    marketplaceGroup.setLayoutData(gd3);

    final GridLayout otherGridLayout = new GridLayout();
    otherGridLayout.numColumns = 3;
    marketplaceGroup.setLayout(otherGridLayout);

    appsMarketplaceCheckbox = new Button(marketplaceGroup, SWT.CHECK);
    appsMarketplaceCheckbox.setText(
        "Add support for listing on Google Apps Marketplace");
  }

  private void createGaeSdkGroup(Group googleSdkGroup,
      SelectionListener useSdkCheckboxSelectionListener, int widthIndent) {
    useGaeCheckbox = new Button(googleSdkGroup, SWT.CHECK);
    useGaeCheckbox.addSelectionListener(useSdkCheckboxSelectionListener);
    useGaeCheckbox.setText("Use Google App Engine");
    useGaeCheckbox.setSelection(true);

    gaeSelectionBlock = new GaeWorkspaceSdkSelectionBlock(googleSdkGroup,
        SWT.NONE);

    gaeSelectionBlock.addSdkSelectionListener(new SdkSelectionBlock.SdkSelectionListener() {
      public void onSdkSelection(SdkSelectionEvent e) {
        updateControls();
      }
    });
    ((GridData) gaeSelectionBlock.getLayoutData()).horizontalIndent = widthIndent;

    gaeHrdLink = new Link(googleSdkGroup, SWT.NONE);
    GridData layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
    layoutData.horizontalIndent = widthIndent;
    gaeHrdLink.setLayoutData(layoutData);
    gaeHrdLink.setText("The project will use App Engine's <a href=\""
        + GaeProjectPropertyPage.APPENGINE_LOCAL_HRD_URL
        + "\">High Replication Datastore (HRD)</a> by default.");
    gaeHrdLink.setToolTipText(GaeProjectPropertyPage.APPENGINE_LOCAL_HRD_URL);
    gaeHrdLink.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event ev) {
        BrowserUtilities.launchBrowserAndHandleExceptions(ev.text);
      }
    });
  }

  private void createGoogleSdkGroup(Composite container) {
    int widthIndent = PixelConverterFactory.createPixelConverter(
        this.getControl()).convertWidthInCharsToPixels(2);

    Group googleSdkGroup = new Group(container, SWT.NONE);
    googleSdkGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    final GridLayout googleSdkGroupLayout = new GridLayout();
    googleSdkGroupLayout.verticalSpacing = 0;
    googleSdkGroupLayout.numColumns = 1;
    googleSdkGroup.setLayout(googleSdkGroupLayout);
    googleSdkGroup.setText("Google SDKs");

    SelectionListener useSdkCheckboxSelectionListener = new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
        updateControls();
      }

      public void widgetSelected(SelectionEvent e) {
        updateControls();
      }
    };

    createGwtSdkGroup(googleSdkGroup, useSdkCheckboxSelectionListener,
        widthIndent);

    // Add a horizontal spacer
    new Label(googleSdkGroup, SWT.HORIZONTAL);

    createGaeSdkGroup(googleSdkGroup, useSdkCheckboxSelectionListener,
        widthIndent);
  }

  private void createGwtSdkGroup(Group googleSdkGroup,
      SelectionListener useSdkCheckboxSelectionListener, int widthIndent) {
    useGwtCheckbox = new Button(googleSdkGroup, SWT.CHECK);
    useGwtCheckbox.addSelectionListener(useSdkCheckboxSelectionListener);
    useGwtCheckbox.setText("Use Google Web Toolkit");
    useGwtCheckbox.setSelection(true);

    gwtSelectionBlock = new GwtWorkspaceSdkSelectionBlock(googleSdkGroup,
        SWT.NONE);

    gwtSelectionBlock.addSdkSelectionListener(new SdkSelectionBlock.SdkSelectionListener() {
      public void onSdkSelection(SdkSelectionEvent e) {
        updateControls();
      }
    });

    ((GridData) gwtSelectionBlock.getLayoutData()).horizontalIndent = widthIndent;
  }

  private void createLocationGroup(Composite container) {
    // Output directory (defaults to subdirectory under workspace)
    final Group outDirGroup = new Group(container, SWT.NULL);
    outDirGroup.setText("Location");
    final GridData gd3 = new GridData(GridData.FILL_HORIZONTAL);
    gd3.horizontalSpan = 2;
    outDirGroup.setLayoutData(gd3);

    final GridLayout outDirGridLayout = new GridLayout();
    outDirGridLayout.numColumns = 3;
    outDirGroup.setLayout(outDirGridLayout);

    outDirWorkspaceButton = new Button(outDirGroup, SWT.RADIO);
    outDirWorkspaceButton.setText("Create new project in workspace");
    outDirWorkspaceButton.setSelection(true);
    final GridData gd4 = new GridData();
    gd4.horizontalAlignment = GridData.FILL;
    gd4.grabExcessHorizontalSpace = true;
    gd4.horizontalSpan = 3;
    outDirWorkspaceButton.setLayoutData(gd4);
    outDirWorkspaceButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateControls();
      }
    });

    outDirCustomButton = new Button(outDirGroup, SWT.RADIO);
    outDirCustomButton.setText("Create new project in:");
    final GridData gd5 = new GridData();
    gd5.horizontalAlignment = GridData.FILL;
    gd5.grabExcessHorizontalSpace = true;
    gd5.horizontalSpan = 3;
    outDirCustomButton.setLayoutData(gd5);
    outDirCustomButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        outDirText.setText(outDirCustom);
        updateControls();
      }
    });

    outDirLabel = new Label(outDirGroup, SWT.NONE);
    outDirLabel.setText("Directory:");

    outDirText = new Text(outDirGroup, SWT.BORDER);
    final GridData gd6 = new GridData();
    gd6.horizontalAlignment = GridData.FILL;
    gd6.grabExcessHorizontalSpace = true;
    outDirText.setLayoutData(gd6);
    outDirText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        if (outDirCustomButton.getSelection()) {
          outDirCustom = getOutputDirectory();
          validatePageAndSetCompletionStatus();
        }
      }
    });

    outDirBrowseButton = new Button(outDirGroup, SWT.NONE);
    outDirBrowseButton.setText("Browse...");
    outDirBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String outDir = browseForOutputDirectory();
        if (outDir != null) {
          outDirText.setText(outDir);
        }
      }
    });
  }

  private void createOtherOptionsGroup(Composite container) {
    // Other-options group
    final Group otherOptionsGroup = new Group(container, SWT.NULL);
    otherOptionsGroup.setText("Sample Code");
    final GridData gd3 = new GridData(GridData.FILL_HORIZONTAL);
    gd3.horizontalSpan = 2;
    otherOptionsGroup.setLayoutData(gd3);

    final GridLayout otherGridLayout = new GridLayout();
    otherGridLayout.numColumns = 3;
    otherOptionsGroup.setLayout(otherGridLayout);

    generateSampleCodeCheckbox = new Button(otherOptionsGroup, SWT.CHECK);
    generateSampleCodeCheckbox.setText("Generate project sample code");
    generateSampleCodeCheckbox.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
        updateControls();
      }

      public void widgetSelected(SelectionEvent e) {
        updateControls();
      }
    });
    generateSampleCodeCheckbox.setSelection(true);
  }

  private String getOutputDirectory() {
    return outDirText.getText().trim();
  }

  private void updateControls() {
    // Set the output directory to the workspace
    if (outDirWorkspaceButton.getSelection()) {
      outDirLabel.setEnabled(false);
      outDirText.setEnabled(false);
      outDirBrowseButton.setEnabled(false);

      String outDir = workspaceDirectory;
      if (getProjectName().length() > 0) {
        outDir += (System.getProperty("file.separator") + getProjectName());
      }

      outDirText.setText(outDir);
    } else {
      outDirLabel.setEnabled(true);
      outDirText.setEnabled(true);
      outDirBrowseButton.setEnabled(true);
    }

    gwtSelectionBlock.setEnabled(useGwtCheckbox.getSelection());
    gaeSelectionBlock.setEnabled(useGaeCheckbox.getSelection());
    gaeHrdLink.setEnabled(useGaeCheckbox.getSelection());

    validatePageAndSetCompletionStatus();
  }

  private boolean validateFromStatus(IStatus status) {
    if (!status.isOK()) {
      setMessage(status.getMessage(),
          convertValidationSeverity(status.getSeverity()));
      return false;
    }

    return true;
  }

  private void validatePageAndSetCompletionStatus() {
    IStatus status;
    boolean pageComplete = false;

    try {
      // Project name cannot be blank
      if (getProjectName().length() == 0) {
        setMessage("Enter a name for the project");
        return;
      }

      // Verify that project name is valid
      status = ResourcesPlugin.getWorkspace().validateName(getProjectName(),
          IResource.PROJECT);
      if (!validateFromStatus(status)) {
        return;
      }

      // Make sure project doesn't already exist in workspace
      if (existingProjectNames.contains(getProjectName())) {
        setMessage("A project with this name already exists.", ERROR);
        return;
      }

      // Output directory cannot be blank
      if (getOutputDirectory().length() == 0) {
        setMessage("Enter the output directory");
        return;
      }

      // If the user wants to use a custom output directory,
      // verify that the directory exists
      if (outDirCustomButton.getSelection()) {
        File outDir = new Path(getOutputDirectory()).toFile();
        if (!outDir.isDirectory()) {
          setMessage("The output directory does not exist", ERROR);
          return;
        }
      }

      // Make sure resource with project's name doesn't already exist in output
      // directory
      IPath outPath = new Path(getOutputDirectory());
      if (outDirWorkspaceButton.getSelection()) {
        if (outPath.toFile().exists()) {
          setMessage(
              "A resource with the project name already exists in the workspace root",
              ERROR);
          return;
        }
      }

      // Make sure output directory doesn't already contain an Eclipse project
      if (outDirCustomButton.getSelection()) {
        outPath = outPath.append(IProjectDescription.DESCRIPTION_FILE_NAME);
        if (outPath.toFile().exists()) {
          setMessage("The output directory already contains a project file",
              ERROR);
          return;
        }
      }

      // Package name cannot be blank
      if (getPackage().length() == 0) {
        setMessage("Enter a package name");
        return;
      }

      String complianceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.compliance");
      String sourceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.source");

      // Verify that package name is valid
      status = JavaConventions.validatePackageName(getPackage(),
          complianceLevel, sourceLevel);
      if (!validateFromStatus(status)) {
        return;
      }

      // If we are using GAE then an SDK must be selected
      if (useGaeCheckbox.getSelection()) {

        Sdk selectedGaeSdk = getSelectedGaeSdk();
        if (selectedGaeSdk == null) {
          setMessage("Please configure an App Engine SDK.", ERROR);
          return;
        }

        IStatus gaeSdkValidationStatus = selectedGaeSdk.validate();
        if (!gaeSdkValidationStatus.isOK()) {
          setMessage("The selected App Engine SDK is not valid: "
              + gaeSdkValidationStatus.getMessage(), ERROR);
          return;
        }
      }

      // If we are using GWT then an SDK must be selected
      if (useGwtCheckbox.getSelection()) {

        IStatus gwtRuntimeValidationStatus;
        GWTRuntime selectedGwtRuntime = getSelectedGwtSdk();
        if (selectedGwtRuntime == null) {
          setMessage("Please configure a GWT SDK.", ERROR);
          return;
        } else if (!(gwtRuntimeValidationStatus = selectedGwtRuntime.validate()).isOK()) {
          setMessage("The selected GWT SDK is not valid: "
              + gwtRuntimeValidationStatus.getMessage(), ERROR);
          return;
        } else {
          if (!selectedGwtRuntime.containsSCL()) {
            if (useGaeCheckbox.getSelection()) {
              setMessage(
                  "Web Application Projects that use Google Web Toolkit and App Engine require a GWT SDK versioned 1.6 or later.",
                  ERROR);
              return;
            }
          }
        }
      }

      // Verify that at least one of "Use GWT" or "Use GAE" is checked.
      if (!useGwtCheckbox.getSelection() && !useGaeCheckbox.getSelection()) {
        setMessage(
            "Web Application projects require the use of GWT and/or AppEngine.",
            ERROR);
        return;
      }

      pageComplete = true;
      setMessage(null);
    } finally {
      setPageComplete(pageComplete);
    }
  }
}
