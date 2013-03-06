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
package com.google.gwt.eclipse.core.uibinder.problems.java;

import com.google.gdt.eclipse.core.markers.GdtProblemCategory;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtProblemType;

/**
 * Defines UiBinder problems that can occur in UiBinder subtypes/owner classes.
 */
public enum UiBinderJavaProblemType implements IGdtProblemType {

  MISSING_UI_FIELD_IN_XML(UIBINDER_JAVA_OFFSET + 1,
      "Field missing in template",
      "Field {0} has no corresponding field in template file {1}",
      GdtProblemSeverity.ERROR),

  MISSING_UI_XML_FILE(UIBINDER_JAVA_OFFSET + 2, "Missing template file",
      "Template file {0} is missing (expected at {1})",
      GdtProblemSeverity.ERROR),

  PRIVATE_UI_BINDER_SUBTYPE(UIBINDER_JAVA_OFFSET + 3,
      "Private UiBinder subtype",
      "UiBinder subtype {0} cannot be declared private",
      GdtProblemSeverity.ERROR),

  PRIVATE_UI_FIELD(UIBINDER_JAVA_OFFSET + 4, "Private @UiField field",
      "Field {0} marked as @UiField cannot be declared private",
      GdtProblemSeverity.ERROR),

  PRIVATE_UI_HANDLER(UIBINDER_JAVA_OFFSET + 5, "Private @UiHandler method",
      "Method {0} marked as @UiHandler cannot be declared private",
      GdtProblemSeverity.ERROR);

  private final GdtProblemSeverity defaultSeverity;

  private final String description;

  private final String message;

  private final int problemId;

  private UiBinderJavaProblemType(int problemId, String description,
      String message, GdtProblemSeverity defaultSeverity) {
    this.problemId = problemId;
    this.description = description;
    this.message = message;
    this.defaultSeverity = defaultSeverity;
  }

  public GdtProblemCategory getCategory() {
    return GdtProblemCategory.UI_BINDER;
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
