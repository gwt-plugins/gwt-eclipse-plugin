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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;

/**
 * Key adapter that hooks the <code>ENTER</code> key for toggling the expansion
 * and collapsing of tree nodes presented by a {@link TreeViewer}.
 * 
 * Note that this tree behavior is not provided on any platform (Mac, Windows,
 * Linux). It is an artifact of the way that GWT's old UI used to work. That is
 * why we are explicitly adding this functionality. Conversely, we're not adding
 * support for expanding tree nodes via arrow keys, because in the case of Mac
 * and Windows, the underlying platform provides this functionality already.
 * Linux's native Tree widget does not have behavior, and we do not want to
 * deviate from it.
 */
public class EnterKeyTreeToggleKeyAdapter extends KeyAdapter {

  private final TreeViewer treeViewer;

  /**
   * Create a new instance for the given {@link TreeViewer}. Note that this
   * method does not add this adapter to the {@link TreeViewer}'s tree. This has
   * to be done by the caller.
   */
  public EnterKeyTreeToggleKeyAdapter(TreeViewer treeViewer) {
    this.treeViewer = treeViewer;
  }

  /**
   * If a node in the tree viewer is currently selected and the ENTER key is
   * pressed, the display of the node's children will be toggled.
   */
  public void keyPressed(KeyEvent event) {
    if (event.character == '\r' || event.character == '\n') {
      enterPressed();
    }
  }

  private void enterPressed() {
    Object selection = getSelectedElement();
    if (selection == null) {
      return;
    }
    boolean expandedState = treeViewer.getExpandedState(selection);
    treeViewer.setExpandedState(selection, !expandedState);
    treeViewer.update(selection, null);
  }

  private Object getSelectedElement() {
    ISelection selection = treeViewer.getSelection();
    if (selection == null || selection.isEmpty()) {
      return null;
    }
    StructuredSelection structuredSelection = (StructuredSelection) (selection);
    return structuredSelection.getFirstElement();
  }
}
