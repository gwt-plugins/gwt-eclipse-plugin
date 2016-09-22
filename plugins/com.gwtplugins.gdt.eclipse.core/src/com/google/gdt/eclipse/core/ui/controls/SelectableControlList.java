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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import java.util.ArrayList;
import java.util.List;

/**
 * The SelectableControlList represents a UI list component with custom controls
 * as elements. The type of control is parameterized.
 * 
 * @param <C> The type of control.
 */
public class SelectableControlList<C extends SelectableControl> extends
    ScrolledComposite {
  private Composite scrolledCanvas;

  @SuppressWarnings("unchecked")
  private List<C> items = (List<C>) new ArrayList<SelectableControl>();

  private List<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();

  private List<SelectionChangeListener<SelectableControl>> selectionChangeListeners = new ArrayList<SelectionChangeListener<SelectableControl>>();

  public SelectableControlList(Composite parent, int style) {
    super(parent, style);
    setupScrolledComposite();
  }

  public void addSelectionChangeListener(
      SelectionChangeListener<SelectableControl> listener) {
    if (!selectionChangeListeners.contains(listener)) {
      selectionChangeListeners.add(listener);
    }
  }

  public void addSelectionListener(SelectionListener listener) {
    if (!selectionListeners.contains(listener)) {
      selectionListeners.add(listener);
    }
  }

  public void clear() {
    for (C item : items) {
      for (SelectionListener listener : item.getSelectionListeners()) {
        item.removeSelectionListener(listener);
      }
    }
    for (Control child : scrolledCanvas.getChildren()) {
      child.dispose();
    }
    items.clear();
    doUpdateContentSize();
  }

  public void clearSelections() {
    for (SelectableControl sc : items) {
      sc.setSelected(false);
    }
  }

  public List<C> getItems() {
    return items;
  }

  public void registerItem(final C selectableControl) {
    items.add(selectableControl);

    selectableControl.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        fireDefaultSelectedEvent(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        itemSelected(selectableControl, e.stateMask);
      }
    });
    selectableControl.addSelectionChangeListener(new SelectionChangeListener<SelectableControl>() {
      public void selectionChanged(List<SelectableControl> selection) {
        fireSelectionChangeEvent();
      }
    });
  }

  @Override
  public void setBackground(Color color) {
    super.setBackground(color);

    updateBackground(scrolledCanvas, color);
  }

  public void updateContents(
      SelectableControlListUpdateCallback selectableControlListUpdateCallback) {
    scrolledCanvas.setRedraw(false);
    try {
      selectableControlListUpdateCallback.execute(scrolledCanvas);
    } finally {
      scrolledCanvas.layout(true);
      scrolledCanvas.setRedraw(true);
      doUpdateContentSize();
    }
  }

  protected void addSeparator(Composite scrolledContents) {
    // a separator between connector descriptors
    Composite separator = new Composite(scrolledContents, SWT.NULL);
    GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 1).applyTo(
        separator);
    GridLayoutFactory.fillDefaults().applyTo(separator);
    separator.addPaintListener(new BorderPainter());
  }

  protected void fireDefaultSelectedEvent(SelectionEvent e) {
    for (SelectionListener listener : selectionListeners) {
      listener.widgetDefaultSelected(e);
    }
  }

  protected void fireSelectionChangeEvent() {
    List<SelectableControl> selectedList = new ArrayList<SelectableControl>();
    for (SelectableControl item : items) {
      if (item.isSelected()) {
        selectedList.add(item);
      }
    }

    for (SelectionChangeListener<SelectableControl> listener : selectionChangeListeners) {
      listener.selectionChanged(selectedList);
    }
  }

  /**
   * 
   * @param selectableControl
   * @param stateMask
   */
  protected void itemSelected(SelectableControl control, int stateMask) {
    if (!control.isDisabled()) {
      if (ctrlClicked(stateMask)) {
        control.setSelected(!control.isSelected());
      } else {
        control.setSelected(true);
        for (SelectableControl sc : items) {
          if (sc != control) {
            sc.setSelected(false);
          }
        }
      }
    }
  }

  /**
   * Internal to ControlList
   * 
   * @param stateMask
   * @return
   */
  boolean ctrlClicked(int stateMask) {
    return (SWT.MOD1 & stateMask) != 0;
  }

  Composite getScrolledCanvas() {
    return scrolledCanvas;
  }

  private void doUpdateContentSize() {
    updateSize(scrolledCanvas);
  }

  /**
   * Do layout. Several magic #s in here...
   * 
   * @param scrolledComposite
   */
  private void setupScrolledComposite() {
    setAlwaysShowScrollBars(true);

    scrolledCanvas = new Composite(this, SWT.NONE);
    GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(scrolledCanvas);

    setMinWidth(100);
    setMinHeight(100);
    setExpandHorizontal(true);
    setExpandVertical(true);
    setMinHeight(1);

    Point size = scrolledCanvas.computeSize(getParent().getSize().x,
        SWT.DEFAULT, true);
    scrolledCanvas.setSize(size);

    addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        doUpdateContentSize();
        updateScrollIncrements();
      }
    });
    setContent(scrolledCanvas);
  }

  private void updateBackground(Composite composite, Color background) {
    composite.setBackground(background);

    for (Control control : composite.getChildren()) {
      if (control instanceof Composite) {
        updateBackground((Composite) control, background);
      } else {
        control.setBackground(background);
      }
    }
  }

  private void updateScrollIncrements() {
    int pageInc = getBounds().height;

    getVerticalBar().setIncrement(20);
    getVerticalBar().setPageIncrement(pageInc);
  }

  private void updateSize(Control control) {
    if (control == null) {
      return;
    }
    Point size = control.computeSize(getClientArea().width - 20, SWT.DEFAULT,
        true);
    control.setSize(size);
    setMinSize(size);
  }

}
