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
package com.google.gdt.eclipse.core.ui;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.resources.CoreImages;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.ui.SdkTable.SdkManagerStateChangeListener.StateChangeEvent;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Tabular view of a set of {@link Sdk}s which also allows {@link Sdk}s to be added, removed or downloaded.
 *
 * @param <T>
 *          type of Sdk that is managed
 */
public abstract class SdkTable<T extends Sdk> extends Composite {

  /**
   *
   */
  public interface SdkManagerStateChangeListener {
    /**
     *
     */
    class StateChangeEvent {
      private final SdkTable<? extends Sdk> sdkManager;

      public StateChangeEvent(SdkTable<? extends Sdk> sdkManager) {
        this.sdkManager = sdkManager;
      }

      public SdkTable<? extends Sdk> getControl() {
        return sdkManager;
      }
    }

    void onSdkManagerStateChanged(StateChangeEvent event);
  }

  class ColumnLabelProvider extends LabelProvider implements ITableLabelProvider {
    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      switch (columnIndex) {
      case 0:
        IStatus validationStatus = ((Sdk) element).validate();
        if (!validationStatus.isOK()) {
          ImageRegistry imageRegistry = CorePlugin.getDefault().getImageRegistry();
          return imageRegistry.get(CoreImages.INVALID_SDK_ICON);
        }

        return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
      default:
        return null;
      }
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
      Sdk sdk = (Sdk) element;
      switch (columnIndex) {
      case 0:
        return sdk.getName();
      case 1:
        return sdk.getVersion();
      case 2:
        return sdk.getInstallationPath().toOSString();
      default:
        return "";
      }
    }
  }

  private final Button dowloadButton;
  private final Button removeButton;
  private final SdkSet<T> sdks;
  private final CheckboxTableViewer sdkTableViewer;
  private final SdkManagerStateChangeListener stateChangeListener;
  private final Table table;
  private final DialogPage dialogPage;

  public SdkTable(Composite parent, int style, SdkSet<T> startingSdks,
      SdkManagerStateChangeListener stateChangeListener, DialogPage dialogPage) {
    super(parent, style);
    setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

    this.dialogPage = dialogPage;
    this.stateChangeListener = stateChangeListener;

    this.sdks = startingSdks;

    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    setLayout(layout);

    final Label headerLabel = new Label(this, SWT.WRAP);
    final GridData headerLabelGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    headerLabelGridData.widthHint = 250;
    headerLabel.setLayoutData(headerLabelGridData);
    headerLabel.setText(
        "Add, remove or download SDKs.\n\nBy default, the checked SDK is added to the build path of newly created projects.");

    Composite panel = this;
    final Label spacerLabel = new Label(panel, SWT.NONE);
    final GridData spacerLabelGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    spacerLabelGridData.heightHint = 1;
    spacerLabel.setLayoutData(spacerLabelGridData);

    final Composite versionsPanel = new Composite(panel, SWT.NONE);
    versionsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    final GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    gridLayout.numColumns = 2;
    versionsPanel.setLayout(gridLayout);

    final Label tableHeaderLabel = new Label(versionsPanel, SWT.NONE);
    tableHeaderLabel.setText("SDKs:");
    GridDataFactory.fillDefaults().span(2, 1).applyTo(tableHeaderLabel);

    sdkTableViewer = CheckboxTableViewer.newCheckList(versionsPanel, SWT.FULL_SELECTION | SWT.BORDER);
    sdkTableViewer.setContentProvider(new ArrayContentProvider());
    sdkTableViewer.setLabelProvider(new ColumnLabelProvider());
    sdkTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateRemoteButtonEnabled();
        SdkTable.this.dialogPage.setMessage(null, IMessageProvider.NONE);

        // NOTE: It is bad form that this control sets the dialog page's message
        // directly.
        ISelection selection = event.getSelection();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
          IStructuredSelection sselection = (IStructuredSelection) selection;
          Object firstElement = sselection.getFirstElement();
          Sdk sdk = (Sdk) firstElement;
          IStatus validationStatus = sdk != null ? sdk.validate() : Status.OK_STATUS;
          if (!validationStatus.isOK()) {
            SdkTable.this.dialogPage.setMessage(validationStatus.getMessage(), IMessageProvider.WARNING);
          }
        }
      }
    });
    sdkTableViewer.addCheckStateListener(new ICheckStateListener() {
      @Override
      @SuppressWarnings("unchecked")
      public void checkStateChanged(CheckStateChangedEvent event) {
        // Only one GWT runtime can be the default
        if (event.getChecked()) {
          T sdk = (T) event.getElement();
          sdks.setDefault(sdk);
        }

        updateControls();

        fireStateChangedEvent();
      }
    });

    table = sdkTableViewer.getTable();
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    GridDataFactory.fillDefaults().grab(true, true).hint(200, 200).applyTo(table);

    final TableColumn nameTableColumn = new TableColumn(table, SWT.NONE);
    nameTableColumn.setWidth(110);
    nameTableColumn.setText("Name");

    final TableColumn versionTableColumn = new TableColumn(table, SWT.NONE);
    versionTableColumn.setWidth(53);
    versionTableColumn.setText("Version");

    final TableColumn locationTableColumn = new TableColumn(table, SWT.NONE);
    locationTableColumn.setWidth(600);
    locationTableColumn.setText("Location");

    final Composite buttonsPanel = new Composite(versionsPanel, SWT.NONE);
    buttonsPanel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    final GridLayout buttonsPanelGridLayout = new GridLayout();
    buttonsPanelGridLayout.marginHeight = 0;
    buttonsPanelGridLayout.marginWidth = 0;
    buttonsPanel.setLayout(buttonsPanelGridLayout);

    final Button addButton = new Button(buttonsPanel, SWT.NONE);
    final GridData addButtonGridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
    addButtonGridData.widthHint = 76;
    addButton.setLayoutData(addButtonGridData);
    addButton.setText("Add...");
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IStatus status = doAddSdk();
        if (status.isOK()) {
          updateControls();
          fireStateChangedEvent();
        }
      }
    });

    removeButton = new Button(buttonsPanel, SWT.NONE);
    final GridData removeButtonGridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
    removeButtonGridData.widthHint = 76;
    removeButton.setLayoutData(removeButtonGridData);
    removeButton.setText("Remove");
    removeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        removeSelectedSdk();
        updateControls();
        fireStateChangedEvent();
      }
    });

    dowloadButton = new Button(buttonsPanel, SWT.NONE);
    removeButtonGridData.widthHint = 76;
    dowloadButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    dowloadButton.setText("Download...");
    dowloadButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IStatus status = doDownloadSdk();
        if (status.isOK()) {
          updateControls();
          fireStateChangedEvent();
        }
      }
    });

    final Label footerLabel = new Label(panel, SWT.NONE);
    footerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    sdkTableViewer.setInput(sdks);
    sdkTableViewer.setContentProvider(new IStructuredContentProvider() {
      @Override
      public void dispose() {
      }

      @Override
      public Object[] getElements(Object inputElement) {
        return sdks.toArray();
      }

      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    });

    updateControls();
    updateRemoteButtonEnabled();
  }

  @SuppressWarnings("unchecked")
  public T getCheckedSdk() {
    Object[] checkedElements = sdkTableViewer.getCheckedElements();
    if (checkedElements.length > 0) {
      assert (checkedElements.length == 1);
      return (T) checkedElements[0];
    }

    return null;
  }

  protected abstract IStatus doAddSdk();

  protected abstract IStatus doDownloadSdk();

  private void fireStateChangedEvent() {
    if (stateChangeListener != null) {
      StateChangeEvent event = new StateChangeEvent(this);
      stateChangeListener.onSdkManagerStateChanged(event);
    }
  }

  @SuppressWarnings("unchecked")
  private T getSelectedSdk() {
    IStructuredSelection selection = (IStructuredSelection) sdkTableViewer.getSelection();
    assert (selection.size() <= 1);
    return (T) selection.getFirstElement();
  }

  private void removeSelectedSdk() {
    T selectedSdk = getSelectedSdk();
    // Remove button is disabled if there are not SDKs selected.
    assert (selectedSdk != null);

    sdks.remove(selectedSdk);
    sdkTableViewer.refresh();
  }

  private void updateControls() {
    sdkTableViewer.refresh();
    if (sdks.getDefault() != null) {
      sdkTableViewer.setCheckedElements(new Object[] { sdks.getDefault() });
    } else {
      assert (sdks.isEmpty());
    }
  }

  private void updateRemoteButtonEnabled() {
    IStructuredSelection selection = (IStructuredSelection) sdkTableViewer.getSelection();
    removeButton.setEnabled(selection.size() != 0);
  }

}
