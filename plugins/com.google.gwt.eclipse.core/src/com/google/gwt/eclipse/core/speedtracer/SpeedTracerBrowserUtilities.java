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
package com.google.gwt.eclipse.core.speedtracer;

import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationAttributeUtilities;
import com.google.gdt.eclipse.core.ui.AddBrowserDialog;
import com.google.gdt.eclipse.core.web.UrlChecker;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Speed Tracer-related browser utility methods.
 */
@SuppressWarnings("restriction")
public final class SpeedTracerBrowserUtilities {

  /**
   * Thrown if Chrome is not found.
   */
  @SuppressWarnings("serial")
  public static class ChromeNotFoundException extends Exception {
  }

  public static String LAUNCH_ATTR_TRAMPOLINE_FILE_URL = GWTPlugin.PLUGIN_ID
      + ".speedtracer.trampolineFileUrl";

  private static final String ARG_TIMELINE_API = "--enable-extension-timeline-api";

  /**
   * Computes the command-line (including the binary location) to launch the
   * Chrome browser with the intent to run Speed Tracer immediately.
   * 
   * @throws CoreException
   */
  public static List<String> computeBrowserCommandLine(ILaunch launch)
      throws ChromeNotFoundException, CoreException {

    if (launch.getLaunchConfiguration() == null) {
      throw new CoreException(StatusUtilities.newErrorStatus(
          "ILaunch without a ILaunchConfiguration", GWTPlugin.PLUGIN_ID));
    }

    String trampolineFileUrl = launch.getAttribute(LAUNCH_ATTR_TRAMPOLINE_FILE_URL);

    String browserName = LaunchConfigurationAttributeUtilities.getString(
        launch.getLaunchConfiguration(),
        SpeedTracerLaunchConfiguration.Attribute.BROWSER);

    IBrowserDescriptor browser = BrowserUtilities.findBrowser(browserName);
    if (browser == null) {
      throw new ChromeNotFoundException();
    }

    List<String> commandLine = new ArrayList<String>(
        BrowserUtilities.computeCommandLine(browser, trampolineFileUrl));

    if (!Platform.OS_MACOSX.equals(Platform.getOS())
        && !commandLine.contains(ARG_TIMELINE_API)) {
      commandLine.add(1, ARG_TIMELINE_API);
    }

    return commandLine;
  }

  /**
   * Generates a trampoline file and sets the launch's trampoline file url to 
   * the file's url. 
   */
  public static void createTrampolineFileAndAddToLaunch(String url,
      ILaunch launch) throws IOException, CoreException {

    File trampolineFile = new SpeedTracerTrampolineFileGenerator(url).generate();
    trampolineFile.deleteOnExit();

    String trampolineFileUrl = trampolineFile.toURI().toString();
    launch.setAttribute(
        SpeedTracerBrowserUtilities.LAUNCH_ATTR_TRAMPOLINE_FILE_URL,
        trampolineFileUrl);
  }

  /**
   * Convenience method to get a working copy from the given
   * {@link ILaunchConfiguration}, call
   * {@link #ensureChromeConfiguredOrPrompt(Shell, ILaunchConfigurationWorkingCopy)}
   * , and save any changes back.
   * 
   * @see #ensureChromeConfiguredOrPrompt(Shell,
   *      ILaunchConfigurationWorkingCopy)
   */
  public static boolean ensureChromeConfiguredOrPrompt(Shell shell,
      ILaunchConfiguration config) throws CoreException {
    ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
    if (!ensureChromeConfiguredOrPrompt(shell, wc)) {
      return false;
    }

    if (wc.isDirty()) {
      wc.doSave();
    }

    return true;
  }

  /**
   * Ensures there is a Chrome browser configured, and failing that either try
   * to find a default installation or will ask the user and update the given
   * Speed Tracer launch config's browser.
   * 
   * @return whether the browser on the config is ready to go
   * @throws CoreException
   */
  public static boolean ensureChromeConfiguredOrPrompt(final Shell shell,
      final ILaunchConfigurationWorkingCopy config) throws CoreException {
    if (doesLaunchConfigurationBrowserExist(config)) {
      return true;
    }

    String chromeLocation = BrowserUtilities.locateChrome();
    if (chromeLocation != null) {
      IBrowserDescriptor chromeDescriptor = BrowserUtilities.createBrowserDescriptor(
          BrowserUtilities.generateUniqueBrowserName("Chrome"), chromeLocation);
      LaunchConfigurationAttributeUtilities.set(config,
          SpeedTracerLaunchConfiguration.Attribute.BROWSER,
          chromeDescriptor.getName());
      return true;
    }

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        String message = null;
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
          message = "Note: On Mac, please select \"ChromeWithSpeedTracer.app\" instead of \"Google Chrome.app\"\nSee the Speed Tracer installation instructions for more information.";
        }

        AddBrowserDialog dialog = new AddBrowserDialog(shell, "Add Chrome",
            "Add Chrome with Speed Tracer", "Chrome", message);
        dialog.addValidator(new AddBrowserDialog.Validator() {
          public IStatus validateEnteredBrowser(String name, String location) {
            if (!BrowserUtilities.containsChromeRelatedString(name)) {
              return StatusUtilities.newErrorStatus(
                  "The browser name should contain \"chrome\".",
                  GWTPlugin.PLUGIN_ID);
            }

            String fileName = new Path(location).lastSegment();
            if (!BrowserUtilities.containsChromeRelatedString(fileName)) {
              return StatusUtilities.newWarningStatus(
                  "Ensure the filename is correct, it typically contains \"chrome\".",
                  GWTPlugin.PLUGIN_ID);
            }

            return StatusUtilities.OK_STATUS;
          }
        });
        if (dialog.open() == Dialog.CANCEL) {
          return;
        }

        LaunchConfigurationAttributeUtilities.set(config,
            SpeedTracerLaunchConfiguration.Attribute.BROWSER,
            dialog.getBrowserName());
      }
    });

    return doesLaunchConfigurationBrowserExist(config);
  }

  public static void openBrowser(final String finalUrl, ILaunch launch)
      throws CoreException {

    ILaunchConfiguration config = launch.getLaunchConfiguration();
    if (config != null) {
      if (!SpeedTracerBrowserUtilities.ensureChromeConfiguredOrPrompt(
          SWTUtilities.getShell(), config)) {
        return;
      }
    }

    List<String> command;
    try {
      command = SpeedTracerBrowserUtilities.computeBrowserCommandLine(launch);
    } catch (ChromeNotFoundException e2) {
      GWTPluginLog.logError(e2,
          "Chrome not found, even though the prior check passed.");
      return;
    }

    URL urlObject = null;
    try {
      urlObject = new URL(finalUrl);
    } catch (MalformedURLException e1) {
      throw new CoreException(StatusUtilities.newErrorStatus("URL is malformed",
        GWTPlugin.PLUGIN_ID));
    }

    final List<String> finalCommand = command;
    new UrlChecker(urlObject, 500, 20000, "Waiting for server to start before launching Speed Tracer",
        new UrlChecker.Listener() {
          private IOException lastException;

          public boolean handleUrlCheckerException(IOException e) {
            // Only log the last exception in the case of failure
            lastException = e;
            return true;
          }

          public void urlCheckerFinished(boolean success) {
            if (!success) {
              GWTPluginLog.logWarning(lastException,
                  "The web server may not be ready, launching Speed Tracer anyway.");
            }

            try {
              new ProcessBuilder(finalCommand).start();
            } catch (IOException e) {
              GWTPluginLog.logError(e, "Could not start browser");
            }
          }
        }).startChecking();
  }

  private static boolean doesLaunchConfigurationBrowserExist(
      final ILaunchConfigurationWorkingCopy config) throws CoreException {
    String launchConfigBrowserName = LaunchConfigurationAttributeUtilities.getString(
        config, SpeedTracerLaunchConfiguration.Attribute.BROWSER);
    return BrowserUtilities.findBrowser(launchConfigBrowserName) != null;
  }

  private SpeedTracerBrowserUtilities() {
  }
}
