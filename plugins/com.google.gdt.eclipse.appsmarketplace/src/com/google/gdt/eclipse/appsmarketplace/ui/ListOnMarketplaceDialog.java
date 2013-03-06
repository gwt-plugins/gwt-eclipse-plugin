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
import com.google.gdt.eclipse.appsmarketplace.data.AppListing;
import com.google.gdt.eclipse.appsmarketplace.data.Category;
import com.google.gdt.eclipse.appsmarketplace.data.DataStorage;
import com.google.gdt.eclipse.appsmarketplace.data.EnterpriseMarketplaceUrl;
import com.google.gdt.eclipse.appsmarketplace.data.VendorProfile;
import com.google.gdt.eclipse.appsmarketplace.job.BackendJob;
import com.google.gdt.eclipse.appsmarketplace.properties.AppsMarketplaceProjectProperties;
import com.google.gdt.eclipse.appsmarketplace.resources.AppsMarketplaceImages;
import com.google.gdt.eclipse.appsmarketplace.resources.AppsMarketplaceProject;
import com.google.gdt.eclipse.appsmarketplace.resources.AppsMarketplaceProjectResources;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

import java.util.List;

/**
 * Displays metadata for deployment an Apps Marketplace and triggers deploy. 
 */
public class ListOnMarketplaceDialog extends TitleAreaDialog {

  private class DescribeListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      if (deploymentStatus) {
        BrowserUtilities.launchBrowserAndHandleExceptions(
            EnterpriseMarketplaceUrl.generateFrontendViewListingUrl());
      } else {
        BrowserUtilities.launchBrowserAndHandleExceptions(
            EnterpriseMarketplaceUrl.generateFrontendCreateListingUrl());
      }
    }
  }

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

  private enum ListingType {
    CREATE,
    UPDATE
  }
  
  private class NewVendorListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent event) {
      BrowserUtilities.launchBrowserAndHandleExceptions(
          EnterpriseMarketplaceUrl.generateFrontendCreateVendorProfileUrl());
      
      String externalBrowserMsg = new String("A web browser window has "
          + "been opened to the \'Create Vendor Profile\' page on Google Apps "
          + "Marketplace.\n\nPlease select \'OK\' after creating your vendor "
          + "profile to continue with listing on Google Apps Marketplace");
      
      final MessageDialog messageDialog = new MessageDialog(
          Display.getDefault().getActiveShell(),
          "Creating Vendor on Google Apps Marketplace",
          null, externalBrowserMsg, MessageDialog.INFORMATION,
          new String[]{"OK"}, 0);
      messageDialog.open();
      
      BackendJob job = new BackendJob("Getting Vendor Profile", 
          BackendJob.Type.GetVendorProfile, requestFactory, appsMarketplaceProject);
      ProgressMonitorDialog pdlg = BackendJob.launchBackendJob(
          job, getShell());

      if (pdlg.open() == Window.OK && job.getOperationStatus() == true) {
        vendorCombo.add(DataStorage.getVendorProfile().vendorName);
        vendorCombo.select(0);
        fieldChanged();
      } else {
        MessageDialog.openError(getShell(), "Error creating Vendor Profle",
            "An error occured while creating Vendor Profile on"
                + " Google Apps Marketplace.\n\nPlease ensure that you enter "
                + "correct email address.");
      }
    }
  }
  private static final Status OK_STATUS = new Status(
      IStatus.OK, AppsMarketplacePlugin.PLUGIN_ID, "");

  private static final String CREATE_LISTING = 
    " Create New Application Listing ";
  
  private static final String UPDATE_LISTING = " Update Application Listing ";
  
  private static final String CREATE_TEXT = "Create";
  
  private static final String UPDATE_TEXT = "Update";
  
  private static final String FINISH_TEXT = "Finish";
  
  private static int convertSeverity(IStatus status) {
    switch (status.getSeverity()) {
      case IStatus.ERROR:
        return IMessageProvider.ERROR;
      case IStatus.WARNING:
        return IMessageProvider.WARNING;
      case IStatus.INFO:
        return IMessageProvider.INFORMATION;
      default:
        return IMessageProvider.NONE;
    }
  }
  
  private Button deployButton = null;
  
  private Combo vendorCombo = null;
  
  private Combo listingCombo = null;
  
  private Text appUrlText = null;
  
  private Text appNameText = null;
  
  private Combo appCategoryCombo;
  
  private Link createVendorLink = null;
  
  private Link describeLink = null;
  
  private Group listingDetailsGroup = null;
  
  private Group listingOptionGroup = null;
  
  private Label vendorLabel = null;
  
  private Button createListingButton = null;

  private Button updateListingButton = null;

  private final FieldListener fieldListener = new FieldListener();

  private final GeneralListener generalListener = new GeneralListener();

  private final NewVendorListener newVendorListener = new NewVendorListener();
  
  private final IProject project;
  
  private final AppsMarketplaceProject appsMarketplaceProject;
  
  private final HttpRequestFactory requestFactory;
  
  private ListingType listingType;
  
  private boolean listingInProgress = false;
  
  private String listingId = null;
  
  private int currentUpdateListIndex;
  
  private int matchingListIndex;

  private boolean deploymentStatus;

  public ListOnMarketplaceDialog(Shell parentShell,
      HttpRequestFactory requestFactory, AppsMarketplaceProject appsMarketplaceProject) {
    super(parentShell);
    this.requestFactory = requestFactory;
    this.appsMarketplaceProject = appsMarketplaceProject;
    this.project = appsMarketplaceProject.getProject();
  }

  public IProject getProject() {
    return project;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("List Project on Google Apps Marketplace");
    setHelpAvailable(false);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    deployButton = getButton(IDialogConstants.OK_ID);

    deployButton.setText(CREATE_TEXT);
    getShell().setDefaultButton(deployButton);
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    setTitle("Select Vendor and Create Application Listing");
    setTitleImage(AppsMarketplacePlugin.getDefault().getImage(
        AppsMarketplaceImages.APPS_MARKETPLACE_LIST_LARGE));

    initializeControls();
    addEventHandlers();
    fieldChanged();

    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    parent = (Composite) super.createDialogArea(parent);
    int numColumns = 3;

    Composite container = new Composite(parent, SWT.NONE);

    GridData containerGridData = new GridData(GridData.FILL_HORIZONTAL);
    container.setLayoutData(containerGridData);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = numColumns;
    gridLayout.marginHeight = 18;
    gridLayout.marginWidth = 8;
    gridLayout.makeColumnsEqualWidth = false;
    container.setLayout(gridLayout);
    createVendorArea(container, numColumns);
    createSpacer(container, numColumns);
    createListingOptionArea(container, numColumns);
    createSpacer(container, numColumns);
    createListingDetailsArea(container, numColumns);
    return container;
  }

  @Override
  protected void okPressed() {
    if (listingInProgress == false) {
      listingInProgress = true;
      
      if (listingType == ListingType.UPDATE) {
        String message = new String(
            "This will overwrite you existing application listing \"" + 
            DataStorage.getAppListings().get(listingCombo.getSelectionIndex()).name 
            + "\" on Google Apps Marketplace.\n\nDo you want to continue ?");
        final MessageDialog messageDialog = new MessageDialog(
            Display.getDefault().getActiveShell(), 
            "Update Application Listing on Google Apps Marketplace",
            null, message , MessageDialog.QUESTION, 
            new String[]{"OK", "Cancel"}, 1);
        if (messageDialog.open() != Window.OK) {
          super.okPressed();
          return;
        }
      }
      deploymentStatus = deployAppsMarketplaceListing(); 
      String messageTitle = new String();
      String messageText = new String();
      if (deploymentStatus) {
        try {
          AppsMarketplaceProjectProperties.setAppListingAlreadyListed(
              project, true);
          String consumerKey = DataStorage.getListedAppListing().consumerKey;
          String consumerSecret = 
            DataStorage.getListedAppListing().consumerSecret;
          AppsMarketplaceProjectProperties.setAppListingConsumerKey(
              project, consumerKey);
          AppsMarketplaceProjectProperties.setAppListingConsumerSecret(
              project, consumerSecret);
        } catch (BackingStoreException e) {
          // Consume  exception
          AppsMarketplacePluginLog.logError(e);
        }
        messageTitle = "Successfully created private listing on Google Apps Marketplace";
        messageText = "Successfully listed application \'" +
          appNameText.getText() + "\'" + " on Google Apps Marketplace." +
          " Click <a href=\"#\">here</a> to view details.";
      } else {
        messageTitle = "Failed to created listing on Google Apps Marketplace";
        messageText = "Failed to list application \'" + appNameText.getText() +
          "\'" + " on Google Apps Marketplace." +
          " Click <a href=\"#\">here</a> to manually create listing.";
      }
      this.setMessage("");
      this.setTitle(messageTitle);
      disposeDialogArea();
      Composite container = (Composite) this.getDialogArea();
      createDescribeLinkArea(container, 1);
      describeLink.setText(messageText);
      container.layout(true);

      getButton(IDialogConstants.CANCEL_ID).setVisible(false);
      deployButton.setText(FINISH_TEXT);
      describeLink.addSelectionListener(new DescribeListener());
      getShell().redraw();
    } else {
      if (deploymentStatus) {
        try {
          String consumerKey = 
            AppsMarketplaceProjectProperties.getAppListingConsumerKey(project);
          String consumerSecret = 
            AppsMarketplaceProjectProperties.getAppListingConsumerSecret(project);
          // Set the consumerKey and consumerSecret in web.xml. 
          // Operation may fail.
          appsMarketplaceProject.setOAuthParams(
              consumerKey, consumerSecret, true);
        } catch (CoreException e) {
          // Consume  exception
          AppsMarketplacePluginLog.logError(e);
        }
      }
      super.okPressed();
    }
  }
  
  private void addEventHandlers() {
    appUrlText.addModifyListener(fieldListener);
    appNameText.addModifyListener(fieldListener);
    appCategoryCombo.addSelectionListener(generalListener);
    listingCombo.addSelectionListener(generalListener);
    createListingButton.addSelectionListener(generalListener);
    updateListingButton.addSelectionListener(generalListener);
    createVendorLink.addSelectionListener(newVendorListener);
  }
  
  private void createDescribeLinkArea(Composite container, int numColumns) {
    GridData containerGridData = new GridData(GridData.FILL_HORIZONTAL);
    container.setLayoutData(containerGridData);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = numColumns;
    gridLayout.marginHeight = 18;
    gridLayout.marginWidth = 8;
    gridLayout.makeColumnsEqualWidth = false;
    container.setLayout(gridLayout);
    describeLink = new Link(container, SWT.BOLD);
    describeLink.setVisible(true);
    describeLink.setLayoutData(new GridData(
        GridData.BEGINNING, GridData.BEGINNING, true, true, numColumns, 1));
  }

  private void createListingDetailsArea(Composite container, int numColumns) {
    listingDetailsGroup = SWTFactory.createGroup(
        container, "  Create New Listing Details ", 1, 1, 
        GridData.FILL_HORIZONTAL);

    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = numColumns;
    listingDetailsGroup.setLayoutData(gridData);

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.marginHeight = 8;
    gridLayout.marginWidth = 8;
    gridLayout.makeColumnsEqualWidth = false;
    listingDetailsGroup.setLayout(gridLayout);

    createSpacer(listingDetailsGroup, numColumns);

    Label appUrlLabel = new Label(listingDetailsGroup, SWT.NONE);
    appUrlLabel.setText("Application Url");
    new Label(listingDetailsGroup, SWT.NONE).setText(":");
    appUrlText = new Text(listingDetailsGroup, SWT.BORDER);
    appUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    createSpacer(listingDetailsGroup, numColumns);

    Label appNameLabel = new Label(listingDetailsGroup, SWT.NONE);
    appNameLabel.setText("Application Name");
    new Label(listingDetailsGroup, SWT.NONE).setText(":");
    appNameText = new Text(listingDetailsGroup, SWT.BORDER);
    appNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    createSpacer(listingDetailsGroup, numColumns);
    
    Label appCategoryLabel = new Label(listingDetailsGroup, SWT.NONE);
    appCategoryLabel.setText("Application Category");
    new Label(listingDetailsGroup, SWT.NONE).setText(":");
    String items[] = Category.getCategories();
    appCategoryCombo = SWTFactory.createCombo(listingDetailsGroup, SWT.NONE, 
        1, items);
    appCategoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
  }

  private void createListingOptionArea(Composite container, int numColumns) {
    listingOptionGroup = SWTFactory.createGroup(
        container, "  Select Listing Option  ", 1, 1, GridData.FILL_HORIZONTAL);

    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = numColumns;
    listingOptionGroup.setLayoutData(gridData);

    int numSubColumns = 2;
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = numSubColumns;
    gridLayout.marginHeight = 8;
    gridLayout.marginWidth = 8;
    gridLayout.makeColumnsEqualWidth = false;
    listingOptionGroup.setLayout(gridLayout);

    // Create radio button
    createListingButton = SWTFactory.createRadioButton(
        listingOptionGroup, "Create New Listing");
    // Set fill Horizontal
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = numSubColumns;
    createListingButton.setLayoutData(gridData);
    // Upgrade radio button
    updateListingButton = SWTFactory.createRadioButton(
        listingOptionGroup, "Upgrade Listing:");

    String items[] = new String[0];
    listingCombo = SWTFactory.createCombo(
        listingOptionGroup, SWT.NONE, 1, items);
  }

  private void createSpacer(Composite container, int numColumns) {
    // Spacer
    new Label(container, SWT.NONE).setLayoutData(new GridData(
        GridData.BEGINNING, GridData.CENTER, true, true, numColumns, 1));
  }

  private void createVendorArea(Composite container, int numColumns) {
    // Vendor field specific runtime selection combo
    vendorLabel = new Label(container, SWT.NONE);
    vendorLabel.setText("Vendor:");

    String items[] = new String[0];
    vendorCombo = SWTFactory.createCombo(
        container, SWT.NONE, numColumns, items);

    createVendorLink = new Link(container, SWT.BOLD);
    createVendorLink.setLayoutData(
          new GridData(GridData.END, GridData.CENTER, false, true, 1, 1));
    createVendorLink.setText("<a href=\"#\">Create Vendor</a>");
    vendorCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
  }

  private boolean deployAppsMarketplaceListing() {
    // Store appUrl and appName
    String appUrl = appUrlText.getText().trim();
    String appName = appNameText.getText().trim();
    Integer categoryId = appCategoryCombo.getSelectionIndex();
    try {
      AppsMarketplaceProjectProperties.setAppListingUrl(
          project, appUrl);
      AppsMarketplaceProjectProperties.setAppListingName(
          project, appName);
      AppsMarketplaceProjectProperties.setAppListingCategory(
          project, categoryId);
      if (listingId != null) {
        AppsMarketplaceProjectProperties.setAppListingId(
            project, listingId);
      }
    } catch (BackingStoreException e) {
      // Do nothing
      AppsMarketplacePluginLog.logError(e);
    }
    
      try {
        // set appUrl and appName values in the application-manifest.xml
        this.appsMarketplaceProject.setAppUrl(appUrl);
        this.appsMarketplaceProject.setAppName(appName);
      } catch (CoreException e) {
        MessageDialog.openWarning(
            AppsMarketplacePlugin.getActiveWorkbenchShell(),
            "Google Plugin for Eclipse",
            "Could not set application url in application-manifest.xml."
            + " Please set it manually.");
        return false;
      }
      BackendJob job;
      ProgressMonitorDialog pdlg = null;
      if (createListingButton.getSelection()) {
        job = new BackendJob(
            "Creating Appication Listing on Google Apps Marketplace",
            BackendJob.Type.CreateAppListing, requestFactory,
            appsMarketplaceProject);
      } else {
        job = new BackendJob(
            "Upgrading Appication Listing on Google Apps Marketplace",
            BackendJob.Type.UpdateAppListing, requestFactory,
            appsMarketplaceProject);
      }
      pdlg = BackendJob.launchBackendJob(job, getShell());
      return (pdlg.open() == Window.OK && job.getOperationStatus() == true);
    }

  private void disableArea(Composite container) {
    container.setEnabled(false);
    for (Control child : container.getChildren()) {
      child.setEnabled(false);
    }
  }

  private void disposeDialogArea() {
    Composite container = (Composite) this.getDialogArea();
    for (Control child : container.getChildren()) {
      child.dispose();
    }
  }

  private void enableArea(Composite container) {
    container.setEnabled(true);
    for (Control child : container.getChildren()) {
      child.setEnabled(true);
    }
  }

  private void fieldChanged() {
    validateFields();
    updateControls();
  }

  private int getAppListingIndex(List<AppListing> appListings) {
    int retIndex = -1, index = 0;
    String listingId =
      AppsMarketplaceProjectProperties.getAppListingId(project);

    if (!StringUtilities.isEmpty(listingId)) {
      for (AppListing appListing : appListings) {
        if (appListing.listingId.equals(listingId)) {
          retIndex = index;
          break;
        }
        index++;
      }
    }
    return retIndex;
  }

  private void initializeControls() {
    VendorProfile vendorProfile = DataStorage.getVendorProfile();

    if (vendorProfile != null) {
      vendorCombo.add(vendorProfile.vendorName);
      vendorCombo.select(0);
    }

    List<AppListing> appListings = DataStorage.getAppListings();
    if (appListings != null && appListings.size() > 0) {
      matchingListIndex = getAppListingIndex(appListings);
      for (AppListing appListing : appListings) {
        listingCombo.add(appListing.name);
      }
    } else {
      matchingListIndex = -1;
      updateListingButton.setEnabled(false);
    }

    if (matchingListIndex != -1) {
      listingType = ListingType.UPDATE;
      listingDetailsGroup.setText(UPDATE_LISTING);
      deployButton.setText(UPDATE_TEXT);
      listingCombo.select(matchingListIndex);
      currentUpdateListIndex = matchingListIndex;
      updateListingButton.setSelection(true);
      populateListingFromProjectProperties();
      listingId = appListings.get(matchingListIndex).listingId;
    } else {
      listingType = ListingType.CREATE;
      listingDetailsGroup.setText(CREATE_LISTING);
      deployButton.setText(CREATE_TEXT);
      createListingButton.setSelection(true);
      populateListingFromProjectProperties();
      listingId = null;
      
    }
  }

  private void populateListingFromAppManifext(AppListing appListing) {
    try {
      appUrlText.setText(appsMarketplaceProject.getAppUrl(
          appListing.appManifest));
    } catch (CoreException e) {
      // Consume exception
      AppsMarketplacePluginLog.logError(e);
    }
    appNameText.setText(appListing.name);
    appCategoryCombo.select(appListing.categoryId);
  }

  private void populateListingFromProjectProperties() {
    appUrlText.setText(
        AppsMarketplaceProjectProperties.getAppListingUrl(project));
    appNameText.setText(
        AppsMarketplaceProjectProperties.getAppListingName(project));
    appCategoryCombo.select(
        AppsMarketplaceProjectProperties.getAppListingCategory(
            project));
  }

  private void updateArea(boolean enable) {
    if (enable) {
      enableArea(this.listingOptionGroup);
      createVendorLink.setVisible(false);
      if (listingCombo.getItemCount() == 0) {
        updateListingButton.setEnabled(false);
        listingCombo.setEnabled(false);
      }
      if (updateListingButton.getSelection() &&
         listingCombo.getSelectionIndex() == -1) {
        disableArea(this.listingDetailsGroup);
      } else {
        enableArea(this.listingDetailsGroup);
      }
    } else {
      disableArea(this.listingOptionGroup);
      disableArea(this.listingDetailsGroup);
    }
  }

  private void updateControls() {
    if (vendorCombo.getSelectionIndex() == -1) {
      updateArea(false);
    } else {
      updateArea(true);
      if (createListingButton.getSelection() &&
          listingType == ListingType.UPDATE) {
        listingDetailsGroup.setText(CREATE_LISTING);
        deployButton.setText(CREATE_TEXT);
        listingType = ListingType.CREATE;
        populateListingFromProjectProperties();
        listingId = null;
      }
      if (updateListingButton.getSelection() && 
          (listingType == ListingType.CREATE ||
              listingCombo.getSelectionIndex() != currentUpdateListIndex)) {
        listingDetailsGroup.setText(UPDATE_LISTING);
        deployButton.setText(UPDATE_TEXT);
        listingType = ListingType.UPDATE;
        int index = listingCombo.getSelectionIndex();
        currentUpdateListIndex = index;
        List<AppListing> appListings = DataStorage.getAppListings();
        if (index != -1) {
          if (index == matchingListIndex) {
            populateListingFromProjectProperties();
          } else {
            populateListingFromAppManifext(appListings.get(index));
          }
          listingId = appListings.get(index).listingId;
        }
      }
    }
  }

  private void updateStatus(IStatus status) {
    boolean allFieldsValid = false;

    if (status.getSeverity() == IStatus.OK
        && status.getMessage().length() == 0) {
      String msg = "Ready to list application '" + appNameText.getText().trim()
          + "' on Google AppsMarketplace";

      status = new Status(IStatus.INFO, AppsMarketplacePlugin.PLUGIN_ID, msg);
      allFieldsValid = true;
    }

    if (status.getSeverity() == IStatus.INFO
        || status.getSeverity() == IStatus.WARNING) {
      allFieldsValid = true;
    }

    this.setMessage(status.getMessage(), convertSeverity(status));
    deployButton.setEnabled(allFieldsValid);
  }

  private void updateStatus(IStatus[] status) {
    updateStatus(StatusUtilities.getMostImportantStatusWithMessage(status));
  }

  private IStatus validateAppCategory() {
    if (appCategoryCombo.getSelectionIndex() == -1) {
      return StatusUtilities.newErrorStatus("Application Category is invalid.\n"
          + "Select an appropiate application category.",
          AppsMarketplacePlugin.PLUGIN_ID);
    }
    return OK_STATUS;
  }

  private IStatus validateAppName() {
    String str = appNameText.getText().trim();
    if (str.length() == 0) {
      return StatusUtilities.newErrorStatus(
          "Application Name is invalid.", AppsMarketplacePlugin.PLUGIN_ID);
    }
    return OK_STATUS;
  }
  
  private IStatus validateAppUrl() {
    String str = appUrlText.getText().trim();
    if (str.indexOf("http://") != 0 || str.length() <= 7) {
      return StatusUtilities.newErrorStatus("Application URL is invalid.\n"
          + "(ex: http://appname.domain.com/somepath)",
          AppsMarketplacePlugin.PLUGIN_ID);
    }
    return OK_STATUS;
  }

  private void validateFields() {
    IStatus projectStatus = validateProject();
    IStatus vendorStatus = validateVendor();
    IStatus listingStatus = validateListing();
    IStatus appUrlStatus = validateAppUrl();
    IStatus appNameStatus = validateAppName();
    IStatus appCategorystatus = validateAppCategory();
    updateStatus(new IStatus[] {
        projectStatus, vendorStatus, listingStatus, appUrlStatus, 
        appNameStatus, appCategorystatus, });
  }
  
  private IStatus validateListing() {
    if (updateListingButton.getSelection() && 
        listingCombo.getSelectionIndex() == -1) {
      return StatusUtilities.newErrorStatus(
          "Application Listing selection is invalid.\n" + 
          "Select a valid application listing from the list",
          AppsMarketplacePlugin.PLUGIN_ID);
    } else {

    }
    return OK_STATUS;
  }

  private IStatus validateProject() {
    if (AppsMarketplaceProjectResources.validateAppsMarketplaceMetadataExists(
        project, 
        AppsMarketplaceProjectProperties.getAppListingWarDirectory(
            project)) == false) {
      return StatusUtilities.newErrorStatus(
          "Apps Marketplace listing files missing.\n" +
          "Ensure \'WEB-INF/application-manifest.xml\' and " +
          "\'WEB-INF/listing-manifest.xml\' files exists in WAR directory", 
          AppsMarketplacePlugin.PLUGIN_ID);
    }
    return OK_STATUS;
  }

  private IStatus validateVendor() {
    if (vendorCombo.getSelectionIndex() == -1) {
      return StatusUtilities.newErrorStatus(
          "Vendor selection is invalid.\n" + "Select a vendor from the list or "
              + "click \'Create Vendor\' to create a new vendor profile",
          AppsMarketplacePlugin.PLUGIN_ID);
    } else {

    }
    return OK_STATUS;
  }
}
