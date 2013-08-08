/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.appengine.swarm.wizards;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.gdt.eclipse.appengine.swarm.AppEngineSwarmPlugin;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmAnnotationUtils;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmType;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator;
import com.google.gdt.eclipse.core.AdapterUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Swarm Generation class of contextual menu. Triggers the service class and API
 * generation.
 */
public class GenerateSwarmApiAction extends Action implements IActionDelegate {

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
            "Error in Generating API", "App Engine SDK should have minimum version of "
                + GaeSdkCapability.CLOUD_ENDPOINTS.minVersion + ".");
        return;
      } else if (entityList.isEmpty()) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Error in Generating API", "This project does not have cloud endpoint classes.");
        return;
      }
      final SwarmServiceCreator serviceCreator = new SwarmServiceCreator();
      serviceCreator.setEntities(entityList);
      serviceCreator.setAppId(getProjectAppId());

      new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()).run(
          false, false, new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) {
              if (!serviceCreator.create(true, monitor)) {
                MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                    "Error in Generating API",
                    "Generating Cloud Endpoint has encountered errors and is not complete.");
              }
            }
          });
    } catch (InvocationTargetException e) {
      AppEngineSwarmPlugin.getLogger().logError(e);
    } catch (InterruptedException e) {
      AppEngineSwarmPlugin.getLogger().logError(e);
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      project = AdapterUtilities.getAdapter(((IStructuredSelection) selection).iterator().next(),
          IProject.class);
      IJavaProject javaProject = JavaCore.create(project);
      entityList = new ArrayList<IType>();
      try {
        for (IPackageFragment pkgFragment : JavaCore.create(project).getPackageFragments()) {
          if (pkgFragment.getKind() != IPackageFragmentRoot.K_SOURCE) {
            continue;
          }
          for (ICompilationUnit cu : pkgFragment.getCompilationUnits()) {
            SwarmAnnotationUtils.collectTypes(entityList, cu, SwarmType.API);
            if (!entityList.isEmpty()) {
              break;
            }
          }
          if (!entityList.isEmpty()) {
            break;
          }
        }
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
