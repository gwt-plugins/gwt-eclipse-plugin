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
package com.google.gwt.eclipse.core.clientbundle.ui;

import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects a set of resources from a project to be included in a
 * <code>ClientBundle</code> interface.
 */
@SuppressWarnings("restriction")
public class BundledResourcesSelectionBlock {

  /**
   * Change listener used by BundledResourcesSelectionBlock.
   */
  public interface IResourcesChangeListener {
    void onResourcesChanged();
  }

  private class ColumnLabelProvider extends LabelProvider implements
      ITableLabelProvider {

    public Image getColumnImage(Object element, int columnIndex) {
      ClientBundleResource resource = (ClientBundleResource) element;
      switch (columnIndex) {
        case COL_FILE:
          IStatus resourceStatus = validateResource(resource);
          return workbenchLabelProvider.getImage(resource, resourceStatus);
        case COL_METHOD:
          return JavaUI.getSharedImages().getImage(
              ISharedImages.IMG_OBJS_PUBLIC);
        default:
          return null;
      }
    }

    public String getColumnText(Object element, int columnIndex) {
      ClientBundleResource resource = (ClientBundleResource) element;
      switch (columnIndex) {
        case COL_FILE:
          return resource.getFile().getName();
        case COL_TYPE:
          return Signature.getSimpleName(resource.getResourceTypeName());
        case COL_METHOD:
          return resource.getMethodName();
        default:
          return "";
      }
    }
  }

  /**
   * Subclass the workbench label provider to add decorate icons for resources
   * which have errors.
   */
  private static class ResourceImageLabelProvider extends
      WorkbenchLabelProvider {

    // Cache the error-decorated resource icon images
    private Map<IFile, Image> errorIconCache = new HashMap<IFile, Image>();

    public Image getImage(ClientBundleResource resource, IStatus status) {
      IFile file = resource.getFile();

      if (status.getSeverity() == IStatus.ERROR) {
        // Use the cached image if available
        if (errorIconCache.containsKey(file)) {
          return errorIconCache.get(file);
        }

        // Decorate the resource icon with an error marker and save in the cache
        ImageDescriptor imageDescriptor = getAdapter(file).getImageDescriptor(
            file);
        imageDescriptor = new JavaElementImageDescriptor(imageDescriptor,
            JavaElementImageDescriptor.ERROR,
            JavaElementImageProvider.SMALL_SIZE);
        Image errorIcon = imageDescriptor.createImage();
        errorIconCache.put(file, errorIcon);

        return errorIcon;
      }

      // No error; don't need to decorate the icon
      return getImage(file);
    }
  }

  private class SelectionAdapter implements IListAdapter {

    @SuppressWarnings("unchecked")
    public void customButtonPressed(ListDialogField field, int index) {
      List<ClientBundleResource> resourcesBefore = resourcesField.getElements();

      switch (index) {
        case IDX_ADD:
          addResource();
          break;
        case IDX_ADD_MULTIPLE:
          addMultipleResources();
          break;
        case IDX_EDIT:
          editSelectedResource();
          break;
        case IDX_REMOVE:
          removeSelectedResources();
          break;
      }

      // Notify the listener if our list of modules changes
      if (listener != null && !getResources().equals(resourcesBefore)) {
        listener.onResourcesChanged();
      }
    }

    public void doubleClicked(ListDialogField field) {
      editSelectedResource();
    }

    public void selectionChanged(ListDialogField field) {
      int selectedElementsCount = resourcesField.getSelectedElements().size();
      resourcesField.enableButton(IDX_EDIT, selectedElementsCount == 1);
      resourcesField.enableButton(IDX_REMOVE, selectedElementsCount > 0);
    }
  }

  private static final int COL_FILE = 0;

  private static final int COL_METHOD = 2;

  private static final int COL_TYPE = 1;

  private static final int IDX_ADD = 0;

  private static final int IDX_ADD_MULTIPLE = 1;

  private static final int IDX_EDIT = 2;

  private static final int IDX_REMOVE = 4;

  private String[] extendedInterfaces;

  private IJavaProject javaProject;

  private final String labelText;

  private final IResourcesChangeListener listener;

  private IPackageFragment pckgFragment;

  private ListDialogField resourcesField;

  private Shell shell;

  private IStatus status = StatusUtilities.OK_STATUS;

  private ResourceImageLabelProvider workbenchLabelProvider = new ResourceImageLabelProvider();

  public BundledResourcesSelectionBlock(String labelText,
      IResourcesChangeListener listener) {
    this.labelText = labelText;
    this.listener = listener;

    createListField();
  }

  public void doFillIntoGrid(Composite parent, int columns) {
    // Cache this for later
    this.shell = parent.getShell();

    resourcesField.doFillIntoGrid(parent, columns);

    GridData modulesFieldGridData = (GridData) resourcesField.getListControl(
        parent).getLayoutData();
    modulesFieldGridData.grabExcessHorizontalSpace = true;
    modulesFieldGridData.grabExcessVerticalSpace = true;
    resourcesField.getListControl(parent).setLayoutData(modulesFieldGridData);
  }

  public String[] getExtendedInterfaces() {
    return extendedInterfaces;
  }

  public IJavaProject getJavaProject() {
    return javaProject;
  }

  public IPackageFragment getPackage() {
    return pckgFragment;
  }

  @SuppressWarnings("unchecked")
  public List<ClientBundleResource> getResources() {
    return resourcesField.getElements();
  }

  public IStatus getStatus() {
    return status;
  }

  public boolean isEnabled() {
    return resourcesField.isEnabled();
  }

  public void setEnabled(boolean enabled) {
    resourcesField.setEnabled(enabled);
    updateStatus();
  }

  public void setExtendedInterfaces(String[] extendedInterfaces) {
    this.extendedInterfaces = extendedInterfaces;
    updateStatus();
  }

  public void setJavaProject(IJavaProject javaProject) {
    this.javaProject = javaProject;
    updateStatus();
  }

  public void setPackage(IPackageFragment pckgFragment) {
    this.pckgFragment = pckgFragment;
  }

  public void setResources(List<ClientBundleResource> resources) {
    resourcesField.setElements(resources);
    updateStatus();
  }

  private void addMultipleResources() {
    if (javaProject == null) {
      MessageDialog.openError(shell, "Resource Selection Error",
          "A valid Java project must be selected.");
      return;
    }

    List<IFile> files = ClientBundleResourceSelectionDialog.getFiles(shell,
        true, javaProject, null);
    if (files != null) {
      for (IFile file : files) {
        ClientBundleResource resource = ClientBundleResource.createFromFile(
            javaProject, file);
        if (!resourcesField.getElements().contains(resource)) {
          resourcesField.addElement(resource);
        }
      }
      resourcesChanged();
    }
  }

  private void addResource() {
    if (javaProject == null) {
      MessageDialog.openError(shell, "Resource Selection Error",
          "A valid Java project must be selected.");
      return;
    }

    ClientBundleResourceDialog dialog = new ClientBundleResourceDialog(shell,
        javaProject, pckgFragment, extendedInterfaces,
        getAllMethodsBeingAdded());
    if (dialog.open() == Window.OK) {
      ClientBundleResource resource = dialog.getClientBundleResource();
      if (resource != null && !resourcesField.getElements().contains(resource)) {
        resourcesField.addElement(resource);
        resourcesChanged();
      }
    }
  }

  private void createListField() {
    String[] buttons = new String[] {
        "Add...", "Add Multiple...", "Edit", null, "Remove", null};

    resourcesField = new ListDialogField(new SelectionAdapter(), buttons,
        new ColumnLabelProvider());
    resourcesField.setLabelText(labelText);

    String[] columnNames = {"File", "Type", "Method name"};
    ColumnLayoutData[] columnLayouts = {
        new ColumnPixelData(100), new ColumnPixelData(100),
        new ColumnPixelData(100)};

    resourcesField.setTableColumns(new ListDialogField.ColumnsDescription(
        columnLayouts, columnNames, false));

    // Edit and Remove buttons disabled by default
    resourcesField.enableButton(IDX_EDIT, false);
    resourcesField.enableButton(IDX_REMOVE, false);
  }

  private void editSelectedResource() {
    ClientBundleResource resource = getSelectedResource();
    assert (resource != null);

    ClientBundleResourceDialog dialog = new ClientBundleResourceDialog(shell,
        javaProject, pckgFragment, extendedInterfaces,
        getMethodsBeingAddedExceptFrom(resource), resource);
    if (dialog.open() == Window.OK) {
      ClientBundleResource newResource = dialog.getClientBundleResource();
      resourcesField.replaceElement(resource, newResource);
      resourcesChanged();
    }
  }

  private String[] getAllMethodsBeingAdded() {
    return getMethodsBeingAddedExceptFrom(null);
  }

  private String[] getMethodsBeingAddedExceptFrom(
      ClientBundleResource excludedResource) {
    List<String> methodNames = new ArrayList<String>();
    for (ClientBundleResource otherResource : getResources()) {
      if (otherResource != excludedResource) {
        methodNames.add(otherResource.getMethodName());
      }
    }
    return methodNames.toArray(new String[0]);
  }

  private ClientBundleResource getSelectedResource() {
    List<?> selectedResources = resourcesField.getSelectedElements();
    if (selectedResources.size() == 1) {
      return (ClientBundleResource) selectedResources.get(0);
    }
    return null;
  }

  private void removeSelectedResources() {
    resourcesField.removeElements(resourcesField.getSelectedElements());
    resourcesChanged();
  }

  private void resourcesChanged() {
    updateStatus();

    // Notify our listener
    listener.onResourcesChanged();
  }

  private void updateStatus() {
    // Ignore errors if we're disabled
    if (!resourcesField.isEnabled()) {
      status = StatusUtilities.OK_STATUS;
      return;
    }

    List<ClientBundleResource> resources = getResources();
    if (resources.isEmpty()) {
      status = StatusUtilities.OK_STATUS;
      return;
    } else {
      // Validate each resource and record the status from each
      List<IStatus> statuses = new ArrayList<IStatus>();
      for (ClientBundleResource resource : resources) {
        statuses.add(validateResource(resource));
      }

      status = StatusUtil.getMostSevere(statuses.toArray(new IStatus[0]));
    }

    resourcesField.refresh();
  }

  private IStatus validateResource(ClientBundleResource resource) {
    // Cancel validation if the Java project is invalid
    if (javaProject == null || !javaProject.getProject().isAccessible()) {
      return StatusUtilities.OK_STATUS;
    }

    // Cancel validation if there are no extended interfaces
    if (extendedInterfaces == null || extendedInterfaces.length == 0) {
      return StatusUtilities.OK_STATUS;
    }

    return resource.validate(javaProject, extendedInterfaces,
        getMethodsBeingAddedExceptFrom(resource));
  }

}
