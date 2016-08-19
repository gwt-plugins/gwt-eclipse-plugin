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

import java.util.List;

/**
 * A SelectionChangeListener provides selection information when the selection
 * changes in a control (or view).
 * 
 * @param <T> The type of element being selected. This could represent a UI
 *          control, or an underlying model type.
 * 
 * @TODO           comment: Generally, selections are passed around as
 *                 ISelections, which are really IStructuredSelections, which
 *                 are really StructuredSelections. It's a lot of abstraction,
 *                 but the benefit of doing it that way is that lots of places
 *                 in Eclipse can interoperate with your selections.
 */
public interface SelectionChangeListener<T> {

  /**
   * Selection events trigger this call with a list representing the new
   * selection.
   */
  void selectionChanged(List<T> selection);

}
