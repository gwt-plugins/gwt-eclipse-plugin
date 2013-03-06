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
package com.google.gwt.eclipse.core.launch.ui;

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.dialogs.ModuleSelectionDialog;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.resources.GWTImages;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import java.util.List;

/**
 * Selects a set of modules from a GWT project. Only modules defined by the
 * ProjectModulesSelectionBlock are included (no modules from JAR's).
 */
@SuppressWarnings("restriction")
public class EntryPointModulesSelectionBlock {

  /**
   * Change listener used by EntryPointModulesSelectionBlock.
   */
  public static interface IModulesChangeListener {
    void onModulesChanged();
  }

  private class ModulesLabelProvider extends LabelProvider {

    private final Image elementImage;

    public ModulesLabelProvider() {
      elementImage = GWTPlugin.getDefault().getImage(GWTImages.MODULE_ICON);
    }

    @Override
    public Image getImage(Object element) {
      return elementImage;
    }

    @Override
    public String getText(Object element) {
      String qualifiedModuleName = (String) element;
      return Signature.getSimpleName(qualifiedModuleName) + " - "
          + Signature.getQualifier(qualifiedModuleName);
    }
  }

  private class ModulesSelectionAdapter implements IListAdapter {

    @SuppressWarnings("unchecked")
    public void customButtonPressed(ListDialogField field, int index) {
      List<String> beforeModules = modulesField.getElements();

      if (index == IDX_ADD) {
        addEntry();
      } else if (index == IDX_REMOVE) {
        removeSelectedEntries();
      } else if (index == IDX_SET_DEFAULTS) {
        setDefaults();
      }

      // Notify the listener if our list of modules changes
      notifyListenerIfChanged(beforeModules);
    }

    public void doubleClicked(ListDialogField field) {
    }

    public void selectionChanged(ListDialogField field) {
      modulesField.enableButton(IDX_REMOVE,
          !modulesField.getSelectedElements().isEmpty());
    }
  }

  private static final int IDX_ADD = 0;

  private static final int IDX_REMOVE = 1;

  private static final int IDX_SET_DEFAULTS = 3;

  private List<String> defaultModules;

  private IJavaProject javaProject;

  private final String labelText;

  private final IModulesChangeListener listener;

  private ListDialogField modulesField;

  private Composite parent;

  public EntryPointModulesSelectionBlock(IModulesChangeListener listener) {
    this(null, listener);
  }

  public EntryPointModulesSelectionBlock(String labelText,
      IModulesChangeListener listener) {
    this.labelText = labelText;
    this.listener = listener;

    createListField();
  }

  public void doFillIntoGrid(Composite parent, int columns) {
    this.parent = parent;
    modulesField.doFillIntoGrid(parent, columns);

    GridData modulesFieldGridData = (GridData) modulesField.getListControl(
        parent).getLayoutData();
    modulesFieldGridData.grabExcessHorizontalSpace = true;
    modulesFieldGridData.grabExcessVerticalSpace = true;
    modulesField.getListControl(parent).setLayoutData(modulesFieldGridData);
  }

  public List<String> getDefaultModules() {
    return defaultModules;
  }

  public IJavaProject getJavaProject() {
    return javaProject;
  }

  @SuppressWarnings("unchecked")
  public List<String> getModules() {
    return modulesField.getElements();
  }

  public boolean isDefault() {
    return getModules().equals(defaultModules);
  }

  public void removeSelectedEntries() {
    modulesField.removeElements(modulesField.getSelectedElements());
  }

  public void setDefaultModules(List<String> modules) {
    this.defaultModules = modules;
  }

  public void setEnabled(boolean enabled) {
    modulesField.setEnabled(enabled);
  }

  public void setJavaProject(IJavaProject javaProject) {
    this.javaProject = javaProject;
  }

  @SuppressWarnings("unchecked")
  public void setModules(List<String> selectedModules) {
    List<String> beforeModules = modulesField.getElements();

    if (selectedModules.isEmpty()) {
      setDefaults();
    } else {
      modulesField.setElements(selectedModules);
      modulesField.selectFirstElement();
    }

    notifyListenerIfChanged(beforeModules);
  }

  private void addEntry() {
    if (javaProject == null) {
      MessageDialog.openError(parent.getShell(), "Module Selection Error",
          "A valid Java project must be selected.");
      return;
    }

    IModule module = ModuleSelectionDialog.show(parent.getShell(), javaProject,
        false);
    if (module != null) {
      String moduleName = module.getQualifiedName();
      if (!modulesField.getElements().contains(moduleName)) {
        modulesField.addElement(moduleName);
      }
    }
  }

  private void createListField() {
    String[] buttons = new String[] {
        "Add...", "Remove", null, "Restore Defaults"};

    modulesField = new ListDialogField(new ModulesSelectionAdapter(), buttons,
        new ModulesLabelProvider());
    modulesField.setLabelText(labelText);
    modulesField.setTableColumns(new ListDialogField.ColumnsDescription(1,
        false));

    // Remove button disabled by default
    modulesField.enableButton(IDX_REMOVE, false);
  }

  private void notifyListenerIfChanged(List<String> beforeModules) {
    if (listener != null && !getModules().equals(beforeModules)) {
      listener.onModulesChanged();
    }
  }

  private void setDefaults() {
    modulesField.setElements(defaultModules);
    modulesField.selectFirstElement();
  }

}
