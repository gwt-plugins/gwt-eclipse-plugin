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

import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.Messages;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;

/**
 * This classpath container page collects the directory and the file extensions
 * for a new or existing SimpleDirContainer.
 */
public class ManagedApiContainerPage extends WizardPage implements IClasspathContainerPage,
    IClasspathContainerPageExtension {

  private IJavaProject proj;
  private Combo dirCombo;
  private Button dirBrowseButton;
  private IPath initPath = null;

  /**
   * Default Constructor - sets title, page name, description
   */
  public ManagedApiContainerPage() {
    super(Messages.PageName, Messages.PageTitle, null);
    setDescription(Messages.PageDesc);
    setPageComplete(true);
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
        | GridData.HORIZONTAL_ALIGN_FILL));
    composite.setFont(parent.getFont());

    createDirGroup(composite);

    setControl(composite);
  }

  public boolean finish() {
    if (!isDirValid(getDirValue())) {
      setErrorMessage(NLS.bind(Messages.DirErr, proj.getProject().getName()));
      return false;
    }
    return true;
  }

  public IClasspathEntry getSelection() {
    String dir = getRelativeDirValue();
    IPath containerPath = ManagedApiPlugin.API_CONTAINER_PATH.append("/" + dir);
    return JavaCore.newContainerEntry(containerPath);
  }

  public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
    proj = project;
  }

  public void setSelection(IClasspathEntry containerEntry) {
    if (containerEntry != null) {
      initPath = containerEntry.getPath();
    }
  }

  /**
   * @return the current directory
   */
  protected String getDirValue() {
    return dirCombo.getText();
  }

  /**
   * @return directory relative to the parent project
   */
  protected String getRelativeDirValue() {
    int projDirLen = proj.getProject().getLocation().toString().length();
    return getDirValue().substring(projDirLen);
  }

  /**
   * Creates a directory dialog
   */
  protected void handleDirBrowseButtonPressed() {
    DirectoryDialog dialog = new DirectoryDialog(getContainer().getShell(), SWT.SAVE);
    dialog.setMessage(Messages.DirSelect);
    dialog.setFilterPath(getDirValue());
    String dir = dialog.open();
    if (dir != null) {
      dirCombo.setText(dir);
    }
  }

  /**
   * Creates the directory label, combo, and browse button
   *
   * @param parent
   *          the parent widget
   */
  private void createDirGroup(Composite parent) {
    Composite dirSelectionGroup = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    dirSelectionGroup.setLayout(layout);
    dirSelectionGroup.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
        | GridData.VERTICAL_ALIGN_FILL));

    new Label(dirSelectionGroup, SWT.NONE).setText(Messages.DirLabel);

    dirCombo = new Combo(dirSelectionGroup, SWT.SINGLE | SWT.BORDER);
    dirCombo.setText(getInitDir());

    dirBrowseButton = new Button(dirSelectionGroup, SWT.PUSH);
    dirBrowseButton.setText(Messages.Browse);
    dirBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
    dirBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleDirBrowseButtonPressed();
      }
    });
    setControl(dirSelectionGroup);
  }

  /**
   * Extracts the initial directory value from a path passed in setSelection()
   *
   * @return the inital directory value
   */
  private String getInitDir() {
    String projDir = proj.getProject().getLocation().toString();
    if (initPath != null && initPath.segmentCount() > 1) {
      return projDir + IPath.SEPARATOR + initPath.segment(1);
    }
    // else
    return projDir;
  }

  /**
   * Checks that the directory is a subdirectory of the project being configured
   *
   * @param dir
   *          a directory to validate
   * @return true if the directory is valid
   */
  private boolean isDirValid(String dir) {
    Path dirPath = new Path(dir);
    return proj.getProject().getLocation().makeAbsolute().isPrefixOf(dirPath);
  }
}
