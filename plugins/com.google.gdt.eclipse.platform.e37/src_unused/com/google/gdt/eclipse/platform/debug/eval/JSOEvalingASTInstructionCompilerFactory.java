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

import org.eclipse.jdt.internal.debug.eval.ast.engine.ASTInstructionCompiler;
import org.eclipse.jdt.internal.debug.eval.ast.engine.IASTInstructionCompilerFactory;

/**
 * Factory to create a new instance of an ASTInstructionCompiler that can
 * interpret JSO expressions.
 * 
 * Will be compiled against Eclipse 3.7 with a custom JDT patch, but will not be
 * executed unless the user has the custom JDT patch installed.
 */
// Using org.eclipse.jdt.internal.debug.*
@SuppressWarnings("restriction")
public class JSOEvalingASTInstructionCompilerFactory implements
    IASTInstructionCompilerFactory {

  public ASTInstructionCompiler create(int snippetStart, String snippet) {
    /*
     * TODO: Don't provide an ASTInstructionCompiler in the case where the
     * project is using a version of GWT earlier than 2.4.0.
     * 
     * To make this work, we need to pass in a reference to the project.
     */
    return new JSOEvalingASTInstructionCompiler(snippetStart, snippet);
  }
}
