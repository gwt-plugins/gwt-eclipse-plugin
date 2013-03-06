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

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the {@link GWTRenameMemberInJsniParticipant} class.
 */
public class GWTRenameMemberInJsniParticipantTest extends
    AbstractRefactoringTestImpl {

  private TestClass implementsIFooClass;

  /**
   * TODO: This fails when run as part of the unit test. It appears that we
   * might not be using the classes correctly since
   * {@link org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor RenameVirtualMethodProcessor}
   * thinks that the starting name and the final name are the same, hence we
   * have disabled the code. We need to trace through
   * {@link org.eclipse.jdt.internal.ui.refactoring.reorg.RenameLinkedMode RenameLinkedMode}
   * to see the proper way to trigger the refactoring.
   */
  public void disabledTestRenameVirtualMethod() throws Exception {
    IMethod oldMethod = implementsIFooClass.getCompilationUnit().getType(
        "ImplementsIFoo").getMethod("willBeRenamed", new String[] {"I", "I"});
    assertMethodRename(oldMethod, "wasRenamed");
  }

  public void testInitializeWithInvalidElement() {
    GWTRenameMemberInJsniParticipant participant = new GWTRenameMemberInJsniParticipant();
    assertFalse(participant.initialize(getTestProject()));
  }

  public void testRenameExternalMethod() throws Exception {
    IMethod oldMethod = rClass.getCompilationUnit().getType("R").getType("B").getMethod(
        "getNumber", new String[0]);
    assertMethodRename(oldMethod, "getNumber2");
  }

  public void testRenameField() throws Exception {
    IField oldField = refactorTestClass.getCompilationUnit().getType(
        "RefactorTest").getField("counter");
    assertFieldRename(oldField, "counter2");
  }

  public void testRenameMethod() throws Exception {
    IMethod oldMethod = refactorTestClass.getCompilationUnit().getType(
        "RefactorTest").getMethod("getNumber", new String[] {"I"});
    assertMethodRename(oldMethod, "getNewNumber");
  }

  @Override
  protected TestClass[] getTestClasses() {
    List<TestClass> testClasses = new ArrayList<TestClass>(
        Arrays.asList(super.getTestClasses()));

    // Declares a method callable from JSNI
    StringBuilder declaresMethodCalledFromJSNI = new StringBuilder();
    declaresMethodCalledFromJSNI.append("package com.hello.client;\n");
    declaresMethodCalledFromJSNI.append("public interface IFoo {\n");
    declaresMethodCalledFromJSNI.append("  void willBeRenamed(int a, int b);\n");
    declaresMethodCalledFromJSNI.append("}\n");
    testClasses.add(new TestClass(declaresMethodCalledFromJSNI.toString(),
        "IFoo"));

    // Implements a method called from JSNI
    StringBuilder implementsMethodCalledFromJSNI = new StringBuilder();
    implementsMethodCalledFromJSNI.append("package com.hello.client;\n");
    implementsMethodCalledFromJSNI.append("public class ImplementsIFoo implements IFoo {\n");
    // implementsMethodCalledFromJSNI.append(" public void willBeRenamed(int a)
    // {}\n");
    implementsMethodCalledFromJSNI.append("  public void willBeRenamed(int a, int b) {}\n");
    implementsMethodCalledFromJSNI.append("}\n");
    implementsIFooClass = new TestClass(
        implementsMethodCalledFromJSNI.toString(), "ImplementsIFoo");
    testClasses.add(implementsIFooClass);

    // Calls IFoo::willBeRenamed(int,int) from JSNI
    StringBuilder callsMethodThroughJSNI = new StringBuilder();
    callsMethodThroughJSNI.append("package com.hello.client;\n");
    callsMethodThroughJSNI.append("class CallsImplementsIFoo {\n");
    callsMethodThroughJSNI.append("  static native void callsImplementsIFoo() /*-{ this.@com.hello.client.ImplementsIFoo::willBeRenamed(II)(0,1); }-*/;\n");
    callsMethodThroughJSNI.append("}\n");
    testClasses.add(new TestClass(callsMethodThroughJSNI.toString(),
        "CallsImplementsIFoo"));

    return testClasses.toArray(new TestClass[0]);
  }
}
