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
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.oophm.Activator;
import com.google.gwt.eclipse.oophm.DevModeImages;
import com.google.gwt.eclipse.oophm.devmode.DevModeServiceClient;
import com.google.gwt.eclipse.oophm.devmode.DevModeServiceClientManager;
import com.google.gwt.eclipse.oophm.model.BrowserTab;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.Log;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModel;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModelEvent;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModelListenerAdapter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import java.util.List;

/**
 * A Web Application debug view that allows inspections of launch
 * configurations, browsers and servers.
 */
public class WebAppLaunchView extends ViewPart {
  /**
   * Enumeration used for persisting and restoring the layout state of this
   * view.
   */
  enum LayoutType {
    BREADCRUMB, TREE;

    public static LayoutType toLayoutType(String layoutName) {
      try {
        if (layoutName != null) {
          return valueOf(layoutName);
        }
      } catch (IllegalArgumentException e) {
        // ignored;
      }

      // Return BREADCRUMB as the default
      return BREADCRUMB;
    }
  }

  /**
   * Action for clearing a log viewer in the current selection.
   */
  private final class ClearLogViewerAction extends Action {
    private ClearLogViewerAction() {
      super("Clear Log Viewer", Activator.getDefault().getImageDescriptor(
          DevModeImages.CLEAR_LOG));
    }

    @Override
    public void run() {
      ISelection selection = currentLayout.getSelection();
      if (selection.isEmpty()) {
        return;
      }

      if (selection instanceof IStructuredSelection) {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        Object firstElement = structuredSelection.getFirstElement();
        if (firstElement instanceof BrowserTab) {
          BrowserTab browserTab = (BrowserTab) firstElement;
          browserTab.clearLog();
        }

        // TODO: When we have a server entry, we'll have to clear its logs as
        // well

        clearLogAction.setEnabled(false);
      }
    }
  }

  /**
   * Controls when the ClearLogViewerAction should be enabled/disabled based on
   * the current selection.
   */
  private final class ClearLogViewerEnablementController implements
      ISelectionChangedListener {

    public void selectionChanged(SelectionChangedEvent event) {
      ISelection selection = event.getSelection();
      if (selection.isEmpty()) {
        // Empty selection disables the action
        clearLogAction.setEnabled(false);
        return;
      }

      if (selection instanceof IStructuredSelection) {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        Object firstElement = structuredSelection.getFirstElement();
        clearLogAction.setEnabled(shouldEnableClearLogAction(firstElement));
      }
    }

    private boolean shouldEnableClearLogAction(Object firstElement) {
      if (firstElement instanceof BrowserTab) {
        Log<?> log = ((BrowserTab) firstElement).getLog();
        return log != null && log.hasDisclosedLogEntries();
      }

      return false;
    }
  }

  /**
   * Action for clearing terminated launches.
   */
  private final class ClearTerminatedLaunchesAction extends Action {
    private ClearTerminatedLaunchesAction() {
      super("Remove all terminated launches",
          Activator.getDefault().getImageDescriptor(
              DevModeImages.CLEAR_TERMINATED_LAUNCHES));
    }

    @Override
    public void run() {
      WebAppDebugModel.getInstance().removeTerminatedLaunchesFromModel();

      /*
       * At this point, we know that the view has been notified of the removals
       * of the terminated launches from the model. It is safe to disable the
       * "Clear Terminated Launches" button, because there is no way that the
       * viewer could have been notified about a newly-terminated launch while
       * this method is executing. Notifications occur on the UI thread, and
       * we're currently executing on the UI thread.
       */
      clearTerminatedLaunches.setEnabled(false);
    }
  }

  /**
   * Controls when the {@link ClearTerminatedLaunchesAction} should be
   * enabled/disabled based on the current selection.
   */
  private final class ClearTerminatedLaunchesEnablementController extends
      WebAppDebugModelListenerAdapter implements ISelectionChangedListener {
    public ClearTerminatedLaunchesEnablementController() {
      WebAppDebugModel.getInstance().addWebAppDebugModelListener(this);
    }

    @Override
    public void launchConfigurationTerminated(
        WebAppDebugModelEvent<LaunchConfiguration> e) {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          /*
           * We execute the runnable synchronously here, because we want to make
           * sure it's safe to enable the "Clear Terminated Launches" button. If
           * we were to execute this runnable asynchronously, the enabling may
           * happen after the user actually clicks the
           * "Clear Terminated Launches" action. (the likelihood of this is very
           * low).
           */
          clearTerminatedLaunches.setEnabled(true);
        }
      });
    }

    public void selectionChanged(SelectionChangedEvent event) {
      ISelection selection = event.getSelection();
      if (selection.isEmpty()) {
        clearTerminatedLaunches.setEnabled(false);
        return;
      }

      if (selection instanceof IStructuredSelection) {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        Object firstElement = structuredSelection.getFirstElement();
        clearTerminatedLaunches.setEnabled(shouldEnableAction(firstElement));
      }
    }

    private boolean shouldEnableAction(Object firstElement) {
      WebAppDebugModel model = WebAppDebugModel.getInstance();
      List<LaunchConfiguration> terminatedLaunchConfigurations = model.getTerminatedLaunchConfigurations();
      return terminatedLaunchConfigurations.size() > 0;
    }
  }

  /**
   * Action for restarting a server.
   */
  private final class ReloadServerAction extends Action {
    private ReloadServerAction() {
      super("Reload web server", Activator.getDefault().getImageDescriptor(
          DevModeImages.RELOAD_WEB_SERVER));
    }

    @Override
    public void run() {
      ISelection selection = currentLayout.getSelection();
      if (selection.isEmpty()) {
        return;
      }

      if (selection instanceof IStructuredSelection) {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        Object firstElement = structuredSelection.getFirstElement();

        final LaunchConfiguration lc;
        if (firstElement instanceof BrowserTab) {
          lc = ((BrowserTab) firstElement).getLaunchConfiguration();
        } else if (firstElement instanceof LaunchConfiguration) {
          lc = (LaunchConfiguration) firstElement;
        } else {
          lc = null;
        }

        if (lc == null || lc.isTerminated()) {
          return;
        }

        final DevModeServiceClient client = DevModeServiceClientManager.getInstance().getClient(
            lc);

        if (client == null) {
          return;
        }
        
        reloadServer.setEnabled(false); // ok because we're on the UI thread
        lc.setServerReloading(true);
        
        Thread t = new Thread(new Runnable() {
          public void run() {
            try {
              client.restartWebServer();
            } catch (Exception e) {
              Activator.getDefault().getLog().log(
                  new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                      "Unable to reload the web server.", e));
              Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                  MessageDialog.openError(getSite().getShell(),
                      "Development Mode View",
                      "Unable to reload the web server. See the Error Log for detals.");
                }
              });
            }
            lc.setServerReloading(false);
            
          }
        });
        t.setName("Restart Web Server Executor Thread");
        t.setDaemon(true);
        t.start();
      }
    }
  }

  /**
   * Controls when the {@link ReloadServerAction} should be enabled/disabled
   * based on the current selection.
   */
  private final class ReloadServerEnablementController extends
      WebAppDebugModelListenerAdapter implements ISelectionChangedListener {

    public ReloadServerEnablementController() {
      WebAppDebugModel.getInstance().addWebAppDebugModelListener(this);
    }

    @Override
    public void launchConfigurationRestartWebServerStatusChanged(
        WebAppDebugModelEvent<LaunchConfiguration> e) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          shouldEnableActionBasedOnSelection(currentLayout.getSelection());
        }
      });
    }

    @Override
    public void launchConfigurationTerminated(
        WebAppDebugModelEvent<LaunchConfiguration> e) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          shouldEnableActionBasedOnSelection(currentLayout.getSelection());
        }
      });
    }

    public void selectionChanged(SelectionChangedEvent event) {
      shouldEnableActionBasedOnSelection(event.getSelection());
    }

    private boolean shouldEnableAction(Object firstElement) {
      LaunchConfiguration launchConfiguration = null;
      if (firstElement instanceof BrowserTab) {
        BrowserTab browserTab = (BrowserTab) firstElement;
        launchConfiguration = browserTab.getLaunchConfiguration();
      } else if (firstElement instanceof LaunchConfiguration) {
        launchConfiguration = (LaunchConfiguration) firstElement;
      } else {
        return false;
      }

      return launchConfiguration.hasWebServer()
          && launchConfiguration.supportsRestartWebServer()
          && !launchConfiguration.isTerminated()
          && !launchConfiguration.isServerReloading();
    }

    private void shouldEnableActionBasedOnSelection(ISelection selection) {
      if (selection.isEmpty()) {
        reloadServer.setEnabled(false);
        return;
      }

      if (selection instanceof IStructuredSelection) {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        Object firstElement = structuredSelection.getFirstElement();
        reloadServer.setEnabled(shouldEnableAction(firstElement));
      }
    }
  }

  /**
   * Listens for a new launch, and if the preference is set,
   * remove terminated launches.
   */
  private final class RemoveTerminatedLaunchesOnLaunchListener extends
      WebAppDebugModelListenerAdapter {

    @Override
    public void launchConfigurationLaunched(
        WebAppDebugModelEvent<LaunchConfiguration> e) {
      Display.getDefault().syncExec(new Runnable() {
        @SuppressWarnings("restriction")
        public void run() {
          if (GWTPreferences.getRemoveTerminatedLaunches()) {
            clearTerminatedLaunches.run();
          }
        }
      });
    }

  }

  /**
   * Action for terminating a launch configuration.
   */
  private final class TerminateLaunchAction extends Action {
    public TerminateLaunchAction() {
      super("Terminate Selected Launch",
          Activator.getDefault().getImageDescriptor(
              DevModeImages.TERMINATE_LAUNCH));
    }

    @Override
    public void run() {
      ISelection selection = currentLayout.getSelection();
      if (selection.isEmpty()) {
        return;
      }

      if (selection instanceof IStructuredSelection) {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        Object firstElement = structuredSelection.getFirstElement();
        LaunchConfiguration launchConfiguration = null;
        if (firstElement instanceof BrowserTab) {
          BrowserTab browserTab = (BrowserTab) firstElement;
          launchConfiguration = browserTab.getLaunchConfiguration();
        } else if (firstElement instanceof LaunchConfiguration) {
          launchConfiguration = (LaunchConfiguration) firstElement;
        } else {
          return;
        }

        try {
          launchConfiguration.getLaunch().terminate();
          terminateLaunchAction.setEnabled(false);
        } catch (DebugException e) {
          Activator.getDefault().getLog().log(
              StatusUtilities.newErrorStatus(e, Activator.PLUGIN_ID));
        }
      }
    }
  }

  /**
   * Controls when the TerminateLaunchAction should be enabled/disabled based on
   * the current selection.
   */
  private final class TerminateLaunchEnablementController
      implements
        ISelectionChangedListener {

    public void selectionChanged(SelectionChangedEvent event) {
      ISelection selection = event.getSelection();
      if (selection.isEmpty()) {
        // Empty selection disables the action
        terminateLaunchAction.setEnabled(false);
        return;
      }

      if (selection instanceof IStructuredSelection) {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        Object firstElement = structuredSelection.getFirstElement();
        terminateLaunchAction.setEnabled(shouldEnableAction(firstElement));
      }
    }

    private boolean shouldEnableAction(Object firstElement) {
      LaunchConfiguration launchConfiguration = null;
      if (firstElement instanceof BrowserTab) {
        BrowserTab browserTab = (BrowserTab) firstElement;
        launchConfiguration = browserTab.getLaunchConfiguration();
      } else if (firstElement instanceof LaunchConfiguration) {
        launchConfiguration = (LaunchConfiguration) firstElement;
      } else {
        return false;
      }

      return !launchConfiguration.isTerminated();
    }
  }

  /**
   * The ID of the view as specified by the extension. Note this is hard-coded
   * since the ID differs from the plugin package name.
   */
  public static final String ID = "com.google.gwt.eclipse.DevModeView";

  protected static final Object[] NO_ELEMENTS = new Object[0];

  /**
   * Key used to store this view's layout in this plugin's
   * {@link IDialogSettings}.
   */
  private static final String DIALOG_PROPERTIES_LAYOUT_KEY = WebAppLaunchView.ID
      + ".layout";

  private BreadcrumbNavigationView breadcrumbLayout;
  private Action clearLogAction;
  private Action clearTerminatedLaunches;
  private ISelectionProvider currentLayout;

  private PageBook pageBook;

  private Action reloadServer;
  private Action switchToBreadcrumbLayoutAction;
  private Action switchToTreeLayoutAction;
  private Action terminateLaunchAction;

  private TreeNavigationView treeLayout;

  /**
   * This is a callback that will allow us to create the viewer and initialize
   * it.
   */
  public void createPartControl(Composite parent) {
    createActions();
    addTerminatedLaunchListener();
    createLayouts(parent);
    contributeToActionBars();
    initControls();
  }

  public IToolBarManager getToolbarManager() {
    return getViewSite().getActionBars().getToolBarManager();
  }

  /**
   * Passing the focus request to the viewer's control.
   */
  public void setFocus() {
    // TODO: Fill me in
  }

  private void addTerminatedLaunchListener() {
    WebAppDebugModel.getInstance().addWebAppDebugModelListener(
      new RemoveTerminatedLaunchesOnLaunchListener());
  }

  private void contributeToActionBars() {
    IActionBars bars = getViewSite().getActionBars();
    fillLocalPullDown(bars.getMenuManager());
    fillLocalToolBar(bars.getToolBarManager());
  }

  private void createActions() {

    switchToTreeLayoutAction = new Action("Tree", IAction.AS_RADIO_BUTTON) {
      @Override
      public void run() {
        switchToTreeLayout();
      }
    };

    switchToBreadcrumbLayoutAction = new Action("Breadcrumb",
        IAction.AS_RADIO_BUTTON) {
      @Override
      public void run() {
        switchToBreadcrumbLayout();
      }
    };

    clearLogAction = new ClearLogViewerAction();
    clearTerminatedLaunches = new ClearTerminatedLaunchesAction();
    reloadServer = new ReloadServerAction();
    terminateLaunchAction = new TerminateLaunchAction();
  }

  private void createLayouts(Composite parent) {
    pageBook = new PageBook(parent, SWT.NONE);

    WebAppDebugModel model = WebAppDebugModel.getInstance();

    treeLayout = new TreeNavigationView(pageBook, SWT.NONE);

    treeLayout.addSelectionChangedListener(new ClearLogViewerEnablementController());
    treeLayout.addSelectionChangedListener(new ClearTerminatedLaunchesEnablementController());
    treeLayout.addSelectionChangedListener(new ReloadServerEnablementController());
    treeLayout.addSelectionChangedListener(new TerminateLaunchEnablementController());

    treeLayout.setInput(model);

    breadcrumbLayout = new BreadcrumbNavigationView(pageBook, SWT.NONE);

    breadcrumbLayout.addSelectionChangedListener(new ClearLogViewerEnablementController());
    breadcrumbLayout.addSelectionChangedListener(new ClearTerminatedLaunchesEnablementController());
    breadcrumbLayout.addSelectionChangedListener(new ReloadServerEnablementController());
    breadcrumbLayout.addSelectionChangedListener(new TerminateLaunchEnablementController());

    breadcrumbLayout.setInput(model);
  }

  private void fillLocalPullDown(IMenuManager manager) {
    MenuManager layoutMenu = new MenuManager("Layout");

    layoutMenu.add(switchToTreeLayoutAction);
    layoutMenu.add(switchToBreadcrumbLayoutAction);

    manager.add(layoutMenu);
  }

  private void fillLocalToolBar(IToolBarManager manager) {
    manager.add(terminateLaunchAction);
    manager.add(clearTerminatedLaunches);
    manager.add(new Separator());
    manager.add(reloadServer);
    manager.add(new Separator());
    manager.add(clearLogAction);
  }

  private IDialogSettings getDialogSettings() {
    return Activator.getDefault().getDialogSettings();
  }

  private void initControls() {
    IDialogSettings dialogSettings = getDialogSettings();
    String layoutName = dialogSettings.get(DIALOG_PROPERTIES_LAYOUT_KEY);
    LayoutType layoutType = LayoutType.toLayoutType(layoutName);
    if (layoutType == LayoutType.BREADCRUMB) {
      switchToBreadcrumbLayout();
    } else {
      switchToTreeLayout();
    }
  }

  private StructuredSelection maybeSelectFirstLaunchConfig() {
    if (WebAppDebugModel.getInstance().getLaunchConfigurations().size() > 0) {
      return new StructuredSelection(
          WebAppDebugModel.getInstance().getLaunchConfigurations().get(0));
    }

    return null;
  }

  private void switchToBreadcrumbLayout() {
    if (currentLayout == breadcrumbLayout) {
      return;
    }

    ISelection selection = null;
    if (currentLayout == null) {
      // Initializing
      selection = maybeSelectFirstLaunchConfig();
    } else {
      assert (currentLayout == treeLayout);
      selection = treeLayout.getSelection();
    }

    switchToBreadcrumbLayoutAction.setChecked(true);
    switchToTreeLayoutAction.setChecked(false);

    currentLayout = breadcrumbLayout;
    if (selection != null && !selection.isEmpty()) {
      currentLayout.setSelection(selection);
    }

    pageBook.showPage(breadcrumbLayout);
    getDialogSettings().put(DIALOG_PROPERTIES_LAYOUT_KEY,
        LayoutType.BREADCRUMB.name());
  }

  private void switchToTreeLayout() {
    if (currentLayout == treeLayout) {
      return;
    }

    switchToTreeLayoutAction.setChecked(true);
    switchToBreadcrumbLayoutAction.setChecked(false);

    ISelection selection = null;
    if (currentLayout == null) {
      // Initializing
      selection = maybeSelectFirstLaunchConfig();
    } else {
      assert (currentLayout == breadcrumbLayout);
      selection = breadcrumbLayout.getSelection();
    }

    currentLayout = treeLayout;
    if (selection != null && !selection.isEmpty()) {
      currentLayout.setSelection(selection);
    }

    pageBook.showPage(treeLayout);
    getDialogSettings().put(DIALOG_PROPERTIES_LAYOUT_KEY,
        LayoutType.TREE.name());
  }

}
