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
import com.google.gdt.eclipse.core.ui.controls.SelectableControlList;
import com.google.gdt.eclipse.core.ui.controls.SelectableControlListUpdateCallback;
import com.google.gdt.eclipse.core.ui.controls.SelectionChangeListener;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SelectableControlListViewer provides a generic implementation of a
 * scrollable, selectable ListViewer. The type follows patterns common in JFace
 * types where a model type provides the backing data and another type of
 * Control provides the presentation version of the data.
 * 
 * @param <T> The backing type.
 * @param <C> The type of the presentation control.
 */
public class SelectableControlListViewer<T, C extends SelectableControl> {

  private SelectableControlListContentProvider<T> contentProvider;

  private SelectableControlListControlFactory<T, C> controlFactory;

  private SelectableControlList<C> selectableControlList;

  private ArrayList<SelectionChangeListener<T>> selectionChangeListeners = new ArrayList<SelectionChangeListener<T>>();

  private List<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();

  private SelectableControlListContentFilter<T> filter;

  private List<T> displayElements;

  public SelectableControlListViewer(Composite parent, int style) {
    this(new SelectableControlList<C>(parent, style));
  }

  public SelectableControlListViewer(
      SelectableControlList<C> selectableControlList) {
    this.selectableControlList = selectableControlList;
    this.selectableControlList.addSelectionListener(new SelectionAdapter() {
      public void widgetDefaultSelected(SelectionEvent e) {
        fireDefaultSelectedEvent(e);
      }
    });
    this.selectableControlList.addSelectionChangeListener(new SelectionChangeListener<SelectableControl>() {
      public void selectionChanged(List<SelectableControl> selection) {
        fireSelectionChangeEvent();
      }
    });
  }

  public void addSelectionChangeListener(SelectionChangeListener<T> listener) {
    if (!selectionChangeListeners.contains(listener)) {
      selectionChangeListeners.add(listener);
    }
  }

  public void addSelectionListener(SelectionListener listener) {
    if (!selectionListeners.contains(listener)) {
      selectionListeners.add(listener);
    }
  }

  public void clearSelection() {
    selectableControlList.clearSelections();
  }

  public SelectableControlList<C> getSelectableControlList() {
    return selectableControlList;
  }

  public void setContentFilter(SelectableControlListContentFilter<T> filter) {
    this.filter = filter;
    filter.addChangeListener(new ChangeListener() {
      public void onChange() {
        update();
      }
    });
  }

  public void setContentProvider(
      SelectableControlListContentProvider<T> contentProvider) {
    this.contentProvider = contentProvider;
    contentProvider.addChangeListener(new ChangeListener() {
      public void onChange() {
        update();
      }
    });
  }

  public void setControlFactory(
      SelectableControlListControlFactory<T, C> controlFactory) {
    this.controlFactory = controlFactory;
  }

  public void update() {
    List<T> previousDisplayElements = displayElements;
    List<T> filteredElements = new ArrayList<T>();
    if (contentProvider != null) {
      T[] elements = contentProvider.getElements();
      if (filter == null) {
        filteredElements.addAll(Arrays.asList(elements));
      } else {
        for (T element1 : elements) {
          T filteredElement = filter.apply(element1);
          if (filteredElement != null) {
            filteredElements.add(filteredElement);
          }
        }
      }
    }
    displayElements = filteredElements;
    if (!displayElements.equals(previousDisplayElements)) {
      selectableControlList.clear();
      selectableControlList.updateContents(new SelectableControlListUpdateCallback() {
        public void execute(Composite scrolledContents) {
          if (contentProvider != null && controlFactory != null) {
            for (T element : displayElements) {
              C control = controlFactory.createControl(scrolledContents,
                  element);
              selectableControlList.registerItem(control);
            }
          }
        }
      });
    }
  }

  protected void fireDefaultSelectedEvent(SelectionEvent e) {
    for (SelectionListener listener : selectionListeners) {
      listener.widgetDefaultSelected(e);
    }
  }

  protected void fireSelectionChangeEvent() {
    SelectableControl[] items = selectableControlList.getItems().toArray(
        new SelectableControl[selectableControlList.getItems().size()]);

    List<T> selections = new ArrayList<T>();
    for (int i = 0; i < displayElements.size() && i < items.length; i++) {
      if (items[i].isSelected())
        selections.add(displayElements.get(i));
    }

    for (SelectionChangeListener<T> listener : selectionChangeListeners) {
      listener.selectionChanged(selections);
    }
  }
}
