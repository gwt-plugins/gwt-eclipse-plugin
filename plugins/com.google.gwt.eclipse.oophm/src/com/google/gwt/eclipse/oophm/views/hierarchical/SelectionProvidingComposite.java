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
package com.google.gwt.eclipse.oophm.views.hierarchical;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite that is a selection provider.
 */
public abstract class SelectionProvidingComposite extends Composite implements
    ISelectionProvider {

  public SelectionProvidingComposite(Composite parent, int style) {
    super(parent, style);
  }

  private final List<ISelectionChangedListener> selectionChangedListeners = new ArrayList<ISelectionChangedListener>();

  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    selectionChangedListeners.add(listener);
  }

  protected void fireSelectionChangedEvent(SelectionChangedEvent event) {
    for (ISelectionChangedListener selectionChangedListener : selectionChangedListeners) {
      selectionChangedListener.selectionChanged(event);
    }
  }

  public abstract ISelection getSelection();

  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    selectionChangedListeners.remove(listener);
  }

  public abstract void setSelection(ISelection selection);

}
