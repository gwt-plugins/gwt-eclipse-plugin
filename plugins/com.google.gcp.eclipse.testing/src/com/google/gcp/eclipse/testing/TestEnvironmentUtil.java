/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.gcp.eclipse.testing;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A set of utilities for adapting the current testing environment.
 */
public class TestEnvironmentUtil {
  private TestEnvironmentUtil() {} // Non-instantiatable utility class

  /**
   * Updates the given environment variable in a testing environment.
   */
  public static void updateEnvironmentVariable(String environmentVariable, String value) {
    Class<?>[] classes = Collections.class.getDeclaredClasses();
    Map<String, String> env = System.getenv();
    for (Class<?> cl : classes) {
      if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
        try {
          Field field = cl.getDeclaredField("m");
          field.setAccessible(true);
          @SuppressWarnings("unchecked")
          Map<String, String> updatableEnv = (Map<String, String>) field.get(env);
          updatableEnv.put(environmentVariable, value);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
          throw new IllegalStateException("Unable to set " + environmentVariable
              + " environment variable", e);
        }
        break;
      }
    }
  }

  /**
   * Installs an SDK or other development resource that is bundled in a testing plug-in into that
   * plug-in's data area and returns the absolute path where it was installed.
   *
   * @param bundle the {@link Bundle} in which the SDK is bundled
   * @param pathToZip a relative path from the root of that bundle to the SDK
   * @return the absolute path of the installed bundle
   */
  public static IPath installTestSdk(Bundle bundle, String pathToZip) {
    URL fileUrl = null;
    try {
      fileUrl = new File(pathToZip).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalStateException(
          "Unable to install the SDK. Path to zip was invalid. pathToZip=" + pathToZip, e);
    }
    return installTestSdk(bundle, fileUrl);
  }


  /**
   * Installs an SDK or other development resource that is bundled in a testing plug-in into that
   * plug-in's data area and returns the absolute path where it was installed.
   *
   * @param bundle the {@link Bundle} in which the SDK is bundled
   * @param sdkPathFromProjectRoot a relative path from the root of that bundle to the SDK
   * @return the absolute path of the installed bundle
   */
  public static IPath installTestSdk(Bundle bundle, IPath sdkPathFromProjectRoot) {
    URL fileUrl = bundle.getEntry(sdkPathFromProjectRoot.toPortableString());
    return installTestSdk(bundle, fileUrl);
  }

  /**
   * Installs an SDK or other development resource that is bundled in a testing plug-in into that
   * plug-in's data area and returns the absolute path where it was installed.
   *
   * @param bundle the {@link Bundle} in which the SDK is bundled
   * @param fileUrl the path to the zip
   * @return the absolute path of the installed bundle
   */
  public static IPath installTestSdk(Bundle bundle, URL fileUrl) {
    byte[] buffer = new byte[1024];
    IPath sdkRootPath = null;
    File output = bundle.getDataFile("");
    try (ZipInputStream is =
        new ZipInputStream(new FileInputStream(new File(FileLocator.toFileURL(fileUrl).getPath())))) {
      ZipEntry entry = is.getNextEntry();
      if (entry != null) {
        String rootEntryPath = Path.fromPortableString(entry.getName()).segment(0);
        sdkRootPath = Path.fromPortableString(new File(output, rootEntryPath).getAbsolutePath());
        if (!sdkRootPath.toFile().exists()) {
          while (entry != null) {
            IPath fileName = Path.fromPortableString(entry.getName());
            if (!"demos".equals(fileName.segment(1)) && !"samples".equals(fileName.segment(1))) {
              File newFile = new File(output + File.separator + fileName);
              if (!entry.isDirectory()) {
                new File(newFile.getParent()).mkdirs();
                try (FileOutputStream os = new FileOutputStream(newFile)) {
                  int bytesRead;
                  while ((bytesRead = is.read(buffer)) > 0) {
                    os.write(buffer, 0, bytesRead);
                  }
                }
              }
            }
            entry = is.getNextEntry();
          }
        }
      }
      is.closeEntry();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to install the SDK. fileUrl=" + fileUrl, e);
    }
    return sdkRootPath;
  }

  public static String getMavenPropertyVersionFor(String key) {
    // Maven compile uses a jar, and is in target
    String basePath = Paths.get("").toAbsolutePath().toString();
    if (basePath.endsWith("target")) {
      basePath = basePath.replace("target/", "");
    }

    String pomfile = basePath + "/../../pom.xml";
    Model model = null;
    MavenXpp3Reader mavenreader = new MavenXpp3Reader();
    try {
      FileReader reader = new FileReader(pomfile);
      model = mavenreader.read(reader);
      model.setPomFile(new File(pomfile));
    } catch (Exception ex) {
      throw new IllegalStateException("Can't find the pom.xml file. basePath=" + basePath + " pomFile=" + pomfile);
    }

    MavenProject project = new MavenProject(model);
    Properties properties = project.getProperties();
    String value = (String) properties.get(key);
    return value;
  }

}
