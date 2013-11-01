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
package com.google.gdt.eclipse.drive.model;
// TODO(nhcohen): Move to com.google.gdt.eclipse.drive.model, with the content and label providers

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.drive.driveapi.DriveCache;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A node in a high-level tree representation of a Drive folder. Children represent either child
 * subfolders or leaf files. There is a method mapping from parent nodes to child nodes and a
 * method mapping from child nodes to parent nodes.
 */
public class FolderTree {
  
  @VisibleForTesting
  public static final String DRIVE_ROOT_TITLE = "My Drive";

  /**
   * A leaf {@link FolderTree} node, representing a file rather than a folder.
   */
  @Immutable
  public static class FolderTreeLeaf extends FolderTree {

    private final String fileId;

    public FolderTreeLeaf(String title, String fileId) {
      super(title);
      this.fileId = fileId;
    }

    public String getFileId() {
      return fileId;
    }
  }
  
  /**
   * Creates a new {@code FolderTree} from the information stored in a given {@link DriveCache}.
   * A parameter controls whether the result should include non-root folders that do not directly or
   * indirectly contain any of the leaf files of the {@code DriveCache}; however, a root folder is
   * always returned regardless of the value of this parameter, even if there are no leaf files
   * directly or indirectly beneath the root.
   * 
   * @param cache the given {@code DriveCache}
   * @param includeEmptySubtrees
   *     whether the result should include non-root folders that do not directly or indirectly
   *     contain any of the leaf files of the {@code DriveCache}
   * @return the new {@code FolderTree}
   */
  public static FolderTree make(DriveCache cache, boolean includeEmptySubtrees) {
    return makeRecursively(cache, cache.getRootId(), DRIVE_ROOT_TITLE, includeEmptySubtrees);
  }

  private static FolderTree makeRecursively(
      DriveCache cache, String rootId, String rootTitle, boolean includeEmptySubtrees) {
    Collection<String> childIds = cache.getChildIds(rootId);
    FolderTree subtreeRoot = new FolderTree(rootTitle);
    for (String childId : childIds) {
      if (cache.isLeafId(childId)) {
        subtreeRoot.addChild(new FolderTreeLeaf(cache.getTitle(childId), childId));
      } else {
        FolderTree subtree =
            makeRecursively(cache, childId, cache.getTitle(childId), includeEmptySubtrees);
        if (includeEmptySubtrees || !subtree.getChildren().isEmpty()) {
          subtreeRoot.addChild(subtree);
        }
      }
    }
    return subtreeRoot;
  }

  private final String title;
  private final List<FolderTree> children;
  private FolderTree parent;

  private FolderTree(String title) {
    this.title = title;
    children = Lists.newLinkedList();
  }

  /**
   * Adds a specified {@code FolderTree} as a child to this {@code FolderTree}.
   * 
   * @param child the child {@code FolderTree}
   */
  private void addChild(FolderTree child) {
    children.add(child);
    child.parent = this;
  }

  /**
   * @return a {@code FolderTree} containing this {@code FolderTree} as its only child
   */
  public FolderTree addDummyParent() {
    FolderTree result = new FolderTree("");
    result.addChild(this);
    return result;
  }
  
  /**
   * @return an immutable snapshot of the children of this {@code FolderTree}
   */
  public List<FolderTree> getChildren() {
    return ImmutableList.copyOf(children);
  }

  @Nullable
  public FolderTree getParent() {
    return parent;
  }
  
  public String getTitle() {
    return title;
  }
  
  @Override
  public String toString() {
    StringBuilder resultBuilder = new StringBuilder();
    appendOutline(resultBuilder, 0);
    return resultBuilder.toString();
  }
  
  private void appendOutline(StringBuilder builder, int indentLevel) {
    for (int i = 0; i < indentLevel; i++) {
      builder.append('\t');
    }
    builder.append(title);
    builder.append('\n');
    int childIndentLevel = indentLevel + 1;
    for (FolderTree subtree : children) {
      subtree.appendOutline(builder, childIndentLevel);
    }
  }
}
