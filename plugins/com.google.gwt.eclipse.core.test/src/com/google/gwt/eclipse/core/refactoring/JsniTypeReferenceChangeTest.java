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

import com.google.gwt.eclipse.core.platformproxy.refactoring.IJsniTypeReferenceChange;
import com.google.gwt.eclipse.core.platformproxy.refactoring.JsniTypeReferenceChangeFactory;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;

/**
 * Tests the {@link JsniTypeReferenceChange} class.
 */
public class JsniTypeReferenceChangeTest extends AbstractRefactoringTestImpl {

  public void testPerformCompilationUnitChanged() {
    final ICompilationUnit targetCu = refactorTestClass.getCompilationUnit();
    final IType newType = targetCu.getType("RefactorTest2");

    GWTTypeRefactoringSupport refactoringSupport = new GWTTypeRefactoringSupport() {
      @Override
      public IType getNewType() {
        return newType;
      }

      @Override
      public IType getOldType() {
        return targetCu.getType("RefactorTest");
      }
    };

    JsniTypeReferenceChangeFactory factory = new JsniTypeReferenceChangeFactory(
        refactoringSupport);
    IJsniTypeReferenceChange change = factory.createChange(targetCu);
    assertEquals(newType.getCompilationUnit(), change.getCompilationUnit());
  }

  public void testPerformCompilationUnitUnchanged() {
    ICompilationUnit targetCu = refactorTestClass.getCompilationUnit();

    GWTTypeRefactoringSupport refactoringSupport = new GWTTypeRefactoringSupport() {
      @Override
      public IType getOldType() {
        return rClass.getCompilationUnit().getType("R");
      }
    };

    JsniTypeReferenceChangeFactory factory = new JsniTypeReferenceChangeFactory(
        refactoringSupport);
    IJsniTypeReferenceChange change = factory.createChange(targetCu);
    assertEquals(targetCu, change.getCompilationUnit());
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

}
