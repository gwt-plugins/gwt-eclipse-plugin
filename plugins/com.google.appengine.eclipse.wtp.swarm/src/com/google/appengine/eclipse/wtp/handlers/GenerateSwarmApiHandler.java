/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.appengine.eclipse.wtp.handlers;

import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.appengine.eclipse.wtp.swarm.AppEngineSwarmPlugin;
import com.google.appengine.eclipse.wtp.swarm.CloudEndpointsUtils;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmAnnotationUtils;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Generated the service class and API.
 */
public final class GenerateSwarmApiHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getActiveWorkbenchWindow(
        event).getActivePage().getSelection();
    IProject gaeProject = (IProject) selection.getFirstElement();
    // perform some checks
    try {
      if (!CloudEndpointsUtils.isEndpointsSupported(gaeProject)) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Error in Generating API", "App Engine SDK should have minimum version of "
                + GaeSdkCapability.CLOUD_ENDPOINTS.minVersion + ".");
        return null;
      }
      List<IType> entityList = getEntityList(gaeProject);
      if (entityList.isEmpty()) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Error in Generating API", "This project does not have cloud endpoint classes.");
        return null;
      }
      // prepare and generate
      final SwarmServiceCreator serviceCreator = CloudEndpointsUtils.createServiceCreator(
          gaeProject, entityList);
      new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()).run(
          false, false, new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
              try {
                serviceCreator.create(true, monitor);
              } catch (Exception e) {
                throw new InvocationTargetException(e);
              }
            }
          });
    } catch (InvocationTargetException ite) {
      throw new ExecutionException("Error generating API", ite.getCause());
    } catch (Throwable e) {
      throw new ExecutionException("Error generating API", e);
    }
    return null;
  }

  /**
   * Traverses the project and collect entities to generate from.
   */
  private List<IType> getEntityList(IProject project) {
    List<IType> entityList = Lists.newArrayList();
    try {
      for (IPackageFragment pkgFragment : JavaCore.create(project).getPackageFragments()) {
        if (pkgFragment.getKind() != IPackageFragmentRoot.K_SOURCE) {
          continue;
        }
        for (ICompilationUnit cu : pkgFragment.getCompilationUnits()) {
          SwarmAnnotationUtils.collectApiTypes(entityList, cu);
        }
      }
    } catch (JavaModelException e) {
      AppEngineSwarmPlugin.logMessage(e);
    }
    return entityList;
  }
}
