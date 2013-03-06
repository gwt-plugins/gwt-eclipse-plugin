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

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.platformproxy.refactoring.IJsniReferenceChange;
import com.google.gwt.eclipse.core.search.IIndexedJavaRef;
import com.google.gwt.eclipse.core.search.IndexedJsniJavaRef;
import com.google.gwt.eclipse.core.search.JavaQueryParticipant;
import com.google.gwt.eclipse.core.validators.java.JavaCompilationParticipant;
import com.google.gwt.eclipse.core.validators.java.JsniJavaRef;
import com.google.gwt.eclipse.core.validators.java.UnresolvedJsniJavaRefException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.util.Iterator;
import java.util.Set;

/**
 * Helper class that is used by the version-specific implementations of the
 * <code>JsniReferenceChange</code> class.
 */
@SuppressWarnings("restriction")
public class JsniReferenceChangeHelper {

  private final IJsniReferenceChange jsniReferenceChange;

  public JsniReferenceChangeHelper(IJsniReferenceChange jsniReferenceChange) {
    this.jsniReferenceChange = jsniReferenceChange;
  }

  public void perform(IProgressMonitor pm, ICompilationUnit cu,
      TextEdit rootEdit) throws CoreException {

    // Remove the original edits we generated, since their offsets may have
    // been shifted by JDT edits that occurred "higher" in the source file
    TextEdit[] oldEdits = rootEdit.removeChildren();

    // Generate a new AST for this compilation unit
    RefactoringASTParser parser = new RefactoringASTParser(AST.JLS3);
    CompilationUnit astNode = parser.parse(cu, false);

    // Now re-validate the compilation unit's AST to get the JSNI Java
    // references' updated positions into the index
    JavaCompilationParticipant.validateCompilationUnit(astNode);

    // Get the index entries matching the old element by name only (we can't
    // resolve the references anymore because the old element no longer exists).
    IJavaElement oldElement = jsniReferenceChange.getRefactoringSupport().getOldElement();
    Set<IIndexedJavaRef> refs = JavaQueryParticipant.findWorkspaceReferences(
        oldElement, false);

    for (Iterator<IIndexedJavaRef> iterator = refs.iterator(); iterator.hasNext();) {
      IIndexedJavaRef ref = iterator.next();

      // Remove any matches that did not come from this compilation unit or
      // which don't resolve to the refactored Java Element
      if (!(ref.getSource().equals(cu.getPath()))
          || (!resolvesToRefactoredElement(cu, ref))) {
        iterator.remove();
      }
    }

    // Get the new edits for the references within this compilation unit (the
    // offsets may have changed if the JDT created edits above us in the
    // compilation unit).
    Set<TextEdit> newEdits = jsniReferenceChange.getRefactoringSupport().createEdits(
        refs).get(cu.getPath());
    assert (newEdits != null);
    assert (oldEdits.length == newEdits.size());

    // Add all those edits back onto this change's root edit
    rootEdit.addChildren(newEdits.toArray(new TextEdit[0]));
  }

  public JsniJavaRef refactorJavaRef(IIndexedJavaRef ref) {
    /*
     * Make a copy of the reference that we can safely modify. This is important
     * because otherwise we'd be modifying the actual references in the index
     * itself. This is generally not a problem since IIndexedJavaRef has no
     * setter methods, but we do have to worry about it here since we explicitly
     * cast the reference to a JsniJavaRef which does allow us to modify the
     * source and offset properties.
     */
    JsniJavaRef tempRef = new JsniJavaRef((JsniJavaRef) ref);

    // Wrap the reference in a document
    tempRef.setOffset(0);
    Document dummyDoc = new Document(tempRef.toString());

    // Have the participant create the edit necessary to refactor the reference
    TextEdit edit = jsniReferenceChange.getRefactoringSupport().createEdit(
        new IndexedJsniJavaRef(tempRef));

    try {
      // Apply the edit to the dummy document
      edit.apply(dummyDoc);
    } catch (Exception e) {
      GWTPluginLog.logError(e);
      return null;
    }

    // Now just parse the document back into a JsniJavaRef
    return JsniJavaRef.parse(dummyDoc.get());
  }

  public boolean resolvesToRefactoredElement(ICompilationUnit cu,
      IIndexedJavaRef ref) {
    IJavaElement refactoredElement = jsniReferenceChange.getRefactoringSupport().getNewElement();
    if (refactoredElement == null) {
      return false;
    }

    // If we're refactoring a type, then all refs have already been matched by
    // their fully-qualified type names so we can return now.
    if (refactoredElement.getElementType() == IJavaElement.TYPE) {
      return true;
    }

    // For method/field renamings, we refactor the Java reference and then try
    // to resolve the refactored Java reference against the refactored Java
    // member (this takes care of inherited members, overloaded methods, etc).
    JsniJavaRef refactoredRef = refactorJavaRef(ref);
    if (refactoredRef != null) {
      try {
        assert (refactoredElement instanceof IMethod || refactoredElement instanceof IField);

        IJavaProject javaProject = cu.getJavaProject();
        IJavaElement refactoredRefElement = refactoredRef.resolveJavaElement(javaProject);
        return refactoredElement.equals(refactoredRefElement);
      } catch (UnresolvedJsniJavaRefException e) {
        // Ignore any unresolved references; it's entirely possible that some
        // of the name matches we got from the index don't resolve to any
        // element in the Java Model
      }
    }

    return false;
  }

}
