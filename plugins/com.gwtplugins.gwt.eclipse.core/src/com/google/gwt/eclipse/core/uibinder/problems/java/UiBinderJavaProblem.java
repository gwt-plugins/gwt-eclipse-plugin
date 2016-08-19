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

import com.google.gdt.eclipse.core.markers.GdtJavaProblem;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtJavaProblemFactory;
import com.google.gwt.eclipse.core.GWTPlugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Defines a problem on a UiBinder subtype or owner class.
 */
public class UiBinderJavaProblem extends
    GdtJavaProblem<UiBinderJavaProblemType> {

  public static final String MARKER_ID = GWTPlugin.PLUGIN_ID
      + ".uiBinderJavaProblemMarker";

  private static final IGdtJavaProblemFactory<UiBinderJavaProblemType> FACTORY = new IGdtJavaProblemFactory<UiBinderJavaProblemType>() {
    public UiBinderJavaProblem createProblem(ASTNode node, int offset,
        int length, UiBinderJavaProblemType problemType,
        GdtProblemSeverity severity, String[] messageArgs, String[] problemArgs) {
      return new UiBinderJavaProblem(node, offset, length, problemType,
          severity, messageArgs, problemArgs);
    }
  };

  public static UiBinderJavaProblem createMissingUiFieldInXml(ASTNode node,
      String fieldName, IPath uiXmlPath) {
    return create(node, UiBinderJavaProblemType.MISSING_UI_FIELD_IN_XML,
        new String[] {fieldName, uiXmlPath.lastSegment()}, NO_STRINGS);
  }

  public static UiBinderJavaProblem createMissingUiXmlFile(ASTNode node,
      String expectedFileName, IPath expectedPath) {
    return create(node, UiBinderJavaProblemType.MISSING_UI_XML_FILE,
        new String[] {expectedFileName, expectedPath.toString()}, NO_STRINGS);
  }

  public static UiBinderJavaProblem createPrivateUiBinderSubtype(
      TypeDeclaration uiBinderSubtypeDecl, Modifier privateModifier) {
    return create(privateModifier,
        UiBinderJavaProblemType.PRIVATE_UI_BINDER_SUBTYPE,
        new String[] {uiBinderSubtypeDecl.getName().getIdentifier()},
        NO_STRINGS);
  }

  @SuppressWarnings("unchecked")
  public static Set<UiBinderJavaProblem> createPrivateUiField(
      FieldDeclaration uiFieldDecl, Modifier privateModifier) {
    Set<UiBinderJavaProblem> problems = new HashSet<UiBinderJavaProblem>();

    List<VariableDeclarationFragment> varDecls = uiFieldDecl.fragments();
    for (VariableDeclarationFragment varDecl : varDecls) {
      String fieldName = varDecl.getName().getIdentifier();

      UiBinderJavaProblem problem = create(privateModifier,
          UiBinderJavaProblemType.PRIVATE_UI_FIELD, new String[] {fieldName},
          NO_STRINGS);
      if (problem != null) {
        problems.add(problem);
      }
    }

    return problems;
  }

  public static UiBinderJavaProblem createPrivateUiHandler(
      MethodDeclaration uiHandlerDecl, Modifier privateModifier) {
    return create(privateModifier, UiBinderJavaProblemType.PRIVATE_UI_HANDLER,
        new String[] {uiHandlerDecl.getName().getIdentifier()}, NO_STRINGS);
  }

  private static UiBinderJavaProblem create(ASTNode node,
      UiBinderJavaProblemType problemType, String[] messageArgs,
      String[] problemArgs) {
    return (UiBinderJavaProblem) GdtJavaProblem.createProblem(FACTORY, node,
        problemType, messageArgs, problemArgs);
  }

  private UiBinderJavaProblem(ASTNode node, int offset, int length,
      UiBinderJavaProblemType type, GdtProblemSeverity severity,
      String[] messageArguments, String[] problemArguments) {
    super(node, offset, length, type, severity, messageArguments,
        problemArguments);
  }

  @Override
  public String getMarkerType() {
    return MARKER_ID;
  }

}
