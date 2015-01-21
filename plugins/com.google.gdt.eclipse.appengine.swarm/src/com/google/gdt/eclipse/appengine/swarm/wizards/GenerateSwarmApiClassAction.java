/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.appengine.swarm.wizards;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.gdt.eclipse.appengine.swarm.AppEngineSwarmPlugin;
import com.google.gdt.eclipse.appengine.swarm.IEndpointsActionCallback;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmAnnotationUtils;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmType;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator;
import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Swarm Generation class of contextual menu. Triggers the service class and API generation.
 */
public class GenerateSwarmApiClassAction extends Action implements IActionDelegate {

  private IProject project;
  private List<IType> entityList;

  public void run(IAction action) {
    try {
      if (!isAppEngineProject()) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Error in Generating API", "Not an App Engine project.");
        return;
      } else if (!GaeSdkCapability.CLOUD_ENDPOINTS.check(GaeProject.create(project).getSdk())) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Error in Generating API", "App Engine SDK should have minimum version "
                + GaeSdkCapability.CLOUD_ENDPOINTS.minVersion + ".");
        return;
      } else if (entityList.isEmpty()
          || (SwarmAnnotationUtils.getSwarmType(entityList.get(0)) != SwarmType.ENTITY)
          && (SwarmAnnotationUtils.getSwarmType(entityList.get(0)) != SwarmType.PERSISTENCE_CAPABLE)) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Error in Generating API", "This is not an JDO/JPA entity class.");
        return;
      } else if (!GaeProjectProperties.getGaeDatanucleusEnabled(project)) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Error in Generating API",
            "The datanucleus library in project is disabled and hence is not supported.");
        return;
      }
      final SwarmServiceCreator serviceCreator = new SwarmServiceCreator();
      serviceCreator.setEntities(entityList);
      serviceCreator.setAppId(getProjectAppId());
      IType entity = entityList.get(0);
      if (serviceCreator.serviceExists(entity.getElementName()
          + SwarmServiceCreator.SERVICE_CLASS_SUFFIX, entity.getPackageFragment())) {
        MessageBox dialog = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_QUESTION
            | SWT.OK | SWT.CANCEL);
        dialog.setText("Endpoint class already exists");
        dialog.setMessage(entity.getElementName() + SwarmServiceCreator.SERVICE_CLASS_SUFFIX
            + ".java already exists. Click OK if you want to overwrite it. "
            + "Click CANCEL otherwise.");
        if (SWT.OK != dialog.open()) {
          return;
        }
      }
      GaeProject gaeProject = GaeProject.create(project);
      serviceCreator.setAppId(gaeProject.getAppId());
      serviceCreator.setProject(project);
      serviceCreator.setGaeSdkPath(gaeProject.getSdk().getInstallationPath());

      new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()).run(
          true, true, new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
              try {
                serviceCreator.create(false, monitor);

                // Reporting
                ExtensionQuery<IEndpointsActionCallback> extensionQuery = new ExtensionQuery<IEndpointsActionCallback>(
                    AppEngineSwarmPlugin.PLUGIN_ID, "endpointscallback", "class");

                for (ExtensionQuery.Data<IEndpointsActionCallback> extensionData : extensionQuery.getData()) {
                  extensionData.getExtensionPointData().onGenerateEndpointClass(project);
                }
              } catch (Exception e) {
                throw new InvocationTargetException(e);
              }
            }
          });

    } catch (InvocationTargetException|InterruptedException|JavaModelException e) {
      MessageDialog.openInformation(Display.getDefault().getActiveShell(),
          "Error in Generating Endpoint Class",
          "An error occurred when generating the endpoint class. See the Error Log for more details.");
      AppEngineSwarmPlugin.getLogger().logError(e);
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      IFile file = AdapterUtilities.getAdapter(
          ((IStructuredSelection) selection).iterator().next(), IFile.class);
      project = file.getProject();
      entityList = new ArrayList<IType>();
      try {
        SwarmAnnotationUtils.collectSwarmTypes(entityList, (ICompilationUnit) JavaCore.create(file));
      } catch (JavaModelException e) {
        AppEngineSwarmPlugin.getLogger().logError(e);
      }
    }
  }

  private String getProjectAppId() {
    if (project == null) {
      return "";
    }
    return GaeProject.create(project).getAppId();
  }

  private boolean isAppEngineProject() {
    try {
      if (project != null && project.isAccessible() && project.hasNature(GaeNature.NATURE_ID)) {
        return true;
      }
    } catch (CoreException e) {
      e.printStackTrace();
    }
    return false;
  }
}
