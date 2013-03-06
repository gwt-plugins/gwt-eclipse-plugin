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
package com.google.appengine.eclipse.core.validators.java;

import com.google.appengine.eclipse.core.markers.AppEngineJavaProblem;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkTestUtilities;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.sdk.SdkSet;

import junit.framework.TestCase;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Tests for the {@link GaeChecker} class.
 */
public class GaeCheckerTest extends TestCase {

  private class SimpleTestHelper {
    private final ASTNode ast;
    private final String unitName;
    private final IJavaProject javaProject;

    private SimpleTestHelper(boolean forceResolvedClassPath)
        throws CoreException {
      GaeProject gaeProject = PluginTestUtils.createGaeProject("TestProject");
      javaProject = gaeProject.getJavaProject();
      javaProject.setOption(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);

      if (forceResolvedClassPath) {
        // Force the classpath containers to be resolved
        javaProject.getResolvedClasspath(false);
      }

      ASTParser astParser = ASTParser.newParser(AST.JLS3);
      astParser.setProject(javaProject);
      astParser.setSource(getTestSource());
      astParser.setResolveBindings(true);
      unitName = "/" + javaProject.getResource().getName() + "/MyFile.java";
      astParser.setUnitName(unitName);
      ast = astParser.createAST(new NullProgressMonitor());
    }

    private void delete() throws CoreException {
      javaProject.getProject().delete(true, true, new NullProgressMonitor());
    }

    private char[] getTestSource() {
      StringBuilder sb = new StringBuilder();
      // Imports should not result in errors
      sb.append("import java.awt.Window;\n");
      sb.append("import java.io.IOException;\n");
      sb.append("import java.net.CookieHandler;\n");
      sb.append("import javax.naming.AuthenticationException;\n");
      sb.append("import com.google.appengine.repackaged.*;\n");
      sb.append("import com.google.appengine.repackaged.com.google.common.util.Base64;\n");
      sb.append("\n");
      // Superclass is not supported
      sb.append("public class MyWindow extends Window {\n");
      sb.append("  @interface MyAnnotation {\n");
      sb.append("   Class<?> value();\n");
      sb.append("  }\n");
      sb.append("\n");
      // Annotation value is not supported
      sb.append("  @MyAnnotation(Window.class)\n");
      sb.append("  static class MyAnnotatedClass {\n");
      sb.append("  }\n");
      sb.append("\n");
      // Field type is not supported
      sb.append("  private final Window otherWindow;\n");
      sb.append("\n");
      // Static field reference on an unsupported type
      sb.append("  private final int status = Window.OPENED;\n");
      sb.append("\n");
      // Class literal reference for an unsupported type
      sb.append("  private final Class clazz = Window.class;\n");
      sb.append("\n");
      sb.append("  MyFile() {\n");
      sb.append("    super(\"\");\n");
      sb.append("    otherWindow = null;\n");
      // Implicit reference to static field on supported super type
      sb.append("    int s = OPENED;\n");
      // Static method call on an unsupported type
      sb.append("    try {\n");
      sb.append("      Window.updateChildFocusableWindowState(null);\n");
      sb.append("    } catch (Throwable e) {\n");
      sb.append("    }\n");
      sb.append("  }\n");
      sb.append("\n");
      // Return type is unsupported, and throws unsupported type
      sb.append("  public Window getWindow() throws AuthenticationException {\n");
      // Throw unsupported type
      sb.append("    throw new AuthenticationException();\n");
      sb.append("  }\n");
      sb.append("\n");
      sb.append("  public void testGenericTypes() {\n");
      // Ensure that raw or parameterized inner types don't fool the validation
      sb.append("    Map.Entry x = new Map.Entry<Integer,Integer>();\n");
      sb.append("  }\n");
      sb.append("}\n");

      return sb.toString().toCharArray();
    }
  }

  /**
   * {@link Comparator} for {@link CategorizedProblem}s.
   * 
   * NOTE: The source start and end offsets are ignored to avoid having to
   * compute them exactly.
   */
  private static final Comparator<CategorizedProblem> CATEGORIZED_PROBLEM_COMPARATOR = new Comparator<CategorizedProblem>() {
    public int compare(CategorizedProblem o1, CategorizedProblem o2) {
      if (o1.getCategoryID() != o2.getCategoryID()) {
        return o1.getCategoryID() - o2.getCategoryID();
      }

      if (o1.getID() != o2.getID()) {
        return o1.getID() - o2.getID();
      }

      if (!o1.getMarkerType().equals(o2.getMarkerType())) {
        return o1.getMarkerType().compareTo(o2.getMarkerType());
      }

      if (!o1.getMessage().equals(o2.getMessage())) {
        return o1.getMessage().compareTo(o2.getMessage());
      }

      if (o1.getSourceLineNumber() != o2.getSourceLineNumber()) {
        return o1.getSourceLineNumber() - o2.getSourceLineNumber();
      }

      return 0;
    }
  };

  private static void assertProblemsEqual(
      List<? extends CategorizedProblem> expected,
      List<? extends CategorizedProblem> actual) {
    TreeSet<CategorizedProblem> expectedSet = new TreeSet<CategorizedProblem>(
        CATEGORIZED_PROBLEM_COMPARATOR);
    expectedSet.addAll(expected);

    TreeSet<CategorizedProblem> actualSet = new TreeSet<CategorizedProblem>(
        CATEGORIZED_PROBLEM_COMPARATOR);
    actualSet.addAll(actual);

    assertTrue(
        "Sets not equal. Expected=\n" + toString(expected) + ", \nactual=\n"
            + toString(actual),
        expectedSet.containsAll(actualSet)
            && actualSet.containsAll(expectedSet));
  }

  /**
   * Note that this method only fills in line numbers for the expected
   * categorized problems. Start offset, end offset, and columns are not used to
   * make the test less brittle.
   */
  private static List<? extends CategorizedProblem> getExpectedImportProblems(
      String fileName) {
    ArrayList<CategorizedProblem> problems = new ArrayList<CategorizedProblem>();
    problems.add(AppEngineJavaProblem.createRepackagedImportError(fileName,
        GdtProblemSeverity.ERROR, -1, -1, 5, -1));
    problems.add(AppEngineJavaProblem.createRepackagedImportError(fileName,
        GdtProblemSeverity.ERROR, -1, -1, 6, -1));
    return problems;
  }

  /**
   * Note that this method only fills in line numbers for the expected
   * categorized problems. Start offset, end offset, and columns are not used to
   * make the test less brittle.
   */
  private static List<? extends CategorizedProblem> getExpectedProblems(
      String fileName) {
    ArrayList<CategorizedProblem> problems = new ArrayList<CategorizedProblem>();
    problems.add(AppEngineJavaProblem.createUnsupportedTypeError(fileName,
        "java.awt.Window", GdtProblemSeverity.ERROR, -1, -1, 8, -1));
    problems.add(AppEngineJavaProblem.createUnsupportedTypeError(fileName,
        "java.awt.Window", GdtProblemSeverity.ERROR, -1, -1, 13, -1));
    problems.add(AppEngineJavaProblem.createUnsupportedTypeError(fileName,
        "java.awt.Window", GdtProblemSeverity.ERROR, -1, -1, 17, -1));
    problems.add(AppEngineJavaProblem.createUnsupportedTypeError(fileName,
        "java.awt.Window", GdtProblemSeverity.ERROR, -1, -1, 19, -1));
    problems.add(AppEngineJavaProblem.createUnsupportedTypeError(fileName,
        "java.awt.Window", GdtProblemSeverity.ERROR, -1, -1, 21, -1));
    problems.add(AppEngineJavaProblem.createUnsupportedTypeError(fileName,
        "java.awt.Window", GdtProblemSeverity.ERROR, -1, -1, 28, -1));
    problems.add(AppEngineJavaProblem.createUnsupportedTypeError(fileName,
        "java.awt.Window", GdtProblemSeverity.ERROR, -1, -1, 33, -1));
    problems.add(AppEngineJavaProblem.createUnsupportedTypeError(fileName,
        "javax.naming.AuthenticationException", GdtProblemSeverity.ERROR, -1,
        -1, 33, -1));
    problems.add(AppEngineJavaProblem.createUnsupportedTypeError(fileName,
        "javax.naming.AuthenticationException", GdtProblemSeverity.ERROR, -1,
        -1, 34, -1));
    problems.addAll(getExpectedImportProblems(fileName));
    return problems;
  }

  private static String toString(List<? extends CategorizedProblem> problems) {
    StringBuilder sb = new StringBuilder();
    for (CategorizedProblem problem : problems) {
      sb.append("{\n");
      sb.append("  categoryId   : " + problem.getCategoryID() + "\n");
      sb.append("  id           : " + problem.getID() + "\n");
      sb.append("  markerType   : " + problem.getMarkerType() + "\n");
      sb.append("  message      : " + problem.getMessage() + "\n");
      sb.append("  sourceEnd    : " + problem.getSourceEnd() + "\n");
      sb.append("  sourceLine   : " + problem.getSourceLineNumber() + "\n");
      sb.append("  sourceStart  : " + problem.getSourceStart() + "\n");
      sb.append("}\n");
      sb.append(", ");
    }
    return sb.toString();
  }

  /**
   * Test method for {@link GaeChecker#check(CompilationUnit, IJavaProject)}.
   */
  public void testCheck() throws CoreException {
    SimpleTestHelper testHelper = new SimpleTestHelper(true);

    List<? extends CategorizedProblem> expected = getExpectedProblems(testHelper.unitName);
    List<? extends CategorizedProblem> actual = GaeChecker.check(
        (CompilationUnit) testHelper.ast, testHelper.javaProject);
    assertProblemsEqual(expected, actual);

    testHelper.delete();
  }

  /**
   * Test method for {@link GaeChecker#check(CompilationUnit, IJavaProject)}
   * when there is not a valid GAE SDK.
   */
  public void testCheckWithInvalidSdk() throws CoreException {
    IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    assertNotNull(workspacePath);
    GaeSdk sdk = GaeSdk.getFactory().newInstance("Invalid GAE SDK",
        workspacePath.append("invalid-sdk"));

    SdkSet<GaeSdk> sdks = GaePreferences.getSdks();
    sdks.clear();
    sdks.add(sdk);
    GaePreferences.setSdks(sdks);

    // Don't resolve classpath since the above path isn't real
    SimpleTestHelper testHelper = new SimpleTestHelper(false);
    List<? extends CategorizedProblem> expected = Collections.emptyList();
    List<? extends CategorizedProblem> actual = GaeChecker.check(
        (CompilationUnit) testHelper.ast, testHelper.javaProject);
    assertProblemsEqual(expected, actual);

    testHelper.delete();
    // Reset back to empty
    GaePreferences.getSdks().clear();
  }

  @Override
  protected void setUp() throws Exception {
    GaeSdkTestUtilities.addDefaultSdk();
  }

  @Override
  protected void tearDown() throws Exception {
    PluginTestUtils.removeDefaultGaeSdk();
  }

}
