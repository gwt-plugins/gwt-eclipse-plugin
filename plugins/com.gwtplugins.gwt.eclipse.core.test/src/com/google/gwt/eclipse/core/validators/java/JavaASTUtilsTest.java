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

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.markers.GWTProblemType;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;
import com.google.gwt.eclipse.core.test.RegionConverter;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.IRegion;

/**
 * Tests the {@link JavaASTUtils} class.
 * 
 * TODO: move to gdt.test project and remove GWT-specific testing.
 */
public class JavaASTUtilsTest extends AbstractGWTPluginTestCase {

  private ASTNode ast;

  private TestClass testClass;

  private String[] testClassSource;

  public void testCreateProblem() {
    String message = "public is bad";
    IRegion problemRegion = RegionConverter.convertWindowsRegion(29, 6,
        JavaASTUtils.getSource(ast));
    GWTJavaProblem problem = GWTJavaProblem.create(ast,
        problemRegion.getOffset(), problemRegion.getLength(),
        GWTProblemType.JSNI_PARSE_ERROR, message);
    assertEquals(1, problem.getSourceLineNumber());
    assertEquals(testClass.getCompilationUnit().getResource().getName(),
        new String(problem.getOriginatingFileName()));
    assertEquals(GWTProblemType.JSNI_PARSE_ERROR, problem.getProblemType());
    assertTrue(problem.getMessage().contains(message));
    assertEquals(problemRegion.getOffset(), problem.getSourceStart());
    assertEquals(problemRegion.getOffset() + problemRegion.getLength() - 1,
        problem.getSourceEnd());
  }

  public void testGetCompilationUnit() {
    assertEquals(testClass.getCompilationUnit(),
        JavaASTUtils.getCompilationUnit(ast));
  }

  public void testGetSource() {
    String expectedSource = createString(testClassSource);
    String actualSource = JavaASTUtils.getSource(ast);
    assertEquals(expectedSource, actualSource);

    CompilationUnit root = (CompilationUnit) ast;
    TypeDeclaration typeDecl = (TypeDeclaration) root.types().get(0);
    TypeDeclaration innerTypeDecl = typeDecl.getTypes()[0];
    MethodDeclaration getNumberDecl = innerTypeDecl.getMethods()[0];

    String expectedMethodSource = createString(new String[] {
        "public static int getNumber() {", "      return 777;", "    }"});
    String actualMethodSource = JavaASTUtils.getSource(getNumberDecl);
    assertEquals(expectedMethodSource, actualMethodSource);
  }

  @Override
  protected TestClass[] getTestClasses() {
    testClassSource = new String[] {
        "package com.hello.client;", "", "public class JavaASTUtilsTest {",
        "  public static class Inner {", "    public static int getNumber() {",
        "      return 777;", "    }", "  }", "}"};

    testClass = new TestClass(testClassSource, "JavaASTUtilsTest");
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
    parser.setSource(testClass.getCompilationUnit());
    ast = parser.createAST(null);
  }

}
