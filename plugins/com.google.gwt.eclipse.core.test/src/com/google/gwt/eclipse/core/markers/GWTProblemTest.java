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
package com.google.gwt.eclipse.core.markers;

import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;

import junit.framework.TestCase;

/**
 * Tests the {@link GWTJavaProblem} class.
 */
public class GWTProblemTest extends TestCase {

  private static final String filename = "Class1.java";

  private static final GWTJavaProblem JSNI_PARSE_PROBLEM = GWTJavaProblem.create(
      filename, 30, 10, 3, 0, GWTProblemType.JSNI_PARSE_ERROR,
      GdtProblemSeverity.WARNING, new String[] {"Syntax error"});
  private static final GWTJavaProblem UNRESOLVED_TYPE_PROBLEM = GWTJavaProblem.create(
      filename, 0, 10, 1, 0, GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE,
      GdtProblemSeverity.ERROR, new String[] {"MissingClass"});
  private static final GWTJavaProblem MISSING_METHOD_PROBLEM = GWTJavaProblem.create(
      filename, 20, 5, 2, 0, GWTProblemType.JSNI_JAVA_REF_MISSING_METHOD,
      GdtProblemSeverity.ERROR, new String[] {"Class", "MissingMethod"});

  public void testGetProblemType() {
    assertEquals(GWTProblemType.JSNI_PARSE_ERROR,
        JSNI_PARSE_PROBLEM.getProblemType());
    assertEquals(GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE,
        UNRESOLVED_TYPE_PROBLEM.getProblemType());
    assertEquals(GWTProblemType.JSNI_JAVA_REF_MISSING_METHOD,
        MISSING_METHOD_PROBLEM.getProblemType());
  }

  public void testGetSeverity() {
    assertEquals(GdtProblemSeverity.WARNING, JSNI_PARSE_PROBLEM.getSeverity());
    assertEquals(GdtProblemSeverity.ERROR,
        UNRESOLVED_TYPE_PROBLEM.getSeverity());
    assertEquals(GdtProblemSeverity.ERROR, MISSING_METHOD_PROBLEM.getSeverity());
  }

  public void testIsError() {
    assertFalse(JSNI_PARSE_PROBLEM.isError());
    assertTrue(UNRESOLVED_TYPE_PROBLEM.isError());
    assertTrue(MISSING_METHOD_PROBLEM.isError());
  }

  public void testIsWarning() {
    assertTrue(JSNI_PARSE_PROBLEM.isWarning());
    assertFalse(UNRESOLVED_TYPE_PROBLEM.isWarning());
    assertFalse(MISSING_METHOD_PROBLEM.isWarning());
  }

}
