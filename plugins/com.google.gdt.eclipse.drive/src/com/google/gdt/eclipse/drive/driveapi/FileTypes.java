/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.driveapi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;

import java.util.Set;

/**
 * Provides utilities for manipulating Drive file-type names, Apps Script project file extensions,
 * and the correspondence between them.
 */
public class FileTypes {

  @VisibleForTesting static final String SCRIPT_FILE_EXTENSION = ".gs";
  private static final String HTML_FILE_EXTENSION = ".html";
  
  private static final String DRIVE_APPS_SCRIPT_PROJECT_SCRIPT_FILE_TYPE = "server_js";
  private static final String DRIVE_APPS_SCRIPT_PROJECT_HTML_FILE_TYPE = "html";
  
  private static final ImmutableMap<String, String>
      ECLIPSE_EXTENSIONS_TO_DRIVE_APPS_SCRIPT_PROJECT_TYPES =
          ImmutableMap.<String, String>builder()
              .put(SCRIPT_FILE_EXTENSION, DRIVE_APPS_SCRIPT_PROJECT_SCRIPT_FILE_TYPE)
              .put(HTML_FILE_EXTENSION, DRIVE_APPS_SCRIPT_PROJECT_HTML_FILE_TYPE)
              .build();
  
  private static final ImmutableMap<String, String>
      DRIVE_APPS_SCRIPT_PROJECT_TYPES_TO_ECLIPSE_EXTENSIONS =
          ImmutableMap.copyOf(
              HashBiMap.create(ECLIPSE_EXTENSIONS_TO_DRIVE_APPS_SCRIPT_PROJECT_TYPES).inverse());
  
  private static final Set<String> SUPPORTED_FILENAME_EXTENSIONS =
      ECLIPSE_EXTENSIONS_TO_DRIVE_APPS_SCRIPT_PROJECT_TYPES.keySet();
  
  private FileTypes() {
    // Prevent instantiation.
  }
  
  /**
   * Returns the Drive file-type name corresponding to a given file extension.
   * 
   * @param extension the extension, starting with the character {@code '.'}
   * @return the Drive file-type name
   * @throws IllegalArgumentException
   *     if the given extension is not valid for a file in a Drive Apps Script project
   */
  public static String driveTypeForExtension(String extension) {
    Preconditions.checkArgument(
        ECLIPSE_EXTENSIONS_TO_DRIVE_APPS_SCRIPT_PROJECT_TYPES.containsKey(extension),
        "Invalid file extension \"%s\"",
        extension);
    return ECLIPSE_EXTENSIONS_TO_DRIVE_APPS_SCRIPT_PROJECT_TYPES.get(extension);
  }
  
  /**
   * Returns a given file name, with the extension for a given Drive file type appended to it if
   * the given file name did not already have that extension.
   * 
   * @param fileName the given file name
   * @param driveType the Drive string denoting the given Drive file type
   * @return a file name that has the extension for the given file type
   * @throws IllegalArgumentException if the given Drive file type string is not valid
   */
  public static String fileNameWithExtension(String fileName, String driveType) {
    Preconditions.checkArgument(
        DRIVE_APPS_SCRIPT_PROJECT_TYPES_TO_ECLIPSE_EXTENSIONS.containsKey(driveType),
        "Invalid Drive type string \"%s\"",
        driveType);
    String expectedExtension = DRIVE_APPS_SCRIPT_PROJECT_TYPES_TO_ECLIPSE_EXTENSIONS.get(driveType);
    return fileName.endsWith(expectedExtension) ? fileName : fileName + expectedExtension;
  }
  
  /**
   * Returns the set of file extensions valid for a file in a Drive Apps Script project.
   * 
   * @return the file extensions, each starting with the character {@code '.'}
   */
  public static Set<String> getSupportedExtensions() {
    return SUPPORTED_FILENAME_EXTENSIONS;
  }
  
  /**
   * Indicates whether a given file name has one of the file extensions valid for a file in a Drive
   * Apps Script project.
   * 
   * @param fileName the given file name
   * @return {@code true} if the file name has one of the extensions, {@code false} otherwise
   */
  public static boolean hasSupportedExtension(String fileName) {
    for (String extension : SUPPORTED_FILENAME_EXTENSIONS) {
      if (fileName.endsWith(extension)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Returns the part of a given file name preceding the extension, if any.
   * 
   * @param fileNameWithExtension the given file name
   * @return the given file name, with the extension removed if there had been one
   */
  public static String stripExtension(String fileNameWithExtension) {
    int lastDotPos = fileNameWithExtension.lastIndexOf('.');
    return
        lastDotPos == -1 ? fileNameWithExtension : fileNameWithExtension.substring(0, lastDotPos);
  }

}
