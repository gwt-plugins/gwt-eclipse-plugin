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

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * Tests the {@link GWTMemberRefactoringSupport} class.
 */
public class GWTMemberRefactoringSupportTest extends AbstractRefactoringTestImpl {

  public void testCreateEdit() {
    GWTMemberRefactoringSupport support = new GWTMemberRefactoringSupport();

    // Change the name of the R.B.getNumber() method to "getInteger"
    IMethod newMethod = rClass.getCompilationUnit().getType("R").getMethod(
        "getInteger", new String[0]);
    support.setNewElement(newMethod);
    String rawRef = "@com.hello.client.R$B::getNumber()";
    IIndexedJavaRef ref = new IndexedJsniJavaRef(JsniJavaRef.parse(rawRef));

    ReplaceEdit edit = (ReplaceEdit) support.createEdit(ref);
    assertEquals(23, edit.getOffset());
    assertEquals(9, edit.getLength());
    assertEquals("getInteger", edit.getText());
  }

  public void testGetEditDescriptionForField() {
    GWTMemberRefactoringSupport support = new GWTMemberRefactoringSupport();

    IType refactorTest = refactorTestClass.getCompilationUnit().getType(
        "RefactorTest");
    IField field = refactorTest.getField("counter");
    support.setOldElement(field);
    assertEquals("Update field reference", support.getEditDescription());
  }

  public void testGetEditDescriptionForMethod() {
    GWTMemberRefactoringSupport support = new GWTMemberRefactoringSupport();

    IType refactorTest = refactorTestClass.getCompilationUnit().getType(
        "RefactorTest");
    IMethod method = refactorTest.getMethod("getMagicNumber", new String[0]);
    support.setOldElement(method);
    assertEquals("Update method reference", support.getEditDescription());
  }

}
