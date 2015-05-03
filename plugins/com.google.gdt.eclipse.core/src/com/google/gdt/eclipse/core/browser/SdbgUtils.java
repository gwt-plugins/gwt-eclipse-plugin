/**
 *
 */
package com.google.gdt.eclipse.core.browser;

import com.google.gdt.eclipse.core.CorePluginLog;

import com.github.sdbg.ui.ISDBGLauncher;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class SdbgUtils {

  public static String PLUGIN_ID = "com.github.sdbg.debug.ui.launcher";

  public static boolean hasSdbgInstalled() {
    IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(ISDBGLauncher.EXTENSION_ID);
    return extensionPoint != null;
  }

  public static void openChrome(IResource resource, String mode , String url) {
    IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(ISDBGLauncher.EXTENSION_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

    try {
      ISDBGLauncher ilauncher = (ISDBGLauncher) elements[0].createExecutableExtension("class");
      ilauncher.launchChrome(resource, mode, url);
    } catch (CoreException e) {
      e.printStackTrace();
      CorePluginLog.logError("SdbgUtils could not launch SDBG. url=" + url, e);
    }
  }

}
