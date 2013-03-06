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
package com.google.gdt.eclipse.core.sdk;

import com.google.gdt.eclipse.core.JavaProjectTestUtilities;
import com.google.gdt.eclipse.core.MockEclipsePreferences;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;

/**
 * Unit tests for {@link SdkUtils}.
 */
public class SdkUtilsTest extends TestCase {
  static class MockSdk implements Sdk {
    private final String version;
    private final String name;

    MockSdk(String name, String version) {
      this.name = name;
      this.version = version;
    }

    public IClasspathEntry[] getClasspathEntries() {
      return null;
    }

    public String getDescription() {
      return name + " - " + version;
    }

    public IPath getInstallationPath() {
      return null;
    }

    public String getName() {
      return name;
    }

    public String getVersion() {
      return SdkUtils.cleanupVersion(version);
    }

    public File[] getWebAppClasspathFiles(IProject project) {
      return null;
    }

    public String toXml() {
      return null;
    }

    public IStatus validate() {
      return Status.OK_STATUS;
    }
  }

  static class MockSdkFactory<T extends Sdk> implements SdkFactory<Sdk> {
    public Sdk newInstance(String name, IPath sdkHome) {
      return null;
    }
  }

  public void testCleanupSdkString() {
    MockSdk msdk = new MockSdk("test-mock-sdk", "2.1.0M1");
    assertEquals(msdk.getVersion(), "2.1.0.M1");
  }

  /**
   * Tests {@link SdkUtils#compareVersionStrings(String, String)}.
   */
  public void testCompareVersionStrings() {
    // v1 == v2
    assertEquals(0, SdkUtils.compareVersionStrings("", ""));

    // v1 < v2
    assertTrue(SdkUtils.compareVersionStrings("0.0", "0.0.1") < 0);

    // v1 > v2
    assertTrue(SdkUtils.compareVersionStrings("1.1.0", "0.1.1") > 0);

    // v1 < v2
    assertTrue(SdkUtils.compareVersionStrings("", "0.1.1") < 0);

    // v1 == v2
    assertTrue(SdkUtils.compareVersionStrings("0.1.1", "0.1.1") == 0);

    // Tests for RC (release candidates) and MS (milestones) comparison
    assertTrue(SdkUtils.compareVersionStrings("2.0.0", "2.0.0-rc2") > 0);
    assertTrue(SdkUtils.compareVersionStrings("2.0", "2.0.0-rc2") > 0);
    assertTrue(SdkUtils.compareVersionStrings("1.9.2", "2.0.0-rc2") < 0);
    assertTrue(SdkUtils.compareVersionStrings("2.0.0.1", "2.0.0-rc2") > 0);
    assertTrue(SdkUtils.compareVersionStrings("2.0.0", "2.0.0-ms99") > 0);
    assertTrue(SdkUtils.compareVersionStrings("2.0.0-rc1", "2.0.0-rc24") < 0);
    assertTrue(SdkUtils.compareVersionStrings("2.0.0-ms99", "2.0.0-rc1") < 0);
    assertTrue(SdkUtils.compareVersionStrings("2.0.0-ms22", "2.0.0-ms1") > 0);
    assertTrue(SdkUtils.compareVersionStrings("2.0.0ms22", "2.0.0ms1") > 0);
    assertTrue(SdkUtils.compareVersionStrings("2.0.1ms1", "2.0.0ms1") > 0);
  }

  /**
   * Tests
   * {@link SdkUtils.MaxSdkVersionComputer#computeMaxSdkVersion(IJavaProject[])}.
   *
   * @throws CoreException
   */
  public void testComputeMaxSdkVersions() throws CoreException {
    String container1ID = "Container1";
    SdkManager<Sdk> manager1 =
        new SdkManager<Sdk>(container1ID, new MockEclipsePreferences(), new MockSdkFactory<Sdk>());
    String maxVersion = "1.0.0";
    final SdkSet<Sdk> sdks = manager1.getSdks();
    sdks.add(new MockSdk("1", maxVersion));
    String minVersion = "0.0.1";
    sdks.add(new MockSdk("2", minVersion));
    manager1.setSdks(sdks);

    IClasspathEntry newContainerEntry1 = JavaCore.newContainerEntry(new Path("Container1/1"));
    final IJavaProject project1 =
        JavaProjectTestUtilities.createProject("p1", new IClasspathEntry[] {newContainerEntry1});

    IClasspathEntry newContainerEntry2 = JavaCore.newContainerEntry(new Path("Container1/2"));
    final IJavaProject project2 =
        JavaProjectTestUtilities.createProject("p2", new IClasspathEntry[] {newContainerEntry2});

    SdkUtils.MaxSdkVersionComputer maxSdkVersionComputerForContainer1 =
        new SdkUtils.MaxSdkVersionComputer() {
          @Override
          public Sdk doFindSdk(IJavaProject project) throws JavaModelException {
            return project == project1 ? sdks.findSdk("1") : null;
          }
        };
    String maxSdkVersionForContainer1 = maxSdkVersionComputerForContainer1.computeMaxSdkVersion(
        new IJavaProject[] {project1, project2});
    assertNotNull(maxSdkVersionForContainer1);
    assertEquals(maxVersion, maxSdkVersionForContainer1);

    SdkUtils.MaxSdkVersionComputer maxSdkVersionComputerForContainer2 =
        new SdkUtils.MaxSdkVersionComputer() {
          @Override
          public Sdk doFindSdk(IJavaProject project) throws JavaModelException {
            return new MockSdk("", null);
          }
        };

    String maxSdkVersionForContainer2 = maxSdkVersionComputerForContainer2.computeMaxSdkVersion(
        new IJavaProject[] {project1, project2});

    assertNull(maxSdkVersionForContainer2);
  }
}

