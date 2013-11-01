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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.api.services.drive.Drive;
import com.google.common.collect.ImmutableSet;
import com.google.gdt.eclipse.drive.driveapi.DriveCache;
import com.google.gdt.eclipse.drive.test.MockDriveProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Unit test for {@link DriveCache}.
 */
@RunWith(JUnit4.class)
public class DriveCacheTest {
  
  private static final int FOLDER_COUNT = MockDriveProvider.getFolderCount();
  private static final int LEAF_COUNT = MockDriveProvider.getLeafCount();
  private static final String QUERY_FOR_LEAVES = MockDriveProvider.getQueryForLeaves();
  
  @Test
  public void testMake() throws IOException {
    Drive mockDrive = MockDriveProvider.getMockDrive();
    DriveCache result = DriveCache.make(mockDrive, QUERY_FOR_LEAVES);
    assertNotNull(result);
  }
  
  @Test
  public void testGetRootId() throws IOException {
    Drive mockDrive = MockDriveProvider.getMockDrive();
    DriveCache cache = DriveCache.make(mockDrive, QUERY_FOR_LEAVES);
    assertEquals(MockDriveProvider.fakeFileId("folder", 0), cache.getRootId());
  }
  
  @Test
  public void testGetChildIds() throws IOException {
    List<ImmutableSet<String>> childIdsByParentFolderNumber =
        MockDriveProvider.getChildIdsByParentFolderNumber();
    Drive mockDrive = MockDriveProvider.getMockDrive();
    DriveCache cache = DriveCache.make(mockDrive, QUERY_FOR_LEAVES);
    for (int i = 0; i < FOLDER_COUNT; i++) {
      assertEquals(
          childIdsByParentFolderNumber.get(i),
          ImmutableSet.copyOf(cache.getChildIds(MockDriveProvider.fakeFileId("folder", i))));
    }
    for (int i = 0; i < LEAF_COUNT; i++) {
      Collection<String> result = cache.getChildIds(MockDriveProvider.fakeFileId("leaf", i));
      assertTrue(result.isEmpty());
    }
    Collection<String> resultForInvalidId = cache.getChildIds("invalid ID");
    assertTrue(resultForInvalidId.isEmpty());
  }
  
  @Test
  public void testGetTitle() throws IOException {
    Drive mockDrive = MockDriveProvider.getMockDrive();
    DriveCache cache = DriveCache.make(mockDrive, QUERY_FOR_LEAVES);
    for (int i = 0; i < FOLDER_COUNT; i++) {
      assertEquals(
          MockDriveProvider.fakeTitle("folder", i),
          cache.getTitle(MockDriveProvider.fakeFileId("folder", i)));
    }
    for (int i = 0; i < LEAF_COUNT; i++) {
      assertEquals(
          MockDriveProvider.fakeTitle("leaf", i),
          cache.getTitle(MockDriveProvider.fakeFileId("leaf", i)));
    }
    assertNull(cache.getTitle("invalid ID"));
  }
  
  @Test
  public void testIsLeafId() throws IOException {
    Drive mockDrive = MockDriveProvider.getMockDrive();
    DriveCache cache = DriveCache.make(mockDrive, QUERY_FOR_LEAVES);
    for (int i = 0; i < FOLDER_COUNT; i++) {
      assertFalse(cache.isLeafId(MockDriveProvider.fakeFileId("folder", i)));
    }
    for (int i = 0; i < LEAF_COUNT; i++) {
      assertTrue(cache.isLeafId(MockDriveProvider.fakeFileId("leaf", i)));
    }
    assertFalse(cache.isLeafId("invalid ID"));
  }

}
