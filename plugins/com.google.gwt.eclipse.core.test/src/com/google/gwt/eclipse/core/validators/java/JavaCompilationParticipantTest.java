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

import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.jobs.JobsUtilities;
import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.markers.GWTProblemType;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.search.JavaRefIndex;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider.ProblemAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests the {@link JavaCompilationParticipant} class.
 */
@SuppressWarnings("restriction")
public class JavaCompilationParticipantTest extends AbstractGWTPluginTestCase {

  private TestClass testClass;

  public void testBuildAddError() throws Exception {
    IProject project = getTestProject().getProject();

    // Verify that we have 1 existing GWT problem marker
    IMarker[] markers = getGWTProblemMarkers(project);
    assertEquals(1, markers.length);

    ICompilationUnit cu = testClass.getCompilationUnit();
    CompilationUnitEditor editor = null;

    // Open the test class in the editor
    editor = (CompilationUnitEditor) EditorUtility.openInEditor(cu);
    IEditorInput editorInput = editor.getEditorInput();

    // Edit the document to create a new error (add 'foobar' to the front of
    // the class name in a Java reference)
    IDocument document = editor.getDocumentProvider().getDocument(editorInput);
    TextEdit errorEdit = new InsertEdit(254, "foobar");
    errorEdit.apply(document);
    // Save the changes
    editor.doSave(null);

    // Rebuild the project
    rebuildTestProject();

    // Verify that we now have 2 GWT problem markers
    markers = getGWTProblemMarkers(project);
    assertEquals(2, markers.length);
  }

  public void testBuildTypeDependencies() throws Exception {
    IProject project = getTestProject().getProject();

    // Verify that we have 1 existing GWT problem marker (it's about the
    // unresolved type com.hello.client.A.B)
    IMarker[] markers = getGWTProblemMarkers(project);
    assertEquals(1, markers.length);

    // Now go ahead and add the missing A.java with nested class B
    String[] aSource = new String[]{
        "package com.hello.client;",
        "public class A {",
        "  public static class B {",
        "    public static int getNumber() {",
        "      return 777;",
        "    }",
        "  }",
        "}"
    };
    TestClass aClass = new TestClass(aSource, "A");
    aClass.addToTestProject();
    SWTUtilities.delay(2000);

    // Verify that we cleared the unresolved type error (the original
    // test class should have rebuilt automatically when we added A.java, thanks
    // to its type dependency on com.hello.client.A.B)
    markers = getGWTProblemMarkers(project);
    assertEquals(0, markers.length);
  }

  public void testClean() throws CoreException {
    IProject project = getTestProject().getProject();
    IEclipsePreferences prefs = new InstanceScope().getNode("org.eclipse.core.resources");

    // Disable auto-building so we don't race with the build process.
    prefs.putBoolean("description.autobuilding", false);

    // Verify that we have existing problem markers
    IMarker[] markers = project.findMarkers(GWTJavaProblem.MARKER_ID, true,
        IResource.DEPTH_INFINITE);
    assertTrue(markers.length > 0);

    // Verify that we have existing JavaRefIndex entries
    assertEquals(3, JavaRefIndex.getInstance().size());

    // Clean the test project
    project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);

    // Wait for the validation job to complete.
    JobsUtilities.waitForIdle();

    // Verify that the problems went away
    markers = project.findMarkers(GWTJavaProblem.MARKER_ID, true,
        IResource.DEPTH_INFINITE);
    assertEquals(0, markers.length);

    // Verify that the JavaRefIndex entries went away
    assertEquals(0, JavaRefIndex.getInstance().size());

    // Re-enable auto-building
    prefs.putBoolean("description.autobuilding", true);
  }

  public void testIsActiveGWTProject() {
    assertTrue(new JavaCompilationParticipant().isActive(getTestProject()));
  }

  public void testIsActiveNonGWTProject() throws CoreException {
    IProject project = getTestProject().getProject();
    GWTNature.removeNatureFromProject(project);

    assertFalse(new JavaCompilationParticipant().isActive(getTestProject()));

    GWTNature.addNatureToProject(project);
  }

  public void testReconcile() throws Exception {
    ICompilationUnit cu = testClass.getCompilationUnit();
    CompilationUnitEditor editor = null;

    try {
      editor = (CompilationUnitEditor) EditorUtility.openInEditor(cu);
      IEditorInput editorInput = editor.getEditorInput();

      JobsUtilities.waitForIdle();

      // Initially, the compilation unit will have one GWT problem marker
      List<GWTJavaProblem> problems = getGWTProblemsInEditor(editor);
      assertEquals(1, problems.size());
      assertEquals(GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE,
          problems.get(0).getProblemType());

      // Edit the document to create a new error (add 'foobar' to the front of
      // the class name in a Java reference)
      IDocument document = editor.getDocumentProvider().getDocument(editorInput);
      TextEdit errorEdit = new InsertEdit(254, "foobar");
      errorEdit.apply(document);

      // There should now be 2 GWT problem markers, wait up to 20 seconds for
      // the editor to reconcile
      for (int i = 0; i < 20 && problems.size() != 2; i++) {
        problems = getGWTProblemsInEditor(editor);
        SWTUtilities.delay(1000);
      }

      assertEquals(2, problems.size());
      assertEquals(GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE,
          problems.get(0).getProblemType());
      assertEquals(GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE,
          problems.get(1).getProblemType());

    } finally {
      if (editor != null) {
        editor.close(false);
      }
    }
  }

  public void testValidateCompilationUnit() throws Exception {
    ASTNode ast = parseTestClass();

    // Validate the compilation unit
    JavaValidationResult result = JavaCompilationParticipant.validateCompilationUnit(ast);

    // Verify that we added the refs to JavaRefIndex
    assertEquals(3, JavaRefIndex.getInstance().size());

    // Verify the problems returned from the validator
    List<GWTJavaProblem> problems = result.getProblems();
    assertEquals(1, problems.size());
    GWTJavaProblem problem = problems.get(0);
    assertEquals(GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE,
        problem.getProblemType());
  }

  @Override
  protected TestClass[] getTestClasses() {
    String[] lines = new String[]{
        "package com.hello.client;",
        "",
        "public class JavaCompilationParticipantTest {",
        "",
        "  private int counter;",
        "",
        "  public native void jsniMethod()/*-{",
        "    // References to some Java types",
        "    var num = obj.@com.hello.client.A$B::getNumber()();",
        "    num += obj.@com.hello.client.JavaCompilationParticipantTest::getSum(II)(2, 2);",
        "    num += obj.@com.hello.client.JavaCompilationParticipantTest::counter;",
        "  }-*/;",
        "",
        "  public static int getSum(int op1, int op2) {",
        "    return op1 + op2; ",
        "  }",
        "",
        "}"
        };

    testClass = new TestClass(lines, "JavaCompilationParticipantTest");
    return new TestClass[] {testClass};
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

  private IMarker[] getGWTProblemMarkers(IProject project) throws CoreException {
    return project.findMarkers(GWTJavaProblem.MARKER_ID, true,
        IResource.DEPTH_INFINITE);
  }

  private List<GWTJavaProblem> getGWTProblemsInEditor(CompilationUnitEditor editor)
      throws Exception {
    List<GWTJavaProblem> problems = new ArrayList<GWTJavaProblem>();

    Field annotationProblemField = CompilationUnitDocumentProvider.ProblemAnnotation.class.getDeclaredField("fProblem");
    annotationProblemField.setAccessible(true);

    IEditorInput editorInput = editor.getEditorInput();
    IDocumentProvider documentProvider = editor.getDocumentProvider();
    IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editorInput);
    Iterator<?> iter = annotationModel.getAnnotationIterator();
    while (iter.hasNext()) {
      Object annotation = iter.next();
      if (annotation instanceof CompilationUnitDocumentProvider.ProblemAnnotation) {
        CompilationUnitDocumentProvider.ProblemAnnotation problemAnnotation = (ProblemAnnotation) annotation;
        if (problemAnnotation.getMarkerType().equals(GWTJavaProblem.MARKER_ID)) {
          GWTJavaProblem problem = (GWTJavaProblem) annotationProblemField.get(problemAnnotation);
          problems.add(problem);
        }
      }
    }

    return problems;
  }

  private ASTNode parseTestClass() {
    // Have JDT parse the compilation unit
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setProject(getTestProject());
    parser.setResolveBindings(false);
    parser.setSource(testClass.getCompilationUnit());
    return parser.createAST(null);
  }

}
