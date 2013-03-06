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

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The BaseSelectableControlListContentProvider is a base implementation. of the
 * SelectableControl.
 */
public abstract class BaseSelectableControl extends Composite implements
    SelectableControl {

  private boolean disabled = false;
  private boolean selected;
  private List<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();
  private List<SelectionChangeListener<SelectableControl>> selectionChangeListeners = new ArrayList<SelectionChangeListener<SelectableControl>>();

  public BaseSelectableControl(Composite parent, int style) {
    super(parent, style);
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

  public SelectionListener[] getSelectionListeners() {
    return selectionListeners.toArray(new SelectionListener[selectionListeners.size()]);
  }

  public boolean isDisabled() {
    return disabled;
  }

  public boolean isSelected() {
    return selected;
  }

  public void removeSelectionListener(SelectionListener listener) {
    selectionListeners.remove(listener);
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  public void setSelected(boolean value) {
    boolean orig = this.selected;
    this.selected = value;
    if (orig != value)
      fireSelectionChangeEvent();
  }

  public String stripCRLF(String text) {
    String stripped = null;
    if (text != null) {
      stripped = text.replaceAll("(\\r\\n)|\\n|\\r", " ");
    }
    return stripped;
  }

  protected void fireDefaultSelectionEvent(MouseEvent me) {
    Event e = new Event();
    e.widget = this;
    SelectionEvent event = new SelectionEvent(e);
    event.stateMask = me.stateMask;
    event.widget = this;

    for (SelectionListener listener : selectionListeners) {
      listener.widgetDefaultSelected(event);
    }
  }

  protected void fireMouseWheelEvent(MouseEvent e) {
    // TODO handle scroll events here.
  }

  protected void fireSelectionChangeEvent() {
    for (SelectionChangeListener<SelectableControl> listener : selectionChangeListeners) {
      listener.selectionChanged(Arrays.asList(new SelectableControl[] {this}));
    }
  }

  protected void fireSelectionEvent(MouseEvent me) {
    Event e = new Event();
    e.widget = this;
    SelectionEvent event = new SelectionEvent(e);
    event.stateMask = me.stateMask;
    event.widget = this;

    for (SelectionListener listener : selectionListeners) {
      listener.widgetSelected(event);
    }
  }

  protected void listenTo(Control control) {
    control.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {
        if (isSelected())
          fireDefaultSelectionEvent(e);
      }

      @Override
      public void mouseDown(MouseEvent e) {
        fireSelectionEvent(e);
      }
    });
    control.addMouseWheelListener(new MouseWheelListener() {

      public void mouseScrolled(MouseEvent e) {
        fireMouseWheelEvent(e);
      }
    });
  }

}
