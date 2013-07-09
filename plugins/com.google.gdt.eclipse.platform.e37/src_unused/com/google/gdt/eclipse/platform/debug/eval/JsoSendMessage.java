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

import com.google.gdt.eclipse.platform.Activator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.InstructionsEvaluationMessages;

/**
 * An implementation of SendMessage that checks to see if the runtime type of
 * the object is JavaScriptObject$. In that case, JSOEval is used to make the
 * method call, instead of directly invoking the method on the class itself.
 * 
 * Will be compiled against Eclipse 3.7 with a custom JDT patch, but will not be
 * executed unless the user has the custom JDT patch installed.
 */
// Using org.eclipse.jdt.internal.debug.*
@SuppressWarnings("restriction")
public class JsoSendMessage extends AbstractJsoDelegationMessage {

  public JsoSendMessage(String selector, String signature, int argCount,
      String declaringType, int start) {
    super(selector, signature, argCount, declaringType, start);
  }

  @Override
  public void execute() throws CoreException {
    IJavaValue[] args = new IJavaValue[argCount];
    // args are in reverse order
    for (int i = argCount - 1; i >= 0; i--) {
      args[i] = boxArg(popValue());
    }

    Object receiver = pop();
    IJavaValue result = null;

    if (receiver instanceof IJavaVariable) {
      receiver = ((IJavaVariable) receiver).getValue();
    }

    if (receiver instanceof IJavaObject) {
      IJavaObject jso = (IJavaObject) receiver;

      /*
       * Edge case - it is possible to simulate an instance call on a JSO using
       * a null ref. In that case, the jso variable here will be null, and we'll
       * get an NPE in this if check.
       */
      if (!jso.isNull()
          && "com.google.gwt.core.client.JavaScriptObject$".equals(jso.getJavaType().getName())) {
        result = invokeJsoEval(jso, args);
      } else {
        /*
         * Normal SendMessage; pass null as the declaring type to sendMessage so
         * that polymorphism is respected.
         */
        result = ((IJavaObject) receiver).sendMessage(selector, signature,
            args, getContext().getThread(), null);
      }
    } else {
      throw new CoreException(
          new Status(
              IStatus.ERROR,
              Activator.PLUGIN_ID,
              InstructionsEvaluationMessages.SendMessage_Attempt_to_send_a_message_to_a_non_object_value_1));
    }

    setMethodReturnValue(result);
  }
}
