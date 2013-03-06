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
package com.google.gdt.eclipse.core.ui.controls;

import org.eclipse.swt.events.SelectionListener;

/**
 * The SelectableControl interface defines necessary methods for any control the
 * user can select. The actual method for selection (mouse-click, keyboard
 * input, etc.) is undefined by this interface, but the provided base
 * implementation provides support for mouse event handling.
 */
public interface SelectableControl {
  void addSelectionChangeListener(
      SelectionChangeListener<SelectableControl> selectionChangeListener);

  void addSelectionListener(SelectionListener selectionListener);

  SelectionListener[] getSelectionListeners();

  boolean isDisabled();

  boolean isSelected();

  void removeSelectionListener(SelectionListener listener);

  void setSelected(boolean selected);
}
