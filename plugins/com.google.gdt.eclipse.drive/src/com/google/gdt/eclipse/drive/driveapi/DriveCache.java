/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.drive.driveapi;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A cached copy of information from Drive, allowing navigation of a filtered folder tree without
 * repeated calls on the Drive API for each node of the tree. The tree is navigated by means of
 * Drive file-ID strings. There is a method to obtain the file ID of the root of the tree, a
 * method to obtain the file IDs of the children of a folder node with a given file ID, a method to
 * test whether the node with a given file ID is a folder node or a leaf node, and a method to
 * obtain the the title corresponding to a node with a given file ID.
 * 
 * <p>A {@code DriveCache} is a low-level representation of a folder tree, tied to Drive file IDs.
 * It can be used to create a {@code FolderTree}, which is a high-level representation of a folder
 * tree representing parent-child relationships with object references instead of file IDs. 
 */
public class DriveCache {
  
  /**
   * Creates a new {@code DriveCache} holding a folder tree consisting of all leaf files satisfying
   * a specified query and all folders directly or indirectly containing those leaf files.
   * 
   * @param drive a {@link Drive} object used to access the Drive service
   * @param leafQuery
   *     the specified query, of the form passed to the {@link Drive.Files.List#setQ(String)} method
   * @return the new {@code DriveCache}
   * @throws IOException if a request to the Drive service fails
   */
  public static DriveCache make(Drive drive, String leafQuery) throws IOException {
    File rootFile = drive.files().get("root").execute();
    DriveCache result = new DriveCache(drive, leafQuery, rootFile.getId());
    result.read();
    return result;
  }

  private final Drive drive;
  private final String leafQuery;
  private final Multimap<String, String> parentIdsToChildIdSets;  
  private final Map<String, File> idsToFiles;  
  private final Set<String> leafIds;
  private final String rootId;
  
  private DriveCache(Drive drive, String leafQuery, String rootId) {
    this.drive = drive;
    this.leafQuery = leafQuery;
    idsToFiles = Maps.newHashMap();
    parentIdsToChildIdSets = HashMultimap.create();
    leafIds = Sets.newHashSet();
    this.rootId = rootId;
  }

  /**
   * Obtains the file ID of the root node of the folder tree of this {@code DriveCache}.
   * 
   * @return the file ID
   */
  public String getRootId() {
    return rootId;
  }

  /**
   * Obtains the file IDs of children of a node with a given file ID.
   * 
   * @param parentId the given file ID
   * @return
   *     if {@code parentId} identifies a folder node, a collection of the file IDs of the children
   *     of that node; otherwise, an empty collection
   */
  public Collection<String> getChildIds(String parentId) {
    return parentIdsToChildIdSets.get(parentId);
  }
  
  /**
   * Reports whether a given string is the file ID of a leaf in the folder tree of this
   * {@code DriveCache}.
   * 
   * @param fileId the given string
   * @return
   *     {@code true} if {@code fileId} is the file ID of a leaf;
   *     {@code false} if it is the file ID of a folder node or if it is not a file ID
   */
  public boolean isLeafId(String fileId) {
    return leafIds.contains(fileId);
  }

  /**
   * Obtains the title associated with a specified node in the folder tree of this
   * {@code DriveCache}.
   * 
   * @param fileId the file ID of the specified node
   * @return
   *     the title associated with the specified node, or {@code null} if {@code fileId} is not the
   *     file ID of a node in the folder tree of this {@code DriveCache}.
   */
  @Nullable
  public String getTitle(String fileId) {
    File metadata = idsToFiles.get(fileId);
    return metadata == null ? null : metadata.getTitle();
  }

  private void read() throws IOException {
    for (File leafFileMetadata : getMatchingFiles(drive, leafQuery)) {
      String id = leafFileMetadata.getId();
      idsToFiles.put(id, leafFileMetadata);
      leafIds.add(id);
    }
    String queryForFolders = DriveQueries.mimeTypeQuery(DriveQueries.FOLDER_MIME_TYPE, true);
    for (File folderMetadata : getMatchingFiles(drive, queryForFolders)) {
      idsToFiles.put(folderMetadata.getId(), folderMetadata);
    }
    for (String childId : idsToFiles.keySet()) {
      List<ParentReference> parents = idsToFiles.get(childId).getParents();
      for (ParentReference parentRef : parents) {
        parentIdsToChildIdSets.put(parentRef.getId(), childId);
      }
    }
  }
  
  private static List<File> getMatchingFiles(Drive drive, String query) throws IOException {
    List<File> matchingFiles = Lists.newLinkedList();
    Drive.Files.List listRequest = drive.files().list().setQ(query);
    while (true) {
      FileList fileList = listRequest.execute();
      matchingFiles.addAll(fileList.getItems());
      String nextPageToken = fileList.getNextPageToken();
      if (Strings.isNullOrEmpty(nextPageToken)) {
        return matchingFiles;
      }
      listRequest.setPageToken(nextPageToken);
    }
  }
    
}