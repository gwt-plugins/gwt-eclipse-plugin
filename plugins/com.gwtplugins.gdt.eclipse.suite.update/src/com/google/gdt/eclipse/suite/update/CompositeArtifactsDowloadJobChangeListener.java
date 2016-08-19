/*******************************************************************************
 * Copyright 2009 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.update;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.jobs.DownloadJob;
import com.google.gdt.eclipse.core.update.internal.core.FeatureUpdateChecker.UpdateInfo;
import com.google.gdt.eclipse.platform.update.IUpdateDetailsPresenter;
import com.google.gdt.eclipse.platform.update.UpdateDetailsPresenter;
import com.google.gdt.eclipse.suite.preferences.GdtPreferences;
import com.google.gdt.eclipse.suite.update.FeatureUpdateCheckersMap.UpdateSiteToken;
import com.google.gdt.eclipse.suite.update.ui.UpdateNotificationToastPopup;
import com.google.gdt.eclipse.suite.update.ui.UpdateNotificationToastPopup.NotificationControlSelectedHandler;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Downloads site.xml for GPE, GAE SDK and GWT SDK when the download for compositeArtifacts.xml is
 * complete.
 */
public class CompositeArtifactsDowloadJobChangeListener extends JobChangeAdapter {

  private static final int SITE_XML_DOWNLOAD_TIMEOUT = 60;

  private final FeatureUpdateManager featureUpdateManager;
  private NotificationControlSelectedHandler featureUpdateNotificationSelectionHandler;
  private URL gaeSdkSiteUrl;
  private URL gpeSiteUrl;
  private URL gwtSdkSiteUrl;
  private CountDownLatch siteXmlDownloadJobsCountDownLatch;
  private final File tempFile;
  private final FeatureUpdateCheckersMap updateCheckersMap;
  private List<UpdateInfo> updateList;

  public CompositeArtifactsDowloadJobChangeListener(File tempFile,
      FeatureUpdateCheckersMap checkers, FeatureUpdateManager featureUpdateManager) {
    this.tempFile = tempFile;
    this.updateCheckersMap = checkers;
    this.featureUpdateManager = featureUpdateManager;
  }

  /**
   * Called on completion of the download of compositeArtifacts.xml. This parses the file and finds
   * the location of the site.xml for GPE and GAE SDK. A sample compositeArtifacts.xml is:
   *
   * <pre>
   * {@code
   * <?compositeArtifactRepository version='1.0.0'?>
   * <repository name="..." type="org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository" version="1.0.0">
   * <properties size="1">
   * ...
   * </properties>
   * <children size="1">
   *  <child location="...{GPE_CORE token}..."/>
   *  <child location="...{GAE_SDK token}..."/>
   *  <child location="...{GWT_SDK token}..."/>
   * </children>
   * </repository>
   * }
   * </pre>
   */
  @Override
  public void done(IJobChangeEvent event) {
    IStatus result = event.getResult();
    if (result != null && result.isOK()) {
      try {
        SAXParser saxparser = SAXParserFactory.newInstance().newSAXParser();
        saxparser.parse(tempFile, new DefaultHandler() {

          private static final String CHILD_ELEMENT_NODE = "child";
          private static final String LOCATION_ATTRIBUTE = "location";
          private static final String SITE_XML_FILE = "site.xml";
          private static final String SLASH = "/";
          @Override
          public void startElement(String uri, String localName, String qualifiedName,
              Attributes attributes) {
            if (qualifiedName.equals(CHILD_ELEMENT_NODE)) {
              try {
                if (attributes.getValue(LOCATION_ATTRIBUTE).contains(
                    UpdateSiteToken.GPE_CORE.getToken())) {
                  gpeSiteUrl = getSiteXmlUrl(attributes.getValue(LOCATION_ATTRIBUTE));
                } else if (attributes.getValue(LOCATION_ATTRIBUTE).contains(
                    UpdateSiteToken.GAE_SDK.getToken())) {
                  gaeSdkSiteUrl = getSiteXmlUrl(attributes.getValue(LOCATION_ATTRIBUTE));
                } else if (attributes.getValue(LOCATION_ATTRIBUTE).contains(
                    UpdateSiteToken.GWT_SDK.getToken())) {
                  gwtSdkSiteUrl = getSiteXmlUrl(attributes.getValue(LOCATION_ATTRIBUTE));
                }
              } catch (MalformedURLException e) {
                CorePluginLog.logError("Downloaded compositeArtifacts.xml is invalid");
              }
            }
          }

          private URL getSiteXmlUrl(String baseUrl) throws MalformedURLException {
            if (!baseUrl.endsWith(SLASH)) {
              baseUrl += SLASH;
            }
            return (new URL(baseUrl + SITE_XML_FILE));
          }
        });

        // After parsing is done, create the download jobs to download the two site.xml from GPE and
        // GAE SDK update sites.
        updateList = new ArrayList<UpdateInfo>();
        DownloadJob gpeDownloadJob = createDownloadJob("download GPE site.xml", gpeSiteUrl,
            UpdateSiteToken.GPE_CORE);
        DownloadJob gaeDownloadJob = createDownloadJob("download GAE SDK site.xml", gaeSdkSiteUrl,
            UpdateSiteToken.GAE_SDK);
        DownloadJob gwtDownloadJob = createDownloadJob(
            "download GWT SDK site.xml", gwtSdkSiteUrl, UpdateSiteToken.GWT_SDK);
        addDownloadJobs(gpeDownloadJob, gaeDownloadJob, gwtDownloadJob);
        siteXmlDownloadJobsCountDownLatch.await(SITE_XML_DOWNLOAD_TIMEOUT, TimeUnit.SECONDS);
        if (!updateList.isEmpty()) {
          notifyOfAvailableUpdates(updateList);
        }
      } catch (SAXException e) {
        CorePluginLog.logError(e, "Unable to parse the downloaded compositeArtifacts.xml");
      } catch (IOException e) {
        CorePluginLog.logError(e, "Error in reading compositeArtifacts.xml");
      } catch (ParserConfigurationException e) {
        CorePluginLog.logError(e);
      } catch (InterruptedException e) {
        CorePluginLog.logError(e);
      }
    }
  }

  private void addDownloadJobs(DownloadJob... downloadJobs) {
    int siteXmlDownloadJobs = 0;
    for (DownloadJob downloadJob : downloadJobs) {
      if (downloadJob != null) {
        siteXmlDownloadJobs++;
      }
    }
    siteXmlDownloadJobsCountDownLatch = new CountDownLatch(siteXmlDownloadJobs);
    // We do not want to trigger countDown() on siteXmlDownloadJobsCountDownLatch on completion of
    // the download before the CountDownLatch is initialized even though it's unlikely. Hence, we
    // have two loops and initialize the CountDownLatch before scheduling any downloads.
    for (DownloadJob downloadJob : downloadJobs) {
      if (downloadJob != null) {
        featureUpdateManager.addDownloadJob(downloadJob);
      }
    }
  }

  /**
   * Creates a download job and adds it to the download jobs list in the featureUpdateManager.
   *
   * @param token determines the JobChangeListener attached to the DownloadJob from the
   *          {@link FeatureUpdateCheckersMap}
   * @return returns the DownloadJob created if successful. @code{null} otherwise.
   */
  private DownloadJob createDownloadJob(String name, URL siteUrl, UpdateSiteToken token) {
    if (siteUrl == null) {
      CorePluginLog.logError("Could not " + name + "becuase the update site was not found in the "
          + "downloaded compositeArtifacts.xml");
      return null;
    }
    try {
      final File temp = FeatureUpdateManager.createTmpFile();
      temp.deleteOnExit();
      DownloadJob newDownloadJob = new DownloadJob(name, siteUrl, temp);
      newDownloadJob.addJobChangeListener(getSiteDownloadJobChangeListener(temp, token));
      return newDownloadJob;
    } catch (IOException e) {
      CorePluginLog.logError(e);
      return null;
    }
  }

  private Element getRootElement(File content) {
    Reader reader = null;
    try {
      reader = new FileReader(content);
      DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      parser.setErrorHandler(new DefaultHandler());
      Element rootElement = parser.parse(new InputSource(reader)).getDocumentElement();
      return rootElement;
    } catch (ParserConfigurationException e) {
      CorePluginLog.logError(e, "Could not parse the XML file " + content.getAbsolutePath());
    } catch (SAXException e) {
      CorePluginLog.logError(e, "Could not parse the XML file " + content.getAbsolutePath());
    } catch (IOException e) {
      CorePluginLog.logError(e, "Could not read the XML file " + content.getAbsolutePath());
    } finally {
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException e) {
        CorePluginLog.logError(e, "Could not read the XML file " + content.getAbsolutePath());
      }
    }

    // Return null if anything goes wrong
    return null;
  }

  private JobChangeAdapter getSiteDownloadJobChangeListener(
      final File tempFile, final UpdateSiteToken token) {

    return new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        IStatus result = event.getResult();
        if (result != null && result.isOK()) {
          Element rootElement = getRootElement(tempFile);
          if (rootElement != null) {
            List<UpdateInfo> updates =
                updateCheckersMap.get(token).checkForUpdates(rootElement);
            assert (updates != null);
            if (!updates.isEmpty()) {
              updateList.addAll(updates);
            }
          }
        }
        // Signal the parent on completion of the download.
        siteXmlDownloadJobsCountDownLatch.countDown();
      }
    };
  }

  private void notifyOfAvailableUpdates(final List<UpdateInfo> updates) {
    // We can only arrive here if an update was available.
    assert (updates != null && !updates.isEmpty());

    /*
     * Asynchronously run the following code on the UI thread since it manipulates widgets.
     */
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      @SuppressWarnings("restriction")
      public void run() {

        if (featureUpdateNotificationSelectionHandler == null) {
          featureUpdateNotificationSelectionHandler = new NotificationControlSelectedHandler() {
            @Override
            public void onNotificationControlSelected() {
              IUpdateDetailsPresenter updateDetailsPresenter = new UpdateDetailsPresenter();
              updateDetailsPresenter.presentUpdateDetails(
                  GdtExtPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
                  GdtExtPlugin.getFeatureUpdateSiteUrl(),
                  GdtExtPlugin.getDefault().getLog(), GdtExtPlugin.PLUGIN_ID);

              /*
               * Setting this preference value will suppress future update notifications for any
               * lower versions.
               */
              for (UpdateInfo info : updates) {
                GdtPreferences.setVersionForLastAcknowlegedUpdateNotification(
                    info.getFeatureId(), info.getUpdatedFeatureVersion());
              }
            }
          };
        }

        /*
         * Only show the notification if the user has their preferences set to enable the display of
         * update notifications, and if there is actually an update available.
         */
        if (GdtPreferences.areUpdateNotificationsEnabled()) {
          UpdateNotificationToastPopup toast = new UpdateNotificationToastPopup(
              Display.getCurrent());
          toast.addNotificationControlSelectedHandler(featureUpdateNotificationSelectionHandler);
          toast.create();
          toast.open();
        }
      }
    });
  }
}
