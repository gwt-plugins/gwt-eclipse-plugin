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
import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.ui.AddBrowserDialog;

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
 * Populates a {@link IMenuManager} with browser choices in "Open" and
 * "Open With" menu items.
 */
public class BrowserMenuPopulator {

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

  private final DefaultBrowserProvider defaultProvider;

  public BrowserMenuPopulator(DefaultBrowserProvider defaultProvider) {
    this.defaultProvider = defaultProvider;
  }

  public void openDefaultBrowser(String url) {
    openBrowser(getDefaultBrowserName(), url);
  }

  public void populate(IMenuManager menu, final String url) {
    menu.add(new Action("&Open") {
      @Override
      public void run() {
        openBrowser(getDefaultBrowserName(), url);
      }
    });

    MenuManager openWithMenuManager = new MenuManager("Open Wit&h");
    openWithMenuManager.add(new ContributionItem() {
      @Override
      public void fill(Menu menu, int index) {
        Menu openWithMenu = menu;

        for (final String browserName : BrowserUtilities.getBrowserNames()) {
          final MenuItem item = new MenuItem(openWithMenu, SWT.RADIO);
          // Add a keyboard accelerator
          item.setText("&" + browserName);
          item.setSelection(JavaUtilities.equalsWithNullCheck(browserName,
              getDefaultBrowserName()));
          item.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
              // The previously selected item will get a callback too, hence the
              // selection check
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
          public void handleEvent(Event event) {
            AddBrowserDialog addBrowserDialog = new AddBrowserDialog(
                SWTUtilities.getShell());
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

  private void openBrowser(String browserName, String url) {
    if (StringUtilities.isEmpty(browserName)) {

      MessageDialog md = new MessageDialog(SWTUtilities.getShell(),
          "No browsers found", null, null, MessageDialog.ERROR,
          new String[]{"Ok"}, 0) {

        @Override
        protected Control createMessageArea(Composite parent) {
          super.createMessageArea(parent);
          Link link = new Link(parent, SWT.NONE);

          link.setText("There are no browsers defined, please add one (Right-click on URL -> "
              + "Open with -> Add a Browser, or <a href=\"#\">Window -> Preferences -> General -> Web Browser</a>).");
          link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
              PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
                  Display.getCurrent().getActiveShell(),
                  "org.eclipse.ui.browser.preferencePage",
                  new String[]{"org.eclipse.ui.browser.preferencePage"}, null);

              if (dialog != null) {
                dialog.open();
              }
            }
          });
          return parent;
        }
      };
      md.open();

      return;
    }

    try {
      BrowserUtilities.launchBrowser(browserName, url);
    } catch (Throwable t) {
      CorePluginLog.logError(t, "Could not open the browser.");
    }
  }
}
