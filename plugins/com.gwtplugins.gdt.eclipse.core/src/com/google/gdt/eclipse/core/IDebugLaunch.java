/**
 *
 */
package com.google.gdt.eclipse.core;

import org.eclipse.core.resources.IProject;

public interface IDebugLaunch {

  String EXTENSION_ID = "com.google.gdt.eclipse.core.debugLauncher";

  void launch(IProject project, String url, String mode);

}
