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
package com.google.gdt.eclipse.core.ui.viewers;

import com.google.gdt.eclipse.core.ui.controls.SelectableControl;

import org.eclipse.swt.widgets.Composite;

/**
 * This interface defines the required signature for the control factories.
 * 
 * @param <T> The type of the model.
 * @param <C> The type of the SelectableControl
 */
public interface SelectableControlListControlFactory<T, C extends SelectableControl> {
  C createControl(Composite parent, T element);
}
