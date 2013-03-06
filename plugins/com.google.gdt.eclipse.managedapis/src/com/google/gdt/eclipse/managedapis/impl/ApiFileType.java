/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.managedapis.impl;

/**
 * Enumeration representing different types of files for a managed API.
 */
public enum ApiFileType {

  ANY(null), BINARY("binary"), SOURCE("source"), LICENSE("license"), ANDROID_PROPERTIES(
      "android-properties");

  private final String id;

  ApiFileType(String fileTypeId) {
    this.id = fileTypeId;
  }

  /**
   * Returns true if the <code>fileTypeId</code> matches the the id of the
   * ApiFileType. If this instance is ApiFileType.ANY, this method always
   * returns true.
   */
  public boolean matches(String fileTypeid) {
    if (id == null) {
      return true;
    }

    return id.equals(fileTypeid);
  }
}
