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

import com.google.common.base.Objects;

/**
 * This class represents a field (emum/variable) for the autocomplete system.
 */
@SuppressWarnings("serial")
public class FieldAutocompleteEntry extends AbstractAutocompleteEntry {

  private String type;
  private String name;

  public FieldAutocompleteEntry(String type, String name, String description) {
    super(description);
    this.type = type;
    this.name = name;
  }

  @Override
  public String getPopupView() {
    return name;
  }

  @Override
  public String getInsertedView() {
    return name;
  }

  @Override
  public String getReturnType() {
    return type;
  }

  @Override
  public String getEntryName() {
    return name;
  }

  @Override
  public int getFinalCursorOffset() {
    return name.length();
  }

  @Override
  public boolean shouldAutoSelect() {
    return false;
  }

  @Override
  public int autoSelectStart() {
    return -1;
  }

  @Override
  public int autoSelectEnd() {
    return -1;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("type", type)
        .add("name", name)
        .toString();
  }
}