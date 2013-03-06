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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectory;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectoryFactory;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.platform.ManagedApiContainer;
import com.google.gdt.eclipse.managedapis.platform.ManagedApiInstallJob;
import com.google.gdt.googleapi.core.ApiDirectoryItem;
import com.google.gdt.googleapi.core.ApiDirectoryListing;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for the Update API action. This handler performs the update action on
 * selected APIs by finding APIs from user selections, cross-referencing these
 * against the preferred APIs from the directory service and calling the
 * ManagedApiInstallJob for each project on which we are updating APIs.
 */
@SuppressWarnings("restriction")
public class ManagedApiUpdateHandler extends AbstractHandler {

  protected static final int TICKS_PER_API = 500;
  protected static final int TICKS_DOWNLOAD_DIRECTORY = 1000;

  /**
   * Count the APIs which are values in the apiSelectionMultiMap.
   * 
   * @param apiSelectionMultiMap
   * @return
   */
  private static int countApis(
      final Map<ManagedApiProject, List<ManagedApi>> apiSelectionMultiMap) {
    int apiRunningCount = 0;
    for (ManagedApiProject project : apiSelectionMultiMap.keySet()) {
      apiRunningCount += apiSelectionMultiMap.get(project).size();
    }
    return apiRunningCount;
  }

  /**
   * Main entry point for Updating selected ManagedApis.
   */
  public Object execute(ExecutionEvent event) throws ExecutionException {
    final Map<ManagedApiProject, List<ManagedApi>> apiSelectionMultiMap = createApiSelectionMap(event);
    final int projectCount = apiSelectionMultiMap.size();
    final int apiCount = countApis(apiSelectionMultiMap);

    // update job defined inline.
    Job updateJob = new Job("Update Google APIs") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        // calculate total ticks
        int totalTicks = TICKS_DOWNLOAD_DIRECTORY + (apiCount * TICKS_PER_API);
        SubMonitor submon = SubMonitor.convert(monitor, "Update Google APIs",
            totalTicks);

        IStatus jobStatus = Status.OK_STATUS;
        // Load the remote directory
        ApiDirectoryFactory apiDirectoryFactory = ManagedApiPlugin.getDefault().getApiDirectoryFactory();
        ApiDirectory apiDirectory = apiDirectoryFactory.buildApiDirectory();
        jobStatus = apiDirectory.run(submon.newChild(TICKS_DOWNLOAD_DIRECTORY));

        // Upgrade by project
        List<IStatus> subtaskStati = new ArrayList<IStatus>(projectCount);
        if (jobStatus == Status.OK_STATUS) {
          ApiDirectoryListing listing = apiDirectory.getApiDirectoryListing();

          for (ManagedApiProject project : apiSelectionMultiMap.keySet()) {
            IStatus projectStatus;

            // convert ManagedApis into ManagedApiEntrys
            List<ManagedApiEntry> entries = new ArrayList<ManagedApiEntry>();
            for (ManagedApi managedApi : apiSelectionMultiMap.get(project)) {
              ApiDirectoryItem directoryEntry = listing.getPreferredByName(managedApi.getName());
              if (directoryEntry != null) {
                entries.add(new ManagedApiEntry(directoryEntry, managedApi));
              }
            }

            ManagedApiInstallJob installJob = new ManagedApiInstallJob(
                entries.size() > 1 ? "Update Google APIs" : "Update Google API",
                entries, project.getProject(), "Update Google API {0}");
            projectStatus = installJob.run(submon.newChild(TICKS_PER_API
                * entries.size()));
            if (projectStatus != Status.OK_STATUS) {
              if (projectStatus == Status.CANCEL_STATUS) {
                jobStatus = Status.CANCEL_STATUS;
                break;
              } else {
                subtaskStati.add(projectStatus);
              }
            }
          }
        }

        if (jobStatus == Status.CANCEL_STATUS) {
          return jobStatus;
        } else if (!subtaskStati.isEmpty()) {

          if (jobStatus != Status.OK_STATUS) {
            subtaskStati.add(jobStatus);
          }
          jobStatus = new MultiStatus(ManagedApiPlugin.PLUGIN_ID,
              IStatus.WARNING,
              subtaskStati.toArray(new IStatus[subtaskStati.size()]),
              "Updating Google API failed. Refer to details.", null);
        }

        if (jobStatus != Status.OK_STATUS) {
          final IStatus jobStatusPtr = jobStatus;
          if (!jobStatus.isOK()) {
            Display.getDefault().asyncExec(new Runnable() {
              public void run() {
                ManagedApiPlugin.getDefault().getLog().log(jobStatusPtr);
                MessageDialog.openError(
                    SWTUtilities.getShell(),
                    "Google Plugin for Eclipse",
                    "There was a problem updating the API bundles. See the Error Log for more details.");
              }
            });
          }
        }
        // Return from the job
        return jobStatus;
      }
    };

    try {
      // Create job, provide a scheduling rule and schedule it.
      PlatformUI.getWorkbench().getProgressService().showInDialog(
          SWTUtilities.getShell(), updateJob);
      updateJob.setUser(true);
      updateJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
      updateJob.schedule();
    } catch (IllegalStateException e) {
      ManagedApiLogger.info(e,
          "Workbench has not been created yet. Running outside eclipse?");
    }

    // return from handler
    return null;
  }

  /**
   * Create the apiSelectionMultiMap from the ExecutionEvent passed to the
   * handler.
   * 
   * @param event the generic event object passed to a Handler.
   * @return a multi-map representing the selected APIs.
   */
  private Map<ManagedApiProject, List<ManagedApi>> createApiSelectionMap(
      ExecutionEvent event) {
    Map<ManagedApiProject, List<ManagedApi>> apiSelectionMap = new HashMap<ManagedApiProject, List<ManagedApi>>();
    // Convert selection into a multi-map from ManagedApiProject to a list of
    // ManagedApis.
    ISelection selection = HandlerUtil.getCurrentSelection(event);
    if (selection instanceof IStructuredSelection) {
      Object[] items = ((IStructuredSelection) selection).toArray();
      if (items != null) {
        for (Object item : ((IStructuredSelection) selection).toArray()) {
          if (item instanceof ClassPathContainer) {
            ClassPathContainer container = (ClassPathContainer) item;
            if (ManagedApiContainer.isManagedApiContainer(container)) {
              ManagedApiProject project = ManagedApiContainer.getManagedApiProjectForClassPathContainer(container);
              ManagedApi managedApi = ManagedApiContainer.getManagedApiForClassPathContainer(
                  project, container);
              List<ManagedApi> apiList;
              if (apiSelectionMap.containsKey(project)) {
                apiList = apiSelectionMap.get(project);
              } else {
                apiList = new ArrayList<ManagedApi>();
                apiSelectionMap.put(project, apiList);
              }
              apiList.add(managedApi);
            }
          }
        }
      }
    }
    return apiSelectionMap;
  }
}
