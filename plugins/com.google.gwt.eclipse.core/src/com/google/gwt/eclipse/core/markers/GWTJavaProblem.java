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

import com.google.gdt.eclipse.core.markers.GdtJavaProblem;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtJavaProblemFactory;
import com.google.gwt.eclipse.core.GWTPlugin;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Defines a custom GWT problems in Java source.
 */
public class GWTJavaProblem extends GdtJavaProblem<GWTProblemType> {

  private static final IGdtJavaProblemFactory<GWTProblemType> FACTORY = new IGdtJavaProblemFactory<GWTProblemType>() {
    public GWTJavaProblem createProblem(ASTNode node, int offset, int length,
        GWTProblemType problemType, GdtProblemSeverity severity,
        String[] messageArgs, String[] problemArgs) {
      return new GWTJavaProblem(node, offset, length, problemType, severity,
          messageArgs, problemArgs);
    }
  };

  public static final String MARKER_ID = GWTPlugin.PLUGIN_ID
      + ".javaProblemMarker";

  public static GWTJavaProblem create(ASTNode node, int offset, int length,
      GWTProblemType problemType, String... messageArgs) {
    return (GWTJavaProblem) GdtJavaProblem.createProblem(FACTORY, node, offset,
        length, problemType, messageArgs, null);
  }

  /**
   * For unit tests.
   */
  public static GWTJavaProblem create(String filename, int offset, int length,
      int line, int column, GWTProblemType problemType,
      GdtProblemSeverity severity, String... messageArgs) {
    return new GWTJavaProblem(filename, offset, length, line, column,
        problemType, severity, messageArgs, null);
  }

  private GWTJavaProblem(ASTNode node, int offset, int length,
      GWTProblemType problemType, GdtProblemSeverity severity,
      String[] messageArguments, String[] problemArguments) {
    super(node, offset, length, problemType, severity, messageArguments,
        problemArguments);
  }

  /**
   * For unit tests.
   */
  private GWTJavaProblem(String filename, int offset, int length, int line,
      int column, GWTProblemType problemType, GdtProblemSeverity severity,
      String[] messageArguments, String[] problemArguments) {
    super(filename, offset, length, line, column, problemType, severity,
        messageArguments, problemArguments);
  }

  @Override
  public String getMarkerType() {
    return MARKER_ID;
  }

}