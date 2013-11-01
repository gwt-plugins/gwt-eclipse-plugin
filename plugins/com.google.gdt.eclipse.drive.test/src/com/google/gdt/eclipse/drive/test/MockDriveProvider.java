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
package com.google.gdt.eclipse.drive.test;

import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.gdt.eclipse.drive.driveapi.DriveQueries;
import com.google.gdt.eclipse.drive.driveapi.DriveServiceFacade;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides methods for building and interpreting the results of a mock {@link Drive} object.
 * We mock the following Drive file structure:
 * <pre>
 * folder 0 (root)
 *     folder 1
 *         folder 2
 *             leaf 0
 *             leaf 1
 *         folder 3
 *         leaf 2
 *     folder 4
 *         leaf 3
 *         leaf 4
 *     folder 5
 *         folder 6
 *     leaf 5
 *     leaf 6
 * </pre>
 * (One can imagine that there are other non-folder files as well, but leaf 0 through leaf 6 are the
 * only ones that match the query returned by {@link #getQueryForLeaves()}, and the only ones that
 * arise in the use of the mock.) The methods {@link #getChildIdsByParentFolderNumber()} and
 * {@link #getTitlesToChildTitles()} return two different representations of the tree structure.
 * 
 * <p>For each folder number {@code i}, folder {@code i} has the file ID returned by
 * {@link #fakeFileId}{@code ("folder", i)} and the title returned by
 * {@link #fakeTitle}{@code ("folder", i)}. For each leaf number {@code i}, leaf {@code i} has the
 * file ID returned by {@link #fakeFileId}{@code ("leaf", i)} and the title returned by
 * {@link #fakeTitle}{@code ("leaf", i)}.
 * 
 * <p>Retrieval of {@link File} objects through {@link Drive.Files.Get} commands is supported for
 * the root folder and for leaf 0. Leaf 0 contains an Apps Script project. 
 */
public class MockDriveProvider {
  
  private static final int NO_PARENT = -1;
  
  // FOLDER_NUMBERS_OF_LEAF_PARENTS[i] = j if and only if the parent of leaf i is folder j:
  private static final int[] FOLDER_NUMBERS_OF_LEAF_PARENTS = {2, 2, 1, 4, 4, 0, 0};

  // For i > 0, FOLDER_NUMBERS_OF_FOLDER_PARENTS[i] = j if and only if
  // the parent of folder i is folder j:
  private static final int[] FOLDER_NUMBERS_OF_FOLDER_PARENTS = {NO_PARENT, 0, 1, 1, 0, 0, 5};
  
  private static final List<ImmutableSet<String>> CHILD_IDS_BY_PARENT_FOLDER_NUMBER =
      ImmutableList.of(
          // file IDs of children of folder 0:
          ImmutableSet.of(
              fakeFileId("folder", 1), fakeFileId("folder", 4), fakeFileId("folder", 5),
              fakeFileId("leaf", 5), fakeFileId("leaf", 6)),
          // file IDs of children of folder 1:
          ImmutableSet.of(fakeFileId("folder", 2), fakeFileId("folder", 3), fakeFileId("leaf", 2)),
          // file IDs of children of folder 2:
          ImmutableSet.of(fakeFileId("leaf", 0), fakeFileId("leaf", 1)),
          // file IDs of children of folder 3:
          ImmutableSet.<String>of(),
          // file IDs of children of folder 4:
          ImmutableSet.of(fakeFileId("leaf", 3), fakeFileId("leaf", 4)),
          // file IDs of children of folder 5:
          ImmutableSet.of(fakeFileId("folder", 6)),
          // file IDs of children of folder 6:
          ImmutableSet.<String>of());

  private static final int LEAF_COUNT = FOLDER_NUMBERS_OF_LEAF_PARENTS.length;
  private static final int FOLDER_COUNT = FOLDER_NUMBERS_OF_FOLDER_PARENTS.length;
  
  private static final String FAKE_LEAF_QUERY = "fake leaf query";
  private static final String FAKE_LEAF_0_EXPORT_LINK = "http://example.com/fakeLeaf0ExportLink";
 
  @Mock private Drive mockDrive;
  @Mock private Drive.Files mockDriveFiles;
  @Mock private Drive.Files.Get mockDriveFilesGetRoot;
  @Mock private Drive.Files.Get mockDriveFilesGetLeaf0;
  @Mock private Drive.Files.List mockDriveFilesList;
  @Mock private Drive.Files.List mockDriveFilesListWithLeafQuery;
  @Mock private Drive.Files.List mockDriveFilesListWithFolderQuery;
  @Mock private HttpTransport mockTransport;
  
  private File rootFile;
  private FileList fileListOfLeaves;
  private FileList fileListOfFolders;
  
  /**
   * @return
   *     a {@link Drive} object whose behavior mocks that of a Drive file system with particular
   *     contents
   * @throws IOException
   */
  public static Drive getMockDrive() throws IOException {
    MockDriveProvider provider = new MockDriveProvider();
    return provider.setUp();
  }
  
  private Drive setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    
    List<File> leafFiles = Lists.newArrayListWithCapacity(LEAF_COUNT);
    
    for (int i = 0; i < LEAF_COUNT; i++) {
      File leafFile = new File();
      String leafFileId = fakeFileId("leaf", i);
      leafFile.setId(leafFileId);
      leafFile.setTitle(fakeTitle("leaf", i));
      ParentReference parent = new ParentReference();
      parent.setId(fakeFileId("folder", FOLDER_NUMBERS_OF_LEAF_PARENTS[i]));
      leafFile.setParents(ImmutableList.of(parent));
      leafFiles.add(leafFile);
    }
    fileListOfLeaves = new FileList();
    fileListOfLeaves.setItems(leafFiles);
    
    File leafFile0 = leafFiles.get(0);
    Map<String, String> jsonMimeTypeToJsonExportLink =
        ImmutableMap.<String, String>builder()
            .put(DriveServiceFacade.SCRIPT_PROJECT_JSON_MIME_TYPE, FAKE_LEAF_0_EXPORT_LINK)
            .build();
    leafFile0.put(DriveServiceFacade.EXPORT_LINKS_API_PROPERTY_NAME, jsonMimeTypeToJsonExportLink);
    
    List<File> folderFiles = Lists.newArrayListWithCapacity(FOLDER_COUNT);
    for (int i = 0; i < FOLDER_COUNT; i++) {
      File folderFile = new File();
      folderFile.setId(fakeFileId("folder", i));
      folderFile.setTitle(fakeTitle("folder", i));
      ImmutableList<ParentReference> parentList;
      if (i == 0) {
        parentList = ImmutableList.of();
      } else {
        ParentReference parent = new ParentReference();
        parent.setId(fakeFileId("folder", FOLDER_NUMBERS_OF_FOLDER_PARENTS[i]));
        parentList = ImmutableList.of(parent);
      }
      folderFile.setParents(parentList);
      folderFiles.add(folderFile);
    }
    rootFile = folderFiles.get(0);
    fileListOfFolders = new FileList();
    fileListOfFolders.setItems(folderFiles);

    when(mockDrive.files()).thenReturn(mockDriveFiles);
    when(mockDriveFiles.get("root")).thenReturn(mockDriveFilesGetRoot);
    when(mockDriveFilesGetRoot.execute()).thenReturn(rootFile);
    when(mockDriveFiles.get(fakeFileId("leaf", 0))).thenReturn(mockDriveFilesGetLeaf0);
    when(mockDriveFilesGetLeaf0.execute()).thenReturn(leafFile0);
    
    when(mockDriveFiles.list()).thenReturn(mockDriveFilesList);
    when(mockDriveFilesList.setQ(FAKE_LEAF_QUERY)).thenReturn(mockDriveFilesListWithLeafQuery);
    when(mockDriveFilesListWithLeafQuery.execute()).thenReturn(fileListOfLeaves);
    when(mockDriveFilesList.setQ(DriveQueries.mimeTypeQuery(DriveQueries.FOLDER_MIME_TYPE, true)))
        .thenReturn(mockDriveFilesListWithFolderQuery);
    when(mockDriveFilesListWithFolderQuery.execute()).thenReturn(fileListOfFolders);
    
    return mockDrive;
  }
  
  /**
   * @return the number of folders in the mocked file system
   */
  public static int getFolderCount() {
    return FOLDER_COUNT;
  }
  
  /**
   * @return
   *     the number of files in the mocked file system matching the query returned by
   *     {@link #getQueryForLeaves()}
   */
  public static int getLeafCount() {
    return LEAF_COUNT;
  }
  
  /**
   * Obtains a query string that, when passed to {@code Drive.files().list().setQ}, results in a
   * {@link Drive.Files.List} object whose execution returns the leaf nodes known to this mock.
   * 
   * @return the query string
   */
  public static String getQueryForLeaves() {
    return FAKE_LEAF_QUERY;
  }

  /**
   * Obtains a representation of the file-system tree in terms of the file IDs of the children of
   * a folder with a given folder number.
   * 
   * @return
   *     a list whose i<sup>th</sup> element, 0 &le; i &lt; {@link #getFolderCount()}, is the set of
   *     file IDs of children of folder i
   */
  public static List<ImmutableSet<String>> getChildIdsByParentFolderNumber() {
    return CHILD_IDS_BY_PARENT_FOLDER_NUMBER;
  }
  
  /**
   * Obtains a representation of the file-system tree in terms of the titles of children of a
   * folder with a given title.
   * 
   * @return
   *     a multimap that maps the title of a folder to the titles of the children of that folder
   */
  public static Multimap<String, String> getTitlesToChildTitles() {
    Multimap<String, String> result = HashMultimap.create();
    for (int i = 0; i < FOLDER_COUNT; i++) {
      result.put(fakeTitle("folder", FOLDER_NUMBERS_OF_FOLDER_PARENTS[i]), fakeTitle("folder", i));
    }
    for (int i = 0; i < LEAF_COUNT; i++) {
      result.put(fakeTitle("folder", FOLDER_NUMBERS_OF_LEAF_PARENTS[i]), fakeTitle("leaf", i));
    }
    return result;
  }
  
  /**
   * Obtains the titles of all folder nodes that are not ancestors in the folder hierarchy of one of
   * the leaf notes matched by {@link #getQueryForLeaves()}. (These are the folders that are not
   * expected to appear in the {@code FolderTree} constructed from the mocked {@link Drive}.)
   * 
   * @return the set of title strings
   */
  public static Set<String> getLeaflessFolderTitles() {
    Set<String> result = Sets.newHashSet();
    for (int i = 0; i < FOLDER_COUNT; i++) {
      result.add(fakeTitle("folder", i));
    }
    for (int i = 0; i < LEAF_COUNT; i++) {
      // Remove all ancestors of leaf i from result:
      int parentFolderNumber = FOLDER_NUMBERS_OF_LEAF_PARENTS[i];
      do {
        String title = fakeTitle("folder", parentFolderNumber);
        if (!result.contains(title)) {
          // We already reached this node (and therefore its ancestors) from another leaf.
          break;
        }
        result.remove(title);
        parentFolderNumber = FOLDER_NUMBERS_OF_FOLDER_PARENTS[parentFolderNumber];
      } while (parentFolderNumber != NO_PARENT);
    }
    return result;
  }
  
  /**
   * Reports the file ID of a folder or leaf in the mocked file system.
   * 
   * @param kind either {@code "folder"} or {@code "leaf"}
   * @param index the folder number or leaf number of the file
   * @return the Drive file ID
   */
  public static String fakeFileId(String kind, int index) {
    return "fake " + kind + " " + index + " file ID";
  }
  
  /**
   * Reports the title of a folder or leaf in the mocked file system.
   * 
   * @param kind either {@code "folder"} or {@code "leaf"}
   * @param index the folder number or leaf number of the file
   * @return the file title
   */
  public static String fakeTitle(String kind, int index) {
    return "fake " + kind + " " + index + " title";
  }
}
