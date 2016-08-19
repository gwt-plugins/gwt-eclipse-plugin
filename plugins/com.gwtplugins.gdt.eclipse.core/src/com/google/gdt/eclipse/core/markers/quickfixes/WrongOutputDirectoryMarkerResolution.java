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
package com.google.gdt.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMarkerResolution;

/**
 * Resolution to the problem of having the output directory set to something
 * other than "<WAR>/WEB-INF/classes", when the WAR output directory is managed.
 * The resolution will set the output directory of the project to
 * "<WAR>/WEB-INF/classes" .
 */
public class WrongOutputDirectoryMarkerResolution implements IMarkerResolution {

  public String getLabel() {
    return "Set output directory to <WAR>/WEB-INF/classes";
  }

  public void run(IMarker marker) {
    IProject project = marker.getResource().getProject();

    IJavaProject javaProject = JavaCore.create(project);
    assert (JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject));

    try {
      WebAppUtilities.verifyHasManagedWarOut(project);

      WebAppUtilities.setOutputLocationToWebInfClasses(javaProject, null);
    } catch (CoreException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Modifying Output Directory",
          "Unable to set output directory to <WAR>/WEB-INF/classes. See the Error Log for more details.");
      return;
    }
  }
}
