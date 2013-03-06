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

import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.eclipse.core.editors.java.GWTDocumentSetupParticipant;
import com.google.gwt.eclipse.core.editors.java.GWTPartitions;
import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.markers.GWTProblemType;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;
import com.google.gwt.eclipse.core.test.RegionConverter;
import com.google.gwt.eclipse.core.validators.java.JsniParser.JavaScriptParseException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextSelection;

import java.util.List;

/**
 * Tests the {@link JsniParser} class.
 */
public class JsniParserTest extends AbstractGWTPluginTestCase {

  private TestClass testClass;
  private String testClassSource;

  public void testExtractMethodBody() {
    String jsniBody = createString(new String[] {
        "var num = obj.@com.hello.client.A$B::getNumber()();",
        "num += obj.@com.hello.client.JsniParserTest::getNumber(IJ)(2);"});

    String jsniMethod = "public native void jsniMethod()/*-{" + jsniBody
        + "}-*/;";

    assertEquals(jsniBody, JsniParser.extractMethodBody(jsniMethod));
  }

  public void testExtractMethodBodyWithEmptyBody() {
    String jsniMethod = "public native void jsniMethod()/*-{}-*/;";
    assertEquals("", JsniParser.extractMethodBody(jsniMethod));
  }

  public void testExtractMethodBodyWithoutEndToken() {
    String jsniMethod = "public native void jsniMethod()/*-{ return; };";
    assertNull(JsniParser.extractMethodBody(jsniMethod));
  }

  public void testExtractMethodBodyWithoutStartToken() {
    String jsniMethod = "public native void jsniMethod() { return; }-*/;";
    assertNull(JsniParser.extractMethodBody(jsniMethod));
  }

  public void testGetEnclosingJsniRegionSelectionInsideJsni() {
    IRegion selRegion = RegionConverter.convertWindowsRegion(169, 3, testClass.getContents());
    ITextSelection sel = new TextSelection(selRegion.getOffset(), selRegion.getLength());
    ITypedRegion jsniRegion = JsniParser.getEnclosingJsniRegion(sel,
        getTestClassDocument());
    assertNotNull(jsniRegion);
    assertEquals(GWTPartitions.JSNI_METHOD, jsniRegion.getType());

    IRegion expectedJsniRegion = RegionConverter.convertWindowsRegion(121, 234, testClass.getContents());
    assertEquals(expectedJsniRegion.getOffset(), jsniRegion.getOffset());
    assertEquals(expectedJsniRegion.getLength(), jsniRegion.getLength());
  }

  public void testGetEnclosingJsniRegionSelectionIsJsni() {
    IRegion selRegion = RegionConverter.convertWindowsRegion(121, 234, testClass.getContents());
    ITextSelection sel = new TextSelection(selRegion.getOffset(), selRegion.getLength());
    ITypedRegion jsniRegion = JsniParser.getEnclosingJsniRegion(sel,
        getTestClassDocument());
    assertNotNull(jsniRegion);
    assertEquals(GWTPartitions.JSNI_METHOD, jsniRegion.getType());
    IRegion expectedJsniRegion = RegionConverter.convertWindowsRegion(121, 234, testClass.getContents());
    assertEquals(expectedJsniRegion.getOffset(), jsniRegion.getOffset());
    assertEquals(expectedJsniRegion.getLength(), jsniRegion.getLength());
  }

  public void testGetEnclosingJsniRegionSelectionOutsideJsni() {
    ITextSelection sel = new TextSelection(76, 7);
    assertNull(JsniParser.getEnclosingJsniRegion(sel, getTestClassDocument()));
  }

  public void testGetEnclosingJsniRegionSelectionOverlapsJsni() {
    ITextSelection sel = new TextSelection(169, 265);
    assertNull(JsniParser.getEnclosingJsniRegion(sel, getTestClassDocument()));
  }

  public void testGetEnclosingJsniRegionSelectionWrapsJsni() {
    ITextSelection sel = new TextSelection(88, 346);
    assertNull(JsniParser.getEnclosingJsniRegion(sel, getTestClassDocument()));
  }

  public void testParseMethodDeclaration() {
    // Have JDT parse the compilation unit
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setProject(getTestProject());
    parser.setResolveBindings(false);
    parser.setSource(testClass.getCompilationUnit());
    CompilationUnit root = (CompilationUnit) parser.createAST(null);

    // Find the JSNI method and parse it
    TypeDeclaration typeDecl = (TypeDeclaration) root.types().get(0);
    MethodDeclaration jsniMethod = typeDecl.getMethods()[0];
    JavaValidationResult result = JsniParser.parse(jsniMethod);

    // Verify the Java refs
    List<JsniJavaRef> refs = result.getJavaRefs();
    assertEquals(3, refs.size());
    assertTrue(refs.contains(JsniJavaRef.parse("@com.hello.client.A$B::getNumber()")));
    assertTrue(refs.contains(JsniJavaRef.parse("@com.hello.client.JsniParserTest::getSum(II)")));
    assertTrue(refs.contains(JsniJavaRef.parse("@com.hello.client.JsniParserTest::counter")));

    // Verify the problems
    List<GWTJavaProblem> problems = result.getProblems();
    assertEquals(1, problems.size());
    GWTJavaProblem problem = problems.get(0);
    assertEquals(GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE,
        problem.getProblemType());
    IRegion expectedProblemRegion = RegionConverter.convertWindowsRegion(184, 0, testClass.getContents());
    assertEquals(expectedProblemRegion.getOffset(), problem.getSourceStart());
  }

  public void testParseString() throws Exception {
    String jsniMethod = createString(new String[] {
        "public native void jsniMethod()/*-{",
        "    // References to some Java types",
        "    var num = obj.@com.hello.client.A$B::getNumber()();",
        "    num += obj.@com.hello.client.RefactorTest::getNumber(IJ)(2);",
        "    num += obj.@com.hello.client.JsniParserTest::counter;", "  }-*/;"});

    JsBlock js = JsniParser.parse(jsniMethod);
    assertNotNull(js);
  }

  public void testParseStringJavaScriptError() throws Exception {
    String jsniMethod = createString(new String[] {
        "public native void jsniMethod()/*-{",
        "    // References to some Java types", "    *** // Syntax error",
        "  }-*/;"});

    try {
      JsniParser.parse(jsniMethod);
      fail("Expected JavaScriptParseException");
    } catch (JavaScriptParseException e) {
      assertEquals("Syntax error", e.getMessage());
      IRegion expectedErrorRegion = RegionConverter.convertWindowsRegion(79, 0, jsniMethod);
      assertEquals(expectedErrorRegion.getOffset(), e.getOffset());
    }
  }

  public void testParseStringMissingBody() throws Exception {
    assertNull(JsniParser.parse("public native void jsniMethod();"));
  }

  @Override
  protected TestClass[] getTestClasses() {
    String[] lines = new String[]{
        "package com.hello.client;",
        "",
        "public class JsniParserTest {",
        "",
        "  private int counter;",
        "",
        "  public native void jsniMethod()/*-{",
        "    // References to some Java types",
        "    var num = obj.@com.hello.client.A$B::getNumber()();",
        "    num += obj.@com.hello.client.JsniParserTest::getSum(II)(2, 2);",
        "    num += obj.@com.hello.client.JsniParserTest::counter;",
        "  }-*/;",
        "",
        "  public static int getSum(int op1, int op2) {",
        "    return op1 + op2; ",
        "  }",
        "",
        "}"
        };

    testClass = new TestClass(lines, "JsniParserTest");
    testClassSource = createString(lines);
    return new TestClass[] {testClass};
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

  private IDocument getTestClassDocument() {
    IDocument document = new Document(testClassSource);
    new GWTDocumentSetupParticipant().setup(document);
    return document;
  }

}
