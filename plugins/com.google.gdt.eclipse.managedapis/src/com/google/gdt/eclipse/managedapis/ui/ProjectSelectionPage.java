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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * TODO:
 * 
 * 
 */
public class ProjectSelectionPage extends WizardPage {

  private TableViewer projectViewer;

  protected ProjectSelectionPage(IWizard wizard, ImageDescriptor wizardIcon) {
    super("projectSelectionPage");

    setTitle(wizard.getWindowTitle());
    setDescription("Choose a project to add Google APIs to.");

    if (wizardIcon != null) {
      setImageDescriptor(wizardIcon);
    }

    setPageComplete(false);
  }

  public void createControl(Composite parent) {
    AdaptableList projectList = new AdaptableList(
        ResourcesPlugin.getWorkspace().getRoot().getProjects());

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(composite);

    projectViewer = new TableViewer(composite, SWT.SINGLE | SWT.BORDER);
    projectViewer.setContentProvider(new WorkbenchContentProvider());
    projectViewer.setLabelProvider(new WorkbenchLabelProvider());
    projectViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
    projectViewer.setInput(projectList);
    projectViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateEnablements();
      }
    });
    GridDataFactory.fillDefaults().grab(true, true).applyTo(
        projectViewer.getControl());

    setControl(composite);
  }

  protected IProject getSelectedProject() {
    ISelection selection = projectViewer.getSelection();

    if (selection instanceof IStructuredSelection) {
      IStructuredSelection ss = (IStructuredSelection) selection;

      IProject project = (IProject) ss.getFirstElement();

      if (project != null && project.isOpen()) {
        return project;
      }
    }

    return null;
  }

  private void updateEnablements() {
    IProject project = getSelectedProject();

    setPageComplete(project != null);
  }

}
