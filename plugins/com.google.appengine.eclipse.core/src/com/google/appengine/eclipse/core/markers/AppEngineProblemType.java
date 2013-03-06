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
package com.google.appengine.eclipse.core.markers;

import com.google.gdt.eclipse.core.markers.GdtProblemCategory;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtProblemType;

/**
 * Defines the types of AppEngine-specific problems.
 */
public enum AppEngineProblemType implements IGdtProblemType {
  
  // The substring {0} must be the jdbc url for the project. 
  WRONG_JDBC_URL(APP_ENGINE_OFFSET + 6, "Wrong jdbc url",
      "JDBC URL does not match configured JDBC URL for Google SQL instance: {0}", 
      GdtProblemSeverity.WARNING),
      
  REPACKAGED_IMPORTS_DISALLOWED(
      APP_ENGINE_OFFSET + 5,
      "Repackaged imports are not allowed",
      "Use of com.google.appengine.repackaged may result in your app breaking without warning.",
      GdtProblemSeverity.ERROR),

  JAVA15_DEPRECATED(APP_ENGINE_OFFSET + 4, "Upgrade to Java 1.6",
      "Future versions of the Dev App Server will require Java 1.6 or later",
      GdtProblemSeverity.WARNING),

  JSP_WITHOUT_JDK(APP_ENGINE_OFFSET + 3, "JSP files in project without JDK",
      "Your project must be configured to use a JDK in order to use JSPs",
      GdtProblemSeverity.ERROR),

  MISSING_APPENGINE_WEB_XML(APP_ENGINE_OFFSET + 2, "Missing appengine-web.xml",
      "The appengine-web.xml file is missing", GdtProblemSeverity.ERROR),

  UNSUPPORTED_JRE_TYPE(APP_ENGINE_OFFSET + 1,
      "Use of non-whitelisted JRE type",
      "{0} is not supported by Google App Engine''s Java runtime environment",
      GdtProblemSeverity.ERROR);

  public static AppEngineProblemType getProblemType(int problemId) {
    for (AppEngineProblemType type : AppEngineProblemType.values()) {
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

  private AppEngineProblemType(int problemId, String description,
      String message, GdtProblemSeverity defaultSeverity) {
    this.problemId = problemId;
    this.description = description;
    this.message = message;
    this.defaultSeverity = defaultSeverity;
  }

  public GdtProblemCategory getCategory() {
    return GdtProblemCategory.APP_ENGINE;
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