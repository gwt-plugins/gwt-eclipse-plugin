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
package com.google.gdt.eclipse.appsmarketplace.job;

import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.http.json.JsonHttpParser;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gdt.eclipse.appsmarketplace.AppsMarketplacePluginLog;
import com.google.gdt.eclipse.appsmarketplace.data.AppListing;
import com.google.gdt.eclipse.appsmarketplace.data.AppListingList;
import com.google.gdt.eclipse.appsmarketplace.data.DataStorage;
import com.google.gdt.eclipse.appsmarketplace.data.EnterpriseMarketplaceUrl;
import com.google.gdt.eclipse.appsmarketplace.data.VendorProfile;
import com.google.gdt.eclipse.appsmarketplace.properties.AppsMarketplaceProjectProperties;
import com.google.gdt.eclipse.appsmarketplace.resources.AppsMarketplaceProject;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Runs jobs interactions with back-end in a separate thread.
 */
public class BackendJob implements Runnable {
  /**
   * Enum defining type of backend operation.
   */
  public enum Type {
    GetVendorProfile, CreateVendorProfile, GetAppListing, CreateAppListing,
    UpdateAppListing
  }
  /**
   * Implements a basic email address parser.
   */
  class EmailAddress {
    private String user;
    private String host;

    EmailAddress(String emailString) {
      if (!parseEmail(emailString)) {
        user = "";
        host = "";
      }
    }

    public String getHost() {
      return host;
    }

    public String getUser() {
      return user;
    }

    @Override
    public String toString() {
      return user + "@" + host;
    }

    boolean isValid() {
      return !(StringUtilities.isEmpty(user) || StringUtilities.isEmpty(host));
    }

    private boolean parseEmail(String emailString) {
      // check for null
      if (emailString == null) {
        return false;
      }

      // Check for an '@' character. Get the last one, in case the local part is
      // quoted.
      int atIndex = emailString.lastIndexOf('@');
      if ((atIndex <= 0) || // no '@' character in the email address
          // or @ on the first position
          (
              atIndex == (emailString.length() - 1))) { // last character, no host
        return false;
      }

      user = emailString.substring(0, atIndex);
      host = emailString.substring(atIndex + 1);
      return true;
    }
  }

  public static ProgressMonitorDialog launchBackendJob(
      BackendJob job, Shell shell) {
    ProgressMonitorDialog pdlg = new ProgressMonitorDialog(shell);
    job.setProgressDialog(pdlg);
    pdlg.open();
    pdlg.setCancelable(true);

    Thread thread = new Thread(job);
    thread.start();
    pdlg.setBlockOnOpen(true);
    return pdlg;
  }

  // Setting read timeout to 50 sec for vendors with long app listings list.
  private final int readTimeout = 50 * 1000;

  private boolean operationStatus = false;

  private IProgressMonitor monitor;

  private ProgressMonitorDialog dlg;

  private HttpRequestFactory requestFactory;

  private AppsMarketplaceProject appsMarketplaceProject;

  private Type type;

  private String name;

  private JsonFactory jsonFactory;

  private JsonHttpParser jsonHttpParser;

  public BackendJob(String name, Type type, HttpRequestFactory requestFactory,
      AppsMarketplaceProject appsMarketplaceProject) {
    this.name = name;
    this.requestFactory = requestFactory;
    this.type = type;
    this.appsMarketplaceProject = appsMarketplaceProject;
    jsonFactory = new JacksonFactory();
    jsonHttpParser = new JsonHttpParser(jsonFactory);
  }

  public boolean getOperationStatus() {
    return operationStatus;
  }

  public void run() {
    operationStatus = false;
    sendInitialProgress();
    if (type == Type.GetVendorProfile) {
      runGetVendorProfile();
    } else if (type == Type.CreateVendorProfile) {
      runCreateVendorProfile();
    } else if (type == Type.GetAppListing) {
      runGetAppListing();
    } else if (type == Type.CreateAppListing) {
      runCreateAppListing();
    } else if (type == Type.UpdateAppListing) {
      runUpdateAppListing();
    }
    sendFinalProgress();
  }

  public void setProgressDialog(ProgressMonitorDialog pdlg) {
    this.dlg = pdlg;
    this.monitor = dlg.getProgressMonitor();
  }

  private AppListing buildAppListing(
      AppsMarketplaceProject appsMarketPlaceProject) {
    AppListing appListing = new AppListing();
    appListing.name = AppsMarketplaceProjectProperties.getAppListingName(
        appsMarketplaceProject.getJavaProject().getProject());
    appListing.categoryId =
        AppsMarketplaceProjectProperties.getAppListingCategory(
            appsMarketplaceProject.getJavaProject().getProject());
    appListing.appManifest = readFileAsString(
        appsMarketPlaceProject.getManifestXml("application-manifest.xml"));
    appListing.listingManifest = readFileAsString(
        appsMarketPlaceProject.getManifestXml("listing-manifest.xml"));
    appListing.listingId = AppsMarketplaceProjectProperties.getAppListingId(
        appsMarketplaceProject.getJavaProject().getProject());
    return appListing;
  }

  private VendorProfile buildVendorProfile() {
    VendorProfile vendorProfile = null;

    if (GoogleLogin.getInstance().isLoggedIn()) {
      EmailAddress emailAddress = new EmailAddress(
          GoogleLogin.getInstance().getEmail());
      if (emailAddress.isValid()) {
        vendorProfile = new VendorProfile();
        vendorProfile.vendorName = emailAddress.getUser();
        vendorProfile.email = emailAddress.toString();
        vendorProfile.vendorId = Long.valueOf(-1);
      }

    }
    return vendorProfile;
  }

  private String readFileAsString(IFile file) {
    StringBuffer fileData = new StringBuffer(1024);
    InputStreamReader inputStreamReader = null;
    try {
      inputStreamReader = new InputStreamReader(file.getContents());
    } catch (CoreException e) {
      AppsMarketplacePluginLog.logError(e);
      return "";
    }
    char[] buf = new char[1024];
    int numRead = 0;
    try {
      while ((numRead = inputStreamReader.read(buf)) != -1) {
        String readData = String.valueOf(buf, 0, numRead);
        fileData.append(readData);
      }
    } catch (IOException e) {
      AppsMarketplacePluginLog.logError(e);
      return "";
    }
    try {
      inputStreamReader.close();
    } catch (IOException e) {
      AppsMarketplacePluginLog.logError(e);
      return "";
    }
    return fileData.toString();
  }

  private void runCreateAppListing() {
    DataStorage.clearListedAppListing();
    GenericUrl url = new GoogleUrl(
        EnterpriseMarketplaceUrl.generateAppListingUrl()
        + DataStorage.getVendorProfile().vendorId);

    AppListing appListingBody = buildAppListing(appsMarketplaceProject);
    JsonHttpContent content = new JsonHttpContent(jsonFactory, appListingBody);

    AppListing appListing;
    try {
      HttpRequest request = requestFactory.buildPostRequest(url, content);
      request.addParser(jsonHttpParser);
      request.setReadTimeout(readTimeout);
      HttpResponse response = request.execute();
      appListing = response.parseAs(AppListing.class);
      operationStatus = validateAppListing(appListing, appListingBody);
      if (operationStatus) {
        DataStorage.setListedAppListing(appListing);
      }
      response.getContent().close();
    } catch (IOException e) {
      AppsMarketplacePluginLog.logError(e);
    }
  }

  private void runCreateVendorProfile() {
    DataStorage.clearVendorProfile();
    GenericUrl url = new GoogleUrl(
        EnterpriseMarketplaceUrl.generateVendorProfileUrl());

    VendorProfile body = buildVendorProfile();

    JsonHttpContent content = new JsonHttpContent(jsonFactory, body);

    VendorProfile vendorProfile = null;
    try {
      HttpRequest request = requestFactory.buildPostRequest(url, content);
      request.addParser(jsonHttpParser);
      request.setReadTimeout(readTimeout);
      HttpResponse response = request.execute();
      vendorProfile = response.parseAs(VendorProfile.class);
      if (vendorProfile != null) {
        DataStorage.setVendorProfile(vendorProfile);
        operationStatus = true;
      }
      response.getContent().close();
    } catch (IOException e) {
      AppsMarketplacePluginLog.logError(e);
    }
  }

  private void runGetAppListing() {
    DataStorage.clearAppListings();
    GenericUrl url = new GoogleUrl(
        EnterpriseMarketplaceUrl.generateAppListingUrl()
        + DataStorage.getVendorProfile().vendorId);

    AppListingList appListingList;
    try {
      HttpRequest request = requestFactory.buildGetRequest(url);
      request.addParser(jsonHttpParser);
      request.setReadTimeout(readTimeout);
      HttpResponse response = request.execute();
      appListingList = response.parseAs(AppListingList.class);
      if (appListingList != null && appListingList.appListings != null) {
        operationStatus = true;
        DataStorage.setAppListings(appListingList.appListings);
      }
      response.getContent().close();
    } catch (IOException e) {
      AppsMarketplacePluginLog.logError(e);
    }
  }

  private void runGetVendorProfile() {
    DataStorage.clearVendorProfile();
    GenericUrl url = new GoogleUrl(
        EnterpriseMarketplaceUrl.generateVendorProfileUrl());

    VendorProfile vendorProfile = null;
    try {
      HttpRequest request = requestFactory.buildGetRequest(url);
      request.addParser(jsonHttpParser);
      request.setReadTimeout(readTimeout);
      HttpResponse response = request.execute();
      vendorProfile = response.parseAs(VendorProfile.class);
      if (vendorProfile != null && vendorProfile.vendorId != null
          && vendorProfile.email != null
          && !StringUtilities.isEmpty(vendorProfile.email)) {
        DataStorage.setVendorProfile(vendorProfile);
        operationStatus = true;
      }
      response.getContent().close();
    } catch (IOException e) {
      AppsMarketplacePluginLog.logError(e);
    }
  }

  private void runUpdateAppListing() {
    DataStorage.clearListedAppListing();
    GenericUrl url = new GoogleUrl(
        EnterpriseMarketplaceUrl.generateAppListingUrl()
        + DataStorage.getVendorProfile().vendorId);

    AppListing appListingBody = buildAppListing(appsMarketplaceProject);

    JsonHttpContent content = new JsonHttpContent(jsonFactory, appListingBody);

    AppListing appListing;
    try {
      HttpRequest request = requestFactory.buildPutRequest(url, content);
      request.addParser(jsonHttpParser);
      request.setReadTimeout(readTimeout);
      HttpResponse response = request.execute();
      appListing = response.parseAs(AppListing.class);
      operationStatus = validateAppListing(appListing, appListingBody);
      if (operationStatus) {
        DataStorage.setListedAppListing(appListing);
      }
      response.getContent().close();
    } catch (IOException e) {
      AppsMarketplacePluginLog.logError(e);
    }
  }

  private void sendFinalProgress() {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        if (monitor.isCanceled() == false) {
          monitor.worked(2);
          monitor.done();
          dlg.close();
        }
      }
    });
  }

  private void sendInitialProgress() {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        monitor.beginTask(name, 2);
        monitor.worked(1);
      }
    });
  }

  private boolean validateAppListing(
      AppListing createdAppListing, AppListing appListing) {
    boolean status = false;

    // Atleast name should match
    if (createdAppListing != null && appListing != null
        && createdAppListing.name.equals(appListing.name)) {
      status = true;
    }
    return status;
  }

}