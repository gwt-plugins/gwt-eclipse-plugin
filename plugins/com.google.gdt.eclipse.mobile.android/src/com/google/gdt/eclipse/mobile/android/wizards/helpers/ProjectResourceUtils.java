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
package com.google.gdt.eclipse.mobile.android.wizards.helpers;

import com.google.gdt.eclipse.mobile.android.GdtAndroidPlugin;

import org.eclipse.core.runtime.CoreException;
import org.osgi.framework.Bundle;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Helper class for creation of the Android Project.
 */
public class ProjectResourceUtils {

  private static String WS_SEP = "/"; //$NON-NLS-N$

  /**
   * Returns the URL of a binary file embedded in the plugin jar file.
   * 
   * @param filepath the file path to the text file
   * @return null if the file was not found.
   */
  public static URL getEmbeddedFileUrl(String filepath) {
    Bundle bundle = null;
    synchronized (GdtAndroidPlugin.class) {
      if (GdtAndroidPlugin.getDefault() != null) {
        bundle = GdtAndroidPlugin.getDefault().getBundle();
      } else {
        GdtAndroidPlugin.getLogger().logError("GDT Android Plugin is missing"); //$NON-NLS-1$
        return null;
      }
    }
    // attempt to get a file to one of the template.
    String path = filepath;
    if (!path.startsWith(WS_SEP)) {
      path = WS_SEP + path;
    }
    URL url = bundle.getEntry(path);
    if (url == null) {
      GdtAndroidPlugin.getLogger().logError(
          "Bundle file URL not found at path '%s'", path); //$NON-NLS-1$
    }
    return url;
  }

  /**
   * Reads and returns the content of a binary file embedded in the plugin jar
   * file.
   * 
   * @param filepath the file path to the text file
   * @return null if the file could not be read
   */
  public static byte[] getResource(String resourceName) {
    try {
      InputStream is = getResourceAsStream(resourceName);
      if (is != null) {
        // create a buffered reader to facilitate reading.
        BufferedInputStream stream = new BufferedInputStream(is);

        // get the size to read.
        int avail = stream.available();

        // create the buffer and reads it.
        byte[] buffer = new byte[avail];
        stream.read(buffer);

        // and return.
        return buffer;
      }
    } catch (IOException e) {
      GdtAndroidPlugin.getLogger().logError(e);
    }

    return null;
  }

  /**
   * Reads and returns the content of a binary file embedded in the plugin jar
   * file.
   * 
   * @param filepath the file path to the text file
   * @return null if the file could not be read
   */
  public static InputStream getResourceAsStream(String resourceName) {
    // attempt to read an embedded file
    try {
      URL url = getEmbeddedFileUrl(WS_SEP + resourceName);
      if (url != null) {
        return url.openStream();
      }
    } catch (MalformedURLException e) {
      // we'll just return null.
      GdtAndroidPlugin.getLogger().logError(e,
          "Failed to read stream '%s'", resourceName); //$NON-NLS-1$
    } catch (IOException e) {
      // we'll just return null;.
      GdtAndroidPlugin.getLogger().logError(e,
          "Failed to read stream '%s'", resourceName); //$NON-NLS-1$
    }
    return null;
    // return AndroidProjectResources.class.getResourceAsStream(resourceName);
  }

  public static String getResourceAsString(String templateName)
      throws CoreException {
    try {
      InputStream is = getResourceAsStream(templateName);
      if (is != null) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        char[] cbuf = new char[1024];
        StringBuilder total = new StringBuilder();
        int nchars;
        while ((nchars = reader.read(cbuf)) != -1) {
          total.append(cbuf, 0, nchars);
        }
        return total.toString();
      }
    } catch (IOException e) {
      // we'll just return null
      GdtAndroidPlugin.getLogger().logError(e,
          "Failed to read text file '%s'", templateName); //$NON-NLS-1$
    }

    return null;
  }

  public static String getResourceAsString(String templateName,
      Map<String, String> replacements) throws CoreException {
    String contents = getResourceAsString(templateName);
    if (contents == null) {
      return null;
    }
    return makeTemplateReplacements(replacements, contents);
  }

  private static String makeTemplateReplacements(
      Map<String, String> replacements, String contents) {
    String replacedContents = contents;
    Set<Entry<String, String>> entries = replacements.entrySet();
    for (Iterator<Entry<String, String>> iter = entries.iterator(); iter.hasNext();) {
      Entry<String, String> entry = iter.next();
      String replaceThis = entry.getKey();
      String withThis = entry.getValue();
      if (replaceThis != null && withThis != null) {
        replacedContents = replacedContents.replaceAll(replaceThis, withThis);
      }
    }

    return replacedContents;
  }

}
