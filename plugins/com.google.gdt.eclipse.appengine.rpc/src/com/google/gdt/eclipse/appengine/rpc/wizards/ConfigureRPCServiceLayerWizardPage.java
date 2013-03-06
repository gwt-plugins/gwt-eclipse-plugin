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
package com.google.gdt.eclipse.appengine.rpc.wizards;

import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;
import com.google.gdt.eclipse.appengine.rpc.nature.AppEngineConnectedNature;
import com.google.gdt.eclipse.appengine.rpc.util.JavaUtils;
import com.google.gdt.eclipse.appengine.rpc.util.RequestFactoryUtils;
import com.google.gdt.eclipse.appengine.rpc.util.StatusUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaPackageCompletionProcessor;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * TODO: doc me.
 */
@SuppressWarnings("restriction")
public class ConfigureRPCServiceLayerWizardPage extends WizardPage implements
    IShellProvider {

  private class SelectionChangedListener implements SelectionListener,
      ICheckStateListener {

    public final void checkStateChanged(CheckStateChangedEvent e) {
      doCheckStateChanged(e);
      doStatusUpdate();
    }

    public void doCheckStateChanged(CheckStateChangedEvent e) {
      // hook for subclasses
    }

    public void doWidgetSelected(SelectionEvent e) {
      // hook for subclasses
    }

    public final void widgetDefaultSelected(SelectionEvent e) {
      widgetSelected(e);
    }

    public final void widgetSelected(SelectionEvent e) {
      doWidgetSelected(e);
      doStatusUpdate();
    }
  }

  protected IPackageFragmentRoot containerRoot;
  protected IStatus containerStatus = StatusUtils.createOKStatus();

  // protected IPackageFragment packageRoot;
  protected IStatus entityStatus = StatusUtils.createOKStatus();
  protected IStatus packageStatus = StatusUtils.createOKStatus();
  protected IStatus typeStatus = StatusUtils.createOKStatus();

  private Composite container;
  private Text containerText;
  private Label detailsLabel;
  private Control entityTree;

  private CheckboxTableViewer entityViewer;

  // private Text packageText;

  private JavaPackageCompletionProcessor packageCompletionProcessor = new JavaPackageCompletionProcessor();
  private IProject project;
  private Text typeText;
  private final RPCWizardUISupport uiSupport = new RPCWizardUISupport(this);

  public ConfigureRPCServiceLayerWizardPage(IProject project) {
    super("wizardPage"); //$NON-NLS-1$
    this.project = project;
    setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
        "com.google.gwt.eclipse.core", //$NON-NLS-1$
        "icons/gdt-new-rpc_66.png")); //$NON-NLS-1$
    setTitle("RPC Service"); //$NON-NLS-1$
    setDescription("Create a new RPC service."); //$NON-NLS-1$
  }

  public void createControl(Composite parent) {

    Composite composite = createMainComposite(parent);

    if (!inferContainerFromWorkbenchSelection()) {
      createContainerSelector(composite);
    }

    // createPackageSelector(composite);
    createTypeNameField(composite);

    createSeparator(composite);

    try {
      createEntityViewer(composite);
    } catch (JavaModelException e) {
      AppEngineRPCPlugin.getLogger().logError(e);
    }

    createDetailsLabel(composite);
  }

  public IPackageFragmentRoot getContainerRoot() {
    return containerRoot;
  }

  public Iterable<IType> getEntityTypes() {
    return JavaUtils.asTypes(entityViewer.getCheckedElements());
  }

  public String getServiceName() {
    return typeText.getText().trim();
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      setInitialFocusAndStatus();
    }
  }

  protected void handleContainerChanged() {

    String containerPathText = containerText.getText();

    containerStatus = JavaUtils.validateContainer(containerPathText);
    containerRoot = containerStatus.isOK()
        ? JavaUtils.createContainer(containerPathText) : null;

    packageCompletionProcessor.setPackageFragmentRoot(containerRoot);

    if (containerRoot != null) {
      project = containerRoot.getJavaProject().getProject();
    }

    refreshEntityViewer();

    doStatusUpdate();
  }

  protected void handleEntitySelectionChanged() {
    updateEntityStatus();
    doStatusUpdate();
  }

  protected void handleTypeChanged() {
    typeStatus = JavaUtils.validateJavaTypeName(typeText.getText());
    doStatusUpdate();
  }

  protected void updateStatus(IStatus status) {
    setPageComplete(!status.matches(IStatus.ERROR));
    StatusUtils.applyToStatusLine(this, status);
  }

  protected void updateStatus(IStatus[] status) {
    updateStatus(StatusUtils.getMostSevere(status));
  }

  // protected void handlePackageChanged() {
  // updatePackageStatus();
  // doStatusUpdate();
  // }

  private void configureMethodTableViewer(final CheckboxTableViewer viewer,
      final ITreeContentProvider contentProvider, ILabelProvider labelProvider,
      Button selectAll, Button deselectAll) {
    viewer.setLabelProvider(labelProvider);

    viewer.setContentProvider(contentProvider);
    viewer.setInput(project);
    viewer.addCheckStateListener(new SelectionChangedListener() {
      @Override
      public void doCheckStateChanged(CheckStateChangedEvent e) {
        handleEntitySelectionChanged();
      }
    });

    selectAll.addSelectionListener(new SelectionChangedListener() {
      @Override
      public void doWidgetSelected(SelectionEvent e) {
        for (Object element : contentProvider.getElements(null)) {
          viewer.setChecked(element, true);
        }
        handleEntitySelectionChanged();
      }
    });
    deselectAll.addSelectionListener(new SelectionChangedListener() {
      @Override
      public void doWidgetSelected(SelectionEvent e) {
        viewer.setCheckedElements(new Object[] {});
        handleEntitySelectionChanged();
      }
    });
    viewer.setAllChecked(true);
    updateEntityStatus();
  }

  private void createContainerSelector(Composite composite) {

    Label sourceFolderLabel = new Label(composite, SWT.NONE);
    sourceFolderLabel.setText("Source folder:"); //$NON-NLS-1$

    containerText = new Text(composite, SWT.BORDER);
    containerText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
        1, 1));
    containerText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        handleContainerChanged();
      }
    });

    initializeContainerText(containerText);

    Button sourceBrowseButton = new Button(composite, SWT.NONE);
    sourceBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
        false, 1, 1));
    sourceBrowseButton.setText("Browse..."); //$NON-NLS-1$
    sourceBrowseButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      public void widgetSelected(SelectionEvent e) {
        IPackageFragmentRoot selectedContainer = uiSupport.chooseContainer(null);
        // a null means the user canceled, in which case we don't update
        if (selectedContainer != null) {
          containerRoot = selectedContainer;
        }
        String str = JavaUtils.getPackageFragmentRootText(containerRoot);
        containerText.setText(str);
      }
    });
  }

  private void createDetailsLabel(Composite composite) {
    // spacer
    new Label(composite, SWT.NONE).setLayoutData(new GridData(SWT.LEFT,
        SWT.CENTER, false, false, 2, 1));

    detailsLabel = new Label(composite, SWT.NONE);
    detailsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false,
        2, 1));
    detailsLabel.setText("Clicking finish will create a RequestFactory, Proxy and Request Contexts for the selected entities."); //$NON-NLS-1$
  }

  private void createEntityViewer(Composite composite)
      throws JavaModelException {

    Label propertyMethodsLabel = new Label(composite, SWT.NONE);
    propertyMethodsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false,
        false, 3, 1));
    propertyMethodsLabel.setText("Select entities to include:"); //$NON-NLS-1$
    String tooltip = "Entities are identified in Java source with the @Entity or @PersistenceCapable annotations."; //$NON-NLS-1$
    // + "To add un-annotated classes, use the \'Add..' button.";
    propertyMethodsLabel.setToolTipText(tooltip);

    // spacer
    new Label(composite, SWT.NONE);

    entityViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
    Table propertyTree = entityViewer.getTable();
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
    gd.heightHint = 100;
    propertyTree.setLayoutData(gd);

    entityTree = entityViewer.getControl();
    entityTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    Composite selectionControls = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    layout.verticalSpacing = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.marginHeight = 0;
    selectionControls.setLayout(layout);
    selectionControls.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false,
        false, 1, 1));

    Button selectAllButton = new Button(selectionControls, SWT.NONE);
    selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
        false, 1, 1));
    selectAllButton.setText("Select All"); //$NON-NLS-1$

    Button deselectAllButton = new Button(selectionControls, SWT.NONE);
    deselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
        false, 1, 1));
    deselectAllButton.setText("Deselect All"); //$NON-NLS-1$

    configureMethodTableViewer(entityViewer,
        RequestFactoryUtils.createEntityListContentProvider(getJavaProject()),
        new JavaElementLabelProvider(), selectAllButton, deselectAllButton);
  }

  private Composite createMainComposite(Composite parent) {
    container = new Composite(parent, SWT.NULL);
    setControl(container);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    gd.heightHint = 282;
    container.setLayoutData(gd);
    container.setLayout(new GridLayout(3, false));
    return container;
  }

  private void createSeparator(Composite composite) {
    new Label(composite, SWT.NONE);
    new Label(composite, SWT.NONE);
    new Label(composite, SWT.NONE);
    Label label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
    label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
  }

  private void createTypeNameField(Composite composite) {
    Label typeLabel = new Label(composite, SWT.NONE);
    typeLabel.setText("Name:"); //$NON-NLS-1$

    typeText = new Text(composite, SWT.BORDER);
    typeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    typeText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        handleTypeChanged();
      }
    });

    initializeTypeText(typeText);
  }

  private void doStatusUpdate() {
    IStatus[] status = new IStatus[] {
        containerStatus, entityStatus, packageStatus, typeStatus};
    updateStatus(status);
  }

  // private void createPackageSelector(Composite composite) {
  //
  // Label packageLabel = new Label(composite, SWT.NONE);
  //    packageLabel.setText("Package:"); //$NON-NLS-1$
  //
  //
  // packageText = new Text(composite, SWT.BORDER);
  // packageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
  // 1, 1));
  //
  // initializePackageText(packageText);
  //
  // packageText.addModifyListener(new ModifyListener() {
  // public void modifyText(ModifyEvent e) {
  // handlePackageChanged();
  // }
  // });
  //
  // ControlContentAssistHelper.createTextContentAssistant(packageText,
  // packageCompletionProcessor);
  // TextFieldNavigationHandler.install(packageText);
  //
  // Button packageBrowseButton = new Button(composite, SWT.NONE);
  // packageBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
  // false, 1, 1));
  //    packageBrowseButton.setText("Browse..."); //$NON-NLS-1$
  //
  // packageBrowseButton.addSelectionListener(new SelectionListener() {
  //
  // public void widgetDefaultSelected(SelectionEvent e) {
  // widgetSelected(e);
  // }
  //
  // public void widgetSelected(SelectionEvent e) {
  // IPackageFragment packageRoot = uiSupport.choosePackage(containerRoot);
  // String str = JavaUtils.getPackageFragmentText(packageRoot);
  // packageText.setText(str);
  // }
  // });
  // }

  private IProject findAppEngineProject(IProject project) {
    if (project != null && project.isAccessible()) {
      try {
        return AppEngineConnectedNature.getAppEngineProject(project);
      } catch (CoreException e) {
        AppEngineRPCPlugin.log(e);
      }
    }
    return null;
  }

  private IJavaProject getJavaProject() {

    if (containerRoot != null) {
      return containerRoot.getJavaProject();
    }
    return null;
  }

  private String getProjectName() {
    if (project == null) {
      return "<null>"; //$NON-NLS-1$
    }
    return project.getName();
  }

  private boolean inferContainerFromWorkbenchSelection() {

    project = findAppEngineProject(project);

    if (project != null && project.isAccessible()) {
      IJavaProject javaProject = JavaCore.create(project);
      if (javaProject != null) {
        try {
          containerRoot = JavaUtils.getSourcePackageFragmentRoot(javaProject);
          return true;
        } catch (JavaModelException e) {
          AppEngineRPCPlugin.log(e);
        }
      }
    }
    return false;
  }

  private void initializeContainerText(Text targetText) {
    String text = ""; //$NON-NLS-1$
    if (project != null && project.isAccessible()) {
      IJavaProject javaProject = JavaCore.create(project);
      if (javaProject != null) {
        try {
          IPackageFragmentRoot srcPackage = JavaUtils.getSourcePackageFragmentRoot(javaProject);
          if (srcPackage != null) {
            text = javaProject.getElementName()
                + "/" + srcPackage.getElementName(); //$NON-NLS-1$
          }
        } catch (JavaModelException e) {
          AppEngineRPCPlugin.log(e);
        }
      }
    }
    targetText.setText(text);
  }

  private void initializeTypeText(Text typeText) {
    String text = ""; //$NON-NLS-1$
    IJavaProject javaProject = getJavaProject();
    if (javaProject != null) {
      text = RequestFactoryUtils.creatServiceNameProposal(javaProject.getElementName());
    }
    typeText.setText(text);
  }

  private void refreshEntityViewer() {
    IJavaProject javaProject = getJavaProject();
    if (javaProject != null) {
      try {
        entityViewer.setInput(javaProject.getProject());
        entityViewer.setContentProvider(RequestFactoryUtils.createEntityListContentProvider(getJavaProject()));
        entityViewer.refresh();
        entityViewer.setAllChecked(true);
        handleEntitySelectionChanged();
      } catch (JavaModelException e) {
        AppEngineRPCPlugin.log(e);
      }
    }
  }

  // private void initializePackageText(Text packageText) {
  //    packageText.setText(""); //$NON-NLS-1$
  // }

  private void setInitialFocusAndStatus() {
    // advance keyboard focus
    // if (containerRoot != null) {
    // packageText.setFocus();
    // }

    // updatePackageStatus();

    setPageComplete(packageStatus.isOK() && entityStatus.isOK());

    // clear validation messages unless there are no entities, in which case
    // error
    if (containerRoot != null && !entityStatus.isOK()) {
      setErrorMessage(entityStatus.getMessage());
    } else {
      setErrorMessage(null);
      setMessage(null);
    }
  }

  private void updateEntityStatus() {
    if (entityViewer.getCheckedElements().length == 0) {
      String msg;
      if (entityViewer.getTable().getItemCount() == 0) {
        msg = "The project '" + getProjectName() //$NON-NLS-1$
            + "' contains no entities."; //$NON-NLS-1$
      } else {
        msg = "At least one entity must be selected."; //$NON-NLS-1$
      }
      entityStatus = new Status(IStatus.ERROR, AppEngineRPCPlugin.PLUGIN_ID,
          -1, msg, null);
    } else {
      entityStatus = StatusUtils.createOKStatus();
    }
  }

  // private void updatePackageStatus() {
  // packageStatus = JavaUtils.validatePackageName(packageText.getText());
  // }

}