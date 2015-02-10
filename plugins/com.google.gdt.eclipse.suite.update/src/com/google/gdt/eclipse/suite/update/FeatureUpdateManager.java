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
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery.Data;
import com.google.gdt.eclipse.core.jobs.DownloadJob;
import com.google.gdt.eclipse.core.update.internal.core.DailyUpdateCheckStrategy;
import com.google.gdt.eclipse.core.update.internal.core.UpdateCheckStrategy;
import com.google.gdt.eclipse.suite.preferences.GdtPreferences;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.progress.IProgressConstants;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles update checks for the plugin.
 */
public class FeatureUpdateManager implements DownloadJobCreator {

  protected static File createTmpFile() throws IOException {
    File tempFile = File.createTempFile("site", "xml");
    tempFile.deleteOnExit();
    return tempFile;
  }

  protected final UpdateSiteURLGenerator urlGenerator;

  private final List<DownloadJob> downloadJobs;

  private IJobChangeListener removeJobOnCompleteListener = new JobChangeAdapter() {
    @Override
    public void done(IJobChangeEvent event) {
      synchronized (downloadJobs) {
        downloadJobs.remove(event.getJob());
      }
    }
  };
  private FeatureUpdateCheckersMap updateCheckersMap;

  private final UpdateCheckStrategy updateCheckStrategy = new DailyUpdateCheckStrategy();

  /**
   * Constructs an instance with a generator for an update site URL, and a
   * {@link FeatureUpdateCheckersMap}.
   *
   * @param urlGenerator generates the URL for the update site
   * @param updateCheckersMap if the update site URL has the string provided by
   *          the key of the UpdateSiteToken then the corresponding checker
   *          scans the update site to determine if an update is available.
   */
  public FeatureUpdateManager(UpdateSiteURLGenerator urlGenerator,
      FeatureUpdateCheckersMap updateCheckersMap) {

    this.updateCheckersMap = updateCheckersMap;
    this.urlGenerator = urlGenerator;
    downloadJobs = new ArrayList<DownloadJob>();
  }

  /**
   * Terminates any update checks that are currently in progress.
   */
  public void cancelPendingUpdates() {
    synchronized (downloadJobs) {
      for (DownloadJob dlj : downloadJobs) {
        dlj.cancel();
      }

      downloadJobs.clear();
    }
  }

  /**
   * Checks the update site to see if any updates are available. The check will
   * be aborted if update notifications are disabled, or an update check is
   * currently in progress, or, if according to the
   * <code>updateCheckStrategy</code>, it is not time for an update check as
   * yet.
   */
  public void checkForUpdates() {
    if (!GdtPreferences.areUpdateNotificationsEnabled()) {
      // Update checks are disabled
      return;
    }

    synchronized (downloadJobs) {
      if (downloadJobs.size() > 0) {
        // Update check is in progress
        return;
      }
    }

    if (!updateCheckStrategy.shouldCheckForUpdates(GdtPreferences.getLastUpdateTimeMillis())) {
      // Still waiting to check again
      return;
    }

    // Assume that the update check will succeed.
    GdtPreferences.setLastUpdateTimeMillis(System.currentTimeMillis());

    ExtensionQuery<DownloadJobCreator> extQuery = new ExtensionQuery<DownloadJobCreator>(
        GdtExtPlugin.PLUGIN_ID, "updatePing", "class");

    List<ExtensionQuery.Data<DownloadJobCreator>> dlCreators = extQuery.getData();

    // add this object too so that its download job gets run too, eg adding a
    // fake extension query result
    dlCreators.add(new Data<DownloadJobCreator>(this, null));

    for (Data<DownloadJobCreator> datum : dlCreators) {
      try {
        File tempFile = createTmpFile();
        DownloadJobCreator dljc = datum.getExtensionPointData();
        DownloadJob downloadJob = dljc.createDownloadJob(tempFile, urlGenerator);
        addDownloadJob(downloadJob);
      } catch (Exception e) {
        CorePluginLog.logError(e);
      }
    }
  }

  @Override
  public DownloadJob createDownloadJob(final File tempFile,
      UpdateSiteURLGenerator generator) {
    URL updateSiteUrl;
    try {
      updateSiteUrl = generator.generateURL();
    } catch (MalformedURLException e) {
      return null;
    }
    DownloadJob newDownloadJob = new DownloadJob(
        "Download compositeArtifacts.xml", updateSiteUrl, tempFile);
    CompositeArtifactsDowloadJobChangeListener
        compositeArtifactsDowloadJobChangeListener = new CompositeArtifactsDowloadJobChangeListener(
        tempFile, updateCheckersMap, this);
    newDownloadJob.addJobChangeListener(compositeArtifactsDowloadJobChangeListener);
    return newDownloadJob;
  }

  protected void addDownloadJob(DownloadJob downloadJob) {

    if (downloadJob != null) {
      // This is a non-user job, so make sure that it has no presence in the
      // Progress View
      downloadJob.setSystem(true);

      downloadJob.addJobChangeListener(removeJobOnCompleteListener);

      // If the job returns an ERROR status, make sure that it does not show
      // a modal dialog with the ERROR information; we want a silent failure
      downloadJob.setProperty(
          IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, true);

      synchronized (downloadJobs) {
        downloadJob.schedule();
        downloadJobs.add(downloadJob);
      }
    }
  }

}
