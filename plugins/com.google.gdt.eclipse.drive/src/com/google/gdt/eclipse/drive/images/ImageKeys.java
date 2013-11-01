package com.google.gdt.eclipse.drive.images;

import com.google.common.collect.ImmutableList;
import com.google.gdt.eclipse.drive.DrivePlugin;

import java.util.Collection;

/**
 * Defines string constants used as keys into the image registry.
 */
public class ImageKeys {

  private ImageKeys() {
    // prevent instantiation
  }
  
  private static final String PREFIX = DrivePlugin.PLUGIN_ID + ".images.";
  
  public static final String FOLDER_ICON = PREFIX + "folderIcon";
  public static final String APPS_SCRIPT_PROJECT_ICON = PREFIX + "appsScriptProjectIcon";
  public static final String UNSAVED_ICON = PREFIX + "unsavedIcon";
  
  public static final Collection<String> ALL_KEYS =
      ImmutableList.of(FOLDER_ICON, APPS_SCRIPT_PROJECT_ICON, UNSAVED_ICON);

}
