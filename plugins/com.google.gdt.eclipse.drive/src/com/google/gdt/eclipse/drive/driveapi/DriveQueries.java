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

/**
 * Defines string constants for queries to be passed to the Drive API.
 */
public class DriveQueries {

  public static final String SCRIPT_PROJECT_MIME_TYPE = "application/vnd.google-apps.script";
  public static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
  
  /**
   * Return the text of a query in the Drive search query language for all files with a specified
   * MIME type, optionally excluding files that have been trashed.
   * 
   * @param mimeType the specified MIME type
   * @param excludeTrash whether trashed files should be excluded from the query results
   * @return the query
   */
  public static String mimeTypeQuery(String mimeType, boolean excludeTrash) {
    String mimeTypeCondition = "mimeType='" + mimeType + "'";
    if (excludeTrash) {
      return mimeTypeCondition + " and trashed=false";
    } else {
      return mimeTypeCondition;
    }
  }

}
