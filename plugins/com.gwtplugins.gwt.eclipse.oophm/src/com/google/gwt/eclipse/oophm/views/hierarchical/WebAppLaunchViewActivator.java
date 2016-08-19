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
package com.google.gwt.eclipse.oophm.views.hierarchical;

import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.processors.RemoteUiArgumentProcessor;
import com.google.gwt.eclipse.oophm.Activator;
import com.google.gwt.eclipse.oophm.model.BrowserTab;
import com.google.gwt.eclipse.oophm.model.ILogListener;
import com.google.gwt.eclipse.oophm.model.IModelNode;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.Log;
import com.google.gwt.eclipse.oophm.model.LogEntriesRemovedEvent;
import com.google.gwt.eclipse.oophm.model.LogEntryAddedEvent;
import com.google.gwt.eclipse.oophm.model.Server;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModelEvent;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModelListenerAdapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.WorkbenchJob;

import java.util.List;

/**
 * Activates the {@link WebAppLaunchView} whenever the model changes in
 * whichever perspective is active.
 */
@SuppressWarnings("unchecked")
public class WebAppLaunchViewActivator extends WebAppDebugModelListenerAdapter
    implements
      ILogListener {

  /**
   * Job that causes the {@link WebAppLaunchView} to be visible.
   */
  private static class ShowViewJob extends WorkbenchJob {

    private final Object boldingLock = new Object();
    private boolean bolding = false;

    public ShowViewJob() {
      super("Web App Launch View Activator");
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor) {
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window != null) {
        IWorkbenchPage page = window.getActivePage();
        if (page != null) {
          IViewPart webAppView = page.findView(WebAppLaunchView.ID);
          if (webAppView == null) {
            try {
              webAppView = page.showView(WebAppLaunchView.ID);
            } catch (PartInitException e) {
              return StatusUtilities.newErrorStatus(e, Activator.PLUGIN_ID);
            }
          }

          if (webAppView != null) {
            synchronized(boldingLock) {
              if (bolding) {
                if (webAppView.getSite() != null) {
                  IWorkbenchSiteProgressService service = 
                    (IWorkbenchSiteProgressService) webAppView.getSite().getAdapter(
                      IWorkbenchSiteProgressService.class);
                  if (service != null) {
                    service.warnOfContentChange();
                  }
                }
              } else {
                page.bringToTop(webAppView);
              }
            }
          }
        }
      }
      return Status.OK_STATUS;
    }

    /**
     * If set to true, then this ShowViewJob will bold the title of the 
     * devmode view tab instead of showing the tab.
     * 
     * @param bolding Bolds the devmode tab title if true
     */
    public void setBolding(boolean bolding) {
      synchronized(boldingLock) {
        this.bolding = bolding;
      }
    }
    
  }

  private static final WebAppLaunchViewActivator instance = new WebAppLaunchViewActivator();

  public static WebAppLaunchViewActivator getInstance() {
    return instance;
  }

  private final ShowViewJob showViewJob = new ShowViewJob();

  private WebAppLaunchViewActivator() {
    // Not externally instantiable
  }

  @Override
  public void browserTabCreated(WebAppDebugModelEvent<BrowserTab> e) {
    BrowserTab browserTab = e.getElement();
    addLogListener(browserTab.getLog());
  }

  @Override
  public void browserTabTerminated(WebAppDebugModelEvent<BrowserTab> e) {
    BrowserTab browserTab = e.getElement();
    removeLogListener(browserTab.getLog());
  }

  @Override
  public void launchConfigurationLaunched(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
    scheduleShowJob(true);
  }

  @Override
  public void launchConfigurationLaunchUrlsChanged(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
    scheduleShowJob(true);
  }

  public void logEntriesRemoved(LogEntriesRemovedEvent e) {
    // Purposely ignored
  }

  public void newLogEntry(LogEntryAddedEvent e) {
    // if the entry needs attention, then set focus to the devmode view,
    // otherwise, just make the tab's text bold, ala the console view
    scheduleShowJob(e.needsAttention());
  }

  @Override
  public void serverCreated(WebAppDebugModelEvent<Server> e) {
    Server serverTab = e.getElement();
    addLogListener(serverTab.getLog());
  }

  @Override
  public void serverTerminated(WebAppDebugModelEvent<Server> e) {
    Server serverTab = e.getElement();
    removeLogListener(serverTab.getLog());
  }

  private void addLogListener(Log<? extends IModelNode> log) {
    log.addLogListener(this);
  }

  private void removeLogListener(Log<? extends IModelNode> log) {
    log.removeLogListener(this);
  }

  private void scheduleShowJob(boolean needsFocus) {
    showViewJob.setBolding(!needsFocus);
    /*
     * Jobs are thread safe, worst case we schedule one more job than necessary.
     * Also, the model fires events using a single thread.
     */
    showViewJob.schedule(100);
  }
}
