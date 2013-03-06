// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.gdt.eclipse.managedapis.ui;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.managedapis.impl.ApiPlatformType;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Utility for error dialog popup in case the project is not an App Engine
 * or Android project.
 */
public class AppEngineAndroidCheckDialog {
  public static boolean isAppEngineAndroidProject(IProject project) {
    try {
      if (!GaeNature.isGaeProject(project)
          && (ApiPlatformType.getAndroidPlatformType(project) == null)) {
        showDialog();
        return false;
      }
    } catch (JavaModelException e) {
      showDialog();
      return false;
    }
    return true;
  }

  private static void showDialog() {
    MessageDialog.openInformation(Display.getDefault().getActiveShell(),
        "Google Plugin for Eclipse",
        "Add Google APIs is supported only for App Engine and Android Projects.");
  }

}
