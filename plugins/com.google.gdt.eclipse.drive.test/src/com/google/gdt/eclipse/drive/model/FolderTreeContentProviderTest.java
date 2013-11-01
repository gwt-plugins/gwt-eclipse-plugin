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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
/**
 * Unit test for {@link FolderTreeContentProvider}.
 */
@RunWith(JUnit4.class)
public class FolderTreeContentProviderTest {
  
  @Mock private FolderTree root;
  @Mock private FolderTree child0;
  @Mock private FolderTree child1;
  
  @Before
  public void setUpMockFolderTree() {
    MockitoAnnotations.initMocks(this);
    when(root.getParent()).thenReturn(null);
    when(root.getChildren()).thenReturn(ImmutableList.of(child0, child1));
    when(child0.getParent()).thenReturn(root);
    when(child0.getChildren()).thenReturn(ImmutableList.<FolderTree>of());
    when(child1.getParent()).thenReturn(root);
    when(child1.getChildren()).thenReturn(ImmutableList.<FolderTree>of());
  }
  
  @Test
  public void testGetChildren() {
    FolderTreeContentProvider contentProvider = new FolderTreeContentProvider();
    Object[] childrenOfRoot = contentProvider.getChildren(root);
    assertEquals(2, childrenOfRoot.length);
    assertSame(child0, childrenOfRoot[0]);
    assertSame(child1, childrenOfRoot[1]);
    Object[] childrenOfChild1 = contentProvider.getChildren(child0);
    assertEquals(0, childrenOfChild1.length);
  }
  
  @Test
  public void testGetElements() {
    FolderTreeContentProvider contentProvider = new FolderTreeContentProvider();
    Object[] childrenOfRoot = contentProvider.getElements(root);
    assertEquals(2, childrenOfRoot.length);
    assertSame(child0, childrenOfRoot[0]);
    assertSame(child1, childrenOfRoot[1]);
    Object[] childrenOfChild1 = contentProvider.getElements(child0);
    assertEquals(0, childrenOfChild1.length);   
  }
  
  @Test
  public void testGetParent() {
    FolderTreeContentProvider contentProvider = new FolderTreeContentProvider();
    Object parentOfRoot = contentProvider.getParent(root);
    assertNull(parentOfRoot);
    Object parentOfChild1 = contentProvider.getParent(child0);
    assertSame(root, parentOfChild1);
  }
  
  @Test
  public void testHasChildren() {
    FolderTreeContentProvider contentProvider = new FolderTreeContentProvider();
    assertTrue(contentProvider.hasChildren(root));
    assertFalse(contentProvider.hasChildren(child0));
    assertFalse(contentProvider.hasChildren(child1));
  }

}
