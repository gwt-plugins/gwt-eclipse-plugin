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

/**
 * Abstract base class for {@link AutocompleteEntry}. None of the methods in here need to be
 * overridden. This is strictly common code.
 *
 * @author ganetsky@google.com (Jason Ganetsky)
 */
@SuppressWarnings("serial")
public abstract class AbstractAutocompleteEntry implements AutocompleteEntry {

  private static final String VARARGS_MARKER = "...";
  
  private final String description;

  protected AbstractAutocompleteEntry(String description) {
    this.description = description;
  }

  @Override
  public final String getDescription() {
    return description;
  }

  /**
   * This method returns a "sanitized" version of the type name:
   * 1. If the type is of the form "Foo.Bar", it returns "Bar"
   * 2. Otherwise, if the type is well-formed and not fully qualified (eg: not
   *    like Foo..Bar or Foo.Bar.Baz), it just returns the type name that was
   *    passed in.
   */
  protected String sanitizeTypeNames(String typeName) {
    if (typeName.contains(AutocompleteEntry.DELIMITER) && !typeName.contains(VARARGS_MARKER)) {
      String[] parts = typeName.split("\\" + AutocompleteEntry.DELIMITER);
      return parts[parts.length - 1];
    }
    return typeName;
  }

  @Override
  public String getDisplayReturnType() {
    return sanitizeTypeNames(getReturnType());
  }

  @Override
  public int compareTo(AutocompleteEntry o) {
    return getPopupView().compareTo(o.getPopupView());
  }

  @Override
  public int hashCode() {
    return getPopupView().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!getClass().equals(o.getClass())) {
      return false;
    }

    return ((AutocompleteEntry) o).getPopupView().equals(getPopupView());
  }
}
