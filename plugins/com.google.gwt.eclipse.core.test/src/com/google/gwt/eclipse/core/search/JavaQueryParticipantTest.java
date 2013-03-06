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
package com.google.gwt.eclipse.core.search;

import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;
import com.google.gwt.eclipse.core.test.RegionConverter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.jface.text.IRegion;
import org.eclipse.search.ui.text.Match;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for the {@link JavaQueryParticipant} class.
 */
public class JavaQueryParticipantTest extends AbstractGWTPluginTestCase {

  private class TestMatch extends Match {
    public TestMatch(IRegion region) {
      // All matches will be inside JSNI method in test class
      super(getJsniMethod(), region.getOffset(), region.getLength());
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Match)) {
        return false;
      }

      // We only care about matching the offset/length; we can ignore the
      // container element (see comment in constructor)
      Match other = (Match) obj;
      return (this.getOffset() == other.getOffset() && this.getLength() == other.getLength());
    }

    @Override
    public int hashCode() {
      return 37 * getOffset() + getLength();
    }
  }

  private static final Match[] NO_MATCHES = new Match[0];

  private static final String[] NO_PARAMS = new String[0];

  private static final IJavaSearchScope WORKSPACE_SCOPE = SearchEngine.createWorkspaceScope();

  private static QuerySpecification createQuery(IJavaElement element) {
    return new ElementQuerySpecification(element,
        IJavaSearchConstants.REFERENCES, WORKSPACE_SCOPE, "");
  }

  private static QuerySpecification createQuery(String pattern, int searchFor) {
    return new PatternQuerySpecification(pattern, searchFor, false,
        IJavaSearchConstants.REFERENCES, WORKSPACE_SCOPE, "");
  }

  /**
   * Cached references to our test classes.
   */
  private TestClass class1, class2;

  /**
   * Our custom GWT search participant.
   */
  private final JavaQueryParticipant gwtSearchEngine = new JavaQueryParticipant();

  public void testElementSearch() throws CoreException {
    IType cu1Type = getTestType1();
    IJavaElement element;

    // Search for type references
    element = cu1Type;
    Match[] expected = new Match[] {
        createWindowsTestMatch(665, 41), createWindowsTestMatch(735, 41),
        createWindowsTestMatch(840, 41), createWindowsTestMatch(947, 41),
        createWindowsTestMatch(1125, 41), createWindowsTestMatch(1207, 41),
        createWindowsTestMatch(1297, 41), createWindowsTestMatch(1419, 41),
        createWindowsTestMatch(1545, 41), createWindowsTestMatch(1619, 41)};
    assertSearchMatches(expected, createQuery(element));

    // Search for field references
    element = cu1Type.getField("keith");
    assertSearchMatch(createWindowsTestMatch(990, 5), createQuery(element));

    // Search for method references
    element = cu1Type.getMethod("getNumber", NO_PARAMS);
    assertSearchMatch(createWindowsTestMatch(1588, 9), createQuery(element));

    // Search for constructor references
    element = cu1Type.getType("InnerSub").getMethod("InnerSub",
        new String[] {"QString;"});
    assertSearchMatch(createWindowsTestMatch(892, 3), createQuery(element));

    // Search for package references (unsupported)
    element = cu1Type.getPackageFragment();
    assertSearchMatches(NO_MATCHES, createQuery(element));
  }

  public void testGetUIParticipant() {
    assertNull(new JavaQueryParticipant().getUIParticipant());
  }

  public void testInheritedMemberSearch() throws CoreException {
    IJavaElement getName = getTestType1().getType("Inner").getMethod("getName",
        NO_PARAMS);
    assertSearchMatch(createWindowsTestMatch(1471, 7), createQuery(getName));
  }

  public void testLimitTo() throws CoreException {
    IJavaElement element = getTestType1().getField("keith");
    QuerySpecification query;

    // Limit to: references
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.REFERENCES, WORKSPACE_SCOPE, "");
    assertSearchMatch(createWindowsTestMatch(990, 5), query);

    // Limit to: all occurrences (declaration and all references, although in
    // this case we're only using our search engine so we'll only have refs)
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.ALL_OCCURRENCES, WORKSPACE_SCOPE, "");
    assertSearchMatch(createWindowsTestMatch(990, 5), query);

    // Limit to: read accesses (we don't differentiate between read/write
    // accesses, so this returns all references)
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.READ_ACCESSES, WORKSPACE_SCOPE, "");
    assertSearchMatch(createWindowsTestMatch(990, 5), query);

    // Limit to: write accesses (we don't differentiate between read/write
    // accesses, so this returns all references)
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.WRITE_ACCESSES, WORKSPACE_SCOPE, "");
    assertSearchMatch(createWindowsTestMatch(990, 5), query);

    // Limit to: declarations (unsupported)
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.DECLARATIONS, WORKSPACE_SCOPE, "");
    assertSearchMatches(NO_MATCHES, query);

    // Limit to: implementors (unsupported)
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.IMPLEMENTORS, WORKSPACE_SCOPE, "");
    assertSearchMatches(NO_MATCHES, query);
  }

  public void testOverloadedMethodSearch() throws CoreException {
    // Find the overload with one int parameter
    IJavaElement getNumberIntParam = getTestType1().getMethod("getNumber",
        new String[] {"I"});
    assertSearchMatch(createWindowsTestMatch(1662, 9),
        createQuery(getNumberIntParam));

    // Find the overload with no parameters
    IJavaElement getNumberNoParams = getTestType1().getMethod("getNumber",
        NO_PARAMS);
    assertSearchMatch(createWindowsTestMatch(1588, 9),
        createQuery(getNumberNoParams));
  }

  public void testParamTypeSearch() throws CoreException {
    // Search for JSNI reference param type
    Match[] expected = new Match[] {
        createWindowsTestMatch(789, 16), createWindowsTestMatch(897, 16)};
    assertSearchMatches(expected, createQuery("java.lang.String",
        IJavaSearchConstants.TYPE));
  }

  public void testPatternSearch() throws CoreException {
    Match[] expected;

    // Search for type references by simple name
    expected = new Match[] {
        createWindowsTestMatch(840, 50), createWindowsTestMatch(1207, 50),
        createWindowsTestMatch(1419, 50)};
    assertSearchMatches(expected, createQuery("InnerSub",
        IJavaSearchConstants.TYPE));

    // Search for type with different casing
    expected = new Match[] {
        createWindowsTestMatch(840, 50), createWindowsTestMatch(1207, 50),
        createWindowsTestMatch(1419, 50)};
    assertSearchMatches(expected, createQuery("innersub",
        IJavaSearchConstants.TYPE));

    // Search for type with different casing with case-sensitive enabled
    QuerySpecification query = new PatternQuerySpecification("innersub",
        IJavaSearchConstants.TYPE, true, IJavaSearchConstants.REFERENCES,
        WORKSPACE_SCOPE, "");
    assertSearchMatches(NO_MATCHES, query);

    // Search for field references
    assertSearchMatch(createWindowsTestMatch(990, 5), createQuery("keith",
        IJavaSearchConstants.FIELD));

    // Search for method references using * wildcard
    expected = new Match[] {
        createWindowsTestMatch(1174, 5), createWindowsTestMatch(1259, 5),
        createWindowsTestMatch(1340, 8)};
    assertSearchMatches(expected, createQuery("sayH*",
        IJavaSearchConstants.METHOD));

    // Search for method references using ? wildcard
    expected = new Match[] {
        createWindowsTestMatch(1174, 5), createWindowsTestMatch(1259, 5)};
    assertSearchMatches(expected, createQuery("sayH?",
        IJavaSearchConstants.METHOD));

    // Search for constructor references with qualified type name and parameters
    assertSearchMatch(createWindowsTestMatch(892, 3), createQuery(
        "com.hello.client.JavaQueryParticipantTest.InnerSub.InnerSub(String)",
        IJavaSearchConstants.CONSTRUCTOR));
  }

  public void testScope() throws CoreException {
    IJavaElement element = getType2().getMethod("getNumber", NO_PARAMS);
    QuerySpecification query;
    IJavaSearchScope scope;

    // Scope: workspace
    scope = SearchEngine.createWorkspaceScope();
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.REFERENCES, scope, "");
    assertSearchMatch(createWindowsTestMatch(1039, 9), query);

    // Scope: project
    scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {getTestProject()});
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.REFERENCES, scope, "");
    assertSearchMatch(createWindowsTestMatch(1039, 9), query);

    // Scope: Compilation unit (the one containing the reference)
    scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {class1.getCompilationUnit()});
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.REFERENCES, scope, "");
    assertSearchMatch(createWindowsTestMatch(1039, 9), query);

    // Scope: Compilation unit (the one without any reference)
    scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {class2.getCompilationUnit()});
    query = new ElementQuerySpecification(element,
        IJavaSearchConstants.REFERENCES, scope, "");
    assertSearchMatches(NO_MATCHES, query);
  }

  @Override
  protected TestClass[] getTestClasses() {
    String[] class1Source = new String[] {
        "package com.hello.client;",
        "",
        "public class JavaQueryParticipantTest {",
        "  private static class Inner {",
        "    private String name;",
        "",
        "    public Inner(String name) {",
        "      this.name = name;",
        "    }",
        "",
        "    public void sayHi(int[] args) {",
        "      System.out.println(name);",
        "    }",
        "",
        "    protected String getName() {",
        "      return name;",
        "    }",
        "  }",
        "  private static class InnerSub extends Inner {",
        "    public InnerSub(String name) {",
        "      super(name);",
        "    }",
        "",
        "    @Override",
        "    public void sayHi(int[] args) {",
        "      super.sayHi(args);",
        "    }",
        "  }",
        "",
        "  private static native void jsniMethod()/*-{",
        "   // References to types, fields, methods, and ctors",
        "    var obj = @com.hello.client.JavaQueryParticipantTest::new()();",
        "    var inner = @com.hello.client.JavaQueryParticipantTest$Inner::new(Ljava/lang/String;)('George');",
        "    var innerSub = @com.hello.client.JavaQueryParticipantTest$InnerSub::new(Ljava/lang/String;)('Jetson');",
        "    var val = obj.@com.hello.client.JavaQueryParticipantTest::keith;",
        "    var num = obj.@com.hello.client.A$B::getNumber()();",
        "    ",
        "    // References to methods that start with 'sayH'",
        "    inner.@com.hello.client.JavaQueryParticipantTest$Inner::sayHi([I)([2,3]);",
        "    innerSub.@com.hello.client.JavaQueryParticipantTest$InnerSub::sayHi([I)([3,4]);",
        "    var msg = obj.@com.hello.client.JavaQueryParticipantTest::sayHello()();",
        "    ",
        "    // Reference to inherited method",
        "    var name = obj.@com.hello.client.JavaQueryParticipantTest$InnerSub::getName()();",
        "    ",
        "    // Reference to overloaded method",
        "    num = obj.@com.hello.client.JavaQueryParticipantTest::getNumber()();",
        "    num = obj.@com.hello.client.JavaQueryParticipantTest::getNumber(I)(1);",
        "  }-*/;",
        "",
        "  private String keith;",
        "",
        "  private String notReferenced;",
        "",
        "  private int getNumber() {",
        "    return 777;",
        "  }",
        "",
        "  private int getNumber(int val) {",
        "    return val;", 
        "  }",
        "",
        "private int getNumber(long val) {",
        "  return 1;",
        "}",
        "",
        "  private String sayHello() {",
        "    return \"Hi!\";",
        "  }",
        "}"
        };

    String[] class2Source = new String[] {
        "package com.hello.client;", 
        "", 
        "public class A {",
        "  public static class B {", 
        "    public static int getNumber() {",
        "      return 7;",
        "    }", 
        "  }", 
        "}"
        };

    // Cache references to the added test classes
    class1 = new TestClass(class1Source, "JavaQueryParticipantTest");
    class2 = new TestClass(class2Source, "A");

    return new TestClass[] {class1, class2};
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

  private void assertSearchMatch(Match expected, QuerySpecification query)
      throws CoreException {
    assertSearchMatches(new Match[] {expected}, query);
  }

  private void assertSearchMatches(Match[] expected, QuerySpecification query)
      throws CoreException {
    List<Match> results = search(query);
    assertEquals(expected.length, results.size());

    for (Match expectedMatch : expected) {
      assertTrue(results.contains(expectedMatch));
    }
  }

  private TestMatch createWindowsTestMatch(int offset, int length) {
    return new TestMatch(RegionConverter.convertWindowsRegion(offset, length,
        class1.getContents()));
  }

  private IMethod getJsniMethod() {
    return getTestType1().getMethod("jsniMethod", NO_PARAMS);
  }

  private IType getTestType1() {
    return class1.getCompilationUnit().getType("JavaQueryParticipantTest");
  }

  private IType getType2() {
    return class2.getCompilationUnit().getType("A").getType("B");
  }

  private List<Match> search(QuerySpecification query) throws CoreException {
    final List<Match> matches = new ArrayList<Match>();

    ISearchRequestor requestor = new ISearchRequestor() {
      public void reportMatch(Match match) {
        matches.add(match);
      }
    };

    gwtSearchEngine.search(requestor, query, new NullProgressMonitor());
    return matches;
  }

}
