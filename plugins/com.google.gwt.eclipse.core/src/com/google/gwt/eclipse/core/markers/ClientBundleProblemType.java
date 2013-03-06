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
 * Defines ClientBundle-specific problems.
 */
public enum ClientBundleProblemType implements IGdtProblemType {

  INVALID_RETURN_TYPE(
      CLIENTBUNDLE_OFFSET + 1,
      "Invalid return type",
      "Return type must be another ClientBundle or a subtype of ResourcePrototype",
      GdtProblemSeverity.ERROR),

  MISSING_RESOURCE_FILE(CLIENTBUNDLE_OFFSET + 2, "Missing resource file",
      "Resource file {0} is missing (expected at {1})",
      GdtProblemSeverity.ERROR),

  NON_EMPTY_PARAMETER_LIST(CLIENTBUNDLE_OFFSET + 3, "Non-empty parameter list",
      "ClientBundle methods should not have parameters",
      GdtProblemSeverity.ERROR),

  SOURCE_ANNOTATION_REQUIRED(
      CLIENTBUNDLE_OFFSET + 4,
      "Missing required @Source annotation",
      "Method requires an @Source annotation ({0} does not define any default extensions)",
      GdtProblemSeverity.ERROR);

  private final GdtProblemSeverity defaultSeverity;

  private final String description;

  private final String message;

  private final int problemId;

  private ClientBundleProblemType(int problemId, String description,
      String message, GdtProblemSeverity defaultSeverity) {
    this.problemId = problemId;
    this.description = description;
    this.message = message;
    this.defaultSeverity = defaultSeverity;
  }

  public GdtProblemCategory getCategory() {
    return GdtProblemCategory.CLIENT_BUNDLE;
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
