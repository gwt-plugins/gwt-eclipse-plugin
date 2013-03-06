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
package com.google.gwt.eclipse.core.refactoring;

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.search.IIndexedJavaRef;
import com.google.gwt.eclipse.core.search.JavaQueryParticipant;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.refactoring.RenameSupport;
import org.eclipse.ui.IWorkbench;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * Adds two compilation units to the test project for testing refactoring.
 */
public abstract class AbstractRefactoringTest extends AbstractGWTPluginTestCase {

  protected TestClass rClass;

  protected TestClass refactorTestClass;

  protected void assertCompilationUnitMove(ICompilationUnit oldCu,
      IPackageFragment dest) throws CoreException {
    // FIXME - Reneable this assertion see comment in AbstractRefactoringTestImpl
//    // Get the references before the move
//    Set<IIndexedJavaRef> oldRefs = JavaQueryParticipant.findWorkspaceReferences(
//        oldCu.findPrimaryType(), true);
//    assertTrue(oldRefs.size() > 0);
//
//    // Move the compilation unit
//    move(oldCu, dest);
//
//    // After the move, verify that the references are updated
//    ICompilationUnit newCu = dest.getCompilationUnit(oldCu.getElementName());
//    Set<IIndexedJavaRef> newRefs = JavaQueryParticipant.findWorkspaceReferences(
//        newCu.findPrimaryType(), true);
//    assertEquals(oldRefs.size(), newRefs.size());
  }

  protected void assertFieldRename(IField oldField, String newName)
      throws Exception {
    // Get the references before the rename
    Set<IIndexedJavaRef> oldRefs = JavaQueryParticipant.findWorkspaceReferences(
        oldField, true);
    assertTrue(oldRefs.size() > 0);

    // Rename the field
    rename(RenameSupport.create(oldField, newName,
        RenameSupport.UPDATE_REFERENCES));

    // After the rename, verify that the references are updated
    IField newField = oldField.getDeclaringType().getField(newName);
    Set<IIndexedJavaRef> newRefs = JavaQueryParticipant.findWorkspaceReferences(
        newField, true);
    assertEquals(oldRefs.size(), newRefs.size());
  }

  protected void assertMethodRename(IMethod oldMethod, String newName)
      throws Exception {
    // Get the references before the rename
    Set<IIndexedJavaRef> oldRefs = JavaQueryParticipant.findWorkspaceReferences(
        oldMethod, true);
    assertTrue(oldRefs.size() > 0);

    // Rename the method
    rename(RenameSupport.create(oldMethod, newName,
        RenameSupport.UPDATE_REFERENCES));

    // After the rename, verify that the references are updated
    IMethod newMethod = oldMethod.getDeclaringType().getMethod(newName,
        oldMethod.getParameterTypes());
    Set<IIndexedJavaRef> newRefs = JavaQueryParticipant.findWorkspaceReferences(
        newMethod, true);
    assertEquals(oldRefs.size(), newRefs.size());
  }

  protected void assertTypeRename(IType oldType, String newName)
      throws Exception {
    // Get the references before the rename
    Set<IIndexedJavaRef> oldRefs = JavaQueryParticipant.findWorkspaceReferences(
        oldType, true);
    assertTrue(oldRefs.size() > 0);

    // Rename the type
    rename(RenameSupport.create(oldType, newName,
        RenameSupport.UPDATE_REFERENCES));

    // Get the type after the name change
    IType newType = null;
    IJavaElement parent = oldType.getParent();
    if (parent instanceof IType) {
      newType = ((IType) parent).getType(newName);
    } else if (parent instanceof ICompilationUnit) {
      newType = ((ICompilationUnit) parent).getType(newName);
    } else {
      fail("Old type parent is invalid");
    }

    // After the rename, verify that the references are updated
    Set<IIndexedJavaRef> newRefs = JavaQueryParticipant.findWorkspaceReferences(
        newType, true);
    assertEquals(oldRefs.size(), newRefs.size());
  }

  @Override
  protected TestClass[] getTestClasses() {
    String[] r = new String[] {
        "package com.hello.client;",
        "",
        "public class R {",
        "  public static class B {",
        "    public static int getNumber() {",
        "      return 7;",
        "    }",
        "  }",
        "}"};

    String[] refactorTest = new String[] {
        "package com.hello.client;",
        "",
        "public class RefactorTest {",
        "",
        "  private int counter;",
        "",
        "  public int getNumber(int val) {",
        "    return R.B.getNumber();", "  }",
        "",
        "  public static native void jsniMethod()/*-{",
        "    var num = obj.@com.hello.client.R$B::getNumber()();",
        "    num += obj.@com.hello.client.RefactorTest::getNumber(I)(2);",
        "    num += obj.@com.hello.client.RefactorTest::counter;",
        "  }-*/;",
        "",
        "  public static int getMagicNumber() {",
        "    return 777;",
        "  }",
        "}"};

    rClass = new TestClass(r, "R");
    refactorTestClass = new TestClass(refactorTest, "RefactorTest");
    return new TestClass[] {rClass, refactorTestClass};
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

  protected abstract void move(ICompilationUnit oldCu, IPackageFragment dest)
      throws CoreException;

  private void rename(RenameSupport rename) throws Exception,
      InvocationTargetException {
    // Set up the rename refactoring
    IWorkbench workbench = GWTPlugin.getDefault().getWorkbench();
    rename.perform(workbench.getActiveWorkbenchWindow().getShell(),
        workbench.getProgressService());

    // Need to rebuild to update the JavaRefIndex
    rebuildTestProject();
  }

}
