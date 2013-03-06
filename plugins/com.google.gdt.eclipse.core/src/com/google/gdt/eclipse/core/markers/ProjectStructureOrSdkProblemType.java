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
package com.google.gdt.eclipse.core.markers;

/**
 * Defines the types of project validation problems.
 */
public enum ProjectStructureOrSdkProblemType implements IGdtProblemType {

  BUILD_OUTPUT_DIR_NOT_WEBINF_CLASSES(PROJECT_STRUCTURE_OR_SDK_OFFSET + 4,
      "Incorrect build output directory",
      "The output directory for the project should be set to {0}",
      GdtProblemSeverity.ERROR),

  INVALID_SDK(PROJECT_STRUCTURE_OR_SDK_OFFSET + 6, "Invalid SDK",
      "The {0} SDK ''{1}'' on the project''s build path is not valid ({2})",
      GdtProblemSeverity.ERROR),

  JAR_OUTSIDE_WEBINF_LIB(
      PROJECT_STRUCTURE_OR_SDK_OFFSET + 3,
      "Build JARs not on server classpath",
      "The following classpath entry ''{0}'' will not be available on the server''s classpath",
      GdtProblemSeverity.WARNING),

  MISSING_WAR_DIR(PROJECT_STRUCTURE_OR_SDK_OFFSET + 1, "Missing WAR directory",
      "WAR source directory {0} is missing", GdtProblemSeverity.ERROR),

  MISSING_WEB_XML(PROJECT_STRUCTURE_OR_SDK_OFFSET + 2, "Missing web.xml",
      "The web.xml file does not exist", GdtProblemSeverity.ERROR),

  NO_SDK(PROJECT_STRUCTURE_OR_SDK_OFFSET + 5, "Missing SDK",
      "The project ''{0}'' does not have any {1} SDKs on its build path",
      GdtProblemSeverity.ERROR),

  SDK_FILE_SIZE_MISMATCH(
      PROJECT_STRUCTURE_OR_SDK_OFFSET + 7,
      "SDK file size mismatch",
      "The file {0} has a different size than {1} SDK library {2}; perhaps it is a different version?",
      GdtProblemSeverity.ERROR),

  SDK_LIBRARY_MISSING_FROM_WEBINF_LIB(PROJECT_STRUCTURE_OR_SDK_OFFSET + 8,
      "SDK JAR missing from WEB-INF/lib",
      "The {0} SDK JAR {1} is missing in the WEB-INF/lib directory",
      GdtProblemSeverity.ERROR),

  WAR_WITH_PRE_GWT_16(
      PROJECT_STRUCTURE_OR_SDK_OFFSET + 9,
      "WAR directory with GWT version before 1.6",
      "Projects that use the web archive layout (WAR) must use GWT 1.6 or later",
      GdtProblemSeverity.ERROR);

  public static ProjectStructureOrSdkProblemType getProblemType(int problemId) {
    for (ProjectStructureOrSdkProblemType type : ProjectStructureOrSdkProblemType.values()) {
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

  private ProjectStructureOrSdkProblemType(int problemId, String description,
      String message, GdtProblemSeverity defaultSeverity) {
    this.problemId = problemId;
    this.description = description;
    this.message = message;
    this.defaultSeverity = defaultSeverity;
  }

  public GdtProblemCategory getCategory() {
    return GdtProblemCategory.SDK;
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