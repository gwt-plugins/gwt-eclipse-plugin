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
package com.google.gwt.eclipse.core.uibinder.sse.css;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.ui.internal.contentassist.CSSContentAssistProcessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper for calling the inaccessible
 * <code>CSSProposalArranger.getProposals</code> method.
 */
@SuppressWarnings("restriction")
public final class CssProposalArrangerCaller {

  public static ICompletionProposal[] getProposals(int documentPosition,
      ICSSNode node, int documentOffset, char quote) throws Throwable {
    return (ICompletionProposal[]) getProposalsInternal(documentPosition, node,
        documentOffset, quote);
  }

  private static Object callGetProposals(Class<?> clazz, Object instance)
      throws SecurityException, NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException,
      InvocationTargetException {
    Method method = clazz.getDeclaredMethod("getProposals", (Class<?>[]) null);
    method.setAccessible(true);

    return method.invoke(instance, (Object[]) null);
  }

  private static Class<?> findClass() throws ClassNotFoundException {
    return CSSContentAssistProcessor.class.getClassLoader().loadClass(
        "org.eclipse.wst.css.ui.internal.contentassist.CSSProposalArranger");
  }

  private static Constructor<?> findConstructor(Class<?> clazz) {
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    for (Constructor<?> constructor : constructors) {
      if (constructor.getParameterTypes().length == 4) {
        return constructor;
      }
    }

    throw new RuntimeException("Could not find constructor");
  }

  private static Object getProposalsInternal(int documentPosition,
      ICSSNode node, int documentOffset, char quote)
      throws ClassNotFoundException, IllegalArgumentException,
      InstantiationException, IllegalAccessException,
      InvocationTargetException, SecurityException, NoSuchMethodException {

    Class<?> cssProposalArrangerClass = findClass();
    Object cssProposalArranger = instantiate(cssProposalArrangerClass,
        documentPosition, node, documentOffset, quote);

    return callGetProposals(cssProposalArrangerClass, cssProposalArranger);
  }

  private static Object instantiate(Class<?> clazz, int documentPosition,
      ICSSNode node, int documentOffset, char quote)
      throws IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    Constructor<?> constructor = findConstructor(clazz);
    constructor.setAccessible(true);
    return constructor.newInstance(documentPosition, node, documentOffset,
        quote);
  }

  private CssProposalArrangerCaller() {
  }
}
