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
package com.google.gdt.eclipse.drive.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gdt.eclipse.drive.driveapi.DriveCache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Unit test for {@link FolderTree}.
 */
@RunWith(JUnit4.class)
public class FolderTreeTest {
  
  // Tests on nonempty subtrees mock a DriveCache representing the following folder structure:
  //
  // 1. folder (id="id-1", title="My Drive")
  //   1.1 leaf (id="id-1.1", title="Title 1.1")
  //   1.2 folder (id="id-1.2", title="Title 1.2")
  //     1.2.1 leaf (id="id-1.2.1", title="Title 1.2.1")
  //     1.2.2 leaf (id="id-1.2.2", title="Title 1.2.2")
  //   1.3 folder (id="id-1.3", title="Title 1.3")
  //     1.3.1 folder (id="id-1.3.1", title="Title 1.3.1")
  
  private static final String ROOT_TITLE = "My Drive";
  
  private static final Map<String, String> FILE_IDS_TO_TITLES =
      ImmutableMap.<String, String>builder()
          .put("id-1", "My Drive")
          .put("id-1.1", "Title 1.1")
          .put("id-1.2", "Title 1.2")
          .put("id-1.2.1", "Title 1.2.1")
          .put("id-1.2.2", "Title 1.2.2")
          .put("id-1.3", "Title 1.3")
          .put("id-1.3.1", "Title 1.3.1")
          .build();
  
  private static final Collection<String> NO_STRINGS = ImmutableList.of();
  
  private static final Map<String, Collection<String>> FILE_IDS_TO_CHILD_ID_COLLECTIONS =
      ImmutableMap.<String, Collection<String>>builder()
          .put("id-1", ImmutableList.of("id-1.1", "id-1.2", "id-1.3"))
          .put("id-1.1", NO_STRINGS)
          .put("id-1.2", ImmutableList.of("id-1.2.1", "id-1.2.2"))
          .put("id-1.2.1", NO_STRINGS)
          .put("id-1.2.2", NO_STRINGS)
          .put("id-1.3", ImmutableList.of("id-1.3.1"))
          .put("id-1.3.1", NO_STRINGS)
          .build();
  
  private static final Set<String> FOLDER_IDS =
      ImmutableSet.of("id-1", "id-1.2", "id-1.3", "id-1.3.1");
  
  @Mock DriveCache mockDriveCache;
  
  @Test
  public void testNonEmptyTree_makeAndGettersIncludeEmptySubtrees() {
    setUpMockDriveCacheForNonemptyTree();
    FolderTree node1 = FolderTree.make(mockDriveCache, true);
    
    assertNull(node1.getParent());
    assertEquals(ROOT_TITLE, node1.getTitle());
    List<FolderTree> node1Children = node1.getChildren();
    assertEquals(3, node1Children.size());
    FolderTree node11 = node1Children.get(0);
    FolderTree node12 = node1Children.get(1);
    FolderTree node13 = node1Children.get(2);
    
    assertSame(node1, node11.getParent());
    assertEquals("Title 1.1", node11.getTitle());
    List<FolderTree> node11Children = node11.getChildren();
    assertEquals(0, node11Children.size());
    
    assertSame(node1, node12.getParent());
    assertEquals("Title 1.2", node12.getTitle());
    List<FolderTree> node12Children = node12.getChildren();
    assertEquals(2, node12Children.size());
    FolderTree node121 = node12Children.get(0);
    FolderTree node122 = node12Children.get(1);
    
    assertSame(node12, node121.getParent());
    assertEquals("Title 1.2.1", node121.getTitle());
    List<FolderTree> node121Children = node121.getChildren();
    assertEquals(0, node121Children.size());
    
    assertSame(node12, node122.getParent());
    assertEquals("Title 1.2.2", node122.getTitle());
    List<FolderTree> node122Children = node122.getChildren();
    assertEquals(0, node122Children.size());
    
    assertSame(node1, node13.getParent());
    assertEquals("Title 1.3", node13.getTitle());
    List<FolderTree> node13Children = node13.getChildren();
    assertEquals(1, node13Children.size());
    FolderTree node131 = node13Children.get(0);
    
    assertSame(node13, node131.getParent());
    assertEquals("Title 1.3.1", node131.getTitle());
    List<FolderTree> node131Children = node131.getChildren();
    assertEquals(0, node131Children.size());
  }
  
  @Test
  public void testNonEmptyTree_makeAndGettersNoEmptySubtrees() {
    setUpMockDriveCacheForNonemptyTree();
    FolderTree node1 = FolderTree.make(mockDriveCache, false);
    
    assertNull(node1.getParent());
    assertEquals(ROOT_TITLE, node1.getTitle());
    List<FolderTree> node1Children = node1.getChildren();
    assertEquals(2, node1Children.size()); // Node 1.3 should not be present.
    FolderTree node11 = node1Children.get(0);
    FolderTree node12 = node1Children.get(1);
    
    assertSame(node1, node11.getParent());
    assertEquals("Title 1.1", node11.getTitle());
    List<FolderTree> node11Children = node11.getChildren();
    assertEquals(0, node11Children.size());
    
    assertSame(node1, node12.getParent());
    assertEquals("Title 1.2", node12.getTitle());
    List<FolderTree> node12Children = node12.getChildren();
    assertEquals(2, node12Children.size());
    FolderTree node121 = node12Children.get(0);
    FolderTree node122 = node12Children.get(1);
    
    assertSame(node12, node121.getParent());
    assertEquals("Title 1.2.1", node121.getTitle());
    List<FolderTree> node121Children = node121.getChildren();
    assertEquals(0, node121Children.size());
    
    assertSame(node12, node122.getParent());
    assertEquals("Title 1.2.2", node122.getTitle());
    List<FolderTree> node122Children = node122.getChildren();
    assertEquals(0, node122Children.size());
  }
  
  @Test
  public void testEmptyFolder_makeAndGettersIncludeEmptySubtrees() {
    setUpMockDriveCacheForOneNodeTree(false);
    FolderTree node1 = FolderTree.make(mockDriveCache, true);
    
    assertNull(node1.getParent());
    assertEquals(ROOT_TITLE, node1.getTitle());
    List<FolderTree> node1Children = node1.getChildren();
    assertEquals(0, node1Children.size());
  }
  
  @Test
  public void testEmptyFolder_makeAndGettersNoEmptySubtrees() {
    setUpMockDriveCacheForOneNodeTree(false);
    FolderTree node1 = FolderTree.make(mockDriveCache, false); // Should return root node anyway
    
    assertNull(node1.getParent());
    assertEquals(ROOT_TITLE, node1.getTitle());
    List<FolderTree> node1Children = node1.getChildren();
    assertEquals(0, node1Children.size());
  }
  
  @Test
  public void testSingleLeaf_makeAndGettersIncludeEmptySubtrees() {
    setUpMockDriveCacheForOneNodeTree(true);
    FolderTree node1 = FolderTree.make(mockDriveCache, true);
    
    assertNull(node1.getParent());
    assertEquals(ROOT_TITLE, node1.getTitle());
    List<FolderTree> node1Children = node1.getChildren();
    assertEquals(0, node1Children.size());
  }
  
  @Test
  public void testSingleLeaf_makeAndGettersNoEmptySubtrees() {
    setUpMockDriveCacheForOneNodeTree(true);
    FolderTree node1 = FolderTree.make(mockDriveCache, false);
    
    assertNull(node1.getParent());
    assertEquals(ROOT_TITLE, node1.getTitle());
    List<FolderTree> node1Children = node1.getChildren();
    assertEquals(0, node1Children.size());
  }
  
  @Test
  public void testAddDummyParent() {
    setUpMockDriveCacheForNonemptyTree();
    FolderTree node1 = FolderTree.make(mockDriveCache, false);
    FolderTree dummyParent = node1.addDummyParent();    
    assertNull(dummyParent.getParent());
    assertEquals("", dummyParent.getTitle());
    List<FolderTree> childrenOfDummyParent = dummyParent.getChildren();
    assertEquals(1, childrenOfDummyParent.size());
    assertSame(node1, childrenOfDummyParent.get(0));
    assertSame(dummyParent, node1.getParent());    
  }

  private void setUpMockDriveCacheForNonemptyTree() {
    MockitoAnnotations.initMocks(this);
    when(mockDriveCache.getRootId()).thenReturn("id-1");
    when(mockDriveCache.getChildIds(Mockito.<String>anyObject()))
        .thenAnswer(
            new Answer<Collection<String>>(){
              @Override public Collection<String> answer(InvocationOnMock invocation) {
                String parentId = (String) invocation.getArguments()[0];
                return FILE_IDS_TO_CHILD_ID_COLLECTIONS.get(parentId);
              }
            });
    when(mockDriveCache.isLeafId(Mockito.<String>anyObject()))
        .thenAnswer(
            new Answer<Boolean>(){
              @Override public Boolean answer(InvocationOnMock invocation) {
                String fileId = (String) invocation.getArguments()[0];
                return
                    FILE_IDS_TO_CHILD_ID_COLLECTIONS.keySet().contains(fileId)
                    && !FOLDER_IDS.contains(fileId);
              }
            });
    when(mockDriveCache.getTitle(Mockito.<String>anyObject()))
        .thenAnswer(
            new Answer<String>(){
              @Override public String answer(InvocationOnMock invocation) {
                String fileId = (String) invocation.getArguments()[0];
                return FILE_IDS_TO_TITLES.get(fileId);
              }
            });
  }

  private void setUpMockDriveCacheForOneNodeTree(boolean isLeaf) {
    MockitoAnnotations.initMocks(this);
    when(mockDriveCache.getRootId()).thenReturn("id-1");
    when(mockDriveCache.getChildIds(Mockito.<String>anyObject()))
        .thenReturn(ImmutableList.<String>of());
    when(mockDriveCache.isLeafId(Mockito.<String>anyObject())).thenReturn(isLeaf);
    when(mockDriveCache.getTitle(Mockito.<String>anyObject())).thenReturn("Title 1.1");
  }

}
