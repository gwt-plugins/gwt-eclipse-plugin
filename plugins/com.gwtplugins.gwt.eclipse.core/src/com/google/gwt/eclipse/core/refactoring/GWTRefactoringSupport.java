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

import com.google.gwt.eclipse.core.search.IIndexedJavaRef;
import com.google.gwt.eclipse.core.search.JavaQueryParticipant;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Common code used by our refactoring participants.
 */
public abstract class GWTRefactoringSupport {

  private IJavaElement newElement;

  private IJavaElement oldElement;

  private boolean updateReferences;

  /**
   * Creates a change for a refactoring that includes all of the necessary text
   * edits inside all affected files. It relies on the change factory supplied
   * by the individual refactor participants to actually create the change for
   * each file and then adds each to a single CompositeChange.
   * 
   * @param participant the refactoring participant being invoked
   * @param changeFactory creates new changes for individual affected files
   * @return a CompositeChange that bundles all refactoring changes
   */
  public CompositeChange createChange(RefactoringParticipant participant,
      IRefactoringChangeFactory changeFactory) {
    // Bail out if we're not updating references
    if (!updateReferences) {
      return null;
    }

    // By default, we return null if we have no changes to contribute
    CompositeChange result = null;

    // Get edits for all the matching Java references we have indexed
    Set<IIndexedJavaRef> refs = JavaQueryParticipant.findWorkspaceReferences(
        oldElement, true);
    Map<IPath, Set<TextEdit>> edits = createEdits(refs);

    for (IPath source : edits.keySet()) {
      IFile file = Util.getWorkspaceRoot().getFile(source);

      // Create the change for this file
      TextChange change = changeFactory.createChange(file);
      MultiTextEdit rootEdit = new MultiTextEdit();
      change.setEdit(rootEdit);
      change.setKeepPreviewEdits(true);

      // Add the text edits to the change
      for (TextEdit edit : edits.get(source)) {
        rootEdit.addChild(edit);
        change.addTextEditGroup(new TextEditGroup(getEditDescription(), edit));
      }

      // Lazily initialize the return value
      if (result == null) {
        result = new CompositeChange(participant.getName());
      }
      result.add(change);
    }

    return result;
  }

  /**
   * Creates text edits for each file affected by the refactoring. This
   * delegates to the createEdit(IIndexedJavaRef) method to actually create the
   * edit (since, for example, edits for renaming a type will be different than
   * those for renaming members). The edits are then grouped together by file
   * before being returned.
   * 
   * @param refs the Java references that need to be updated
   * @return the set of text edits to update the references, grouped by file
   */
  public Map<IPath, Set<TextEdit>> createEdits(Set<IIndexedJavaRef> refs) {
    Map<IPath, Set<TextEdit>> edits = new HashMap<IPath, Set<TextEdit>>();

    for (IIndexedJavaRef ref : refs) {
      TextEdit edit = createEdit(ref);
      IPath source = ref.getSource();

      // Add the edit to the map
      if (edits.containsKey(source)) {
        edits.get(source).add(edit);
      } else {
        Set<TextEdit> sourcEdits = new HashSet<TextEdit>();
        sourcEdits.add(edit);
        edits.put(source, sourcEdits);
      }
    }

    return edits;
  }

  public IJavaElement getNewElement() {
    return newElement;
  }

  public IJavaElement getOldElement() {
    return oldElement;
  }

  public boolean getUpdateReferences() {
    return updateReferences;
  }

  public void setNewElement(IJavaElement newElement) {
    this.newElement = newElement;
  }

  public void setOldElement(IJavaElement oldElement) {
    this.oldElement = oldElement;
  }

  public void setUpdateReferences(boolean updateReferences) {
    this.updateReferences = updateReferences;
  }

  protected abstract TextEdit createEdit(IIndexedJavaRef ref);

  protected abstract String getEditDescription();

}
