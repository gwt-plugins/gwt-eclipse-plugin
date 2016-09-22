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
package com.google.gdt.eclipse.core.properties.ui;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects a JAR file from the project's build classpath.
 */
@SuppressWarnings("restriction")
public class BuildpathJarSelectionDialog extends StatusDialog {

  private class JarsLabelProvider extends LabelProvider implements
      ITableLabelProvider {

    private final Image elementImage;

    public JarsLabelProvider() {
      ImageDescriptorRegistry registry = JavaPlugin.getImageDescriptorRegistry();
      elementImage = registry.get(JavaPluginImages.DESC_OBJS_JAR);
    }

    public Image getColumnImage(Object element, int columnIndex) {
      if (columnIndex == 0) {
        return elementImage;
      }
      return null;
    }

    public String getColumnText(Object element, int columnIndex) {
      if (columnIndex == 0) {
        IPath path = (IPath) element;
        // Display name first, then path
        return path.lastSegment() + " - " + path.removeLastSegments(1);
      }
      return null;
    }
  }

  private class JarsViewerListener implements ISelectionChangedListener,
      IDoubleClickListener {
    public void doubleClick(DoubleClickEvent event) {
      BuildpathJarSelectionDialog.this.okPressed();
    }

    public void selectionChanged(SelectionChangedEvent event) {
      validateFields();
    }
  }

  private final List<IPath> excludedJars;

  private final ViewerSorter jarsSorter = new ViewerSorter() {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      // Sort alphabetically based on JAR filename
      String jarName1 = ((IPath) e1).lastSegment();
      String jarName2 = ((IPath) e2).lastSegment();
      return jarName1.compareTo(jarName2);
    }
  };

  private TableViewer jarsViewer;

  private final IJavaProject javaProject;

  private List<IPath> selectedJars;

  protected BuildpathJarSelectionDialog(Shell shell, IJavaProject javaProject,
      List<IPath> excludedJars) {
    super(shell);
    this.javaProject = javaProject;
    this.excludedJars = excludedJars;

    // Make the dialog resizable
    setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
  }

  public List<IPath> getJars() {
    return selectedJars;
  }

  @Override
  protected void configureShell(Shell shell) {
    setTitle("JAR File Selection");
    setHelpAvailable(false);

    super.configureShell(shell);
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    initializeControls();
    addEventHandlers();
    validateFields();

    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite grid = new Composite(parent, SWT.NONE);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    grid.setLayoutData(gridData);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginHeight = 8;
    gridLayout.marginWidth = 8;
    grid.setLayout(gridLayout);

    Label descriptionLabel = new Label(grid, SWT.NONE);
    descriptionLabel.setText("Select JAR file(s) from the build path:");

    jarsViewer = new TableViewer(grid);
    GridData jarsListGridData = new GridData(GridData.FILL_BOTH);
    jarsViewer.getTable().setLayoutData(jarsListGridData);
    jarsViewer.setContentProvider(new ArrayContentProvider());
    jarsViewer.setLabelProvider(new JarsLabelProvider());
    jarsViewer.setSorter(jarsSorter);

    return grid;
  }

  @Override
  protected Point getInitialSize() {
    return new Point(400, 300);
  }

  @SuppressWarnings("unchecked")
  protected void validateFields() {
    IStatus status;
    selectedJars = new ArrayList<IPath>();

    // User must select at least one JAR file
    if (jarsViewer.getSelection().isEmpty()) {
      status = new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, "");
    } else {
      status = new Status(IStatus.OK, CorePlugin.PLUGIN_ID, "");
      selectedJars = ((IStructuredSelection) jarsViewer.getSelection()).toList();
    }

    updateStatus(status);
  }

  private void addEventHandlers() {
    JarsViewerListener adapter = new JarsViewerListener();
    jarsViewer.addSelectionChangedListener(adapter);
    jarsViewer.addDoubleClickListener(adapter);
  }

  /**
   * Returns the absolute file system paths of all JAR files on the project's
   * build classpath, which are not part of a classpath container.
   */
  private List<IPath> getJarsOnBuildPath() {
    List<IPath> jars = new ArrayList<IPath>();

    try {
      IClasspathEntry[] rawClasspaths = javaProject.getRawClasspath();

      for (IClasspathEntry rawClasspath : rawClasspaths) {
        rawClasspath = JavaCore.getResolvedClasspathEntry(rawClasspath);
        if (rawClasspath.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
          IPath jarPath = ResourceUtils.resolveToAbsoluteFileSystemPath(rawClasspath.getPath());
          jars.add(jarPath);
        }
      }
    } catch (JavaModelException e) {
      CorePluginLog.logError(e);
    }

    return jars;
  }

  private void initializeControls() {
    List<IPath> jars = getJarsOnBuildPath();

    // Remove any JAR files that we've already excluded
    jars.removeAll(excludedJars);

    jarsViewer.setInput(jars);
  }

}
