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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaValue;

/**
 * An implementation of SendStaticMessage that deals with static method
 * invocations on JSOs.
 * 
 * Will be compiled against Eclipse 3.7 with a custom JDT patch, but will not be
 * executed unless the user has the custom JDT patch installed.
 */
// Using org.eclipse.jdt.internal.debug.*
@SuppressWarnings("restriction")
public class JsoSendStaticMessage extends AbstractJsoDelegationMessage {

  public JsoSendStaticMessage(String typeName, String selector,
      String signature, int argCount, int start) {
    super(selector, signature, argCount, typeName, start);
  }

  @Override
  public void execute() throws CoreException {
    IJavaValue[] args = new IJavaValue[argCount];
    // args are in reverse order
    for (int i = argCount - 1; i >= 0; i--) {
      args[i] = boxArg(popValue());
    }

    IJavaValue result = null;

    // Static invocation; use null JDI value
    IJavaObject jso = (IJavaObject) nullValue();

    result = invokeJsoEval(jso, args);

    setMethodReturnValue(result);
  }
}
