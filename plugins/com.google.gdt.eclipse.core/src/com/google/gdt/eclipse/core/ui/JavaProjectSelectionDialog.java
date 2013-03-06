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

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.natures.NatureUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Selects a Java project from a list, optionally filtered to only those having
 * a particular nature.
 */
public class JavaProjectSelectionDialog extends ElementListSelectionDialog {

  /**
   * Opens the dialog for the user to choose a Java project.
   * 
   * @param shell the parent shell
   * @param initialSelection the initially-selected project, or
   *          <code>null</code>
   * @param requiredNatures the natures that chosen project must have, or
   *          <code>null</code>
   * @return the selected project, or <code>null</code> if the dialog was
   *         canceled
   */
  public static IJavaProject chooseProject(Shell shell,
      IJavaProject initialSelection, Set<String> requiredNatures) {
    // Start with all the Java projects in the workspace
    List<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
    try {
      javaProjects.addAll(Arrays.asList(JavaCore.create(
          ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects()));
    } catch (JavaModelException e) {
      CorePluginLog.logError(e);
    }

    // Filter the list to only show projects with the specified nature
    if (requiredNatures != null) {
      Iterator<IJavaProject> iter = javaProjects.iterator();
      while (iter.hasNext()) {
        IJavaProject javaProject = iter.next();
        for (String requiredNature : requiredNatures) {
          try {
            if (!NatureUtils.hasNature(javaProject.getProject(), requiredNature)) {
              iter.remove();
              break;
            }
          } catch (CoreException e) {
            CorePluginLog.logError(e);
          }
        }
      }
    }

    ElementListSelectionDialog dialog = new JavaProjectSelectionDialog(shell,
        javaProjects, initialSelection);

    if (dialog.open() == Window.OK) {
      return (IJavaProject) dialog.getFirstResult();
    }
    return null;
  }

  private JavaProjectSelectionDialog(Shell parent, List<IJavaProject> projects,
      IJavaProject initialSelection) {
    super(parent, new JavaElementLabelProvider(
        JavaElementLabelProvider.SHOW_DEFAULT));
    setTitle("Project Selection");
    setMessage("Choose a project:");
    setElements(projects.toArray(new IJavaProject[0]));
    setInitialSelections(new Object[] {initialSelection});
    setHelpAvailable(false);
  }

}
