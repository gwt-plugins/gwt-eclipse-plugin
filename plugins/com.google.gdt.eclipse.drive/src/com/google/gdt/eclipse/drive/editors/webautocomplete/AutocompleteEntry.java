/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.editors.webautocomplete;

import java.io.Serializable;

/**
 * This interface should be implemented by any type that will be exposed in the
 * autocomplete system. It defines a set of functions that lay out how the
 * entry will be shown in different contexts, as well as some semantic
 * information about the entry itself.
 *
 * @author nikhilsinghal@google.com (Nikhil Singhal)
 */
public interface AutocompleteEntry extends Comparable<AutocompleteEntry>, Serializable {

  static String DELIMITER = ".";

  /**
   * @return The way this entry should be shown in the autocomplete popup.
   */
  String getPopupView();
  
  /**
   * @return The description that should appear in content assist.
   */
  String getDescription();

  /**
   * @return The full string that should be inserted into the editor when this
   *     item is selected.
   */
  String getInsertedView();

  /**
   * @return The type returned by this entry.
   */
  String getReturnType();

  /**
   * @return The type returned by this entry in a format suitable for
   *     displaying to the user.
   */
  String getDisplayReturnType();

  /**
   * @return The raw name of the entry. For a method foo(String s, int i), this
   *     will be just foo. This is useful for figuring out autocomplete
   *     options.
   */
  String getEntryName();

  /**
   * @return The location the cursor should be positioned at after inserting
   *     the entry into the editor (relative to the entry).
   */
  int getFinalCursorOffset();

  /**
   * Returns true if this entry contains a section that should be automatically selected so the user
   * can start typing instantly. (Right now, this is true only if the parameter list for a function
   * has 1 parameter).
   */
  boolean shouldAutoSelect();

  /**
   * Returns the index at which to start auto-selecting text for the given entry. Callers
   * <b>must</b> check {@link AutocompleteEntry#shouldAutoSelect()} to decide whether or not to
   * read from this field.
   */
  int autoSelectStart();

  /**
   * Returns the index at which to end the auto-selected text for the given entry. Callers
   * <b>must</b> check {@link AutocompleteEntry#shouldAutoSelect()} to decide whether or not to
   * read from this field.
   */
  int autoSelectEnd();
}