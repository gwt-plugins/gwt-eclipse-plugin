/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.core.markers;

import com.google.gdt.eclipse.core.markers.GdtProblemCategory;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtProblemType;

/**
 * Defines the types of GWT-specific problems.
 * NOTE: Don't format this file, because it makes the problem listing much
 * harder to read.
 */
public enum GWTProblemType implements IGdtProblemType {
  
  // TODO: pull all strings from properties file
  JSNI_JAVA_REF_UNRESOLVED_TYPE(GWT_OFFSET + 1,
      "Unresolved type reference",
      "{0} could not be resolved to a type",
      GdtProblemSeverity.ERROR),
  JSNI_JAVA_REF_MISSING_METHOD(GWT_OFFSET + 2,
      "Reference to non-existent method",
      "{0} does not contain a method named {1}",
      GdtProblemSeverity.ERROR),
  JSNI_JAVA_REF_MISSING_FIELD(GWT_OFFSET + 3,
      "Reference to non-existent field",
      "{0} does not contain a field named {1}",
      GdtProblemSeverity.ERROR),
  JSNI_JAVA_REF_NO_MATCHING_METHOD(GWT_OFFSET + 4,
      "Incorrect method signature",
      "The method {0} in the type {1} is undefined",
      GdtProblemSeverity.ERROR),
  JSNI_JAVA_REF_NO_MATCHING_CTOR(GWT_OFFSET + 5,
      "Incorrect constructor signature",
      "The constructor {0} is undefined",
      GdtProblemSeverity.ERROR),
  JSNI_PARSE_ERROR(GWT_OFFSET + 6,
      "JavaScript parsing error",
      "JavaScript parsing: {0}",
      GdtProblemSeverity.WARNING),
   REQ_FACTORY_SERVICE_METHOD_ERROR(GWT_OFFSET + 7,
       "Request Factory Service Method validation error", 
       "The method {0} is missing a client side implementation",
       GdtProblemSeverity.WARNING);

  public static GWTProblemType getProblemType(int problemId) {
    for (GWTProblemType type : GWTProblemType.values()) {
      if (type.getProblemId() == problemId) {
        return type;
      }
    }
    return null;
  }

  private final GdtProblemSeverity defaultSeverity;

  private final String description;

  private final String message;

  private final int problemId;

  private GWTProblemType(int problemId, String description, String message,
      GdtProblemSeverity defaultSeverity) {
    this.problemId = problemId;
    this.description = description;
    this.message = message;
    this.defaultSeverity = defaultSeverity;
  }

  public GdtProblemCategory getCategory() {
    return GdtProblemCategory.JSNI;
  }

  public GdtProblemSeverity getDefaultSeverity() {
    return defaultSeverity;
  }

  public String getDescription() {
    return description;
  }

  public String getMessage() {
    return message;
  }

  public int getProblemId() {
    return problemId;
  }

  @Override
  public String toString() {
    return description;
  }

}
