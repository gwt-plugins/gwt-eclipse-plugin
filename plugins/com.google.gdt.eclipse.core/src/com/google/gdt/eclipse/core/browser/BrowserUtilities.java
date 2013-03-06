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

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.eclipse.ui.internal.browser.IBrowserDescriptorWorkingCopy;
import org.eclipse.ui.internal.browser.SystemBrowserDescriptor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Miscellaneous static methods that may need to be called from several places.
 */
@SuppressWarnings("restriction")
public class BrowserUtilities {

  /**
   * The General > Web Browser preference page ID (this constant does not exist
   * in the browser plugin java code).
   */
  private static final String BROWSER_PREFERENCE_PAGE_ID = "org.eclipse.ui.browser.preferencePage";

  private static final String IE32_LOCATION = "C:\\Program Files (x86)\\Internet Explorer\\iexplore.exe";
  private static final String IE32_NAME = "Internet Explorer 32-bit";

  /**
   * Computes the full command-line that can be passed directly to
   * {@link ProcessBuilder}.
   */
  public static List<String> computeCommandLine(IBrowserDescriptor browser,
      String url) {
    String params = browser.getParameters();
    if (params == null) {
      params = "";
    }

    boolean paramsHasUrlVariable = params.contains("%URL%");
    if (paramsHasUrlVariable) {
      // Quote the URL in case it has spaces
      params = params.replaceAll("%URL%", '"' + url + '"');
    }

    final List<String> command = LaunchConfigurationProcessorUtilities.parseArgs(params);
    command.add(0, browser.getLocation());
    if (!paramsHasUrlVariable) {
      // Cover the case where the user forgot the %URL%
      command.add(url);
    }

    return command;
  }

  public static boolean containsChromeRelatedString(String string) {
    String lowerCaseString = string != null ? string.toLowerCase() : "";
    return lowerCaseString.contains("chrome")
        || lowerCaseString.contains("chromium");
  }

  /**
   * Creates and saves a browser model object.
   * 
   * @param browserName the name of the browser
   * @param browserLocation the location, unescaped
   * @return the new object
   */
  public static IBrowserDescriptor createBrowserDescriptor(String browserName,
      String browserLocation) {
    assert browserName != null;
    assert browserLocation != null;

    IBrowserDescriptorWorkingCopy browserWc = BrowserManager.getInstance().createExternalWebBrowser();
    browserWc.setName(browserName);

    if (Platform.OS_MACOSX.equals(Platform.getOS())) {
      // Special handling necessary to get smooth launches on Mac
      browserWc.setLocation("/usr/bin/open");
      browserWc.setParameters("-a \"" + browserLocation + "\" %URL%");
    } else {
      browserWc.setLocation(browserLocation);
      browserWc.setParameters("%URL%");
    }

    return browserWc.save();
  }

  /**
   * If iexplore.exe exists in "Program Files (x86)", then we must be on 64 bit
   * windows with 32-bit IE installed, and we have to use that one because there
   * is no 64 bit devmode plugin for IE. This adds an entry for 32bit IE to the
   * browser registry if it exists.
   */
  public static void ensure32BitIe() {
    File ie32 = new File(IE32_LOCATION);

    if (findBrowser(IE32_NAME) == null && ie32.exists()) {
      createBrowserDescriptor(IE32_NAME, IE32_LOCATION);
    }
  }

  @SuppressWarnings("unchecked")
  public static IBrowserDescriptor findBrowser(String browserName) {
    List<IBrowserDescriptor> browsers = BrowserManager.getInstance().getWebBrowsers();
    for (IBrowserDescriptor browser : browsers) {
      if (browser.getName().equals(browserName)) {
        return browser;
      }
    }

    return null;
  }

  /**
   * @return a browser that looks like Chrome, or null
   * @see #findChromeBrowserName()
   */
  @SuppressWarnings("unchecked")
  public static IBrowserDescriptor findChromeBrowser() {
    BrowserManager browserManager = BrowserManager.getInstance();
    List<IBrowserDescriptor> browsers = new ArrayList<IBrowserDescriptor>(
        browserManager.getWebBrowsers());

    // Give the default web browser the highest priority
    IBrowserDescriptor currentWebBrowser = browserManager.getCurrentWebBrowser();
    if (currentWebBrowser != null) {
      browsers.remove(currentWebBrowser);
      browsers.add(0, currentWebBrowser);
    }

    for (IBrowserDescriptor browser : browsers) {
      if (containsChromeRelatedString(browser.getName())
          || containsChromeRelatedString(browser.getLocation())) {
        return browser;
      }
    }

    return null;
  }

  /**
   * @return the name for a Chrome browser or empty string
   * @see #findChromeBrowser()
   */
  public static String findChromeBrowserName() {
    IBrowserDescriptor browser = findChromeBrowser();
    return browser != null ? browser.getName() : "";
  }

  public static String generateUniqueBrowserName(String preferredName) {
    String name = preferredName;
    int counter = 1;

    while (findBrowser(name) != null) {
      name = preferredName + " (" + counter + ")";
      counter++;
    }

    return name;
  }

  public static List<String> getBrowserNames() {
    List<String> browserNames = new ArrayList<String>();
    for (Object browserObj : BrowserManager.getInstance().getWebBrowsers()) {
      IBrowserDescriptor browser = (IBrowserDescriptor) browserObj;
      browserNames.add(browser.getName());
    }
    return browserNames;
  }

  public static IWebBrowser launchBrowser(String targetUrl)
      throws MalformedURLException, PartInitException {
    Workbench workbench = Workbench.getInstance();
    if (workbench == null) {
      throw new PartInitException("No workbench is available");
    }

    IWorkbenchBrowserSupport browserSupport = workbench.getBrowserSupport();

    URL url = new URL(targetUrl);

    IWebBrowser browser = browserSupport.createBrowser(
        IWorkbenchBrowserSupport.AS_EXTERNAL, null,
        "Google Plugin for Eclipse", "Google Plugin for Eclipse");
    browser.openURL(url);
    return browser;
  }

  /**
   * Launches the browser with the given name. This method does not use the
   * Eclipse browser methods to launch the browser since they do not properly
   * pass quoted strings as a single argument.
   */
  public static void launchBrowser(String browserName, String url)
      throws CoreException, IOException {
    IBrowserDescriptor browser = findBrowser(browserName);
    if (browser == null) {
      throw new CoreException(StatusUtilities.newErrorStatus(
          "Could not find browser \"" + browserName + "\".",
          CorePlugin.PLUGIN_ID));
    }

    // SystemBrowserDescriptors have no info in them...
    if (browser instanceof SystemBrowserDescriptor) {
      Program p = Program.findProgram("html");
      boolean launched = false;
      if (p != null) {
        launched = p.execute(url);
      }

      if (!launched) {
        String msg = "Could not launch the default "
            + "browser, please configure a browser in "
            + "Preferences -> General -> Web Browsers";
        MessageBox mb = new MessageBox(Display.getCurrent().getActiveShell());
        mb.setMessage(msg);
        mb.open();
        throw new CoreException(StatusUtilities.newErrorStatus(msg,
            CorePlugin.PLUGIN_ID));
      }
    } else {
      List<String> command = computeCommandLine(browser, url);
      new ProcessBuilder(command).start();
    }
  }

  public static IWebBrowser launchBrowserAndHandleExceptions(String targetUrl) {
    String errorMsg = "Could not launch the default "
        + "browser. Please configure a browser in "
        + "Preferences -> General -> Web Browsers, or browse to:\n\n"
        + targetUrl + "\n\n" + "using your browser of choice.";
    try {
      return launchBrowser(targetUrl);
    } catch (MalformedURLException e) {
      CorePluginLog.logError(e);
      errorMsg = "There was a problem launching the browser and navigating to:"
          + "\n\n"
          + targetUrl
          + "\n\n"
          + "You may try and navigate to this URL using your browser of choice, "
          + "or post a message to Google Web Toolkit users forum on Google Groups for assistance.";
    } catch (PartInitException e) {
      // Ignore; use the default error message
    }
    // If we've reached this point, there must be some sort of error that we
    // want to display to the user.
    MessageBox mb = new MessageBox(Display.getCurrent().getActiveShell());
    mb.setMessage(errorMsg);
    mb.open();
    return null;
  }

  /**
   * Locates a Chrome installation in one of the default installation
   * directories.
   * 
   * @return an absolute path, or null
   */
  public static String locateChrome() {
    String os = Platform.getOS();

    List<File> locationsToCheck = new ArrayList<File>();

    if (Platform.OS_WIN32.equals(os)) {
      /*
       * Both "home" and "userprofile" will likely point to the same directory,
       * but include both just in case they don't.
       */
      String[] envVariables = new String[] {
          "home", "userprofile", "home", "userprofile", "ProgramFiles(X86)",
          "ProgramFiles"};
      String[] appendedPaths = new String[] {
          "\\Local Settings\\Application Data\\Google\\Chrome\\Application\\chrome.exe", // XP
          "\\Local Settings\\Application Data\\Google\\Chrome\\Application\\chrome.exe", // XP
          "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe", // Vista/7
          "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe", // Vista/7
          "\\Google\\Chrome\\Application\\chrome.exe", // All
          "\\Google\\Chrome\\Application\\chrome.exe" // All
      };
      assert envVariables.length == appendedPaths.length;

      for (int i = 0; i < envVariables.length; i++) {
        String envValue = System.getenv(envVariables[i]);
        if (envValue != null) {
          locationsToCheck.add(new File(envValue + appendedPaths[i]));
        }
      }

    } else if (Platform.OS_MACOSX.equals(os)) {
      /*
       * Only check for ChromeWithSpeedTracer.app (and not Google Chrome.app)
       * since we require that for proper argument passing to Chrome.
       */
      locationsToCheck.add(new File("/Applications/ChromeWithSpeedTracer.app"));
      String homeDir = System.getenv("HOME");
      if (homeDir != null) {
        locationsToCheck.add(new File(homeDir
            + "/Applications/ChromeWithSpeedTracer.app"));
      }

    } else {
      // *NIX variety
      locationsToCheck.add(new File("/usr/bin/chrome"));
      locationsToCheck.add(new File("/usr/local/bin/chrome"));
      locationsToCheck.add(new File("/usr/bin/google-chrome"));
      locationsToCheck.add(new File("/usr/local/bin/google-chrome"));
      locationsToCheck.add(new File("/usr/bin/chromium"));
      locationsToCheck.add(new File("/usr/local/bin/chromium"));
      locationsToCheck.add(new File("/usr/bin/chromium-browser"));
      locationsToCheck.add(new File("/usr/local/bin/chromium-browser"));
    }

    for (File location : locationsToCheck) {
      if (location.exists()
          && (Platform.OS_MACOSX.equals(os) || location.isFile())) {
        return location.getAbsolutePath();
      }
    }

    return null;
  }

  public static int openBrowserPreferences(Shell shell) {
    return PreferencesUtil.createPreferenceDialogOn(shell,
        BROWSER_PREFERENCE_PAGE_ID, null, null).open();
  }
}
