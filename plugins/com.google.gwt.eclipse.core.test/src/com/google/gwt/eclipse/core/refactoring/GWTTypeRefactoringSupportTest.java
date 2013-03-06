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

import com.google.gwt.eclipse.core.validators.java.JsniJavaRef;
import com.google.gwt.eclipse.core.search.IIndexedJavaRef;
import com.google.gwt.eclipse.core.search.IndexedJsniJavaRef;
import com.google.gwt.eclipse.core.search.JsniJavaRefParamType;

import org.eclipse.jdt.core.IType;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * Tests the {@link GWTTypeRefactoringSupport} class.
 */
public class GWTTypeRefactoringSupportTest extends AbstractRefactoringTestImpl {

  public void testCreateEdit() {
    GWTTypeRefactoringSupport support = new GWTTypeRefactoringSupport();

    // Change the name of the R class to RRR
    IType oldType = rClass.getCompilationUnit().getType("R");
    support.setOldElement(oldType);
    IType newType = rClass.getCompilationUnit().getType("RRR");
    support.setNewElement(newType);

    String rawRef = "@com.hello.client.R.B::getNumber()";
    IIndexedJavaRef ref = new IndexedJsniJavaRef(JsniJavaRef.parse(rawRef));

    ReplaceEdit edit = (ReplaceEdit) support.createEdit(ref);
    assertEquals(1, edit.getOffset());
    assertEquals(18, edit.getLength());
    assertEquals("com.hello.client.RRR", edit.getText());
  }

  public void testCreateEditForInnerClass() {
    GWTTypeRefactoringSupport support = new GWTTypeRefactoringSupport();

    // Change the name of the inner class B to BBB
    IType r = rClass.getCompilationUnit().getType("R");
    IType oldType = r.getType("B");
    support.setOldElement(oldType);
    IType newType = r.getType("BBB");
    support.setNewElement(newType);

    String rawRef = "@com.hello.client.R.B::getNumber()";
    IIndexedJavaRef ref = new IndexedJsniJavaRef(JsniJavaRef.parse(rawRef));

    ReplaceEdit edit = (ReplaceEdit) support.createEdit(ref);
    assertEquals(1, edit.getOffset());
    assertEquals(20, edit.getLength());
    assertEquals("com.hello.client.R.BBB", edit.getText());
  }

  public void testCreateEditPreserveDollarClassSeparator() {
    GWTTypeRefactoringSupport support = new GWTTypeRefactoringSupport();

    // Change the name of the inner class B to BBB
    IType r = rClass.getCompilationUnit().getType("R");
    IType oldType = r.getType("B");
    support.setOldElement(oldType);
    IType newType = r.getType("BBB");
    support.setNewElement(newType);

    String rawRef = "@com.hello.client.R$B::getNumber()";
    IIndexedJavaRef ref = new IndexedJsniJavaRef(JsniJavaRef.parse(rawRef));

    ReplaceEdit edit = (ReplaceEdit) support.createEdit(ref);
    assertEquals(1, edit.getOffset());
    assertEquals(20, edit.getLength());
    assertEquals("com.hello.client.R$BBB", edit.getText());
  }

  public void testCreateEditPreserveSlashPackageSeparator() {
    GWTTypeRefactoringSupport support = new GWTTypeRefactoringSupport();

    // Change the name of the R class to RRR
    IType oldType = rClass.getCompilationUnit().getType("R");
    support.setOldElement(oldType);
    IType newType = rClass.getCompilationUnit().getType("RRR");
    support.setNewElement(newType);

    String rawRef = "Lcom/hello/client/R;";
    IIndexedJavaRef ref = JsniJavaRefParamType.parse(null, 0, rawRef);

    ReplaceEdit edit = (ReplaceEdit) support.createEdit(ref);
    assertEquals(1, edit.getOffset());
    assertEquals(18, edit.getLength());
    assertEquals("com/hello/client/RRR", edit.getText());
  }
  
  // TODO: add test for renaming an inner class used as a JSNI ref type param

  public void testGetNewType() {
    GWTTypeRefactoringSupport support = new GWTTypeRefactoringSupport();
    IType refactorTest = refactorTestClass.getCompilationUnit().getType(
        "RefactorTest");
    support.setNewElement(refactorTest);

    assertEquals(refactorTest, support.getNewType());
  }

  public void testGetOldType() {
    GWTTypeRefactoringSupport support = new GWTTypeRefactoringSupport();
    IType refactorTest = refactorTestClass.getCompilationUnit().getType(
        "RefactorTest");
    support.setOldElement(refactorTest);

    assertEquals(refactorTest, support.getOldType());
  }

}
