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
package com.google.gdt.eclipse.managedapis.ui;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * The SelectionProvider provides an access point for reading Selection
 * information.
 */
public class SelectionProvider extends EventManager implements
    ISelectionProvider, ISelectionChangedListener {
  private ISelection selection;

  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    addListenerObject(listener);
  }

  public ISelection getSelection() {
    return selection;
  }

  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    removeListenerObject(listener);
  }

  public void selectionChanged(final SelectionChangedEvent event) {
    this.selection = event.getSelection();
    Object[] listeners = getListeners();
    for (int i = 0; i < listeners.length; ++i) {
      final ISelectionChangedListener listener = (ISelectionChangedListener) listeners[i];
      SafeRunner.run(new SafeRunnable() {
        public void run() {
          listener.selectionChanged(event);
        }
      });
    }
  }

  public void setSelection(ISelection selection) {
    selectionChanged(new SelectionChangedEvent(this, selection));
  }

}
