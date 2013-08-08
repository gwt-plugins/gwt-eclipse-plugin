/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.appengine.swarm_backend.resources;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Utility classes for generating sample code used in Android + App Engine
 * projects.
 * 
 * TODO(rdayal): Move a bunch of this code to ResourceUtils.
 */
public class GenerationUtils {

  public static final String SRC_PACKAGE_FRAGMENT_ROOT = "src";

  public static InputStream getResource(String resourceName) {
    InputStream in = GenerationUtils.class.getClassLoader().getResourceAsStream(
        GenerationUtils.class.getPackage().getName().replace('.', '/') + '/' + resourceName);
    return in;
  }

  public static String getResourceAsString(String resourceName) throws IOException {
    InputStream in = getResource(resourceName);
    // read it with BufferedReader
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line + "\n");
    }
    br.close();
    return sb.toString();
  }

  public static void createFile(File f, InputStream in) throws IOException {
    createResource(f);
    OutputStream out = new FileOutputStream(f);
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  public static void createResource(File f) throws IOException {
    if (f.exists()) {
      return;
    }
    createResource(f.getParentFile());
    if (f.getName().contains(".")) {
      f.createNewFile();
    } else {
      f.mkdir();
    }
  }

  /**
   * Finds the package fragment with the package name in the java project.
   */
  public static IPackageFragment findpackageFragment(IJavaProject javaProject, String packageName)
      throws JavaModelException {
    for (IPackageFragment fragment : javaProject.getPackageFragments()) {
      if (packageName == null) {
        if (!fragment.isDefaultPackage() && !fragment.getElementName().equals("com")) {
          return fragment;
        }
      } else if (fragment.getElementName().equals(packageName)) {
        if (fragment.getCorrespondingResource().getLocation().toString().contains(
            "/" + SRC_PACKAGE_FRAGMENT_ROOT + "/")) {
          return fragment;
        }
      }
    }
    return null;
  }

}
