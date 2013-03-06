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
package com.google.gdt.eclipse.suite.markers.quickfixes;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.resources.GaeProjectResources;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMarkerResolution;

/**
 * Resolution to the problem of a missing appengine-web.xml file. This
 * resolution will generate a new appengine-web.xml file with an empty
 * application ID under the <WAR>/WEB-INF directory. If those directories do not
 * exist, they will be created.
 * 
 * No analysis is done to see if the project uses GWT RPC, so the special
 * inclusions/exclusions for the GWT RPC policy file are not present in the
 * generated appengine-web.xml file. A placeholder tag with instructions for
 * projects that use GWT RPC is generated instead.
 * 
 * TODO: This class is similar to CreateWebXMLFileMarkerResolution. Consider
 * refactoring out the common functionality between the two.
 */
public class CreateAppEngineWebXMLFileMarkerResolution implements
    IMarkerResolution {

  public String getLabel() {
    return "Create a new appengine-web.xml file";
  }

  public void run(IMarker marker) {
    IProject project = marker.getResource().getProject();

    try {
      WebAppUtilities.verifyIsWebApp(project);

      IPath webInfFolderPath = WebAppUtilities.getWarSrc(project).getProjectRelativePath().append(
          "WEB-INF");
      ResourceUtils.createFolderStructure(project, webInfFolderPath);

      IFile appengineWebXMLFile = ResourceUtils.createFile(
          project.getFullPath().append(
              webInfFolderPath.append("appengine-web.xml")),
          GaeProjectResources.createAppEngineWebXmlSource(GWTNature.isGWTProject(project)));

      ResourceUtils.openInDefaultEditor(
          AppEngineCorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          appengineWebXMLFile, true);
    } catch (CoreException e) {
      AppEngineCorePluginLog.logError(e);
      MessageDialog.openError(
          AppEngineCorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Create <WAR>/WEB-INF/appengine-web.xml",
          "Unable to create the <WAR>/WEB-INF/appengine-web.xml file. See the Error Log for more details.");
      return;
    }
  }

}
