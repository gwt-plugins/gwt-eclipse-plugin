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

import org.eclipse.swt.widgets.Composite;

/**
 * Methods that update the contents of a SelectableControlList can wrap their
 * updates in a SelectableControlListUpdateCallback in order to properly isolate
 * modifications to the UI (e.g. redraws are suspended until modifications are
 * complete).
 */
public interface SelectableControlListUpdateCallback {

  /**
   * The callback.
   * 
   * @param scrolledContents The scrolling canvas (e.g. potentially larger that
   *          the display area) used to perform actions (e.g. construct new
   *          children).
   */
  void execute(Composite scrolledContents);

}
