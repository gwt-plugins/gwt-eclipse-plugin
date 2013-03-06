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
package com.google.gdt.eclipse.platform.debug.eval;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.debug.eval.ast.engine.ASTInstructionCompiler;
import org.eclipse.jdt.internal.debug.eval.ast.engine.EvaluationEngineMessages;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushThis;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.SendStaticMessage;

import java.util.List;

/**
 * An ASTInstructionCompiler that is able to generate instructions for JSO
 * expressions.
 * 
 * Will be compiled against Eclipse 3.7 with a custom JDT patch, but will not be
 * executed unless the user has the custom JDT patch installed.
 */
// Using org.eclipse.jdt.internal.debug.*
@SuppressWarnings("restriction")
public class JSOEvalingASTInstructionCompiler extends ASTInstructionCompiler {

  public JSOEvalingASTInstructionCompiler(int startPosition, String snippet) {
    super(startPosition, snippet);
  }

  @Override
  public boolean visit(MethodInvocation node) {

    if (!isActive()) {
      return false;
    }

    IMethodBinding methodBinding = (IMethodBinding) node.getName().resolveBinding();
    if (methodBinding == null) {
      // could be the receiver is not visible - for example a private field
      // access from super class
      ASTNode root = node.getRoot();
      if (root instanceof CompilationUnit) {
        CompilationUnit cu = (CompilationUnit) root;
        IProblem[] problems = cu.getProblems();
        for (int i = 0; i < problems.length; i++) {
          IProblem problem = problems[i];
          setHasError(true);
          addErrorMessage(problem.getMessage());
        }
      }
    }

    if (hasErrors()) {
      return true;
    }

    if (containsALocalType(methodBinding)) {
      setHasError(true);
      addErrorMessage(EvaluationEngineMessages.ASTInstructionCompiler_Method_which_contains_a_local_type_as_parameter_cannot_be_used_in_an_evaluation_expression_32);
    }

    if (hasErrors()) {
      return true;
    }

    int paramCount = methodBinding.getParameterTypes().length;
    String selector = methodBinding.getName();

    String signature = getMethodSignature(methodBinding, null).replace('.', '/');

    boolean isStatic = Flags.isStatic(methodBinding.getModifiers());
    Expression expression = node.getExpression();

    String typeName = getTypeName(methodBinding.getDeclaringClass());

    if (isStatic) {
      boolean isJso = false;
      ITypeBinding parent = methodBinding.getDeclaringClass();
      while (parent != null) {
        if (parent.getQualifiedName().equals(
            "com.google.gwt.core.client.JavaScriptObject")) {
          isJso = true;
          break;
        }
        parent = parent.getSuperclass();
      }

      if (isJso) {
        push(new JsoSendStaticMessage(typeName, selector, signature,
            paramCount, getCounter()));
      } else {
        push(new SendStaticMessage(typeName, selector, signature, paramCount,
            getCounter()));
      }
      if (expression != null) {
        node.getExpression().accept(this);
        addPopInstruction();
      }
    } else {
      /*
       * Since we're dealing with a non-static method, we can't tell whether or
       * not this method is declared on a class that extends JavaScriptObject;
       * the method could be declared on an interface that MAY be implemented by
       * a subclass of JavaScriptObject.
       */
      push(new JsoSendMessage(selector, signature, paramCount, typeName,
          getCounter()));
      if (expression == null) {
        push(new PushThis(getEnclosingLevel(node,
            methodBinding.getDeclaringClass())));
        storeInstruction();
      } else {
        node.getExpression().accept(this);
      }
    }

    List<?> arguments = node.arguments();
    pushMethodArguments(methodBinding, arguments);

    return false;
  }
}
