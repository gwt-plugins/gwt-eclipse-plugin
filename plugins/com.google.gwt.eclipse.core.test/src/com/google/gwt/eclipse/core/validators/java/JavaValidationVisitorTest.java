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

import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.markers.GWTProblemType;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import java.util.List;

/**
 * Tests the {@link JavaValidationVisitor} class.
 */
public class JavaValidationVisitorTest extends AbstractGWTPluginTestCase {

  private TestClass testClass;

  private ASTNode ast;

  public void testGetValidationResult() {
    JavaValidationVisitor visitor = new JavaValidationVisitor();
    ast.accept(visitor);
    JavaValidationResult result = visitor.getValidationResult();

    // Verify the refs we found
    List<JsniJavaRef> refs = result.getJavaRefs();
    assertEquals(6, refs.size());
    assertTrue(refs.contains(JsniJavaRef.parse("@com.hello.MissingType::field")));
    assertTrue(refs.contains(JsniJavaRef.parse("@com.hello.MissingType::method()")));

    // Verify the problems returned (we should get only two; the rest of the
    // JSNI errors should have been suppressed by the annotations).
    List<GWTJavaProblem> problems = result.getProblems();
    assertEquals(2, problems.size());
    assertEquals(GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE,
        problems.get(0).getProblemType());
    assertEquals(GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE,
        problems.get(1).getProblemType());
  }

  @Override
  protected TestClass[] getTestClasses() {
    String[] testClassSource = new String[] {
        "package com.hello.client;",
        "",
        "public class JavaValidationVisitorTest {",
        "  public static native int jsni()/*-{",
        "      return @com.hello.MissingType::field;",
        "  }-*/;",
        "",
        "  @SuppressWarnings(\"jsni\")",
        "  public static native int suppressed1()/*-{",
        "      return @com.hello.MissingType::method()();",
        "  }-*/;",
        "",
        "  @SuppressWarnings(value=\"jsni\")",
        "  public static native int suppressed2()/*-{",
        "      return @com.hello.MissingType::field;",
        "  }-*/;",
        "",
        "  @SuppressWarnings({\"jsni\",\"unchecked\"})",
        "  public static native int suppressed3()/*-{",
        "      return @com.hello.MissingType::field;",
        "  }-*/;",
        "",
        "  @java.lang.SuppressWarnings(\"jsni\")",
        "  public static native int suppressed4()/*-{",
        "      return @com.hello.MissingType::field;",
        "  }-*/;",
        "",
        "  @SuppressWarnings(\"unrecognized\")",
        "  public static native int notSuppressed()/*-{",
        "      return @com.hello.MissingType::field;",
        "  }-*/;",
        "}"
        };

    testClass = new TestClass(testClassSource, "JavaValidationVisitorTest");
    return new TestClass[] {testClass};
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Have JDT parse the compilation unit
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setProject(getTestProject());
    parser.setResolveBindings(false);
    parser.setSource(testClass.getCompilationUnit());
    ast = parser.createAST(null);
  }

}
