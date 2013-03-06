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
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.googleapi.core.ApiDirectoryListingJsonCodec;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The IconCache represents a local cache of image files that act as a pre-fetch
 * cache that can be accessed locally rather than remotely fetched. Create using
 * a source file and a directory to contain the exploded ZIP.
 */
public class IconCache {
  private static final int BUFFER = 2048;
  private static final String ICON_BUNDLE_MAP_NAME = "icon_bundle_map";
  private static final ApiDirectoryListingJsonCodec jsonCodec = new ApiDirectoryListingJsonCodec();

  /**
   * Create an IconCache with a ZIP file and a root directory to use to explode
   * the zip.
   * 
   * @param sourceZipFile
   * @param destDirectory
   * @return
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public static IconCache createIconCache(File sourceZipFile, File destDirectory)
      throws IOException {
    IconCache cache = new IconCache();
    cache.root = destDirectory;

    // explode zip within root directory
    FileInputStream zipFileIn = null;
    BufferedInputStream buffZipFileIn = null;
    ZipInputStream sourceZipIn = null;
    ZipEntry entry;

    BufferedOutputStream entryOut = null;

    try {
      zipFileIn = new FileInputStream(sourceZipFile);
      buffZipFileIn = new BufferedInputStream(zipFileIn);
      sourceZipIn = new ZipInputStream(buffZipFileIn);
      while ((entry = sourceZipIn.getNextEntry()) != null) {
        int count;
        byte data[] = new byte[BUFFER];
        File entryTargetFile = new File(destDirectory, entry.getName());
        FileOutputStream fileOut = new FileOutputStream(entryTargetFile);
        try {
          entryOut = new BufferedOutputStream(fileOut, BUFFER);
          while ((count = sourceZipIn.read(data, 0, BUFFER)) != -1) {
            entryOut.write(data, 0, count);
          }
          entryOut.flush();
        } finally {
          if (entryOut != null) {
            entryOut.close();
          }
        }
      }
    } finally {
      IOException exception = null;
      if (sourceZipIn != null) {
        try {
          sourceZipIn.close();
        } catch (IOException e) {
          exception = e;
        }
      }
      if (buffZipFileIn != null) {
        try {
          buffZipFileIn.close();
        } catch (IOException e) {
          exception = e;
        }
      }
      if (zipFileIn != null) {
        try {
          zipFileIn.close();
        } catch (IOException e) {
          exception = e;
        }
      }
      if (exception != null) {
        throw exception;
      }
    }

    // read image map from root directory
    File mapFile = new File(destDirectory, ICON_BUNDLE_MAP_NAME);
    InputStream mapFileInputStream = new FileInputStream(mapFile);

    try {
      cache.iconMap = (Map<String, String>) jsonCodec.toIconCacheMap(mapFileInputStream);
    } catch (Exception e) {
      ManagedApiLogger.log(ManagedApiLogger.ERROR, e,
          "Failure to read icon map");
      cache.iconMap = (Map<String, String>) Collections.EMPTY_MAP;
    }
    return cache;
  }

  private Map<String, String> iconMap = new HashMap<String, String>();

  private File root;

  private IconCache() {
  }

  /**
   * Remove the files in the local fs. After calling this, the cache is no
   * longer in a stable state and should be deleted.
   */
  public void delete() throws IOException {
    ResourceUtils.deleteFileRecursively(root);
  }

  /**
   * Return the local URL for a general URL or null if the key URL is not in the
   * cache.
   */
  public URL getLocalImageURL(String key) throws MalformedURLException {
    URL localImageURL = null;
    if (iconMap.containsKey(key)) {
      String localImageName = iconMap.get(key);
      File localImageFile = new File(root, localImageName);
      if (localImageFile.exists()) {
        localImageURL = localImageFile.toURI().toURL();
      }
    }
    return localImageURL;
  }
}
