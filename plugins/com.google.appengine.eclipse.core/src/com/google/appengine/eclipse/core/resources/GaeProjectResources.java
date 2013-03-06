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
package com.google.appengine.eclipse.core.resources;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.resources.ProjectResources;

import org.eclipse.core.runtime.CoreException;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains methods used to generate the source for App Engine projects.
 * 
 * TODO: Replace this class by making use of template files.
 */
public class GaeProjectResources {
  public static InputStream createAppEngineWebXmlSource(boolean isGwtProject) {
    if (isGwtProject) {
      return getResourceAsStream("appengine-web.xml.gwt.template");
    } else {
      return getResourceAsStream("appengine-web.xml.template");
    }
  }

  public static InputStream createFavicon() {
    return getResourceAsStream("favicon.ico");
  }

  public static InputStream createJdoConfigXmlSource(String version) {
    if (version.compareTo("v2") == 0) {
      return getResourceAsStream("jdoconfig.xml.v2.template");
    } else {
      return getResourceAsStream("jdoconfig.xml.template");
    }
  }

  public static InputStream createPersistenceXmlSource(String version) {
    if (version.compareTo("v2") == 0) {
      return getResourceAsStream("persistence.xml.v2.template");
    } else {
      return getResourceAsStream("persistence.xml.template");
    }
  }

  public static String createSampleServletSource(String servletPackageName,
      String servletSimpleClassName) throws CoreException {
    String servletClassSource = getServletClassSource(servletPackageName,
        servletSimpleClassName);
    return ProjectResources.reformatJavaSourceAsString(servletClassSource);
  }

  public static InputStream createWelcomePageSource(String servletName,
      String servletPath) throws CoreException {
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@ServletPath@", servletPath);
    replacements.put("@ServletName@", servletName);
    return ResourceUtils.getResourceAsStreamAndFilterContents(
        GaeProjectResources.class, replacements, "WelcomePage.template");
  }
  
  public static InputStream createEmptyWebXml() throws CoreException {
    return ResourceUtils.getResourceAsStreamAndFilterContents(
        GaeProjectResources.class, new HashMap<String, String>(), "web.xml.template");
  }

  private static InputStream getResourceAsStream(String resourceName) {
    return GaeProjectResources.class.getResourceAsStream(resourceName);
  }

  private static String getServletClassSource(String servletPackageName,
      String servletClassSimpleName) throws CoreException {
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@ServletPackageName@", servletPackageName);
    replacements.put("@ServletClassSimpleName@", servletClassSimpleName);
    return ResourceUtils.getResourceAsStringAndFilterContents(
        GaeProjectResources.class, replacements, "Servlet.java.template");
  }
}
