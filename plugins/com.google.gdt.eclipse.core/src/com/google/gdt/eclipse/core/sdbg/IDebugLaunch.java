/**
 *
 */
package com.google.gdt.eclipse.core.sdbg;

import org.eclipse.core.resources.IProject;

public interface IDebugLaunch {

  String EXTENSION_ID = "com.google.gdt.eclipse.core.debugLauncher";

  boolean hasChromeLauncher();

  void launchChrome(IProject project, String mode, String url);

}
