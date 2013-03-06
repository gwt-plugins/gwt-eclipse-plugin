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
package com.google.gwt.eclipse.core.editors.java;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.pde.BundleUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.resources.GWTImages;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Test cases for {@link JsniMethodBodyCompletionProposalComputer}.
 */
@SuppressWarnings("restriction")
public class JsniMethodBodyCompletionProposalComputerTest extends TestCase {
  /**
   * Images that the JSNI completion proposal computer depends on.
   */
  private static final String IMAGE_IDS[] = new String[] {
      GWTImages.JSNI_DEFAULT_METHOD_SMALL, GWTImages.JSNI_PRIVATE_METHOD_SMALL,
      GWTImages.JSNI_PROTECTED_METHOD_SMALL, GWTImages.JSNI_PUBLIC_METHOD_SMALL};

  private static void assertExpectedProposals(List<String> expectedCompletions,
      List<ICompletionProposal> actualCompletions, int numCharsToOverwrite) {
    Set<String> expectedSet = new TreeSet<String>(expectedCompletions);
    Set<String> actualSet = new TreeSet<String>();
    for (ICompletionProposal actualCompletion : actualCompletions) {
      assertTrue(actualCompletion instanceof JavaCompletionProposal);
      actualSet.add(((JavaCompletionProposal) actualCompletion).getReplacementString());
      assertEquals("Expected Overwrite: " + numCharsToOverwrite + 
          "Actual Overwrite: " + 
          ((JavaCompletionProposal) actualCompletion).getReplacementLength(),
          ((JavaCompletionProposal) actualCompletion).getReplacementLength(), numCharsToOverwrite);
    }

    assertTrue("Expected: " + expectedSet.toString() + "\nActual: "
        + actualSet.toString(), actualSet.containsAll(expectedSet)
        && expectedSet.containsAll(actualSet));
  }

  private static void assertNoProposals(IProgressMonitor monitor,
      JsniMethodBodyCompletionProposalComputer jcpc,
      CompilationUnitEditor cuEditor, ISourceViewer viewer, int offset) {
    assertEquals(0, jcpc.computeCompletionProposals(
        new JavaContentAssistInvocationContext(viewer, offset, cuEditor),
        monitor).size());
  }

  private static List<String> createJsniBlocks(IJavaProject javaProject,
      int indentationUnits, String... snippets) {
    List<String> blocks = new ArrayList<String>();
    for (String snippet : snippets) {
      String jsniBlock = JsniMethodBodyCompletionProposalComputer.createJsniBlock(
          javaProject, snippet, indentationUnits);
      blocks.add(jsniBlock);
    }

    return blocks;
  }

  private static String synthesizeProjectNameForThisTest(TestCase test) {
    return (test.getClass().getCanonicalName() + "." + test.getName()).replace(
        '.', '_');
  }
  
  // Validate all proposals for invocation index at end of second line.
  private static void validateExpectedProposals(IJavaProject javaProject,
      String fullyQualifiedClassName, String source,
      String... expectedProposals) throws CoreException, BadLocationException {
    validateExpectedProposals(javaProject, fullyQualifiedClassName, source, 1, 0,
        expectedProposals);
  }
  
  /**
   *  If length of line at 'lineNum' is 'len', then validate all proposals for invocation
   *  index varying from '(len - numCharsCompleted)' to 'len'.
   */
  private static void validateExpectedProposals(IJavaProject javaProject,
      String fullyQualifiedClassName, String source, int lineNum, int numCharsCompleted,
      String... expectedProposals) throws CoreException, BadLocationException {
    IProgressMonitor monitor = new NullProgressMonitor();

    ICompilationUnit iCompilationUnit = JavaProjectUtilities.createCompilationUnit(
        javaProject, fullyQualifiedClassName, source);
    CompilationUnitEditor cuEditor = (CompilationUnitEditor) EditorUtility.openInEditor(iCompilationUnit);

    ISourceViewer viewer = cuEditor.getViewer();
    IDocument document = viewer.getDocument();

    IRegion lineInformation = document.getLineInformation(lineNum);
    JsniMethodBodyCompletionProposalComputer jcpc = new JsniMethodBodyCompletionProposalComputer();
    
    for (int numCharsToOverwrite = 0; numCharsToOverwrite <= numCharsCompleted; 
        numCharsToOverwrite++){
      int invocationOffset = lineInformation.getOffset()
          + lineInformation.getLength() - numCharsToOverwrite;
      JavaContentAssistInvocationContext context = new JavaContentAssistInvocationContext(
          viewer, invocationOffset, cuEditor);
      List<ICompletionProposal> completions = jcpc.computeCompletionProposals(
          context, monitor);
  
      int indentationUnits = JsniMethodBodyCompletionProposalComputer.measureIndentationUnits(
          document, lineNum, lineInformation.getOffset(), javaProject);
      List<String> expected = createJsniBlocks(javaProject, indentationUnits,
          expectedProposals);
      for (int i = 0; i < expected.size(); i++){
        String expectedBlock = expected.get(i).substring(numCharsCompleted - numCharsToOverwrite);
        expected.set(i, expectedBlock);
      }
      assertExpectedProposals(expected, completions, numCharsToOverwrite);
    }
  }
  
  /**
   * Constructs source for testing JSNI method body completion
   * with partial braces and comments. An example of source with
   * comments on both sides and having partial brace is as follow.
   */
   // class A{
   // /* Global comment above with brackets () */
   // // Single line global comment ()
   // public native int bar()/*-
   // /* Global comment below with brackets () */
   // public  void bar1(){
   // /* A java method body */
   // }
   // public native void bar2()/*-{
   // /* A JSNI method body */
   // }-*/;
  
  private static int constructPartialBracesSource(String fullyQualifiedClassName, 
      StringBuilder source, int numCharsCompleted, Boolean isCommentAbove, 
      Boolean isCommentBelow) {
    
    // invocationLineNum is line number where auto-complete is to be tested. 
    int invocationLineNum = 1;
    String str = "/*-{".substring(0, numCharsCompleted) + "\n";
    
    source.append("class " + fullyQualifiedClassName + "{\n");
    
    if (isCommentAbove){
      source.append("  /* Global comment above with brackets () */\n");
      source.append("  // Single line global comment ()\n");
      invocationLineNum += 2;
    }
    
    // The line where auto-completion is tested.
    source.append("  public native int bar()");
    source.append(str);
    
    if (isCommentBelow){
      source.append("  /* Global comment below with brackets () */\n");
    }
    
    source.append("  public  void bar1(){\n");
    source.append("  /* A java method body */\n");
    source.append("  }\n");
    source.append("  public native void bar2()/*-{\n");
    source.append("  /* A JSNI method body */\n");
    source.append("  }-*/;\n");
    source.append("}\n");
    
    return invocationLineNum;
    
  }

  /**
   * Test that we do not generate any proposals for our boundary conditions.
   * 
   * @throws CoreException
   * @throws BadLocationException
   */
  public void testComputeCompletionProposalsNoProposals() throws CoreException,
      BadLocationException {
    IProgressMonitor monitor = new NullProgressMonitor();

    JsniMethodBodyCompletionProposalComputer jcpc = new JsniMethodBodyCompletionProposalComputer();
    IJavaProject javaProject = JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    StringBuilder source = new StringBuilder();
    source.append("class A {\n");

    // > 0 proposals if at the signature end.
    int methodALineNum = 1;
    source.append("  private native void a()\n");

    // No proposals; already has JSNI method body.
    int methodBLineNum = 2;
    source.append("  private native void b()/*-{}-*/;\n");

    // No proposals; signature ends in a semicolon.
    int methodCLineNum = 3;
    source.append("  private native void c();\n");

    ICompilationUnit iCompilationUnit = JavaProjectUtilities.createCompilationUnit(
        javaProject, "A", source.toString());
    CompilationUnitEditor cuEditor = (CompilationUnitEditor) EditorUtility.openInEditor(iCompilationUnit);

    ISourceViewer viewer = cuEditor.getViewer();
    IDocument document = viewer.getDocument();

    // No completions if offset is before the signature.
    IRegion methodALineInfo = document.getLineInformation(methodALineNum);
    assertNoProposals(monitor, jcpc, cuEditor, viewer,
        methodALineInfo.getOffset());

    // No completions if the offset is in the middle of method a() signature.
    assertNoProposals(monitor, jcpc, cuEditor, viewer,
        methodALineInfo.getOffset() + (methodALineInfo.getLength() / 2));

    // > 0 completions at end of the line.
    assertTrue(jcpc.computeCompletionProposals(
        new JavaContentAssistInvocationContext(viewer,
            methodALineInfo.getOffset() + methodALineInfo.getLength(), cuEditor),
        monitor).size() > 0);

    // No proposals for method B.
    IRegion methodBLineInfo = document.getLineInformation(methodBLineNum);
    assertNoProposals(monitor, jcpc, cuEditor, viewer,
        methodBLineInfo.getOffset() + methodBLineInfo.getLength());

    // No proposals for method C.
    IRegion methodCLineInfo = document.getLineInformation(methodCLineNum);
    assertNoProposals(monitor, jcpc, cuEditor, viewer,
        methodCLineInfo.getOffset() + methodCLineInfo.getLength());

    // No proposals for interfaces.
    StringBuilder sourceI = new StringBuilder();
    sourceI.append("interface B {\n");

    sourceI.append("  private native void d()\n");
    iCompilationUnit = JavaProjectUtilities.createCompilationUnit(javaProject,
        "B", sourceI.toString());
    cuEditor = (CompilationUnitEditor) EditorUtility.openInEditor(iCompilationUnit);

    viewer = cuEditor.getViewer();
    document = viewer.getDocument();

    int methodDLineNum = 1;
    IRegion methodDLineInfo = document.getLineInformation(methodDLineNum);
    // No completions if offset is before the signature.
    assertNoProposals(monitor, jcpc, cuEditor, viewer,
        methodDLineInfo.getOffset());

    // No completions after the signature either.
    assertNoProposals(monitor, jcpc, cuEditor, viewer,
        methodDLineInfo.getOffset() + methodDLineInfo.getLength());
  }

  public void testComputeCompletionsProposalsForGetters() throws CoreException,
      BadLocationException {
    IJavaProject javaProject = JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    StringBuilder aSource = new StringBuilder();
    aSource.append("class A {\n");
    aSource.append("  private native int getA()\n");
    aSource.append("}\n");

    validateExpectedProposals(javaProject, "A", aSource.toString(), "",
        "return this.getA();", "return this.a();", "return this.a;");

    StringBuilder aStaticSource = new StringBuilder();
    aStaticSource.append("class AStatic {\n");
    aStaticSource.append("  private static native int getA()\n");
    aStaticSource.append("}\n");

    validateExpectedProposals(javaProject, "AStatic", aStaticSource.toString(),
        "", "return $wnd.getA();", "return $wnd.a();", "return $wnd.a;");

    StringBuilder bSource = new StringBuilder();
    bSource.append("class B {\n");
    bSource.append("  private native int get()\n");
    bSource.append("}\n");

    validateExpectedProposals(javaProject, "B", bSource.toString(), "",
        "return this.get();");

    StringBuilder bStaticSource = new StringBuilder();
    bStaticSource.append("class BStatic {\n");
    bStaticSource.append("  private static native int get()\n");
    bStaticSource.append("}\n");

    validateExpectedProposals(javaProject, "BStatic", bStaticSource.toString(),
        "", "return $wnd.get();");
  }

  public void testComputeCompletionsProposalsForIndexedGetters()
      throws CoreException, BadLocationException {
    IJavaProject javaProject = JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    StringBuilder aSource = new StringBuilder();
    aSource.append("class A {\n");
    aSource.append("  private native int getA(int x)\n");
    aSource.append("}\n");

    validateExpectedProposals(javaProject, "A", aSource.toString(), "",
        "return this.getA(x);", "return this.a(x);", "return this.a[x];");

    StringBuilder aStaticSource = new StringBuilder();
    aStaticSource.append("class AStatic {\n");
    aStaticSource.append("  private static native int getA(int x)\n");
    aStaticSource.append("}\n");

    validateExpectedProposals(javaProject, "AStatic", aStaticSource.toString(),
        "", "return $wnd.getA(x);", "return $wnd.a(x);", "return $wnd.a[x];");

    StringBuilder bSource = new StringBuilder();
    bSource.append("class B {\n");
    bSource.append("  private native int get(int x)\n");
    bSource.append("}\n");

    validateExpectedProposals(javaProject, "B", bSource.toString(), "",
        "return this.get(x);", "return this[x];");

    StringBuilder bStaticSource = new StringBuilder();
    bStaticSource.append("class BStatic {\n");
    bStaticSource.append("  private static native int get(int x)\n");
    bSource.append("}\n");

    validateExpectedProposals(javaProject, "BStatic", bStaticSource.toString(),
        "", "return $wnd.get(x);", "return $wnd[x];");
  }

  public void testComputeCompletionsProposalsForIndexedSetters()
      throws CoreException, BadLocationException {
    IJavaProject javaProject = JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    StringBuilder aSource = new StringBuilder();
    aSource.append("class A {\n");
    aSource.append("  private native void setA(int x, int y)\n");
    aSource.append("}\n");

    validateExpectedProposals(javaProject, "A", aSource.toString(), "",
        "this.setA(x, y);", "this.a(x, y);", "this.a[x] = y;");

    StringBuilder aStaticSource = new StringBuilder();
    aStaticSource.append("class AStatic {\n");
    aStaticSource.append("  private static native void setA(int x, int y)\n");
    aStaticSource.append("}\n");

    validateExpectedProposals(javaProject, "AStatic", aStaticSource.toString(),
        "", "$wnd.setA(x, y);", "$wnd.a(x, y);", "$wnd.a[x] = y;");

    StringBuilder bSource = new StringBuilder();
    bSource.append("class B {\n");
    bSource.append("  private native void set(int x, int y)\n");
    bSource.append("}\n");

    validateExpectedProposals(javaProject, "B", bSource.toString(), "",
        "this.set(x, y);", "this[x] = y;");

    StringBuilder bStaticSource = new StringBuilder();
    bStaticSource.append("class BStatic {\n");
    bStaticSource.append("  private static native void set(int x, int y)\n");
    bStaticSource.append("}\n");

    validateExpectedProposals(javaProject, "BStatic", bStaticSource.toString(),
        "", "$wnd.set(x, y);", "$wnd[x] = y;");
  }

  public void testComputeCompletionsProposalsForSetters() throws CoreException,
      BadLocationException {
    IJavaProject javaProject = JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    StringBuilder aSource = new StringBuilder();
    aSource.append("class A {\n");
    aSource.append("  private native void setA(int x)\n");
    aSource.append("}\n");

    validateExpectedProposals(javaProject, "A", aSource.toString(), "",
        "this.setA(x);", "this.a(x);", "this.a = x;");

    StringBuilder aStaticSource = new StringBuilder();
    aStaticSource.append("class AStatic {\n");
    aStaticSource.append("  private static native void setA(int x)\n");
    aStaticSource.append("}\n");

    validateExpectedProposals(javaProject, "AStatic", aStaticSource.toString(),
        "", "$wnd.setA(x);", "$wnd.a(x);", "$wnd.a = x;");

    StringBuilder bSource = new StringBuilder();
    bSource.append("class B {\n");
    bSource.append("  private native void set(int x)\n");
    bSource.append("}\n");

    validateExpectedProposals(javaProject, "B", bSource.toString(), "",
        "this.set(x);");

    StringBuilder bStaticSource = new StringBuilder();
    bStaticSource.append("class B {\n");
    bStaticSource.append("  private static native void set(int x)\n");
    bStaticSource.append("}\n");

    validateExpectedProposals(javaProject, "BStatic", bStaticSource.toString(),
        "", "$wnd.set(x);");
  }

  public void testComputeCompletionsProposalsWhenBracesArePresent()
      throws CoreException, BadLocationException {
    IJavaProject javaProject = 
        JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    StringBuilder aSource = new StringBuilder();
    aSource.append("class A {\n");
    aSource.append("  private native int getA() {\n");
    aSource.append("}\n");

    validateExpectedProposals(javaProject, "A", aSource.toString());

    StringBuilder bSource = new StringBuilder();
    bSource.append("class B {\n");
    bSource.append("  private native int get() {\n");
    bSource.append("  private native someMethod() {\n");
    bSource.append("  }\n");
    bSource.append("}\n");

    validateExpectedProposals(javaProject, "B", bSource.toString());
  }
  
  public void testComputeCompletionsProposalsWithPartialBraces()
      throws CoreException, BadLocationException {
    IJavaProject javaProject = 
        JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    String className = "A";
    
    for (int numCharsCompleted = 0; numCharsCompleted <= 4; numCharsCompleted++){
      StringBuilder source = new StringBuilder();      
      int invocationLineNum = constructPartialBracesSource(className, source, numCharsCompleted, 
          false, false);
      
      validateExpectedProposals(javaProject, className, source.toString(), invocationLineNum, 
          numCharsCompleted, "", "return this.bar();", "return this.bar;");
      
      className = className.concat("A");
    }
  }

  public void testComputeCompletionsProposalsWithPartialBracesAndCommentsBelow()
      throws CoreException, BadLocationException {
    IJavaProject javaProject = 
        JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    String className = "A";
    
    for (int numCharsCompleted = 0; numCharsCompleted <= 4; numCharsCompleted++){
      StringBuilder source = new StringBuilder();      
      int invocationLineNum = constructPartialBracesSource(className, source, numCharsCompleted, 
          false, true);
      
      validateExpectedProposals(javaProject, className, source.toString(), invocationLineNum, 
          numCharsCompleted, "", "return this.bar();", "return this.bar;");
      
      className = className.concat("A");
    }
  }

  public void testComputeCompletionsProposalsWithPartialBracesAndCommentsAbove()
      throws CoreException, BadLocationException {
    IJavaProject javaProject = 
        JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    String className = "A";
    
    for (int numCharsCompleted = 0; numCharsCompleted <= 4; numCharsCompleted++){
      StringBuilder source = new StringBuilder();      
      int invocationLineNum = constructPartialBracesSource(className, source, numCharsCompleted, 
          true, false);
      
      validateExpectedProposals(javaProject, className, source.toString(), invocationLineNum, 
          numCharsCompleted, "", "return this.bar();", "return this.bar;");
      
      className = className.concat("A");
    }
  }
  
  public void testComputeCompletionsProposalsWithPartialBracesAndCommentsOnBothSides()
      throws CoreException, BadLocationException {
    IJavaProject javaProject = 
        JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));

    String className = "A";
    
    for (int numCharsCompleted = 0; numCharsCompleted <= 4; numCharsCompleted++){
      StringBuilder source = new StringBuilder();      
      int invocationLineNum = constructPartialBracesSource(className, source, numCharsCompleted, 
          true, true);
      
      validateExpectedProposals(javaProject, className, source.toString(), invocationLineNum, 
          numCharsCompleted, "", "return this.bar();", "return this.bar;");
      
      className = className.concat("A");
    }
  }

  /**
   * Ensure that our plugin xml included the correct extension to enable this
   * {@link JsniMethodBodyCompletionProposalComputer}.
   */
  public void testExtensionPointExistence() {
    Bundle bundle = GWTPlugin.getDefault().getBundle();
    String extensionPointId = "org.eclipse.jdt.ui.javaCompletionProposalComputer";

    assertTrue("Plugin " + GWTPlugin.getName()
        + " did not contribute to extension point " + extensionPointId,
        BundleUtilities.contributesToExtensionPoint(bundle,
            JsniMethodBodyCompletionProposalComputer.SIMPLE_EXTENSION_ID,
            extensionPointId));
  }

  /**
   * Test method for
   * {@link JsniMethodBodyCompletionProposalComputer#computePropertyNameFromAccessorMethodName
   *                                                      (java.lang.String, java.lang.String)}
   */
  public void testGetPropertyName() {
    assertEquals(
        "foo",
        JsniMethodBodyCompletionProposalComputer.computePropertyNameFromAccessorMethodName(
            "is", "isFoo"));
    assertEquals(
        "bar",
        JsniMethodBodyCompletionProposalComputer.computePropertyNameFromAccessorMethodName(
            "get", "getBar"));
  }

  /**
   * Test that the icons that we depend on are included in the registry.
   */
  public void testIcons() {
    // TODO: This should really be a test of GWTImages.
    GWTPlugin plugin = GWTPlugin.getDefault();
    ImageRegistry imageRegistry = plugin.getImageRegistry();
    for (String imageId : IMAGE_IDS) {
      Image image = imageRegistry.get(imageId);
      assertNotNull("ImageId: " + imageId + " was not in the ImageRegistry",
          image);
    }
  }
}
