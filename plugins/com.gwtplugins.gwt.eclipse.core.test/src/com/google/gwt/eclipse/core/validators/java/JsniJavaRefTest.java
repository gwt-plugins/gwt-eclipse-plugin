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
package com.google.gwt.eclipse.core.validators.java;

import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Tests the {@link JsniJavaRef} class. Based on the test class for
 * {@link com.google.gwt.dev.util.JsniRef} in GWT.
 */
public class JsniJavaRefTest extends AbstractGWTPluginTestCase {

  private static final String CTOR_REF = "@some.package.SomeClass::new()";
  private static final String FIELD_REF = "@some.package.SomeClass::someField";
  private static final String INNER_CLASS_FIELD_REF = "@some.package.SomeClass$Inner::someField";
  private static final String METHOD_REF = "@some.package.SomeClass::someMeth()";
  private static final String METHOD_WITH_PARAMS_REF = "@some.package.SomeClass::someMeth([[ZBCDFIJLjava/lang/String;S)";
  private static final String METHOD_WITH_WILDCARD_REF = "@some.package.SomeClass::someMeth(*)";
  private static final String[] REFS = new String[] {
      FIELD_REF, METHOD_REF, METHOD_WITH_PARAMS_REF, CTOR_REF, METHOD_WITH_WILDCARD_REF};

  public void testEquals() {
    for (String test : REFS) {
      JsniJavaRef ref1 = JsniJavaRef.parse(test);
      JsniJavaRef ref2 = JsniJavaRef.parse(test);
      assertEquals(ref1, ref2);
    }
  }

  public void testFindEnclosingJavaRef() {
    // TODO: we're going to refactor the findEnclosingJavaRef method out of
    // JsniJavaRef soon anyway, so I'm not going to add any tests for this
  }

  public void testHashCode() {
    for (String test : REFS) {
      JsniJavaRef ref1 = JsniJavaRef.parse(test);
      JsniJavaRef ref2 = JsniJavaRef.parse(test);
      assertEquals(ref1.hashCode(), ref2.hashCode());
    }
  }

  public void testOffsets() {
    int offset = 100;

    {
      // test field reference
      JsniJavaRef ref = JsniJavaRef.parse(FIELD_REF);
      ref.setOffset(offset);
      assertEquals(ref.getOffset(), offset);
      assertEquals(ref.getClassOffset(), offset + 1);
      assertEquals(ref.getMemberOffset(), offset + 25);
    }

    {
      // test field reference on inner class
      JsniJavaRef ref = JsniJavaRef.parse(INNER_CLASS_FIELD_REF);
      ref.setOffset(offset);
      assertEquals(ref.getOffset(), offset);
      assertEquals(ref.getClassOffset(), offset + 1);
      assertEquals(ref.getMemberOffset(), offset + 31);
    }

    {
      // test method reference
      JsniJavaRef ref = JsniJavaRef.parse(METHOD_REF);
      ref.setOffset(offset);
      assertEquals(ref.getOffset(), offset);
      assertEquals(ref.getClassOffset(), offset + 1);
      assertEquals(ref.getMemberOffset(), offset + 25);
    }

    {
      // test method reference with every JNI type in parameter type list
      JsniJavaRef ref = JsniJavaRef.parse(METHOD_WITH_PARAMS_REF);
      ref.setOffset(offset);
      assertEquals(ref.getOffset(), offset);
      assertEquals(ref.getClassOffset(), offset + 1);
      assertEquals(ref.getMemberOffset(), offset + 25);
    }

    {
      // test method reference with a wildcard parameter type list
      JsniJavaRef ref = JsniJavaRef.parse(METHOD_WITH_WILDCARD_REF);
      ref.setOffset(offset);
      assertEquals(ref.getOffset(), offset);
      assertEquals(ref.getClassOffset(), offset + 1);
      assertEquals(ref.getMemberOffset(), offset + 25);
    }

    {
      // test constructor reference
      JsniJavaRef ref = JsniJavaRef.parse(CTOR_REF);
      ref.setOffset(offset);
      assertEquals(ref.getOffset(), offset);
      assertEquals(ref.getClassOffset(), offset + 1);
      assertEquals(ref.getMemberOffset(), offset + 25);
    }
  }

  public void testParse() {
    {
      // test field reference
      JsniJavaRef ref = JsniJavaRef.parse(FIELD_REF);
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("SomeClass", ref.simpleClassName());
      assertEquals("someField", ref.memberName());
      assertEquals("someField", ref.memberSignature());
      assertEquals("someField", ref.readableMemberSignature());
      assertFalse(ref.isMethod());
      assertTrue(ref.isField());
      assertFalse(ref.isConstructor());
    }

    {
      // test field reference on inner class
      JsniJavaRef ref = JsniJavaRef.parse(INNER_CLASS_FIELD_REF);
      assertEquals("some.package.SomeClass$Inner", ref.className());
      assertEquals("some.package.SomeClass.Inner", ref.dottedClassName());
      assertEquals("Inner", ref.simpleClassName());
      assertEquals("someField", ref.memberName());
      assertEquals("someField", ref.memberSignature());
      assertEquals("someField", ref.readableMemberSignature());
      assertFalse(ref.isMethod());
      assertTrue(ref.isField());
      assertFalse(ref.isConstructor());
    }

    {
      // test method reference
      JsniJavaRef ref = JsniJavaRef.parse(METHOD_REF);
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("SomeClass", ref.simpleClassName());
      assertEquals("someMeth", ref.memberName());
      assertEquals("someMeth()", ref.memberSignature());
      assertEquals("someMeth()", ref.readableMemberSignature());
      assertTrue(ref.isMethod());
      assertFalse(ref.isField());
      assertFalse(ref.isConstructor());
      assertEquals(0, ref.paramTypes().length);
    }

    {
      // test method reference with every JNI type in parameter type list
      JsniJavaRef ref = JsniJavaRef.parse(METHOD_WITH_PARAMS_REF);
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("SomeClass", ref.simpleClassName());
      assertEquals("someMeth", ref.memberName());
      assertEquals("someMeth([[ZBCDFIJLjava/lang/String;S)",
          ref.memberSignature());
      assertEquals(
          "someMeth(boolean[][], byte, char, double, float, int, long, java.lang.String, short)",
          ref.readableMemberSignature());
      assertTrue(ref.isMethod());
      assertFalse(ref.isConstructor());
      assertEquals(9, ref.paramTypes().length);
      assertEquals("[[Z", ref.paramTypes()[0]);
      assertEquals("B", ref.paramTypes()[1]);
      assertEquals("C", ref.paramTypes()[2]);
      assertEquals("D", ref.paramTypes()[3]);
      assertEquals("F", ref.paramTypes()[4]);
      assertEquals("I", ref.paramTypes()[5]);
      assertEquals("J", ref.paramTypes()[6]);
      assertEquals("Ljava/lang/String;", ref.paramTypes()[7]);
      assertEquals("S", ref.paramTypes()[8]);
    }

    {
      // test constructor reference
      JsniJavaRef ref = JsniJavaRef.parse(CTOR_REF);
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("SomeClass", ref.simpleClassName());
      assertEquals("new", ref.memberName());
      assertEquals("new()", ref.memberSignature());
      assertTrue(ref.isMethod());
      assertFalse(ref.isField());
      assertTrue(ref.isConstructor());
    }
    
    {
      // test method reference with wildcard type parameter
      JsniJavaRef ref = JsniJavaRef.parse(METHOD_WITH_WILDCARD_REF);
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("SomeClass", ref.simpleClassName());
      assertEquals("someMeth", ref.memberName());
      assertEquals("someMeth(*)", ref.memberSignature());
      assertTrue(ref.isMethod());
      assertFalse(ref.isField());
      assertFalse(ref.isConstructor());
      assertTrue(ref.matchesAnyOverload());
    }
  }

  public void testResolveJavaElement() {
    IJavaProject project = getTestProject();
    assertNotNull(project);

    JsniJavaRef ref;
    IJavaElement element;

    try {
      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest::new()");
      element = ref.resolveJavaElement(project);
      assertTrue(element instanceof IMethod);
      assertTrue(((IMethod) element).isConstructor());

      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest::getNumber()");
      element = ref.resolveJavaElement(project);
      assertTrue(element instanceof IMethod);
      assertEquals(element.getElementName(), "getNumber");

      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest$Inner::new(Ljava/lang/String;)");
      element = ref.resolveJavaElement(project);
      assertTrue(element instanceof IMethod);
      assertTrue(((IMethod) element).isConstructor());

      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest$InnerSub::sayHi([I)");
      element = ref.resolveJavaElement(project);
      assertTrue(element instanceof IMethod);
      assertEquals(element.getElementName(), "sayHi");
      assertEquals(element.getParent().getElementName(), "InnerSub");

      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest$InnerSub::sayHi(*)");
      element = ref.resolveJavaElement(project);
      assertTrue(element instanceof IMethod);
      assertEquals(element.getElementName(), "sayHi");
      assertEquals(element.getParent().getElementName(), "InnerSub");
      
      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest$InnerSub::takeInner(Lcom/hello/client/JsniJavaRefTest$Inner;)");
      element = ref.resolveJavaElement(project);
      assertTrue(element instanceof IMethod);
      assertEquals(element.getElementName(), "takeInner");
      assertEquals(element.getParent().getElementName(), "Inner");

      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest$InnerSub::takeT(Ljava/lang/String;I)");
      element = ref.resolveJavaElement(project);
      assertTrue(element instanceof IMethod);
      assertEquals(element.getElementName(), "takeT");
      assertEquals(element.getParent().getElementName(), "InnerSub");

      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest::keith");
      element = ref.resolveJavaElement(project);
      assertTrue(element instanceof IField);
      assertEquals(element.getElementName(), "keith");

      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest.InnerInterface::getKey()");
      element = ref.resolveJavaElement(project);
      assertTrue(element instanceof IMethod);
      assertEquals(element.getElementName(), "getKey");
      assertEquals(element.getParent().getElementName(), "InnerInterface");

    } catch (UnresolvedJsniJavaRefException e) {
      fail(e.getMessage());
    } catch (JavaModelException e) {
      fail(e.getMessage());
    }

    try {
      // unresolved reference
      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest$InnerSub::takeInner(Lcom/hello/client/JsniJavaRefTest;)");
      element = ref.resolveJavaElement(project);
      fail("resolveElement should have thrown UnresolvedJsniJavaRefException");

    } catch (UnresolvedJsniJavaRefException e) {
      // We're expecting the exception, so do nothing
    }

    try {
      // unresolved interface member reference
      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest.InnerInterface::badRef()");
      element = ref.resolveJavaElement(project);
      fail("resolveElement should have thrown UnresolvedJsniJavaRefException");

    } catch (UnresolvedJsniJavaRefException e) {
      // We're expecting the exception, so do nothing
    }

    try {
      // magic null reference (should be ignored)
      ref = JsniJavaRef.parse("@null::nullMethod()");
      element = ref.resolveJavaElement(project);
      fail("resolveElement should have thrown UnresolvedJsniJavaRefException");

    } catch (UnresolvedJsniJavaRefException e) {
      assertNull(e.getProblemType());
    }

    try {
      // reference to JRE type (should be ignored)
      ref = JsniJavaRef.parse("@java.lang.String::valueOf(J)");
      element = ref.resolveJavaElement(project);
      fail("resolveElement should have thrown UnresolvedJsniJavaRefException");

    } catch (UnresolvedJsniJavaRefException e) {
      assertNull(e.getProblemType());
    }

    try {
      // synthetic Enum::values() reference (should be ignored)
      ref = JsniJavaRef.parse("@com.hello.client.JsniJavaRefTest.MyEnum::values()");
      element = ref.resolveJavaElement(project);
      fail("resolveElement should have thrown UnresolvedJsniJavaRefException");

    } catch (UnresolvedJsniJavaRefException e) {
      assertNull(e.getProblemType());
    }
  }

  public void testToString() {
    for (String test : REFS) {
      JsniJavaRef ref = JsniJavaRef.parse(test);
      assertEquals(test, ref.toString());
    }
  }

  @Override
  protected TestClass[] getTestClasses() {
    String[] testClass = new String[] {
        "package com.hello.client;",
        "",
        "public class JsniJavaRefTest {",
        "  private static class Inner {",
        "    private String name;",
        "    ",
        "    public Inner(String name) {",
        "      this.name = name;",
        "    }",
        "",
        "    public void sayHi(int[] args) {",
        "      System.out.println(name);",
        "    }",
        "    ",
        "    public void takeInner(Inner x) {",
        "    }",
        "  }",
        "  ",
        "  private static class InnerSub<T> extends Inner {",
        "    public InnerSub(String name) {",
        "      super(name);",
        "    }",
        "",
        "    @Override",
        "    public void sayHi(int[] args) {",
        "      super.sayHi(args);",
        "    }",
        "    ",
        "    public <U> void takeT(T param, U param2) { ",
        "    }    ",
        "  }",
        "  ",
        "  private interface InnerInterface {",
        "    String getKey();",
        "  }",
        "",
        "",
        "  private enum MyEnum { KEITH }",
        "",
        "  private String keith;",
        "  ",
        "  private int getNumber() {",
        "    return 777;",
        "  }",
        "  ",
        "  private static native void jsniMethod(int requestId)/*-{",
        "    var script = document.createElement('script');",
        "    window[callback] = function(jsonObj) {",
        "      var requestString = @java.lang.String::valueOf(J)(requestId);",
        "    };",
        "    var obj = @com.hello.client.JsniJavaRefTest::new()()",
        "    var num = z.@com.hello.client.JsniJavaRefTest::getNumber()();",
        "    var inner = @com.hello.client.JsniJavaRefTest$Inner::new(Ljava/lang/String;)(callback);",
        "    inner.@com.hello.client.JsniJavaRefTest$InnerSub::sayHi([I)();",
        "    inner.@com.hello.client.JsniJavaRefTest$InnerSub::takeInner(Lcom/hello/client/JsniJavaRefTest$Inner;)();",
        "    @com.hello.client.JsniJavaRefTest$InnerSub::takeT(Ljava/lang/String;I)('Hi');",
        "    var x = hello.@com.hello.client.JsniJavaRefTest::keith;",
        "    document.body.appendChild(script);",
        "    // bad reference",
        "    inner.@com.hello.client.JsniJavaRefTest$InnerSub::takeInner(Lcom/hello/client/JsniJavaRefTest;)();",
        "  }-*/;",
        "}"
        };

    return new TestClass[] {new TestClass(testClass, "JsniJavaRefTest")};
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

}
