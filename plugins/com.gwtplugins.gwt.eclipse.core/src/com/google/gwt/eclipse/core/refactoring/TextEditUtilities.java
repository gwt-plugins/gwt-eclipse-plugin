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

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for TextEdit and related classes.
 */
public final class TextEditUtilities {

  /**
   * Finds the text edit group which has the given text edit as a child.
   * 
   * @param change the change that contains the groups
   * @param edit the text edit to find
   * @return the group that has the text edit as a child, or null
   */
  public static TextEditGroup findTextEditGroup(TextEditBasedChange change, TextEdit edit) {
    for (TextEditGroup group : getTextEditGroups(change.getChangeGroups())) {
      for (TextEdit curEdit : group.getTextEdits()) {
        if (edit == curEdit) {
          return group;
        }
      }
    }

    return null;
  }

  /**
   * Returns a list of {@link TextEditGroup TextEditGroup} created from array of
   * {@link TextEditBasedChangeGroup}.
   */
  public static List<TextEditGroup> getTextEditGroups(
      TextEditBasedChangeGroup[] changeGroups) {
    List<TextEditGroup> textEditGroups = new ArrayList<TextEditGroup>(
        changeGroups.length);
    for (TextEditBasedChangeGroup changeGroup : changeGroups) {
      textEditGroups.add(changeGroup.getTextEditGroup());
    }
    return textEditGroups;
  }

  /**
   * Moves the given text edit groups (and its text edits) into the given
   * change.
   */
  public static void moveTextEditGroupsIntoChange(
      TextEditBasedChangeGroup[] groups, TextChange textChange) {
    for (TextEditBasedChangeGroup changeGroup : groups) {
      TextEditGroup group = changeGroup.getTextEditGroup();
      for (TextEdit edit : group.getTextEdits()) {
        if (edit.getParent() != null) {
          edit.getParent().removeChild(edit);
        }

        textChange.addEdit(edit);
      }

      // We must create a new change group since it doesn't have API to change
      // the parent change
      TextEditBasedChangeGroup newChangeGroup = new TextEditBasedChangeGroup(
          textChange, group);
      newChangeGroup.setEnabled(changeGroup.isEnabled());
      textChange.addChangeGroup(newChangeGroup);
    }
  }

  /**
   * Removes a text edit from a group, optionally updating its owner change. If
   * the edit is the root edit of the owner change, the change will be removed
   * from its parent.
   * 
   * @param edit the text edit
   * @param group the text edit group to update, optional
   * @param change the change to update, optional
   * @return true if the text edit was removed
   */
  public static boolean removeTextEdit(TextEdit edit, TextEditGroup group,
      TextEditBasedChange change) {
    boolean removed = false;
    boolean removeChange = false;

    // First remove this edit from its parent, if it has one
    TextEdit parentEdit = edit.getParent();
    if (parentEdit != null) {
      removed |= parentEdit.removeChild(edit);
      if (!parentEdit.hasChildren()) {
        // This parent edit is now empty, so remove it from the change and group
        edit = parentEdit;
      }
    }

    // Remove the edit from the group
    if (group != null) {
      removed |= group.removeTextEdit(edit);
      if (group.getTextEdits().length == 0) {
        // The group has no more edits. We'd like to remove it from the change,
        // but there is no API. Instead, see if this group is the only group in
        // the change and trigger removing the change altogether.
        if (change != null) {
          TextEditBasedChangeGroup[] changeGroups = change.getChangeGroups();
          if (changeGroups.length == 1
              && changeGroups[0].getTextEditGroup().equals(group)) {
            // This is the only group in the change, remove the change
            removeChange = true;
          }
        }
      }
    }

    // Remove the change if this was its root edit
    if (!removeChange && change != null && change instanceof TextFileChange) {
      TextFileChange textFileChange = (TextFileChange) change;
      if (edit.equals(textFileChange.getEdit())) {
        removeChange = true;
      }
    }

    // Execute change removal
    if (removeChange && change != null) {
      Change parentChange = change.getParent();
      if (parentChange instanceof CompositeChange) {
        removed |= ((CompositeChange) parentChange).remove(change);
      }
    }

    return removed;
  }

  /**
   * Replaces an existing TextEdit (which is reachable from the given list of
   * TextEditGroups) with another TextEdit.
   * <p>
   * If the TextEdit is a root of a TextEdit tree, the TextEditGroup's reference
   * will be updated. If it is not a root, its parent TextEdit's reference will
   * be updated.
   * 
   * @return whether a replacement occurred
   */
  public static boolean replaceTextEdit(List<TextEditGroup> textEditGroups,
      TextEdit oldEdit, TextEdit newEdit) {

    TextEdit parentEdit = oldEdit.getParent();
    if (parentEdit != null) {
      // This is not a root edit, so just replace the edit in the parent
      return replaceTextEdit(parentEdit, oldEdit, newEdit);
    }

    // This is a root edit, find the corresponding group and replace it there
    for (TextEditGroup group : textEditGroups) {
      TextEdit[] edits = group.getTextEdits();
      if (!replaceTextEdit(oldEdit, newEdit, edits)) {
        return false;
      }

      // Replace text edits, in order
      group.clearTextEdits();
      // We already swapped the edit in the edits array, add the array back
      for (TextEdit edit : edits) {
        group.addTextEdit(edit);
      }
    }

    return true;
  }

  private static boolean replaceTextEdit(TextEdit parentEdit, TextEdit oldEdit,
      TextEdit newEdit) {
    // The text edit API does not allow replacing an edit, so remove all and add
    // them all back (with the replaced edit)
    TextEdit[] children = parentEdit.removeChildren();
    replaceTextEdit(oldEdit, newEdit, children);
    parentEdit.addChildren(children);

    return true;
  }

  private static boolean replaceTextEdit(TextEdit oldEdit, TextEdit newEdit,
      TextEdit[] children) {
    int index = 0;
    for (; index < children.length; index++) {
      if (children[index] == oldEdit) {
        children[index] = newEdit;
        break;
      }
    }

    return index != children.length;
  }

  /* Non-instantiable */
  private TextEditUtilities() {
  }
}
