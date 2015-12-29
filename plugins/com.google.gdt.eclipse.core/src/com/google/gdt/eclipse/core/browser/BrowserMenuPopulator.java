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
package com.google.gdt.eclipse.core.browser;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.IDebugLaunch;
import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.ui.AddBrowserDialog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.List;

/**
 * Populates a {@link IMenuManager} with browser choices in "Open" and "Open With" menu items.
 */
public class BrowserMenuPopulator {

  /**
   * Add menus to the development view.
   */
  public interface ILaunchMenus {
    String EXTENSION_ID = "com.google.gdt.eclipse.core.browser.launchMenus";

    List<LaunchMenuModel> getMenus(IProject project);
  }

  /**
   * Provides default browser information.
   */
  public interface DefaultBrowserProvider {
    /**
     * @return the default browser's name, or null
     */
    String getDefaultBrowserName();

    void setDefaultBrowserName(String browserName);
  }

  // TODO Replace with extension
  @Deprecated
  protected static final String SDBG_BROWSERNAME_ID = "CHROME_SDBG";

  private final DefaultBrowserProvider defaultProvider;

  private IProject project;

  public BrowserMenuPopulator(DefaultBrowserProvider defaultProvider) {
    this.defaultProvider = defaultProvider;
  }

  public void openDefaultBrowser(IProject project, String url) {
    this.project = project;

    openBrowser(getDefaultBrowserName(), url);
  }

  public void populate(final IProject project, IMenuManager menu, final String url) {
    this.project = project;

    menu.add(new Action("&Open") {
      @Override
      public void run() {
        openBrowser(getDefaultBrowserName(), url);
      }
    });

    // Open With:
    MenuManager openWithMenuManager = new MenuManager("Open wit&h");
    openWithMenuManager.add(new ContributionItem() {
      @Override
      public void fill(Menu menu, int index) {
        Menu openWithMenu = menu;
        for (final String browserName : BrowserUtilities.getBrowserNames()) {
          final MenuItem item = new MenuItem(openWithMenu, SWT.RADIO);
          // Add a keyboard accelerator
          item.setText("&" + browserName);
          item.setSelection(JavaUtilities.equalsWithNullCheck(browserName, getDefaultBrowserName()));
          item.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
              // The previously selected item will get a callback too, hence the selection check
              if (item.getSelection()) {
                openBrowser(browserName, url);
                defaultProvider.setDefaultBrowserName(browserName);
              }
            }
          });
        }

        new MenuItem(openWithMenu, SWT.SEPARATOR);

        MenuItem configureItem = new MenuItem(openWithMenu, SWT.NONE);
        configureItem.setText("&Add a Browser");
        configureItem.addListener(SWT.Selection, new Listener() {
          @Override
          public void handleEvent(Event event) {
            AddBrowserDialog addBrowserDialog = new AddBrowserDialog(SWTUtilities.getShell());
            if (addBrowserDialog.open() == Window.OK) {
              String browserName = addBrowserDialog.getBrowserName();
              openBrowser(browserName, url);
              defaultProvider.setDefaultBrowserName(browserName);
            }
          }
        });
      }
    });

    menu.add(openWithMenuManager);

    // Registered launchers will be added to the menu
    addDebugLauncherMenus(menu, url);
  }

  private void addDebugLauncherMenus(IMenuManager menu, String url) {
    IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(IDebugLaunch.EXTENSION_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
    if (elements == null || elements.length == 0) {
      return;
    }

    for (IConfigurationElement element : elements) {
      try {
        IDebugLaunch debugLaunch = (IDebugLaunch) element.createExecutableExtension("class");
        String label = element.getAttribute("label");

        addDebugLauncherMenu(menu, label, debugLaunch, url);
      } catch (CoreException e) {
        CorePluginLog.logError("Could not add launcher menu.", e);
      }
    }
  }

  private void addDebugLauncherMenu(IMenuManager menu, final String label, final IDebugLaunch debugLaunch,
      final String url) {
    if (debugLaunch == null) {
      CorePluginLog.logError("Could not add debug launch.");
      return;
    }

    // add menus from extension
    boolean hasMenus = false;
    try {
      hasMenus = addMenus(debugLaunch, menu);
    } catch (CoreException e) {
      e.printStackTrace();
    }

    // SDBG
    // TODO deprecated
    if (!hasMenus) {
      String menuName = "Open with " + label;
      menu.add(new Action("&" + menuName) {
        @Override
        public void run() {
          debugLaunch.launch(project, url, "debug");
          defaultProvider.setDefaultBrowserName(SDBG_BROWSERNAME_ID + "_" + label);
        }
      });

    }
  }

  /**
   * Add launcher menus.
   *
   * @param debugLaunch
   * @param menu
   * @return returns true if there are menu items added
   * @throws CoreException
   */
  private boolean addMenus(IDebugLaunch debugLaunch, IMenuManager menu) throws CoreException {
    IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(ILaunchMenus.EXTENSION_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
    if (elements == null || elements.length == 0) {
      return false;
    }

    boolean hasMenus = false;
    for (IConfigurationElement element : elements) {
      ILaunchMenus imenus = (ILaunchMenus) element.createExecutableExtension("class");
      boolean hm = addMenus(debugLaunch, menu, imenus);
      if (hm) {
        hasMenus = true;
      }
    }

    return hasMenus;
  }

  /**
   * Add Menus.
   *
   * @param debugLaunch
   * @param menu
   * @param menus
   * @return if there were menus added
   */
  private boolean addMenus(final IDebugLaunch debugLaunch, IMenuManager menu, ILaunchMenus imenus) {
    if (imenus == null) {
      return false;
    }

    List<LaunchMenuModel> menus = imenus.getMenus(project);
    if (menus == null || menus.size() == 0) {
      return false;
    }

    for (LaunchMenuModel menuModel : menus) {
      final String id = menuModel.getId();
      final String label = menuModel.getMenuLabel();
      String menuName = "Open with " + label;
      final String url = menuModel.getUrl();
      final String mode = menuModel.getDebugMode();

      menu.add(new Action("&" + menuName) {
        @Override
        public void run() {
          debugLaunch.launch(project, url, mode);
          defaultProvider.setDefaultBrowserName(id);
        }
      });
    }

    return true;
  }

  private void launchExtension(String browserName, String url) throws CoreException {
    if (browserName == null || browserName.isEmpty()) {
      return;
    }

    // Deprecated: remove the id, which sets the default browser id
    browserName = browserName.replace(SDBG_BROWSERNAME_ID + "_", "");

    // Remove the uniqueness
    browserName = browserName.replaceFirst("(.*?_)", "");

    IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(IDebugLaunch.EXTENSION_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
    if (elements == null || elements.length == 0) {
      return;
    }

    for (IConfigurationElement element : elements) {
      IDebugLaunch debugLaunch = (IDebugLaunch) element.createExecutableExtension("class");
      String label = element.getAttribute("label");
      if (debugLaunch != null && label != null && label.equals(browserName)) {
        debugLaunch.launch(project, url, "debug");
      }
    }
  }

  private String getDefaultBrowserName() {
    String browserName = defaultProvider.getDefaultBrowserName();
    if (StringUtilities.isEmpty(browserName)) {
      List<String> browserNames = BrowserUtilities.getBrowserNames();
      if (!browserNames.isEmpty()) {
        browserName = browserNames.get(0);
      }
    }

    return browserName;
  }

  /**
   * Open browser or launch an extension launcher
   */
  private void openBrowser(String browserName, String url) {
    // No browser stated, find the default browser
    if (StringUtilities.isEmpty(browserName)) {
      findBrowser();
      return;
    }

    // The extension was used to launch, re-use it.
    if (browserName.contains(SDBG_BROWSERNAME_ID)) {
      try {
        launchExtension(browserName, url);
      } catch (CoreException e) {
        CorePluginLog.logError("Couldn't use the extension to launch.");
      }
      return;
    }

    // Browser was specified, use it to launch url.
    try {
      BrowserUtilities.launchBrowser(browserName, url);
    } catch (Throwable t) {
      CorePluginLog.logError(t, "Could not open the browser.");
    }
  }

  /**
   * Find a browser to open url
   */
  private void findBrowser() {
    MessageDialog md = new MessageDialog(SWTUtilities.getShell(), "No browsers found", null, null, MessageDialog.ERROR,
        new String[] { "Ok" }, 0) {

      @Override
      protected Control createMessageArea(Composite parent) {
        super.createMessageArea(parent);
        Link link = new Link(parent, SWT.NONE);

        link.setText("There are no browsers defined, please add one (Right-click on URL -> "
            + "Open with -> Add a Browser, or <a href=\"#\">Window -> Preferences -> General -> Web Browser</a>).");
        link.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            PreferenceDialog dialog = PreferencesUtil
                .createPreferenceDialogOn(Display.getCurrent().getActiveShell(),
                    "org.eclipse.ui.browser.preferencePage", new String[] { "org.eclipse.ui.browser.preferencePage" },
                    null);

            if (dialog != null) {
              dialog.open();
            }
          }
        });
        return parent;
      }
    };
    md.open();
  }

}
