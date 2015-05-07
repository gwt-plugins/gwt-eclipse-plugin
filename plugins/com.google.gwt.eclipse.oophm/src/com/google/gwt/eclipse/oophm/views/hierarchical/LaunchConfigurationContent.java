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

import com.google.gdt.eclipse.core.browser.BrowserMenuPopulator;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerBrowserUtilities;
import com.google.gwt.eclipse.oophm.model.IWebAppDebugModelListener;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.SpeedTracerLaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModelEvent;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModelListenerAdapter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;

import java.util.List;

/**
 * Displays a filtered list of launch URLs for a given launch configuration.
 * This pane is displayed when a launch configuration is selected in the
 * hierarchical view.
 */
public class LaunchConfigurationContent extends Composite {

  /*
   * All methods must be called on the UI thread, otherwise there needs to be
   * locking on the viewer .
   */
  private static class ContentProvider implements ITreeContentProvider {
    private final IWebAppDebugModelListener launchUrlsChangedListener = new WebAppDebugModelListenerAdapter() {
      @Override
      public void launchConfigurationLaunchUrlsChanged(
          WebAppDebugModelEvent<LaunchConfiguration> e) {
        if (e.getElement().equals(launchConfigurationModelObject)) {
          Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
              updateLaunchUrlsOrLoadingOnViewer();
            }
          });

        }
      }
    };

    private TreeViewer viewer;

    private LaunchConfiguration launchConfigurationModelObject;

    private final Label launchUrlsCaptionLabel;

    /*
     * TODO: The launchUrlsCaptionLabel is leaking the abstraction of the
     * provider being separate from the viewer (this label counts as part of the
     * "viewer" since it is updated along with viewer updates). But a fix is
     * overly complicated for the current usage of this class.
     */
    private ContentProvider(Label launchUrlsCaptionLabel) {
      this.launchUrlsCaptionLabel = launchUrlsCaptionLabel;
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (!(parentElement instanceof LaunchConfiguration)) {
        return null;
      }

      LaunchConfiguration parentLaunchConfiguration = (LaunchConfiguration) parentElement;
      if (!parentLaunchConfiguration.isServing()) {
        return new String[] {"Waiting for launch URLs..."};
      } else {
        List<String> launchUrls = parentLaunchConfiguration.getLaunchUrls();
        return launchUrls.toArray(new String[launchUrls.size()]);
      }
    }

    @Override
    public Object[] getElements(Object inputElement) {
      return getChildren(inputElement);
    }

    @Override
    public Object getParent(Object element) {
      return element != launchConfigurationModelObject
          ? launchConfigurationModelObject : null;
    }

    @Override
    public boolean hasChildren(Object element) {
      return element == launchConfigurationModelObject;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

      this.launchConfigurationModelObject = (LaunchConfiguration) newInput;
      this.viewer = (TreeViewer) viewer;

      if (oldInput != null) {
        ((LaunchConfiguration) oldInput).getModel().removeWebAppDebugModelListener(
            launchUrlsChangedListener);
      }

      if (newInput != null) {
        ((LaunchConfiguration) newInput).getModel().addWebAppDebugModelListener(
            launchUrlsChangedListener);
        updateLaunchUrlsOrLoadingOnViewer();
      }
    }

    private void updateLaunchUrlsOrLoadingOnViewer() {
      if (viewer != null) {
        boolean isServing = launchConfigurationModelObject.isServing();
        viewer.getTree().setEnabled(isServing);

        if (isServing) {
          launchUrlsCaptionLabel.setText("Double-click to open a URL or right-click for more options.");
          launchUrlsCaptionLabel.getParent().layout();
        } else {
          launchUrlsCaptionLabel.setText("Development mode is loading...");
        }

        viewer.refresh();
      }
    }
  }

  private static class LabelProvider implements ILabelProvider {
    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public Image getImage(Object element) {
      return null;
    }

    @Override
    public String getText(Object element) {
      return (String) element;
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }
  }

  private final Clipboard clipboard = new Clipboard(Display.getCurrent());
  private final TextTransfer textTransferInstance = TextTransfer.getInstance();
  private final LaunchConfiguration launchConfiguration;
  private TreeViewer viewer;
  private BrowserMenuPopulator browserMenuPopulator = new BrowserMenuPopulator(
      new BrowserMenuPopulator.DefaultBrowserProvider() {
        private final String ATTR_PREVIOUS_BROWSER = GWTPlugin.PLUGIN_ID
            + ".oophm." + LaunchConfigurationContent.class.getSimpleName();

        @Override
        public String getDefaultBrowserName() {
          return GWTPlugin.getDefault().getPreferenceStore().getString(
              ATTR_PREVIOUS_BROWSER);
        }

        @Override
        public void setDefaultBrowserName(String browserName) {
          GWTPlugin.getDefault().getPreferenceStore().setValue(
              ATTR_PREVIOUS_BROWSER, browserName);
        }
      });

  public LaunchConfigurationContent(final Composite parent,
      LaunchConfiguration launchConfigurationModelObject) {
    super(parent, SWT.NONE);
    this.launchConfiguration = launchConfigurationModelObject;
    GridLayout layout = new GridLayout();
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // if the parent of launchUrlsCaptionLabel changes, then update the code
    // in the isServing condition in updateLaunchUrlsOrLoadingOnViewer
    Label launchUrlsCaptionLabel = new Label(this, SWT.WRAP);
    GridData labelLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    labelLayoutData.horizontalIndent = 2;
    labelLayoutData.verticalIndent = 2;
    launchUrlsCaptionLabel.setLayoutData(labelLayoutData);

    createViewer(launchUrlsCaptionLabel);
    createContextMenu();
  }

  private void copySelectionToClipboard() {
    String url = getSelectedUrl();
    if (url != null) {
      clipboard.setContents(new Object[] {url},
          new Transfer[] {textTransferInstance});
    }
  }

  private void createContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager manager) {

        if (launchConfiguration instanceof SpeedTracerLaunchConfiguration) {
          manager.add(new Action("&Open") {
            @Override
            public void run() {
              launchSpeedTracer();
            }
          });
        } else {
          populateBrowserActions(launchConfiguration, manager);
          manager.add(new Separator());
        }

        manager.add(new Action("&Copy") {
          @Override
          public void run() {
            copySelectionToClipboard();
          }
        });
      }
    });
    Menu menu = menuMgr.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);
  }

  private void createViewer(Label launchUrlsCaptionLabel) {
    viewer = new TreeViewer(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
        | SWT.BORDER);
    viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    viewer.setLabelProvider(new LabelProvider());
    viewer.setContentProvider(new ContentProvider(launchUrlsCaptionLabel));
    viewer.setInput(launchConfiguration);

    viewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        if (launchConfiguration instanceof SpeedTracerLaunchConfiguration) {
          launchSpeedTracer();
        } else {
          browserMenuPopulator
              .openDefaultBrowser(getProject(launchConfiguration), getSelectedUrl());
        }
      }
    });
  }

  private String getSelectedUrl() {
    StructuredSelection selection = (StructuredSelection) viewer.getSelection();
    return selection != null && !selection.isEmpty()
        ? (String) selection.getFirstElement() : null;
  }

  private void launchSpeedTracer() {
    try {
      ILaunch launch = launchConfiguration.getLaunch();
      ILaunchConfiguration config = launch.getLaunchConfiguration();
      if (config != null) {
        if (!SpeedTracerBrowserUtilities.ensureChromeConfiguredOrPrompt(
            getShell(), config)) {
          return;
        }
      }

      List<String> command = SpeedTracerBrowserUtilities.computeBrowserCommandLine(launch);
      new ProcessBuilder(command).start();
    } catch (Throwable t) {
      GWTPluginLog.logError(t, "Could not open Chrome.");
    }
  }

  private void populateBrowserActions(LaunchConfiguration launchConfig, IMenuManager manager) {
    browserMenuPopulator.populate(getProject(launchConfig), manager, getSelectedUrl());
  }

  private IProject getProject(LaunchConfiguration launchConfig) {
    if (launchConfig == null) {
      return null;
    }

    ILaunch launch = launchConfig.getLaunch();
    if (launch == null) {
      return null;
    }

    ILaunchConfiguration lc = launch.getLaunchConfiguration();
    if (lc == null) {
      return null;
    }

    IJavaProject project;
    try {
      project = JavaRuntime.getJavaProject(lc);
    } catch (CoreException e) {
      return null;
    }
    if (project == null) {
      return null;
    }

    return project.getProject();
  }

}
