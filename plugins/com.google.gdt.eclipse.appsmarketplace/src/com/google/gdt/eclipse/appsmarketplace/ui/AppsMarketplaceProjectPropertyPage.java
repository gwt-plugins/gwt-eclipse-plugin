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

import com.google.gdt.eclipse.appsmarketplace.AppsMarketplacePlugin;
import com.google.gdt.eclipse.appsmarketplace.data.Category;
import com.google.gdt.eclipse.appsmarketplace.properties.AppsMarketplaceProjectProperties;
import com.google.gdt.eclipse.appsmarketplace.resources.AppsMarketplaceProject;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.ui.AbstractProjectPropertyPage;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Property page for setting apps marketplace project properties.
 */
public class AppsMarketplaceProjectPropertyPage
    extends AbstractProjectPropertyPage {

  private class FieldListener implements ModifyListener {
    public void modifyText(ModifyEvent e) {
      fieldChanged();
    }
  }

  private class GeneralListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      fieldChanged();
    }
  }

  public static final String ID = AppsMarketplacePlugin.PLUGIN_ID
      + ".appsMarketplaceProjectPropertyPage";

  private Button addMarketplaceCheckbox;
  private Text appUrlText;
  private Text appNameText;
  private Text consumerKeyText;
  private Text consumerSecretText;
  private Combo appCategoryCombo;
  private Group group;
  private Button listOnAppsMarketplaceButton;
  private boolean initialAddAppsMarketplaceSupport;
  private boolean addAppsMarketplaceSupport;
  private boolean manageWarDirectory;
  private Group warLocationGroup;
  private Text warLocationText;
  private IFolder warDirectory;
  private final FieldListener fieldListener = new FieldListener();
  private final GeneralListener generalListener = new GeneralListener();

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.jface.preference.PreferencePage#applyData(java.lang.Object)
   */
  @Override
  public void applyData(Object data) {
    if (data instanceof Boolean) {
      boolean selection = (Boolean) data;
      addMarketplaceCheckbox.setSelection(selection);
      fieldChanged();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
   * .swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    panel.setLayoutData(GridData.FILL_HORIZONTAL);
    panel.setLayout(new GridLayout());

    addMarketplaceCheckbox = new Button(panel, SWT.CHECK);
    addMarketplaceCheckbox.setText("Add support for Google Apps Marketplace");
    updateManageWarDirectory();
    if (manageWarDirectory) {
      createWarLocation(panel);
    }
    createAppsMarketplaceListing(panel);
    recordInitialSettings();
    initializeControls();
    addEventHandlers();
    fieldChanged();
    return panel;
  }

  /*
   * (non-Javadoc)
   *
   * @seecom.google.gdt.eclipse.core.ui.AbstractProjectPropertyPage#
   * saveProjectProperties()
   */
  @Override
  protected void saveProjectProperties()
      throws BackingStoreException, CoreException {

    if (hasNatureChanged()) {
      if (addAppsMarketplaceSupport) {
        addAppsMarketplaceSupport();
      } else {
        removeAppsMarketplaceSupport();
      }
      recordInitialSettings();
    }

    if (addAppsMarketplaceSupport) {
      saveApplicationListingDetails();
    }
  }

  private void addAppsMarketplaceSupport() throws CoreException {
    if (!manageWarDirectory) {
      warDirectory = WebAppUtilities.getManagedWarOut(getProject());
      if (warDirectory == null) {
        throw new CoreException(
            new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
                "Could not find war directory"));
      }
    }
    AppsMarketplaceProject.addAppsMarketplaceSupport(
        getProject(), warDirectory);
  }

  private void addEventHandlers() {
    addMarketplaceCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fieldChanged();
      }
    });
    appUrlText.addModifyListener(fieldListener);
    appNameText.addModifyListener(fieldListener);
    appCategoryCombo.addSelectionListener(generalListener);
    if (manageWarDirectory) {
      warLocationText.addModifyListener(fieldListener);
    }
    listOnAppsMarketplaceButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        performOk();
        new ListOnMarketplaceHandler().execute(null);
        initializeControls();
      }
    });
  }

  private void createAppsMarketplaceListing(Composite parent) {
    group = SWTFactory.createGroup(parent, " Google Apps Marketplace Listing ",
        1, 1, GridData.FILL_HORIZONTAL);
    int numOfCols = 3;
    GridData containerGroupGridData = new GridData(GridData.FILL_HORIZONTAL);
    containerGroupGridData.horizontalSpan = numOfCols;
    group.setLayoutData(containerGroupGridData);

    GridLayout gridListingLayout = new GridLayout();
    gridListingLayout.numColumns = numOfCols;
    gridListingLayout.marginHeight = 8;
    gridListingLayout.marginWidth = 8;
    gridListingLayout.makeColumnsEqualWidth = false;
    group.setLayout(gridListingLayout);

    createSpacer(group, numOfCols);

    Label appUrlLabel = new Label(group, SWT.NONE);
    appUrlLabel.setText("Application Url");
    new Label(group, SWT.NONE).setText(":");
    appUrlText = new Text(group, SWT.BORDER);
    appUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    createSpacer(group, numOfCols);

    Label appNameLabel = new Label(group, SWT.NONE);
    appNameLabel.setText("Application Name");
    new Label(group, SWT.NONE).setText(":");
    appNameText = new Text(group, SWT.BORDER);
    appNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    createSpacer(group, numOfCols);

    Label appCategoryLabel = new Label(group, SWT.NONE);
    appCategoryLabel.setText("Application Category");
    new Label(group, SWT.NONE).setText(":");
    String items[] = Category.getCategories();
    appCategoryCombo = SWTFactory.createCombo(group, SWT.NONE, 1, items);
    appCategoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    createSpacer(group, numOfCols);

    Label consumerKeyLabel = new Label(group, SWT.NONE);
    consumerKeyLabel.setText("Consumer Key");
    new Label(group, SWT.NONE).setText(":");
    consumerKeyText = new Text(group, SWT.BORDER);
    consumerKeyText.setEditable(false);
    consumerKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    createSpacer(group, numOfCols);

    Label consumerSecretLabel = new Label(group, SWT.NONE);
    consumerSecretLabel.setText("Consumer Secret");
    new Label(group, SWT.NONE).setText(":");
    consumerSecretText = new Text(group, SWT.BORDER);
    consumerSecretText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    consumerSecretText.setEditable(false);

    createSpacer(group, numOfCols);

    listOnAppsMarketplaceButton = new Button(group, SWT.PUSH);
    // new Link(group, SWT.BOLD);
    listOnAppsMarketplaceButton.setText("List on Google Apps Marketplace");
    listOnAppsMarketplaceButton.setLayoutData(new GridData(
        GridData.BEGINNING, GridData.CENTER, true, true, numOfCols, 1));

    createSpacer(group, numOfCols);
  }

  private void createSpacer(Composite parent, int numOfCols) {
    // Spacer
    new Label(parent, SWT.NONE).setLayoutData(new GridData(
        GridData.BEGINNING, GridData.CENTER, true, true, numOfCols, 1));
  }

  private void createWarLocation(Composite parent) {
    warLocationGroup = SWTFactory.createGroup(
        parent, " Enter WAR Directory ", 1, 1, GridData.FILL_HORIZONTAL);
    int numOfCols = 2;

    GridLayout gridListingLayout = new GridLayout();
    gridListingLayout.numColumns = numOfCols;
    gridListingLayout.marginHeight = 8;
    gridListingLayout.marginWidth = 8;
    gridListingLayout.makeColumnsEqualWidth = false;
    warLocationGroup.setLayout(gridListingLayout);

    // Spacer
    new Label(warLocationGroup, SWT.NONE).setLayoutData(
        new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));

    Label warLocationLabel = new Label(warLocationGroup, SWT.NONE);
    warLocationLabel.setText("WAR Directory:");
    warLocationText = new Text(warLocationGroup, SWT.BORDER);
    warLocationText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // Spacer
    new Label(warLocationGroup, SWT.NONE).setLayoutData(
        new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));

  }

  private void fieldChanged() {
    validateFields();
    updateControls(addAppsMarketplaceSupport);
  }

  private boolean hasNatureChanged() {
    return initialAddAppsMarketplaceSupport ^ addAppsMarketplaceSupport;
  }

  private void initializeControls() {
    addMarketplaceCheckbox.setSelection(initialAddAppsMarketplaceSupport);
    group.setEnabled(initialAddAppsMarketplaceSupport);
    for (Control child : group.getChildren()) {
      child.setEnabled(initialAddAppsMarketplaceSupport);
    }

    if (manageWarDirectory) {
      warLocationGroup.setEnabled(initialAddAppsMarketplaceSupport);
      for (Control child : warLocationGroup.getChildren()) {
        child.setEnabled(initialAddAppsMarketplaceSupport);
      }
    }
    appUrlText.setText(
        AppsMarketplaceProjectProperties.getAppListingUrl(getProject()));
    appNameText.setText(
        AppsMarketplaceProjectProperties.getAppListingName(getProject()));
    // assuming category index matches array index
    appCategoryCombo.select(
        AppsMarketplaceProjectProperties.getAppListingCategory(getProject()));
    if (manageWarDirectory) {
      warLocationText.setText(
          AppsMarketplaceProjectProperties.getAppListingWarDirectory(
              getProject()));
    }
    consumerKeyText.setText(
        AppsMarketplaceProjectProperties.getAppListingConsumerKey(
            getProject()));
    consumerKeyText.setEnabled(false);
    consumerSecretText.setText(
        AppsMarketplaceProjectProperties.getAppListingConsumerSecret(
            getProject()));
    consumerSecretText.setEnabled(false);
  }

  private void recordInitialSettings() {
    initialAddAppsMarketplaceSupport =
      AppsMarketplaceProject.isAppsMarketplaceEnabled(getProject());
    addAppsMarketplaceSupport = initialAddAppsMarketplaceSupport;
  }

  private void removeAppsMarketplaceSupport() throws CoreException {
    if (!manageWarDirectory) {
      warDirectory = WebAppUtilities.getManagedWarOut(getProject());
      if (warDirectory == null) {
        throw new CoreException(
            new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
                "Could not find war directory"));
      }
    }
    AppsMarketplaceProject.removeAppsMarketplaceSupport(
        getProject(), warDirectory);
  }

  private void saveApplicationListingDetails() throws BackingStoreException {
    String appName = appNameText.getText().trim();
    String appUrl = appUrlText.getText().trim();
    AppsMarketplaceProjectProperties.setAppListingUrl(getProject(), appUrl);
    AppsMarketplaceProjectProperties.setAppListingName(getProject(), appName);

    if (appCategoryCombo.getSelectionIndex() != -1) {
      AppsMarketplaceProjectProperties.setAppListingCategory(
          getProject(), appCategoryCombo.getSelectionIndex());
    }
    if (manageWarDirectory) {
      AppsMarketplaceProjectProperties.setAppListingWarDirectory(
          getProject(), warLocationText.getText().trim());
    }
    AppsMarketplaceProject appsMktProject = AppsMarketplaceProject.create(
        getProject());
    try {
      // set appUrl and appName values in the application-manifest.xml
      appsMktProject.setAppUrl(appUrl);
      appsMktProject.setAppName(appName);
    } catch (CoreException e) {
      MessageDialog.openWarning(AppsMarketplacePlugin.getActiveWorkbenchShell(),
          "Google Plugin for Eclipse",
          "Could not set application url in application-manifest.xml."
              + " Please set it manually.");
    }
  }

  private void updateControls(boolean appsMarketplaceSupport) {
    if (group.getEnabled() != appsMarketplaceSupport) {
      group.setEnabled(appsMarketplaceSupport);
      for (Control child : group.getChildren()) {
        child.setEnabled(appsMarketplaceSupport);
      }
      consumerKeyText.setEnabled(false);
      consumerSecretText.setEnabled(false);
    }
    if (manageWarDirectory) {
      if (warLocationGroup.getEnabled() != appsMarketplaceSupport) {
        warLocationGroup.setEnabled(appsMarketplaceSupport);
        for (Control child : warLocationGroup.getChildren()) {
          child.setEnabled(appsMarketplaceSupport);
        }
      }
    }
  }

  private void updateManageWarDirectory() {
    if (WebAppUtilities.isWebApp(getProject())) {
      manageWarDirectory = false;
    } else {
      manageWarDirectory = true;
    }
  }

  private IStatus validateAddAppsMarketplaceSupport() {
    addAppsMarketplaceSupport = addMarketplaceCheckbox.getSelection();
    return StatusUtilities.OK_STATUS;
  }

  private IStatus validateAppUrl() {
    String str = appUrlText.getText().trim();
    if (!StringUtilities.isEmpty(str)) {
      if (str.indexOf("http://") != 0 || str.length() <= 7) {
        return StatusUtilities.newErrorStatus("Application URL is invalid."
            + "(ex: http://yourapplication.example.com)",
            AppsMarketplacePlugin.PLUGIN_ID);
      }
    }
    return StatusUtilities.OK_STATUS;
  }

  private void validateFields() {
    IStatus addAppsMarketplaceStatus = validateAddAppsMarketplaceSupport();
    IStatus appUrlStatus = validateAppUrl();
    IStatus warDirectoryStatus = StatusUtilities.OK_STATUS;
    if (manageWarDirectory && addAppsMarketplaceSupport) {
      warDirectoryStatus = validateWarDirectoryStatus();
    }
    updateStatus(new IStatus[] {
        addAppsMarketplaceStatus, appUrlStatus, warDirectoryStatus});
  }

  private IStatus validateWarDirectoryStatus() {
    warDirectory = null;

    String warDirectoryString = warLocationText.getText().trim();

    if (warDirectoryString.length() == 0) {
      return StatusUtilities.newErrorStatus(
          "Enter the WAR source directory", AppsMarketplacePlugin.PLUGIN_ID);
    }

    IPath path = new Path(warDirectoryString);
    IProject project = getProject();
    IResource resource = project.findMember(path);

    if (!(resource instanceof IFolder)) {
      return StatusUtilities.newErrorStatus(
          "The folder ''{0}/{1}'' does not exist",
          AppsMarketplacePlugin.PLUGIN_ID, project.getName(), path);
    }

    warDirectory = (IFolder) resource;
    return StatusUtilities.OK_STATUS;
  }
}
