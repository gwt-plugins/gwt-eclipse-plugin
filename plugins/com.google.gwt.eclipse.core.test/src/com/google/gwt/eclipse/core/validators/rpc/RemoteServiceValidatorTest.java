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
package com.google.gwt.eclipse.core.validators.rpc;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.core.validation.ValidationResult;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;
import com.google.gwt.eclipse.core.runtime.GwtRuntimeTestUtilities;

import junit.framework.TestCase;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link RemoteServiceValidator}.
 */
public final class RemoteServiceValidatorTest extends TestCase {
  
  private static boolean equals(CategorizedProblem p1, CategorizedProblem p2) {

    if (p1 == p2) {
      return true;
    }
    if (p2 == null) {
      return false;
    }

    if (p1.getClass() != p2.getClass()) {
      return false;
    }

    if (p1.getClass() != RemoteServiceProblem.class) {
      return false;
    }

    RemoteServiceProblem self = (RemoteServiceProblem) p1;
    RemoteServiceProblem other = (RemoteServiceProblem) p2;

    if (self.getSourceEnd() != other.getSourceEnd()) {
      return false;
    }
    if (self.getOriginatingFileName() == null) {
      if (other.getOriginatingFileName() != null) {
        return false;
      }
    } else if (!Arrays.equals(self.getOriginatingFileName(),
        other.getOriginatingFileName())) {
      return false;
    }
    if (self.getID() != other.getID()) {
      return false;
    }
    if (self.getSourceLineNumber() != other.getSourceLineNumber()) {
      return false;
    }
    if (self.getMessage() == null) {
      if (other.getMessage() != null) {
        return false;
      }
    } else if (!self.getMessage().equals(other.getMessage())) {
      return false;
    }
    if (!Arrays.equals(self.getArguments(), other.getArguments())) {
      return false;
    }
    if (self.getSeverity() == null) {
      if (other.getSeverity() != null) {
        return false;
      }
    } else if (!self.getSeverity().equals(other.getSeverity())) {
      return false;
    }
    if (self.getSourceStart() != other.getSourceStart()) {
      return false;
    }
    if (self.getProblemType() == null) {
      if (other.getProblemType() != null) {
        return false;
      }
    } else if (!self.getProblemType().equals(other.getProblemType())) {
      return false;
    }
    return true;
  }

  private static ASTNode newAST(ICompilationUnit syncInterface) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setResolveBindings(true);
    parser.setProject(syncInterface.getJavaProject());
    parser.setSource(syncInterface);
    return parser.createAST(null);
  }

  private static String synthesizeProjectNameForThisTest(TestCase test) {
    return (test.getClass().getCanonicalName() + "." + test.getName()).replace(
        '.', '_');
  }

  private IJavaProject javaProject;

  public void testAsyncCallbackMismatch() {
  }

  public void testDependentTypeIsNotSyncInterface() throws JavaModelException {
    JavaProjectUtilities.createCompilationUnit(javaProject,
        "com.google.TestService",
        "package com.google;\npublic interface TestService { void foo(); }\n");

    ICompilationUnit asyncInterface = JavaProjectUtilities.createCompilationUnit(
        javaProject,
        "com.google.TestServiceAsync",
        "package com.google;\nimport com.google.gwt.user.client.rpc.AsyncCallback;\npublic interface TestServiceAsync { void foo(AsyncCallback foo); }\n");

    RemoteServiceValidator rsv = new RemoteServiceValidator();
    ValidationResult validationResults;

    ASTNode asyncAst = newAST(asyncInterface);

    // Test that no errors are reported on the "Async" interface since the sync
    // interface does not extend RemoteService
    validationResults = rsv.validate(asyncAst);
    assertProblemsEqual(Collections.<CategorizedProblem> emptyList(),
        validationResults.getProblems());
    assertEquals(Arrays.asList("com.google.TestService"),
        validationResults.getTypeDependencies());
  }

  public void testEverythingOkay() throws JavaModelException {
    StringBuilder syncCode = new StringBuilder();
    syncCode.append("package com.google;\n");
    syncCode.append("public interface TestService<T extends java.io.Serializable> ");
    syncCode.append("extends com.google.gwt.user.client.rpc.RemoteService { ");
    syncCode.append("int[] foo(); ");
    syncCode.append("Integer[] bar(); ");
    syncCode.append("List<T> getList(); ");
    syncCode.append("}\n");
    ICompilationUnit syncInterface = JavaProjectUtilities.createCompilationUnit(
        javaProject, "com.google.TestService", syncCode.toString());

    StringBuilder asyncCode = new StringBuilder();
    asyncCode.append("package com.google;\n");
    asyncCode.append("import com.google.gwt.user.client.rpc.AsyncCallback;\n");
    asyncCode.append("public interface TestServiceAsync<T extends java.io.Serializable> { ");
    asyncCode.append("void foo(AsyncCallback<int[]> callback); ");
    asyncCode.append("void bar(AsyncCallback<Integer[]> callback); ");
    asyncCode.append("void getList(AsyncCallback<List<T>> callback); }\n");
    ICompilationUnit asyncInterface = JavaProjectUtilities.createCompilationUnit(
        javaProject, "com.google.TestServiceAsync", asyncCode.toString());

    RemoteServiceValidator rsv = new RemoteServiceValidator();
    ValidationResult validationResults;

    ASTNode syncAst = newAST(syncInterface);
    validationResults = rsv.validate(syncAst);
    assertTrue(validationResults.getProblems().isEmpty());
    assertEquals(Arrays.asList("com.google.TestServiceAsync"),
        validationResults.getTypeDependencies());

    ASTNode asyncAst = newAST(asyncInterface);
    validationResults = rsv.validate(asyncAst);
    assertTrue(validationResults.getProblems().isEmpty());
    assertEquals(Arrays.asList("com.google.TestService"),
        validationResults.getTypeDependencies());
  }

  public void testInvalidAsyncMethodReturnType() {
  }

  public void testMissingAsyncInterface() throws JavaModelException {
    ICompilationUnit syncInterface = JavaProjectUtilities.createCompilationUnit(
        javaProject,
        "com.google.TestService",
        "package com.google;\npublic interface TestService extends com.google.gwt.user.client.rpc.RemoteService {}\n");
    ASTNode node = newAST(syncInterface);
    CompilationUnit compilationUnit = (CompilationUnit) node.getRoot();
    TypeDeclaration typeDeclaration = JavaASTUtils.findTypeDeclaration(
        compilationUnit, "com.google.TestService");
    assertTrue(compilationUnit.getProblems().length == 0);

    RemoteServiceValidator rsv = new RemoteServiceValidator();
    ValidationResult validationResults = rsv.validate(node);

    List<RemoteServiceProblem> expected = Arrays.asList(RemoteServiceProblemFactory.newMissingAsyncType(typeDeclaration));
    assertProblemsEqual(expected, validationResults.getProblems());
    assertEquals(
        Arrays.asList(RemoteServiceUtilities.computeAsyncTypeName("com.google.TestService")),
        validationResults.getTypeDependencies());
  }

  public void testMissingAsyncMethod() throws JavaModelException {
    ICompilationUnit syncInterface = JavaProjectUtilities.createCompilationUnit(
        javaProject,
        "com.google.TestService",
        "package com.google;\npublic interface TestService extends com.google.gwt.user.client.rpc.RemoteService { void foo(); }\n");

    ICompilationUnit asyncInterface = JavaProjectUtilities.createCompilationUnit(
        javaProject,
        "com.google.TestServiceAsync",
        "package com.google;\nimport com.google.gwt.user.client.rpc.AsyncCallback;\npublic interface TestServiceAsync { }\n");

    RemoteServiceValidator rsv = new RemoteServiceValidator();
    ValidationResult validationResults;

    ASTNode syncAst = newAST(syncInterface);
    CompilationUnit syncUnit = (CompilationUnit) syncAst;
    TypeDeclaration syncTypeDecl = JavaASTUtils.findTypeDeclaration(syncUnit,
        "com.google.TestService");

    ASTNode asyncAst = newAST(asyncInterface);
    CompilationUnit asyncUnit = (CompilationUnit) asyncAst;
    TypeDeclaration asyncTypeDecl = JavaASTUtils.findTypeDeclaration(asyncUnit,
        "com.google.TestServiceAsync");

    // Test that an error is reported on TestInteface.foo()
    validationResults = rsv.validate(syncAst);
    MethodDeclaration syncMethodDeclaration = syncTypeDecl.getMethods()[0];
    assertProblemsEqual(
        Arrays.asList(RemoteServiceProblemFactory.newMissingAsyncMethodOnSync(
            syncMethodDeclaration, asyncTypeDecl.resolveBinding())),
        validationResults.getProblems());
    assertEquals(Arrays.asList("com.google.TestServiceAsync"),
        validationResults.getTypeDependencies());

    // Test that an error is reported on TestIntefaceAsync because it has no
    // foo(AsyncCallback) method
    validationResults = rsv.validate(asyncAst);
    ITypeBinding resolveBinding = syncTypeDecl.resolveBinding();
    assertProblemsEqual(
        Arrays.asList(RemoteServiceProblemFactory.newMissingAsyncMethodOnAsync(
            resolveBinding.getDeclaredMethods()[0], asyncTypeDecl)),
        validationResults.getProblems());
    assertEquals(Arrays.asList("com.google.TestService"),
        validationResults.getTypeDependencies());
  }

  public void testMissingSyncInterface() throws JavaModelException {
    ICompilationUnit syncInterface = JavaProjectUtilities.createCompilationUnit(
        javaProject, "com.google.TestServiceAsync",
        "package com.google;\npublic interface TestServiceAsync {}\n");
    ASTNode node = newAST(syncInterface);

    RemoteServiceValidator rsv = new RemoteServiceValidator();
    ValidationResult validationResults = rsv.validate(node);
    assertTrue(validationResults.getProblems().isEmpty());
    assertEquals(
        Arrays.asList(RemoteServiceUtilities.computeSyncTypeName("com.google.TestServiceAsync")),
        validationResults.getTypeDependencies());
  }

  public void testMissingSyncMethod() throws JavaModelException {
    ICompilationUnit syncInterface = JavaProjectUtilities.createCompilationUnit(
        javaProject,
        "com.google.TestService",
        "package com.google;\npublic interface TestService extends com.google.gwt.user.client.rpc.RemoteService {  }\n");

    ICompilationUnit asyncInterface = JavaProjectUtilities.createCompilationUnit(
        javaProject,
        "com.google.TestServiceAsync",
        "package com.google;\nimport com.google.gwt.user.client.rpc.AsyncCallback;\npublic interface TestServiceAsync { void foo(AsyncCallback foo); }\n");

    RemoteServiceValidator rsv = new RemoteServiceValidator();
    ValidationResult validationResults;

    ASTNode syncAst = newAST(syncInterface);
    CompilationUnit syncUnit = (CompilationUnit) syncAst;
    TypeDeclaration syncTypeDecl = JavaASTUtils.findTypeDeclaration(syncUnit,
        "com.google.TestService");

    ASTNode asyncAst = newAST(asyncInterface);
    CompilationUnit asyncUnit = (CompilationUnit) asyncAst;
    TypeDeclaration asyncTypeDecl = JavaASTUtils.findTypeDeclaration(asyncUnit,
        "com.google.TestServiceAsync");

    // Test that an error is reported on TestInteface because it is missing the
    // sync version of method void foo(AsyncCallback);
    validationResults = rsv.validate(syncAst);
    List<? extends CategorizedProblem> expectedSyncProblems = Arrays.asList(RemoteServiceProblemFactory.newMissingSyncMethodOnSync(
        syncTypeDecl, asyncTypeDecl.resolveBinding().getDeclaredMethods()[0]));
    assertProblemsEqual(expectedSyncProblems, validationResults.getProblems());
    assertEquals(Arrays.asList("com.google.TestServiceAsync"),
        validationResults.getTypeDependencies());

    // Test that an error is reported on TestIntefaceAsync because it has no
    // foo(AsyncCallback) method
    validationResults = rsv.validate(asyncAst);
    MethodDeclaration asyncMethodDeclaration = asyncTypeDecl.getMethods()[0];
    List<? extends CategorizedProblem> expectedAsyncProblems = Arrays.asList(RemoteServiceProblemFactory.newMissingSyncMethodOnAsync(
        asyncMethodDeclaration,
        syncTypeDecl.resolveBinding()));
    assertProblemsEqual(expectedAsyncProblems, validationResults.getProblems());
    assertEquals(Arrays.asList("com.google.TestService"),
        validationResults.getTypeDependencies());
  }

  public void testSyncMethodReturnTypeAndAsyncCallbackMismatch() {
  }

  @Override
  protected void setUp() throws Exception {
    GwtRuntimeTestUtilities.addDefaultRuntime();
    javaProject = JavaProjectUtilities.createJavaProject(synthesizeProjectNameForThisTest(this));
    assertNotNull(javaProject);
    JavaProjectUtilities.addRawClassPathEntry(javaProject,
        JavaCore.newContainerEntry(GWTRuntimeContainer.getDefaultRuntimePath()));
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    GwtRuntimeTestUtilities.removeDefaultRuntime();
    ProjectUtilities.deleteProject(javaProject.getElementName());
    super.tearDown();
  }

  private void assertProblemsEqual(List<? extends CategorizedProblem> expected,
      List<? extends CategorizedProblem> actual) {
    if (expected.size() == actual.size()) {
      for (int i = 0, n = expected.size(); i < n; ++i) {
        if (!equals(expected.get(i), actual.get(i))) {
          failNotEquals(null, expected, actual);
        }
      }
    }
  }
}
