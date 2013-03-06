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

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;

/**
 * Defines a problem on a ClientBundle interface.
 */
public class ClientBundleProblem extends
    GdtJavaProblem<ClientBundleProblemType> {

  public static final String MARKER_ID = GWTPlugin.PLUGIN_ID
      + ".clientBundleProblemMarker";

  private static final IGdtJavaProblemFactory<ClientBundleProblemType> FACTORY = new IGdtJavaProblemFactory<ClientBundleProblemType>() {
    public ClientBundleProblem createProblem(ASTNode node, int offset,
        int length, ClientBundleProblemType problemType,
        GdtProblemSeverity severity, String[] messageArgs, String[] problemArgs) {
      return new ClientBundleProblem(node, offset, length, problemType,
          severity, messageArgs, problemArgs);
    }
  };

  public static ClientBundleProblem createInvalidReturnType(Type returnType) {
    return create(returnType, ClientBundleProblemType.INVALID_RETURN_TYPE,
        NO_STRINGS, NO_STRINGS);
  }

  public static ClientBundleProblem createMissingResourceFile(ASTNode node,
      String expectedFileName, IPath expectedResourcePath) {
    return create(node, ClientBundleProblemType.MISSING_RESOURCE_FILE,
        new String[] {expectedFileName, expectedResourcePath.toString()},
        NO_STRINGS);
  }

  public static ClientBundleProblem createNonEmptyParameterList(
      MethodDeclaration methodDecl) {
    return create(methodDecl.getName(),
        ClientBundleProblemType.NON_EMPTY_PARAMETER_LIST, NO_STRINGS,
        NO_STRINGS);
  }

  public static ClientBundleProblem createSourceAnnotationRequired(
      MethodDeclaration methodDecl, String resourceTypeName) {
    return create(methodDecl.getName(),
        ClientBundleProblemType.SOURCE_ANNOTATION_REQUIRED,
        new String[] {resourceTypeName}, NO_STRINGS);
  }

  private static ClientBundleProblem create(ASTNode node,
      ClientBundleProblemType problemType, String[] messageArgs,
      String[] problemArgs) {
    return (ClientBundleProblem) GdtJavaProblem.createProblem(FACTORY, node,
        problemType, messageArgs, problemArgs);
  }

  private ClientBundleProblem(ASTNode node, int offset, int length,
      ClientBundleProblemType type, GdtProblemSeverity severity,
      String[] messageArguments, String[] problemArguments) {
    super(node, offset, length, type, severity, messageArguments,
        problemArguments);
  }

  @Override
  public String getMarkerType() {
    return MARKER_ID;
  }

}
