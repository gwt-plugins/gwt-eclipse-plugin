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

import com.google.gwt.eclipse.core.validators.java.JsniJavaRef;
import com.google.gwt.eclipse.core.search.IIndexedJavaRef;
import com.google.gwt.eclipse.core.search.IndexedJsniJavaRef;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests the {@link GWTRefactoringSupport} class.
 */
public class GWTRefactoringSupportTest extends AbstractRefactoringTestImpl {

  private static class DummyGWTRefactoringSupport extends GWTRefactoringSupport {

    @Override
    protected TextEdit createEdit(IIndexedJavaRef ref) {
      return new ReplaceEdit(0, 0, "");
    }

    @Override
    protected String getEditDescription() {
      return "";
    }
  }

  private static class DummyRefactoringParticipant extends
      RefactoringParticipant {

    @Override
    public RefactoringStatus checkConditions(IProgressMonitor pm,
        CheckConditionsContext context) throws OperationCanceledException {
      return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException,
        OperationCanceledException {
      return null;
    }

    @Override
    public String getName() {
      return "";
    }

    @Override
    protected boolean initialize(Object element) {
      return true;
    }

    @Override
    protected void initialize(RefactoringArguments arguments) {
    }
  }

  private static final IPath FILE_1 = new Path("/ProjectA/src/Hello.java");

  private static final IPath FILE_2 = new Path("/ProjectB/src/Second.java");

  private static final String[] REFS_GROUP_1 = new String[] {
      "@com.hello.Hello::sayHi(Ljava/lang/String;)", "@com.hello.Hello::new()",
      "@com.hello.Hello::field1",
      "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)",
      "@com.hello.Greeter::sayHello(Ljava/lang/String;)"};

  private static final String[] REFS_GROUP_2 = new String[] {
      "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)",
      "@com.hello.Greeter::sayHello(Ljava/lang/String;)"};

  public void testCreateChange() {
    GWTRefactoringSupport support = new DummyGWTRefactoringSupport();
    support.setUpdateReferences(true);

    IType refactorTestType = refactorTestClass.getCompilationUnit().findPrimaryType();
    support.setOldElement(refactorTestType);

    RefactoringParticipant participant = new DummyRefactoringParticipant();
    IRefactoringChangeFactory changeFactory = new DefaultChangeFactory();
    CompositeChange change = support.createChange(participant, changeFactory);

    // Return value should contain one child change
    Change[] changeChildren = change.getChildren();
    assertEquals(1, changeChildren.length);

    // Root edit should contain two child edits, one for each JSNI ref
    TextChange childChange = (TextChange) changeChildren[0];
    TextEdit changeEdit = childChange.getEdit();
    assertEquals(2, changeEdit.getChildrenSize());
  }

  public void testCreateChangeWithoutEdits() throws JavaModelException {
    GWTRefactoringSupport support = new DummyGWTRefactoringSupport();
    support.setUpdateReferences(true);

    IType entryPointType = getTestProject().findType(
        AbstractGWTPluginTestCase.TEST_PROJECT_SRC_PACKAGE,
        AbstractGWTPluginTestCase.TEST_PROJECT_ENTRY_POINT);
    support.setOldElement(entryPointType);

    // There are no references to the entry point class, so the Change returned
    // should be null
    RefactoringParticipant participant = new DummyRefactoringParticipant();
    IRefactoringChangeFactory changeFactory = new DefaultChangeFactory();
    CompositeChange change = support.createChange(participant, changeFactory);
    assertNull(change);
  }

  public void testCreateChangeWithoutUpdateReferences() {
    GWTRefactoringSupport support = new DummyGWTRefactoringSupport();
    support.setUpdateReferences(false);

    // With update references turned off, Change returned should be null
    RefactoringParticipant participant = new DummyRefactoringParticipant();
    IRefactoringChangeFactory changeFactory = new DefaultChangeFactory();
    Change change = support.createChange(participant, changeFactory);
    assertNull(change);
  }

  public void testCreateEdits() {
    // Simulate refs in two files
    Set<IIndexedJavaRef> refs1 = createRefSet(REFS_GROUP_1, FILE_1);
    Set<IIndexedJavaRef> refs2 = createRefSet(REFS_GROUP_2, FILE_2);

    Set<IIndexedJavaRef> allRefs = new HashSet<IIndexedJavaRef>();
    allRefs.addAll(refs1);
    allRefs.addAll(refs2);

    // Create edits for all refs, grouped by file
    GWTRefactoringSupport support = new DummyGWTRefactoringSupport();
    Map<IPath, Set<TextEdit>> edits = support.createEdits(allRefs);

    assertEquals(2, edits.size());
    assertTrue(edits.containsKey(FILE_1));
    assertEquals(refs1.size(), edits.get(FILE_1).size());
    assertTrue(edits.containsKey(FILE_2));
    assertEquals(refs2.size(), edits.get(FILE_2).size());
  }

  public void testGetAndSetNewElement() throws JavaModelException {
    GWTRefactoringSupport support = new DummyGWTRefactoringSupport();
    IJavaElement element = getTestProjectEntryPointClass();

    support.setNewElement(element);
    assertEquals(element, support.getNewElement());
  }

  public void testGetAndSetOldElement() throws JavaModelException {
    GWTRefactoringSupport support = new DummyGWTRefactoringSupport();
    IJavaElement element = getTestProjectEntryPointClass();

    support.setOldElement(element);
    assertEquals(element, support.getOldElement());
  }

  public void testGetAndSetUpdateReferences() {
    GWTRefactoringSupport support = new DummyGWTRefactoringSupport();

    support.setUpdateReferences(true);
    assertEquals(true, support.getUpdateReferences());
  }

  private Set<IIndexedJavaRef> createRefSet(String[] rawRefs, IPath source) {
    Set<IIndexedJavaRef> refs = new HashSet<IIndexedJavaRef>();

    for (String rawRef : rawRefs) {
      JsniJavaRef ref = JsniJavaRef.parse(rawRef);
      ref.setSource(source);
      IndexedJsniJavaRef indexedRef = new IndexedJsniJavaRef(ref);
      refs.add(indexedRef);
    }

    return refs;
  }

  private IJavaElement getTestProjectEntryPointClass()
      throws JavaModelException {
    return getTestProject().findType(
        AbstractGWTPluginTestCase.TEST_PROJECT_SRC_PACKAGE,
        AbstractGWTPluginTestCase.TEST_PROJECT_SRC_PACKAGE);
  }

}
