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
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.resources.ProjectResources;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMarkerResolution;

import java.io.UnsupportedEncodingException;

/**
 * Resolution for the problem of a missing <WAR>/WEB-INF/web.xml file. The
 * resolution will create a web.xml file with placeholders for the servlet,
 * servlet-mapping, and welcome-file-list tags.
 * 
 * TODO: This class is similar to CreateAppEngineWebXMLFileMarkerResolution.
 * Consider refactoring out the common functionality between the two.
 */
public class CreateWebXMLFileMarkerResolution implements IMarkerResolution {

  public String getLabel() {
    return "Create a new web.xml file";
  }

  public void run(IMarker marker) {
    IProject project = marker.getResource().getProject();

    try {
      WebAppUtilities.verifyIsWebApp(project);

      IPath webInfFolderPath = WebAppUtilities.getWarSrc(project).getProjectRelativePath().append(
          "WEB-INF");

      ResourceUtils.createFolderStructure(project, webInfFolderPath);
      IFile webXMLFile = ResourceUtils.createFile(project.getFullPath().append(
          webInfFolderPath.append("web.xml")),
          ProjectResources.createWebXmlSource());
      ResourceUtils.openInDefaultEditor(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          webXMLFile, true);
    } catch (CoreException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Create <WAR>/WEB-INF/web.xml",
          "Unable to create the <WAR>/WEB-INF/web.xml file. See the Error Log for more details.");
      return;
    } catch (UnsupportedEncodingException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Create <WAR>/WEB-INF/web.xml",
          "Your platform does not support UTF-8 encoding. See the Error Log for more details.");
      return;
    }
  }
}
