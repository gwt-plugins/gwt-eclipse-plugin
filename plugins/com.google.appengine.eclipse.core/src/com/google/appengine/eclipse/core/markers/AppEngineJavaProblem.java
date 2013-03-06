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

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.gdt.eclipse.core.markers.GdtJavaProblem;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtJavaProblemFactory;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Defines a custom App Engine problem. App Engine problems reuse the standard
 * Java problem marker type, so we can transparently integrate with the Java
 * Editor. For example, the JDT editor will only allow inline quick fixes on
 * Java problems, and will not work on any other types of problems.
 */
public class AppEngineJavaProblem extends GdtJavaProblem<AppEngineProblemType> {

  public static final String MARKER_ID = AppEngineCorePlugin.PLUGIN_ID
      + ".javaProblemMarker";

  private static final IGdtJavaProblemFactory<AppEngineProblemType> FACTORY = new IGdtJavaProblemFactory<AppEngineProblemType>() {
    public AppEngineJavaProblem createProblem(ASTNode node, int offset,
        int length, AppEngineProblemType problemType,
        GdtProblemSeverity severity, String[] messageArgs, String[] problemArgs) {
      return new AppEngineJavaProblem(node, offset, length, problemType,
          severity, messageArgs, problemArgs);
    }
  };

  public static AppEngineJavaProblem createRepackagedImportError(ASTNode node) {
    return create(node, AppEngineProblemType.REPACKAGED_IMPORTS_DISALLOWED,
        null, null);
  }

  /**
   * For unit tests.
   */
  public static AppEngineJavaProblem createRepackagedImportError(
      String filename, GdtProblemSeverity severity, int offset, int length,
      int line, int column) {
    return new AppEngineJavaProblem(filename, offset, length, line, column,
        AppEngineProblemType.REPACKAGED_IMPORTS_DISALLOWED, severity, null,
        null);
  }

  public static AppEngineJavaProblem createUnsupportedTypeError(ASTNode node,
      String fullyQualifiedName) {
    return create(node, AppEngineProblemType.UNSUPPORTED_JRE_TYPE,
        new String[] {fullyQualifiedName}, null);
  }
  
  public static AppEngineJavaProblem createWrongJdbcUrlError(ASTNode node, int length, String jdbcUrl) {
    AppEngineProblemType problemType = AppEngineProblemType.WRONG_JDBC_URL;
    return new AppEngineJavaProblem(node, node.getStartPosition(), length, 
        problemType, problemType.getDefaultSeverity(), new String[] {jdbcUrl}, null);
  }

  /**
   * For unit tests.
   */
  public static AppEngineJavaProblem createUnsupportedTypeError(
      String filename, String fullyQualifiedName, GdtProblemSeverity severity,
      int offset, int length, int line, int column) {
    return new AppEngineJavaProblem(filename, offset, length, line, column,
        AppEngineProblemType.UNSUPPORTED_JRE_TYPE, severity,
        new String[] {fullyQualifiedName}, null);
  }  

  private static AppEngineJavaProblem create(ASTNode node,
      AppEngineProblemType problemType, String[] messageArgs,
      String[] problemArgs) {
    return (AppEngineJavaProblem) GdtJavaProblem.createProblem(FACTORY, node,
        problemType, messageArgs, problemArgs);
  }

  private AppEngineJavaProblem(ASTNode node, int offset, int length,
      AppEngineProblemType problemType, GdtProblemSeverity severity,
      String[] messageArguments, String[] problemArguments) {
    super(node, offset, length, problemType, severity, messageArguments,
        problemArguments);
  }

  /**
   * For unit tests.
   */
  private AppEngineJavaProblem(String filename, int offset, int length,
      int line, int column, AppEngineProblemType problemType,
      GdtProblemSeverity severity, String[] messageArguments,
      String[] problemArguments) {
    super(filename, offset, length, line, column, problemType, severity,
        messageArguments, problemArguments);
  }

  @Override
  public String getMarkerType() {
    return MARKER_ID;
  }

}