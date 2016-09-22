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

import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.markers.GWTProblemType;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 * Tests the {@link JavaValidationResult} class.
 */
public class JavaValidationResultTest extends TestCase {

  private static final JsniJavaRef CTOR_REF = JsniJavaRef.parse("@some.package.SomeClass::new()");
  private static final JsniJavaRef FIELD_REF = JsniJavaRef.parse("@some.package.SomeClass::someField");
  private static final JsniJavaRef METHOD_REF = JsniJavaRef.parse("@some.package.SomeClass::someMeth()");
  private static final JsniJavaRef METHOD_WITH_PARAMS_REF = JsniJavaRef.parse("@some.package.SomeClass::someMeth([[ZBCDFIJLjava/lang/String;S)");
  private static final JsniJavaRef[] REFS = new JsniJavaRef[] {
      FIELD_REF, METHOD_REF, METHOD_WITH_PARAMS_REF, CTOR_REF};

  private static final String filename = "Class1.java";

  private static final GWTJavaProblem JSNI_PARSE_PROBLEM = GWTJavaProblem.create(
      filename, 30, 10, 3, 0, GWTProblemType.JSNI_PARSE_ERROR,
      GdtProblemSeverity.ERROR, "Syntax error");
  private static final GWTJavaProblem[] PROBLEMS = new GWTJavaProblem[] {JSNI_PARSE_PROBLEM};

  private JavaValidationResult result;

  public void testAddAllJavaRefs() {
    JsniJavaRef[] refs = new JsniJavaRef[] {
        JsniJavaRef.parse("@some.package.SomeClass$Inner::someField"),
        JsniJavaRef.parse("@some.package.SomeClass$Inner::someMethod()")};

    result.addAllJavaRefs(Arrays.asList(refs));
    List<JsniJavaRef> resultRefs = result.getJavaRefs();
    for (JsniJavaRef ref : refs) {
      assertTrue(resultRefs.contains(ref));
    }
  }

  public void testAddAllProblems() {
    GWTJavaProblem[] problems = new GWTJavaProblem[] {
        GWTJavaProblem.create(filename, 0, 10, 1, 0,
            GWTProblemType.JSNI_PARSE_ERROR, GdtProblemSeverity.ERROR,
            new String[] {"Error1"}),
        GWTJavaProblem.create(filename, 0, 10, 1, 0,
            GWTProblemType.JSNI_PARSE_ERROR, GdtProblemSeverity.ERROR,
            new String[] {"Error2"})};

    result.addAllProblems(Arrays.asList(problems));
    List<GWTJavaProblem> resultProblems = result.getProblems();
    for (GWTJavaProblem problem : problems) {
      assertTrue(resultProblems.contains(problem));
    }
  }

  public void testAddJavaRef() {
    JsniJavaRef ref = JsniJavaRef.parse("@some.package.SomeClass$Inner::someField");
    result.addJavaRef(ref);
    assertTrue(result.getJavaRefs().contains(ref));
  }

  public void testAddProblem() {
    GWTJavaProblem problem = GWTJavaProblem.create(filename, 0, 10, 1, 0,
        GWTProblemType.JSNI_PARSE_ERROR, GdtProblemSeverity.ERROR,
        new String[0]);
    result.addProblem(problem);
    assertTrue(result.getProblems().contains(problem));
  }

  public void testGetJavaRefs() {
    List<JsniJavaRef> resultRefs = result.getJavaRefs();
    for (JsniJavaRef ref : REFS) {
      assertTrue(resultRefs.contains(ref));
    }
  }

  public void testGetProblems() {
    List<GWTJavaProblem> resultProblems = result.getProblems();
    for (GWTJavaProblem problem : PROBLEMS) {
      assertTrue(resultProblems.contains(problem));
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    result = new JavaValidationResult();
    result.addAllJavaRefs(Arrays.asList(REFS));
    result.addAllProblems(Arrays.asList(PROBLEMS));
  }

}
