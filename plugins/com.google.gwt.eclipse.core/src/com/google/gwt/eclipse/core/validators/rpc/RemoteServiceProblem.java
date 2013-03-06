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
package com.google.gwt.eclipse.core.validators.rpc;

import com.google.gdt.eclipse.core.markers.GdtJavaProblem;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtJavaProblemFactory;
import com.google.gwt.eclipse.core.GWTPlugin;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * {@link org.eclipse.jdt.core.compiler.CategorizedProblem CategorizedProblem}
 * associated with RPC problems.
 * 
 * NOTE: There is test code RemoteServiceValidatorTest that does equals on these
 * object types. Changing this class need to update the corresponding test.
 */
public class RemoteServiceProblem extends
    GdtJavaProblem<RemoteServiceProblemType> {

  public static final String MARKER_ID = GWTPlugin.PLUGIN_ID
      + ".rpcProblemMarker";

  private static final IGdtJavaProblemFactory<RemoteServiceProblemType> FACTORY = new IGdtJavaProblemFactory<RemoteServiceProblemType>() {
    public RemoteServiceProblem createProblem(ASTNode node, int offset,
        int length, RemoteServiceProblemType problemType,
        GdtProblemSeverity severity, String[] messageArgs, String[] problemArgs) {
      return new RemoteServiceProblem(node, offset, length, problemType,
          severity, messageArgs, problemArgs);
    }
  };

  static RemoteServiceProblem create(ASTNode node,
      RemoteServiceProblemType problemType, String[] messageArgs,
      String[] problemArgs) {
    return (RemoteServiceProblem) GdtJavaProblem.createProblem(FACTORY, node,
        problemType, messageArgs, problemArgs);
  }

  private RemoteServiceProblem(ASTNode node, int offset, int length,
      RemoteServiceProblemType type, GdtProblemSeverity severity,
      String[] messageArguments, String[] problemArguments) {
    super(node, offset, length, type, severity, messageArguments,
        problemArguments);
  }

  @Override
  public String getMarkerType() {
    return MARKER_ID;
  }

}