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

import com.google.common.base.Objects;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Information about a particular script stored in a Drive Apps Script project.
 */
@Immutable
public final class DriveScriptInfo {
  private final String importName;
  private final String documentId;
  private final String type;
  private final String contents;
  
  /**
   * Construct a new {@code DriveScriptInfo} with a specified import name, document ID, type, and
   * file contents.
   * 
   * @param importName
   *     the specified import name (the Drive file name with the extension removed), or null for a
   *     file whose contents should remain unchanged in Drive
   * @param documentId
   *     the specified document ID, or null in the case of a file that has been created on Eclipse
   *     and not yet written to Drive
   * @param type the Drive file type
   * @param contents
   *     the file contents, or null for a file whose contents should remain unchanged in Drive
   */
  public DriveScriptInfo(
      @Nullable String importName,
      @Nullable String documentId,
      String type,
      @Nullable String contents) {
    this.importName = importName;
    this.documentId = documentId;
    this.type = type;
    this.contents = contents;
  }
  
  public String getImportName() {
    return importName;
  }

  public String getDocumentId() {
    return documentId;
  }
  
  public String getType() {
    return type;
  }

  public String getContents() {
    return contents;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DriveScriptInfo) {
      DriveScriptInfo that = (DriveScriptInfo) obj;
      return
          Objects.equal(this.importName, that.importName)
            && Objects.equal(this.documentId, that.documentId)
            && Objects.equal(this.type, that.type)
            && Objects.equal(this.contents, that.contents);
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(importName, documentId, type, contents);
  }
  
  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("importName", importName)
        .add("documentId", documentId)
        .add("type", type)
        .add("contents", contents)
        .toString();
  }
}