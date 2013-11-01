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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit test for {@link FileTypes}.
 */
@RunWith(JUnit4.class)
public class FileTypesTest {
  
  @Test
  public void testDriveTypeForExtension() {
    assertEquals("server_js", FileTypes.driveTypeForExtension(".gs"));
    assertEquals("html", FileTypes.driveTypeForExtension(".html"));
    try {
      FileTypes.driveTypeForExtension(".bat");
      fail("IllegalArgumentException not raised for invalid extension.");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  @Test
  public void testFileNameWithExtension() {
    assertEquals("Code.gs", FileTypes.fileNameWithExtension("Code", "server_js"));
    assertEquals("Code.gs", FileTypes.fileNameWithExtension("Code.gs", "server_js"));
    assertEquals("home.html.gs", FileTypes.fileNameWithExtension("home.html", "server_js"));
    assertEquals("home.html", FileTypes.fileNameWithExtension("home", "html"));
    assertEquals("home.html", FileTypes.fileNameWithExtension("home.html", "html"));
    assertEquals("Code.gs.html", FileTypes.fileNameWithExtension("Code.gs", "html"));
    try {
      FileTypes.fileNameWithExtension("autoexec", "batch");
      fail("IllegalArgumentException not raised for invalid file type.");
    } catch (IllegalArgumentException e) {
      // expected
    }
    try {
      FileTypes.fileNameWithExtension("autoexec.bat", "batch");
      fail("IllegalArgumentException not raised for invalid file type.");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  @Test
  public void testGetSupportedExtensions() {
    assertEquals(ImmutableSet.of(".gs", ".html"), FileTypes.getSupportedExtensions());
  }
  
  @Test
  public void testHasSupportedExtension() {
    assertTrue(FileTypes.hasSupportedExtension("Code.gs"));
    assertFalse(FileTypes.hasSupportedExtension("Code.js"));
    assertFalse(FileTypes.hasSupportedExtension("Code"));
    assertTrue(FileTypes.hasSupportedExtension("home.html"));
    assertTrue(FileTypes.hasSupportedExtension("Code.js.gs"));
    assertFalse(FileTypes.hasSupportedExtension("Code.gs.js"));
    assertFalse(FileTypes.hasSupportedExtension("archive.tar.gz"));
  }
  
  @Test
  public void testStripExtension() {
    assertEquals("autoexec", FileTypes.stripExtension("autoexec.bat"));
    assertEquals("README", FileTypes.stripExtension("README"));
    assertEquals("", FileTypes.stripExtension(".bashrc"));
    assertEquals("period", FileTypes.stripExtension("period."));
    assertEquals("", FileTypes.stripExtension("."));
    assertEquals("", FileTypes.stripExtension(""));
  }

}
