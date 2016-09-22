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

import com.google.gwt.eclipse.core.platformproxy.refactoring.IJsniReferenceChange;
import com.google.gwt.eclipse.core.platformproxy.refactoring.JsniReferenceChangeFactory;
import com.google.gwt.eclipse.core.search.IIndexedJavaRef;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Tests the {@link JsniReferenceChange} class.
 */
public class JsniReferenceChangeTest extends AbstractRefactoringTestImpl {

  private class DummyRefactoringSupport extends GWTRefactoringSupport {

    public DummyRefactoringSupport() {
      // We'll simulate renaming the type R to 'RRR'
      IJavaElement oldElement = rClass.getCompilationUnit().getType("R");
      setOldElement(oldElement);
      IJavaElement newElement = rClass.getCompilationUnit().getType("RRR");
      setNewElement(newElement);
    }

    @Override
    protected TextEdit createEdit(IIndexedJavaRef ref) {
      return new ReplaceEdit(ref.getClassOffset(), 0, "");
    }

    @Override
    protected String getEditDescription() {
      return "";
    }
  }

  public void testPerform() throws CoreException {
    // Create a dummy change for updating references in RefactorTest.java
    // to the class R to its new name 'RRR'
    ICompilationUnit cu = refactorTestClass.getCompilationUnit();
    GWTRefactoringSupport support = new DummyRefactoringSupport();
    JsniReferenceChangeFactory factory = new JsniReferenceChangeFactory(support);
    IJsniReferenceChange jsniChange = factory.createChange(cu);

    // Add one dummy edit to the change
    TextEdit oldRootEdit = new MultiTextEdit();
    oldRootEdit.addChild(new ReplaceEdit(252, 0, ""));
    ((TextFileChange)jsniChange).setEdit(oldRootEdit);

    // Perform the change (this should replace the original edits with new ones
    // with the correct offsets).
    ((TextFileChange)jsniChange).perform(new NullProgressMonitor());

    // Verify that the change still has one edit
    TextEdit newRootEdit = ((TextFileChange)jsniChange).getEdit();
    assertEquals(1, newRootEdit.getChildrenSize());
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

}
