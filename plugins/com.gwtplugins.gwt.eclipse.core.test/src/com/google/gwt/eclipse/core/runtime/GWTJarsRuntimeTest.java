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
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;
import com.google.gwt.eclipse.core.util.Util;
import com.google.gwt.eclipse.testing.GwtRuntimeTestUtilities;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the {@link GWTJarsRuntime} class.
 */
public class GWTJarsRuntimeTest extends AbstractGWTPluginTestCase {

  private GWTJarsRuntime runtime;

  public void testCreateClassLoader() throws Exception {
    URLClassLoader classLoader = runtime.createClassLoader();
    URL[] urls = classLoader.getURLs();

    int numGwtJars = 0;
    for (URL url : urls) {
      String urlFile = url.getFile();
      String jar = urlFile.substring(urlFile.lastIndexOf('/') + 1);
      if (isGWTJar(runtime, jar)) {
        numGwtJars++;
      }
    }

    // There should be two GWT jars
    assertEquals(3, numGwtJars);
  }

  public void testGetClasspathEntries() throws Exception {
    IClasspathEntry[] cpEntries = runtime.getClasspathEntries();

    // Look for the gwt-specific classpath entries
    List<IClasspathEntry> gwtCpEntries = new ArrayList<IClasspathEntry>();
    for (IClasspathEntry cpEntry : cpEntries) {
      if (isGWTJar(runtime, cpEntry.getPath().lastSegment())) {
        gwtCpEntries.add(cpEntry);
      }
    }

    // Make sure that there are two of them
    assertEquals(3, gwtCpEntries.size());

    for (int i = 0; i < gwtCpEntries.size(); i++) {
      IClasspathEntry gwtClasspathEntry = gwtCpEntries.get(i);
      assertEquals(IClasspathEntry.CPE_LIBRARY, gwtClasspathEntry.getEntryKind());
      assertEquals(IPackageFragmentRoot.K_BINARY, gwtClasspathEntry.getContentKind());

      // Verify that our classpath entries point at the GWT javadoc.
      IClasspathAttribute[] extraAttributes = gwtClasspathEntry.getExtraAttributes();
      assertTrue("No extra attributes seen for classpath entry: " + gwtClasspathEntry, extraAttributes.length > 0);
      assertEquals(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, extraAttributes[0].getName());

      /*
       * Entries should have their javadoc location point at a directory with "index.html". Strangely, the values of
       * these classpath attributes are specified as "file://" urls.
       */
      File jdLocation = new File(new URL(extraAttributes[0].getValue()).getFile());
      assertTrue("Javadoc file does not exist", jdLocation.exists());
      List<String> files1 = Arrays.asList(jdLocation.list());
      assertTrue("Javadoc file is not an index.html file.", files1.contains("index.html"));
    }
  }

  public void testGetDevJar() throws Exception {
    File devJar = runtime.getDevJar();
    assertNotNull(devJar);
    String devJarName = devJar.getName();
    assertTrue(devJarName.equalsIgnoreCase(Util.getDevJarName(runtime.getInstallationPath())));
  }

  public void testGetValidationClasspathEntries() throws Exception {
    IClasspathEntry[] cpEntries = runtime.getClasspathEntries();

    // Look for the validation-specific classpath entries
    List<IClasspathEntry> validationCpEntries = new ArrayList<IClasspathEntry>();
    for (IClasspathEntry cpEntry : cpEntries) {
      if (cpEntry.getPath().lastSegment().startsWith(GwtSdk.VALIDATION_API_JAR_PREFIX)) {
        validationCpEntries.add(cpEntry);
      }
    }

    if (validationCpEntries.size() == 0) {
      String sdkVersion = runtime.getVersion();

      // Can't be an internal version, because it would have the
      // validation jars
      assertFalse(SdkUtils.isInternal(sdkVersion));

      // Sdk version must be pre-GWT 2.3.0
      assertTrue(SdkUtils.compareVersionStrings(sdkVersion, "2.3.0") < 0);

      return;
    }

    // Make sure that there are at least two of them
    assertEquals(2, validationCpEntries.size());

    IClasspathEntry sourcesEntry = null;
    IClasspathEntry binaryEntry = null;

    for (IClasspathEntry validationClasspathEntry : validationCpEntries) {
      // Verify the entry types
      assertEquals(IClasspathEntry.CPE_LIBRARY, validationClasspathEntry.getEntryKind());
      assertEquals(IPackageFragmentRoot.K_BINARY, validationClasspathEntry.getContentKind());

      if (validationClasspathEntry.getPath().lastSegment().contains("sources")) {
        sourcesEntry = validationClasspathEntry;
      } else {
        binaryEntry = validationClasspathEntry;
      }
    }

    // Verify that the sources and binary entries correspond to each other
    assertTrue(Util.findSourcesJarForClassesJar(binaryEntry.getPath()).equals(sourcesEntry.getPath()));

    // Verify that the source attachment path has been set for the binary
    // entry
    assertTrue(binaryEntry.getSourceAttachmentPath().equals(sourcesEntry.getPath()));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // add the default runtime
    GwtRuntimeTestUtilities.addDefaultRuntime();

    // We know the default runtime is a GWTJarsRuntime
    runtime = GwtRuntimeTestUtilities.getDefaultRuntime();
  }

  private boolean isGWTJar(GWTJarsRuntime jarsRuntime, String jarName) {
    return GwtSdk.GWT_USER_JAR.equals(jarName) || GwtSdk.GWT_CODESERVER_JAR.equals(jarName)
        || jarName.equalsIgnoreCase(Util.getDevJarName(jarsRuntime.getInstallationPath()))
        || "gwt-servlet.jar".equals(jarName);
  }

}
