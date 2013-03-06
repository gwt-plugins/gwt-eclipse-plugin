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
package com.google.gwt.eclipse.core.refactoring;

import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import java.util.WeakHashMap;

/**
 * A context for a nested refactoring. This class is usually instantiated by the
 * caller of a nested refactoring, and will later be retrieved by the callee of
 * the nested refactoring.
 * <p>
 * Some of our refactoring participants use a pattern of calling out to JDT to
 * create a refactoring change (hence the term nested refactoring). Using only
 * the refactoring framework, there isn't a way to pass arbitrary data from the
 * caller (the original refactoring participant) to the callee (the called JDT
 * refactoring and its participants). This class allows us to pass data.
 * <p>
 * Imagine the user is renaming FooService to Foo2Service. A participant of this
 * rename refactoring would handle the rename of FooServiceAsync to
 * Foo2ServiceAsync. It would achieve that rename by creating a nested
 * refactoring (hence the JDT code would do the real work in the rename of
 * FooServiceAsync to Foo2ServiceAsync.)
 * <p>
 * There is a single {@link RefactoringProcessor} for each refactoring (its
 * participants share this processor). In the above scenario, there would be two
 * processors -- one for the user-initiated refactoring for which we are a
 * participant, and the other for the JDT refactoring initiated by us. To store
 * a different context for each refactoring, we use a {@link WeakHashMap} keyed
 * by the processor. This weak hash map provides us with a context scope that is
 * the same as the lifetime of the refactoring.
 * <p>
 * One added benefit of using a processor is that the processor for a
 * refactoring is reachable using public APIs by both the refactoring creator
 * (original participant in the above example) and the created refactoring
 * itself (the JDT refactoring and its participants in the above example).
 */
public abstract class NestedRefactoringContext {

  private static WeakHashMap<RefactoringProcessor, NestedRefactoringContext> processorToData = new WeakHashMap<RefactoringProcessor, NestedRefactoringContext>();

  public static NestedRefactoringContext forProcessor(
      RefactoringProcessor processor) {
    return processorToData.get(processor);
  }

  public static NestedRefactoringContext storeForProcessor(
      RefactoringProcessor processor, NestedRefactoringContext data) {
    processorToData.put(processor, data);
    return data;
  }

}
