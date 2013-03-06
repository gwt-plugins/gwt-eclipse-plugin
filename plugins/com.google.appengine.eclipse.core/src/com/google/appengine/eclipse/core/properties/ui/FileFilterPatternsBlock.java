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
package com.google.appengine.eclipse.core.properties.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import java.util.List;

/**
 * Creates an editable list of filter patterns for file inclusion/exclusion.
 */
@SuppressWarnings("restriction")
public class FileFilterPatternsBlock {

  private class PatternsListLabelProvider extends LabelProvider {

    private final Image elementImage;

    public PatternsListLabelProvider() {
      ImageDescriptorRegistry registry = JavaPlugin.getImageDescriptorRegistry();

      if (isExclusions) {
        elementImage = registry.get(JavaPluginImages.DESC_OBJS_EXCLUSION_FILTER_ATTRIB);
      } else {
        elementImage = registry.get(JavaPluginImages.DESC_OBJS_INCLUSION_FILTER_ATTRIB);
      }
    }

    @Override
    public Image getImage(Object element) {
      return elementImage;
    }

    @Override
    public String getText(Object element) {
      return ((IPath) element).toString();
    }
  }

  private class PatternsListSelectionAdapter implements IListAdapter {

    public void customButtonPressed(ListDialogField field, int index) {
      if (index == IDX_ADD) {
        addEntry(field);
      } else if (index == IDX_EDIT) {
        editEntry(field);
      } else if (index == IDX_ADD_MULTIPLE) {
        addMultipleEntries(field);
      }
    }

    public void doubleClicked(ListDialogField field) {
      editEntry(field);
    }

    public void selectionChanged(ListDialogField field) {
      field.enableButton(IDX_EDIT, (field.getSelectedElements().size() == 1));
    }
  }

  private static final int IDX_ADD = 0;

  private static final int IDX_ADD_MULTIPLE = 1;

  private static final int IDX_EDIT = 2;

  private static final int IDX_REMOVE = 4;

  private final boolean isExclusions;

  private ListDialogField patternsField;

  private final IProject project;

  private final Shell shell;

  public FileFilterPatternsBlock(Composite parent, IProject project,
      boolean isExclusions, List<IPath> initialPatterns) {
    this.shell = parent.getShell();
    this.project = project;
    this.isExclusions = isExclusions;

    createContents(parent);
    initializeControls(initialPatterns);
  }

  @SuppressWarnings("unchecked")
  public List<IPath> getPatterns() {
    return patternsField.getElements();
  }

  private void addEntry(ListDialogField field) {
    FileFilterPatternDialog dialog = new FileFilterPatternDialog(shell,
        project, isExclusions, null);
    if (dialog.open() == Window.OK) {
      field.addElement(dialog.getPattern());
    }
  }

  private void addMultipleEntries(ListDialogField field) {
    String title = (isExclusions ? "Exclusion" : "Inclusion") + " Selection";
    String message = "Choose files or folders to "
        + (isExclusions ? "exclude" : "include") + ":";
    List<IPath> patterns = FileFilterPatternDialog.choosePatterns(shell, title,
        message, project, null, true);
    if (patterns != null) {
      field.addElements(patterns);
    }
  }

  private void createContents(Composite parent) {
    Composite grid = new Composite(parent, SWT.NONE);
    grid.setLayout(new GridLayout(3, false));
    grid.setLayoutData(new GridData(GridData.FILL_BOTH));

    String[] addButtons = new String[] {
        "Add...", "Add Multiple...", "Edit...", null
        /* null give us a little extra space before "Remove" */, "Remove"};

    patternsField = new ListDialogField(new PatternsListSelectionAdapter(),
        addButtons, new PatternsListLabelProvider());

    patternsField.enableButton(IDX_EDIT, false);
    patternsField.setRemoveButtonIndex(IDX_REMOVE);
    patternsField.setTableColumns(new ListDialogField.ColumnsDescription(1,
        false));

    patternsField.doFillIntoGrid(grid, 3);

    GridData layoutData = (GridData) patternsField.getListControl(parent).getLayoutData();
    layoutData.grabExcessHorizontalSpace = true;
    layoutData.grabExcessVerticalSpace = true;
    patternsField.getListControl(parent).setLayoutData(layoutData);
  }

  private void editEntry(ListDialogField field) {
    IPath entry = (IPath) patternsField.getSelectedElements().get(0);
    FileFilterPatternDialog dialog = new FileFilterPatternDialog(shell,
        project, isExclusions, entry);
    if (dialog.open() == Window.OK) {
      field.replaceElement(entry, dialog.getPattern());
    }
  }

  private void initializeControls(List<IPath> initialPatterns) {
    patternsField.setElements(initialPatterns);
    patternsField.selectFirstElement();
  }

}
