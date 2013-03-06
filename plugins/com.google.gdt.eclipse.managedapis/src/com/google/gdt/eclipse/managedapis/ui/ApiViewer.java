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

import com.google.gdt.eclipse.core.EclipseUtilities;
import com.google.gdt.eclipse.core.ui.controls.SelectableControlList;
import com.google.gdt.eclipse.core.ui.controls.SelectionChangeListener;
import com.google.gdt.eclipse.core.ui.viewers.BaseSelectableControlListControlFactory;
import com.google.gdt.eclipse.core.ui.viewers.SelectableControlListControlFactory;
import com.google.gdt.eclipse.core.ui.viewers.SelectableControlListViewer;
import com.google.gdt.eclipse.managedapis.Resources;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiListing;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiListingSource;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiListingSourceFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.Comparator;

/**
 * Simple container of a ListingViewer. Creates a control with no layout data.
 * 
 * Filter features (can) borrow from FilteredTree. Look to that code as
 * touchpoint.
 */
public class ApiViewer {
  private static String INITIAL_SEARCH_TEXT = "Search Text";

  private static final int ICON_CANCEL = 1 << 8;
  
  protected final IRunnableContext context;

  private SelectableControlListViewer<ManagedApiEntry, ApiListingItem> apiViewer;

  private Composite container;

  private ManagedApiListing listing;

  private ManagedApiEntryContentProvider managedApiContentProvider;

  private Comparator<ManagedApiEntry> managedApiEntryDefaultComparator = new Comparator<ManagedApiEntry>() {
    Collator collator = Collator.getInstance();

    public int compare(ManagedApiEntry a, ManagedApiEntry b) {
      int c1 = (b.getRanking() - a.getRanking());
      if (c1 != 0) {
        return (c1 < 0) ? -1 : 1;
      } else {
        return collator.compare(a.getDisplayName(), b.getDisplayName());
      }
    }
  };

  private ManagedApiListingSourceFactory managedApiListingSourceFactory;

  private Resources resources;

  private SelectionProvider selectionProvider;
  private ApiSearchFilter searchFilter;

  public ApiViewer(IRunnableContext context,
      ManagedApiListingSourceFactory managedApiListingSourceFactory) {
    super();
    this.context = context;
    this.managedApiListingSourceFactory = managedApiListingSourceFactory;
    this.selectionProvider = new SelectionProvider();
  }
  
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    selectionProvider.addSelectionChangedListener(listener);
  }

  public void createControl(Composite parent) {
    container = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(container);
    
    int textFlags = SWT.SEARCH;
    
    if (EclipseUtilities.isAtLeastEclipse35()) {
      textFlags |= ICON_CANCEL;
    }
    
    final Text filterText = new Text(container, textFlags);
    filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    filterText.setText(INITIAL_SEARCH_TEXT);

    searchFilter = new ApiSearchFilter();
    filterText.addSelectionListener(new SelectionAdapter() {
      public void widgetDefaultSelected(SelectionEvent e) {
        if (e.detail == SWT.CANCEL) {
          searchFilter.setSearchQuery("");
        }
      }
    });

    filterText.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        if (filterText.getText().equals(INITIAL_SEARCH_TEXT)) {
          // We cannot call clearText(), see
          // https://bugs.eclipse.org/bugs/show_bug.cgi?id=260664
          if (filterText != null) {
            filterText.setText("");
          }
        }
      }

      public void focusLost(FocusEvent e) {
      }

    });
    filterText.addKeyListener(new KeyListener() {
      public void keyPressed(KeyEvent e) {
        if (apiViewer != null && e.character != 0) {
          apiViewer.clearSelection();
        }
      }

      public void keyReleased(KeyEvent e) {
        if (apiViewer != null && e.character != 0) {
          container.getDisplay().asyncExec(new Runnable() {
            public void run() {
              String searchQuery = filterText.getText().trim();
              searchFilter.setSearchQuery(INITIAL_SEARCH_TEXT.equals(searchQuery)
                  ? "" : filterText.getText());
            }
          });
        }
      }
    });

    filterText.setFocus();
    
    SelectableControlList<ApiListingItem> selectableControlList = new SelectableControlList<ApiListingItem>(
        container, SWT.V_SCROLL | SWT.BORDER);
    selectableControlList.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    apiViewer = new SelectableControlListViewer<ManagedApiEntry, ApiListingItem>(
        selectableControlList);
    managedApiContentProvider = new ManagedApiEntryContentProvider(
        managedApiEntryDefaultComparator);
    apiViewer.setContentProvider(managedApiContentProvider);
    ManagedApiEntryControlFactory controlFactory = new ManagedApiEntryControlFactory(
        resources);
    apiViewer.setControlFactory(controlFactory);
    GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 100).applyTo(
        apiViewer.getSelectableControlList());

    apiViewer.setContentFilter(searchFilter);
  }

  public Control getControl() {
    return container;
  }

  public void registerSelectionChangeListener(
      SelectionChangeListener<ManagedApiEntry> selectionChangeListener) {
    apiViewer.addSelectionChangeListener(selectionChangeListener);
  }

  public void registerSelectionListener(SelectionListener selectionListener) {
    apiViewer.addSelectionListener(selectionListener);
  }

  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    selectionProvider.removeSelectionChangedListener(listener);
  }

  public void setManagedApiListingSourceFactory(
      ManagedApiListingSourceFactory managedApiListingSourceFactory) {
    this.managedApiListingSourceFactory = managedApiListingSourceFactory;
  }
  
  public void setResources(Resources resources) {
    this.resources = resources;
  }

  public IStatus updateListing(IProgressMonitor monitor) {
    final ManagedApiListingSource managedApiListingSource = managedApiListingSourceFactory.buildManagedApiListingSource();

    IStatus result = managedApiListingSource.run(monitor);

    if (!result.isOK()) {
      return result;
    }

    listing = managedApiListingSource.getManagedApiListing();

    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        managedApiContentProvider.update(listing);
        // If the results are not empty, we could select the first item in the
        // list.
      }
    });

    return Status.OK_STATUS;
  }

  protected IStatus computeStatus(InvocationTargetException e, String message) {
    Throwable cause = e.getCause();
    if (cause.getMessage() != null) {
      message = "";
    }
    return new Status(IStatus.ERROR, "", message, e);
  }
}

class ManagedApiEntryControlFactory extends
    BaseSelectableControlListControlFactory implements
    SelectableControlListControlFactory<ManagedApiEntry, ApiListingItem> {

  private Resources resources;

  public ManagedApiEntryControlFactory(Resources resources) {
    this.resources = resources;
  }

  public ApiListingItem createControl(Composite parent, ManagedApiEntry element) {
    ApiListingItem control = new ApiListingItem(parent, element, resources);
    addSeparator(parent);
    return control;
  }
}
