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
package com.google.gwt.eclipse.core.validators.java;

import com.google.gdt.eclipse.core.JavaASTUtils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;

/**
 * Traverses the Java AST to find GWT problems and parse JSNI methods.
 */
class JavaValidationVisitor extends ASTVisitor {

  private final JavaValidationResult result = new JavaValidationResult();

  @Override
  public void endVisit(MethodDeclaration method) {
    if (Modifier.isNative(method.getModifiers())) {
      JavaValidationResult methodResult = JsniParser.parse(method);

      // Collect all Java references we found in this method
      result.addAllJavaRefs(methodResult.getJavaRefs());

      // Report problems, unless we're suppressing them
      if (!JavaASTUtils.hasSuppressWarnings(method, "jsni")) {
        result.addAllProblems(methodResult.getProblems());
      }
    }
  }

  public JavaValidationResult getValidationResult() {
    return result;
  }

}
