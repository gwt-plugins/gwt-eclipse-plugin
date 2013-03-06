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

import com.google.gwt.eclipse.oophm.model.ModelLabelProvider;
import com.google.gwt.eclipse.oophm.model.TreeContentProvider;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModel;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

class TreeNavigationView extends SelectionProvidingComposite {
  private final SashForm sashForm;
  private final TreeViewer contentTypes;
  private final ContentPanel contentPanel;

  public TreeNavigationView(Composite parent, int style) {
    super(parent, style);
    setLayout(new FillLayout(SWT.VERTICAL));

    sashForm = new SashForm(this, SWT.HORIZONTAL);
    contentTypes = new TreeViewer(sashForm);
    contentTypes.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        fireSelectionChangedEvent(event);
        
        Object contentPanelSelection = null;
        ISelection selection = contentTypes.getSelection();
        if (selection != null && !selection.isEmpty()) {
          contentPanelSelection = ((ITreeSelection) selection).getFirstElement();
        }
        
        contentPanel.setSelection(contentPanelSelection);
      }
    });
    
    contentPanel = new ContentPanel(sashForm, SWT.NONE);

    contentTypes.setComparator(new ModelNodeViewerComparator());
    sashForm.setWeights(new int[] {20, 80});
    contentTypes.setLabelProvider(new ModelLabelProvider());
    contentTypes.setContentProvider(new TreeContentProvider());
    contentTypes.getTree().addKeyListener(
        new EnterKeyTreeToggleKeyAdapter(contentTypes));
  }

  public ISelection getSelection() {
    return contentTypes.getSelection();
  }

  public void setInput(WebAppDebugModel model) {
    contentTypes.setInput(model);
  }

  public void setSelection(ISelection selection) {
    contentTypes.setSelection(selection);
  }
}