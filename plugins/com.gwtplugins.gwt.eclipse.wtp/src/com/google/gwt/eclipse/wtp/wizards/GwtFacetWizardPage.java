/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.wizards;

import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.preferences.ui.GwtPreferencePage;
import com.google.gwt.eclipse.core.runtime.GwtSdk;
import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.internal.datamodel.ui.DataModelWizardPage;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;

import java.util.List;

/**
 * Base class for GAE facets wizard pages.
 */
public class GwtFacetWizardPage extends DataModelWizardPage implements IFacetWizardPage,
    IGwtFacetConstants {

  private static final String WIZARD_NAME = IGwtFacetConstants.GWT_FACET_ID + ".install.page";

  private IFacetedProjectListener runtimeChangedListener;

  private ComboViewer comboViewer;
  private Button btnAddSdk;
  private Text textPath;
  private Button btnDirBrowser;
  private Combo comboViewerCombo;

  public GwtFacetWizardPage() {
    super(DataModelFactory.createDataModel(new AbstractDataModelProvider() {
      // fake one, to make super constructor happy, real model will be set in setConfig()
    }), WIZARD_NAME);

    setTitle("GWT Facet Configuration");
    setDescription("Complete selections below to setup the GWT facet configuration.");
  }

  @Override
  public void setConfig(Object config) {
    model.removeListener(this);
    model = (IDataModel) config;
    model.addListener(this);

    synchHelper.dispose();
    synchHelper = initializeSynchHelper(model);
  }

  @Override
  public void setWizardContext(IWizardContext context) {
    // do nothing here
  }

  @Override
  public void transferStateToConfig() {
    // do nothing here
  }

  @Override
  protected boolean showValidationErrorsOnEnter() {
    return true;
  }

  /**
   * Predefined path | [x] | Sdks Combo | |
   *
   * Custom path | [ ] | [ path to sdk ] | [browse] |
   */
  @Override
  protected Composite createTopLevelComposite(Composite parent) {
    initializeDialogUnits(parent);

    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout());

    Group sdkSelectionGroup = new Group(container, SWT.NONE);
    sdkSelectionGroup.setText("Select GWT SDK");
    sdkSelectionGroup.setLayout(new GridLayout(3, false));
    sdkSelectionGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

    Label lblSdks = new Label(sdkSelectionGroup, SWT.NONE);
    lblSdks.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    lblSdks.setText("Select from the defined SDKs");
    new Label(sdkSelectionGroup, SWT.NONE);

    Button radioDefined = new Button(sdkSelectionGroup, SWT.RADIO);
    radioDefined.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        enablePaths(false);
      }
    });
    radioDefined.setSelection(true);

    comboViewer = new ComboViewer(sdkSelectionGroup, SWT.NONE);
    comboViewer.setLabelProvider(new LabelProvider() {
      @Override
      @SuppressWarnings("unchecked")
      public String getText(Object element) {
        GwtSdk sdk = (GwtSdk) element;
        return sdk.getName();
      }
    });
    comboViewer.setContentProvider(new ArrayContentProvider());
    comboViewerCombo = comboViewer.getCombo();
    comboViewerCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    comboViewerCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent e) {
        setSdkSelection();
      }
    });

    btnAddSdk = new Button(sdkSelectionGroup, SWT.NONE);
    btnAddSdk.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        openGwtPreferencesDialog();
      }
    });
    btnAddSdk.setText("Add SDK");

    Label lblBlank = new Label(sdkSelectionGroup, SWT.NONE);
    new Label(sdkSelectionGroup, SWT.NONE);
    new Label(sdkSelectionGroup, SWT.NONE);

    Label lblCustomPath = new Label(sdkSelectionGroup, SWT.NONE);
    lblCustomPath.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    lblCustomPath.setText("Select a custom SDK location");
    new Label(sdkSelectionGroup, SWT.NONE);

    Button radioPath = new Button(sdkSelectionGroup, SWT.RADIO);
    radioPath.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        enablePaths(true);
      }
    });

    textPath = new Text(sdkSelectionGroup, SWT.BORDER);
    textPath.setEditable(false);
    textPath.setEnabled(false);
    textPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    btnDirBrowser = new Button(sdkSelectionGroup, SWT.NONE);
    btnDirBrowser.setEnabled(false);
    btnDirBrowser.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        showDirectoryDialog();
      }
    });
    btnDirBrowser.setText("Select SDK Directory");

    addSdkOptionsToCombo();

    Dialog.applyDialogFont(container);

    synchHelper.synchAllUIWithModel();

    return container;
  }

  private void openGwtPreferencesDialog() {
    if (Window.OK == PreferencesUtil.createPreferenceDialogOn(getShell(), GwtPreferencePage.ID,
        new String[] {GwtPreferencePage.ID}, null).open()) {
      addSdkOptionsToCombo();
    }
  }

  protected void setSdkSelection() {
    ISelection selection = comboViewer.getSelection();
    if (!selection.isEmpty()) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;

      // Get the selected SDK
      GwtSdk selectedGwtSdk = (GwtSdk) structuredSelection.getFirstElement();

      // Set selection to model
      model.setProperty(GWT_SDK, selectedGwtSdk);
    }
  }

  /**
   * Setup the combo viewer combo with the list of predefined sdks.
   */
  private void addSdkOptionsToCombo() {
    List<GwtSdk> sdks = GWTPreferences.getSdkManager().getSdksSortedList();

    // List selections in combo
    comboViewer.setInput(sdks);

    // Update UI
    comboViewer.refresh();

    // Select the first item
    if (comboViewerCombo.getSelectionIndex() < 0 && sdks.size() > 0) {
      GwtSdk defaultSdk = GWTPreferences.getDefaultRuntime();

      model.setProperty(GWT_SDK, defaultSdk);

      ISelection selection = new StructuredSelection(defaultSdk);

      comboViewer.setSelection(selection, true);
    }
  }

  private void showDirectoryDialog() {
    DirectoryDialog directoryDialog = new DirectoryDialog(getShell(), SWT.OPEN);
    directoryDialog.setFilterPath("Choose SDK Directory");
    directoryDialog.setMessage("Please select the root directory of your SDK installation.");

    String pathToDir = directoryDialog.open();
    if (pathToDir != null) {
      textPath.setText(pathToDir);
    }
  }

  private void enablePaths(boolean enablePathEntry) {
    comboViewerCombo.setEnabled(!enablePathEntry);
    btnAddSdk.setEnabled(!enablePathEntry);

    textPath.setEnabled(enablePathEntry);
    btnDirBrowser.setEnabled(enablePathEntry);
  }

  @Override
  protected String[] getValidationPropertyNames() {
    return new String[] {GWT_SDK};
  }

}
