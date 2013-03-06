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
package com.google.gwt.eclipse.core.wizards;

import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResource;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleUtilities;
import com.google.gwt.eclipse.core.clientbundle.ui.BundledResourcesSelectionBlock;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Wizard page for creating ClientBundle interfaces. Very Similar to
 * {@link org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage}, but adds an
 * editable list of bundled resources to the page, which will correspond to
 * resource accessor methods in the generated interface.
 */
@SuppressWarnings("restriction")
public class NewClientBundleWizardPage extends NewTypeWizardPage {

  private class BundledResourcesFieldAdapter implements
      BundledResourcesSelectionBlock.IResourcesChangeListener {

    public void onResourcesChanged() {
      doStatusUpdate();
    }
  }

  private BundledResourcesSelectionBlock bundledResourcesBlock;

  public NewClientBundleWizardPage() {
    super(false, "NewClientBundleWizardPage");
    setTitle("ClientBundle");
    setDescription("Create a new ClientBundle interface.");

    bundledResourcesBlock = new BundledResourcesSelectionBlock(
        "Bundled resources:", new BundledResourcesFieldAdapter());
  }

  /**
   * This method duplicates the method in
   * {@link org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage}, but inserts our
   * Bundled Resources list just after the Extended Interfaces list.
   */
  public void createControl(Composite parent) {
    initializeDialogUnits(parent);

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setFont(parent.getFont());

    int nColumns = 4;

    GridLayout layout = new GridLayout();
    layout.numColumns = nColumns;
    composite.setLayout(layout);

    createContainerControls(composite, nColumns);
    createPackageControls(composite, nColumns);
    createEnclosingTypeControls(composite, nColumns);

    createSeparator(composite, nColumns);

    createTypeNameControls(composite, nColumns);
    createModifierControls(composite, nColumns);

    createSuperInterfacesControls(composite, nColumns);

    // This differs from the stock interface wizard
    createBundledResourcesControls(composite, nColumns);

    createCommentControls(composite, nColumns);
    enableCommentControl(true);

    setControl(composite);

    Dialog.applyDialogFont(composite);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
        IJavaHelpContextIds.NEW_INTERFACE_WIZARD_PAGE);
  }

  public void init(IStructuredSelection selection) {
    IJavaElement jelem = getInitialJavaElement(selection);

    initContainerPage(jelem);
    initTypePage(jelem);

    IJavaProject javaProject = getJavaProject();
    if (javaProject != null) {
      initBundledResources(javaProject, selection);
    }

    // Only add if new type does not already extend ClientBundle
    if (!newTypeExtendsClientBundle()) {
      addSuperInterface(ClientBundleUtilities.CLIENT_BUNDLE_TYPE_NAME);
    }

    doStatusUpdate();
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      setFocus();
    }
  }

  @Override
  protected IStatus containerChanged() {
    IStatus superStatus = super.containerChanged();

    // Errors from our super get precedence
    if (superStatus.getSeverity() == IStatus.ERROR) {
      return superStatus;
    }

    if (ClientBundleUtilities.findClientBundleType(getJavaProject()) == null) {
      return StatusUtilities.newErrorStatus(
          "Project must be using GWT 2.0 or later to use ClientBundle.",
          GWTPlugin.PLUGIN_ID);
    }

    // Update the bundled resources block
    bundledResourcesBlock.setJavaProject(getJavaProject());

    return superStatus;
  }

  @Override
  protected void createTypeMembers(IType newType, ImportsManager imports,
      IProgressMonitor monitor) throws CoreException {
    boolean addComments = isAddComments();
    final MultiStatus status = new MultiStatus(GWTPlugin.PLUGIN_ID, 0,
        "Click the Details button for more information", null);

    // Create an accessor method for each bundled resource
    for (ClientBundleResource resource : bundledResourcesBlock.getResources()) {
      try {
        resource.addToClientBundle(newType, imports, addComments, monitor);
      } catch (CoreException e) {
        status.add(e.getStatus());
      }
    }

    // We want to handle any errors here, since if we let it escape this method
    // it short-circuits NewTypeWizardPage.createType, which ends up creating
    // a new ClientBundle source file with no content.
    if (!status.isOK()) {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          ErrorDialog.openError(
              getShell(),
              "Error Adding Resources",
              "One or more selected resources could not be added to the ClientBundle.",
              status);
        }
      });
    }
  }

  @Override
  protected void handleFieldChanged(String fieldName) {
    super.handleFieldChanged(fieldName);

    // Bundled resources block is enabled iff the container (src root) and
    // extended interfaces fields do not have errors.
    if (fieldName == CONTAINER || fieldName == INTERFACES) {
      boolean enable = (fSuperInterfacesStatus.getSeverity() != IStatus.ERROR && fContainerStatus.getSeverity() != IStatus.ERROR);
      if (enable ^ bundledResourcesBlock.isEnabled()) {
        bundledResourcesBlock.setEnabled(enable);
      }
    }

    doStatusUpdate();
  }

  @Override
  protected IStatus packageChanged() {
    IStatus superStatus = super.packageChanged();

    // Update the bundled resources block
    bundledResourcesBlock.setPackage(getPackageFragment());

    return superStatus;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected IStatus superInterfacesChanged() {
    IStatus superStatus = super.superInterfacesChanged();

    // Errors from our super get precedence
    if (superStatus.getSeverity() == IStatus.ERROR) {
      return superStatus;
    }

    // Ensure that at least one interface is/extends ClientBundle
    if (!newTypeExtendsClientBundle()) {
      return StatusUtilities.newErrorStatus(
          "ClientBundle interface must extend "
              + ClientBundleUtilities.CLIENT_BUNDLE_TYPE_NAME,
          GWTPlugin.PLUGIN_ID);
    }

    // Tell the bundled resources block about the new extended interfaces
    String[] extendedInterfaces = (String[]) getSuperInterfaces().toArray(
        new String[0]);
    bundledResourcesBlock.setExtendedInterfaces(extendedInterfaces);

    return superStatus;
  }

  private void createBundledResourcesControls(Composite parent, int columns) {
    bundledResourcesBlock.doFillIntoGrid(parent, columns);
  }

  private void doStatusUpdate() {
    IStatus[] status = new IStatus[] {
        fContainerStatus,
        isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
        fTypeNameStatus, fModifierStatus, fSuperInterfacesStatus,
        getBundledResourcesStatus()};

    updateStatus(status);
  }

  private IStatus getBundledResourcesStatus() {
    IStatus status = bundledResourcesBlock.getStatus();
    if (status.isOK()) {
      return status;
    }
    return new Status(status.getSeverity(), status.getPlugin(),
        "Bundled resource problem: " + status.getMessage());
  }

  private void initBundledResources(IJavaProject javaProject,
      IStructuredSelection selection) {
    List<ClientBundleResource> resources = new ArrayList<ClientBundleResource>();

    Iterator<?> iter = selection.iterator();
    while (iter.hasNext()) {
      Object element = iter.next();

      // Turn each file in the selection into a bundled resource
      IFile file = AdapterUtilities.getAdapter(element, IFile.class);
      if (file != null) {
        ClientBundleResource resource = ClientBundleResource.createFromFile(
            javaProject, file);
        if (resource != null) {
          resources.add(resource);
        }
      }
    }

    bundledResourcesBlock.setResources(resources);
  }

  private boolean newTypeExtendsClientBundle() {
    IJavaProject javaProject = getJavaProject();
    if (javaProject == null) {
      return false;
    }

    for (Object superInterface : getSuperInterfaces()) {
      String interfaceTypeName = (String) superInterface;
      try {
        if (ClientBundleUtilities.isClientBundle(javaProject, interfaceTypeName)) {
          return true;
        }
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
      }
    }
    return false;
  }

}
