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
package com.google.gdt.eclipse.core;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tests file equality utility methods in {@link ResourceUtils}.
 */
public class ResourceUtilsFileEqualityTest extends TestCase {

  private static final Random RANDOM = new Random(351521);

  private static void deleteFilesAndDir(List<File> files) {
    File dir = files.get(0).getParentFile();
    for (File file : files) {
      file.delete();
    }
    dir.delete();
  }

  private final List<File> files = new ArrayList<File>();

  private final List<File> copiedFiles = new ArrayList<File>();

  public void testAreFileListsEqual() throws Exception {
    // Test lists that are the same
    assertTrue(ResourceUtils.areFileListsEqual(files, copiedFiles));

    // Test lists that have mostly the same files, but missing one
    List<File> missingOneOfTheFiles = new ArrayList<File>(files);
    missingOneOfTheFiles.remove(0);
    assertFalse(ResourceUtils.areFileListsEqual(files, missingOneOfTheFiles));
    assertFalse(ResourceUtils.areFileListsEqual(missingOneOfTheFiles, files));

    // Test different lists of files
    List<File> firstHalfOfFiles = new ArrayList<File>(files);
    while (firstHalfOfFiles.size() > files.size() / 2) {
      firstHalfOfFiles.remove(firstHalfOfFiles.size() - 1);
    }
    List<File> secondHalfOfFiles = new ArrayList<File>(files);
    while (secondHalfOfFiles.size() > files.size() / 2) {
      secondHalfOfFiles.remove(0);
    }
    assertFalse(ResourceUtils.areFileListsEqual(firstHalfOfFiles,
        secondHalfOfFiles));

    // Test with empty sets
    assertFalse(ResourceUtils.areFileListsEqual(files,
        Collections.<File> emptyList()));
  }

  public void testAreFilesEqual() throws Exception {
    // Test files that are the same
    for (int i = 0; i < files.size(); i++) {
      assertTrue(ResourceUtils.areFileContentsEqual(files.get(i), copiedFiles.get(i)));
    }

    // Test files that are not the same
    for (int i = 0; i < files.size(); i++) {
      assertFalse(ResourceUtils.areFileContentsEqual(files.get(i), files.get((i + 1)
          % files.size())));
    }

    // Test one file that does not exist
    File tmpFile = File.createTempFile("tempFile", "");
    assertTrue(tmpFile.delete());
    ResourceUtils.areFileContentsEqual(files.get(0), tmpFile);
  }

  @Override
  protected void setUp() throws Exception {
    File tmpDir1 = ResourceUtils.createTempDir("tmp", "");
    for (int i = 0; i < 20; i++) {
      File tmpFile = File.createTempFile("tempFile", "", tmpDir1);
      files.add(tmpFile);

      FileOutputStream fos = new FileOutputStream(tmpFile);
      try {
        fos.write(generateTempData());
      } finally {
        fos.close();
      }
    }

    byte[] buf = new byte[4096];
    File tmpDir2 = ResourceUtils.createTempDir("tmp", "");
    for (int i = 0; i < files.size(); i++) {
      File srcFile = files.get(i);
      File destFile = new File(tmpDir2, srcFile.getName());
      copiedFiles.add(destFile);

      FileInputStream srcFis = new FileInputStream(srcFile);
      FileOutputStream destFos = new FileOutputStream(destFile);
      try {
        int numRead;
        while ((numRead = srcFis.read(buf)) != -1) {
          destFos.write(buf, 0, numRead);
        }
      } finally {
        srcFis.close();
        destFos.close();
      }
    }
  }

  @Override
  protected void tearDown() throws Exception {
    deleteFilesAndDir(files);
    deleteFilesAndDir(copiedFiles);
  }

  private byte[] generateTempData() {
    byte[] tmpData = new byte[RANDOM.nextInt(10000)];
    RANDOM.nextBytes(tmpData);
    return tmpData;
  }
}
