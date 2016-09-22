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
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.natures.NatureUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO: convert this to subclass TextSelectionBlock
// TODO: convert existing UIs that have a project selection area to use this
/**
 * A helper to create a composite that consists of a label, text box, and button
 * to choose a project.
 */
@SuppressWarnings("restriction")
public class ProjectSelectionBlock {

  /**
   * Listens to callbacks from the {@link ProjectSelectionBlock}.
   */
  public interface Listener {
    /**
     * @param project the project, or null if the entered project is not in a
     *          workable state
     * @param status the status of the project
     */
    void projectSelected(IProject project, IStatus status);
  }

  private final Map<String, String> requiredNaturesToErrors;
  private final List<Listener> listeners;

  private IProject project;
  private Text projectText;
  private Button chooseProjectButton;
  private Composite composite;

  /**
   * @param requiredNaturesToErrors a map of required natures to the error
   *          message if the nature is missing (in
   *          {@link String#format(String, Object...)} style)
   */
  public ProjectSelectionBlock(IProject project,
      Map<String, String> requiredNaturesToErrors, Listener listener) {
    this.project = project;
    this.requiredNaturesToErrors = requiredNaturesToErrors;
    this.listeners = new ArrayList<Listener>();
    listeners.add(listener);
  }

  public void addListener(Listener l) {
    this.listeners.add(l);
  }
  
  public void createContents(Composite parent) {
    createControls(parent);
    initializeControls();
    addEventHandlers();
    validateProject();
  }

  public IProject getProject() {
    return project;
  }

  public void removeListener(Listener l) {
    listeners.remove(l);
  }
  
  public IStatus validateProject() {
    project = null;

    String projectName = projectText.getText().trim();
    if (projectName.length() == 0) {
      return StatusUtilities.newErrorStatus("Enter the project name",
          CorePlugin.PLUGIN_ID);
    }

    IProject enteredProject = ResourcesPlugin.getWorkspace().getRoot().getProject(
        projectName);
    if (!enteredProject.exists()) {
      return StatusUtilities.newErrorStatus("Project does not exist",
          CorePlugin.PLUGIN_ID);
    }

    if (!enteredProject.isOpen()) {
      return StatusUtilities.newErrorStatus("Project is not open",
          CorePlugin.PLUGIN_ID);
    }

    if (requiredNaturesToErrors != null) {
      for (String nature : requiredNaturesToErrors.keySet()) {
        try {
          if (!NatureUtils.hasNature(enteredProject, nature)) {
            String errorMessage = String.format(
                requiredNaturesToErrors.get(nature), projectName);
            return StatusUtilities.newErrorStatus(errorMessage,
                CorePlugin.PLUGIN_ID);
          }
        } catch (CoreException e) {
          CorePluginLog.logError(e,
              "Could not check if the project has the required natures.");
          return StatusUtilities.newErrorStatus(
              "Project natures could not be checked", CorePlugin.PLUGIN_ID);
        }
      }
    }

    // Project is valid (no errors)
    project = enteredProject;

    try {
      if (IMarker.SEVERITY_ERROR == enteredProject.findMaxProblemSeverity(
          IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) {
        return StatusUtilities.newWarningStatus("The project {0} has errors.",
            CorePlugin.PLUGIN_ID, enteredProject.getName());
      }
    } catch (CoreException e) {
      return StatusUtilities.newWarningStatus(
          "Could not check the project for errors", CorePlugin.PLUGIN_ID);
    }

    return StatusUtilities.OK_STATUS;
  }

  private void addEventHandlers() {
    projectText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        IStatus status = validateProject();
        for (Listener listener : listeners) {
          listener.projectSelected(project, status);
        }
      }
    });

    chooseProjectButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Set<String> requiredNatures = requiredNaturesToErrors != null
            ? requiredNaturesToErrors.keySet() : null;
        IJavaProject selectedProject = JavaProjectSelectionDialog.chooseProject(
            composite.getShell(), JavaCore.create(project), requiredNatures);
        if (selectedProject != null) {
          projectText.setText(selectedProject.getElementName());
        }
      }
    });
  }

  private void createControls(Composite parent) {
    composite = new Composite(parent, SWT.NONE);
    GridData containerGridData = new GridData(GridData.FILL_HORIZONTAL);
    composite.setLayoutData(containerGridData);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.marginHeight = 8;
    gridLayout.marginWidth = 8;
    composite.setLayout(gridLayout);

    // Project field
    SWTFactory.createLabel(composite, "Project:", 1);
    projectText = new Text(composite, SWT.BORDER);
    projectText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    chooseProjectButton = new Button(composite, SWT.NONE);
    chooseProjectButton.setText("Browse...");
  }

  private void initializeControls() {
    if (project != null) {
      projectText.setText(project.getName());
    }
  }

}
