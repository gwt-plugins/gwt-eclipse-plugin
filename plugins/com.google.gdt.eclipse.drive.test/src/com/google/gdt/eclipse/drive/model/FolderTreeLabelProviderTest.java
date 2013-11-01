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
import static org.mockito.Mockito.when;

import com.google.gdt.eclipse.drive.model.FolderTree.FolderTreeLeaf;

import org.eclipse.swt.graphics.Device;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for {@link FolderTreeLabelProvider}.
 */
@RunWith(JUnit4.class)
public class FolderTreeLabelProviderTest {
  
  private static final String FOLDER_TITLE = "folder title";
  private static final String LEAF_TITLE = "leaf title";
  
  @Mock private FolderTree folder;
  @Mock private FolderTreeLeaf leaf;
  @Mock private Device mockDevice;
  
  @Before
  public void setUpMockFolderTree() {
    MockitoAnnotations.initMocks(this);
    when(folder.getTitle()).thenReturn(FOLDER_TITLE);
    when(leaf.getTitle()).thenReturn(LEAF_TITLE);
  }
  
  @Test
  public void testGetText() {
    FolderTreeLabelProvider labelProvider = new FolderTreeLabelProvider();
    assertEquals(FOLDER_TITLE, labelProvider.getText(folder));
    assertEquals(LEAF_TITLE, labelProvider.getText(leaf));
  }
  
  @Test
  public void testIsLabelProperty() {
    FolderTreeLabelProvider labelProvider = new FolderTreeLabelProvider();
    assertFalse(labelProvider.isLabelProperty(folder, "anything"));
    assertFalse(labelProvider.isLabelProperty(leaf, "anything"));
  }
  
  // We would like to test getImage as shown below, but Image cannot be mocked because it is a
  // final class. Furthermore, there seems to be no way to create a fake Image object, because
  // the initialization of an Image object seems to require a real workbench running on a real OS.
  // 
  //@Test
  //public void testGetImage() {
  //  FolderTreeLabelProvider labelProvider =
  //      new FolderTreeLabelProvider(mockFolderImage, mockLeafImage);
  //  assertSame(mockFolderImage, labelProvider.getImage(folder));
  //  assertSame(mockLeafImage, labelProvider.getImage(leaf));
  //}

}
