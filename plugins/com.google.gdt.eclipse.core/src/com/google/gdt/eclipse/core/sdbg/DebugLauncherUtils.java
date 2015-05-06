/**
 *
 */
package com.google.gdt.eclipse.core.sdbg;

import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class DebugLauncherUtils {

  public static boolean hasChromeLauncher() {
    boolean hasChromeLauncher = false;
    try {
      IDebugLaunch debugLaunch = getDebugLauncher();
      if (debugLaunch != null) {
        hasChromeLauncher = debugLaunch.hasChromeLauncher();
      }
    } catch (CoreException e) {
      CorePluginLog.logError("Couldn't determine Chrome.", e);
    }

    return hasChromeLauncher;
  }

  public static void launchChrome(IProject project, String mode, String url) {
    try {
      IDebugLaunch debugLaunch = getDebugLauncher();
      if (debugLaunch != null) {
        debugLaunch.launchChrome(project, mode, url);
      }
    } catch (CoreException e) {
      CorePluginLog.logError("Couldn't launch Chrome. url=" + url, e);
    }
  }

  private static IDebugLaunch getDebugLauncher() throws CoreException {
    IExtensionPoint extensionPoint =
        Platform.getExtensionRegistry().getExtensionPoint(IDebugLaunch.EXTENSION_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
    if (elements.length == 0) {
      return null;
    }
    IDebugLaunch debugLaunch = (IDebugLaunch) elements[0].createExecutableExtension("class");

    return debugLaunch;
  }

}
