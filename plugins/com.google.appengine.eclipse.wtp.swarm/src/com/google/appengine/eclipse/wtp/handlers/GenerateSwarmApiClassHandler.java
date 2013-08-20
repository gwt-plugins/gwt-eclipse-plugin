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
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmAnnotationUtils;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator;
import com.google.gdt.eclipse.core.DynamicWebProjectUtilities;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;

import java.util.List;

/**
 * Swarm Generation for class handler. Triggers the service class and API generation.
 */
public final class GenerateSwarmApiClassHandler extends AbstractSwarmApiHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getActiveWorkbenchWindow(
        event).getActivePage().getSelection();
    ICompilationUnit cu = (ICompilationUnit) selection.getFirstElement();
    IProject project = cu.getJavaProject().getProject();
    try {
      boolean good = DynamicWebProjectUtilities.isDynamicWebProject(project)
          && FacetedProjectFramework.hasProjectFacet(project, IGaeFacetConstants.GAE_FACET_ID)
          && FacetedProjectFramework.hasProjectFacet(project, "jpt.jpa");
      if (!good) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Error in Generating API",
            "Not an App Engine dynamic web project or has no JPA facet installed.");
        return null;
      }
      if (!isEndpointsSupported(project)) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Error in Generating API", "App Engine SDK should have minimum version of "
                + GaeSdkCapability.CLOUD_ENDPOINTS.minVersion + ".");
        return null;
      }
      List<IType> entityList = Lists.newArrayList();
      SwarmAnnotationUtils.collectSwarmTypes(entityList, cu);
      SwarmServiceCreator serviceCreator = createServiceCreator(project, entityList);
      IType entity = entityList.get(0);
      if (serviceCreator.serviceExists(entity.getElementName()
          + SwarmServiceCreator.SERVICE_CLASS_SUFFIX, entity.getPackageFragment())) {
        boolean question = MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
            "Endpoint class already exists", entity.getElementName()
                + SwarmServiceCreator.SERVICE_CLASS_SUFFIX
                + ".java already exists. Do you want to overwrite it?");
        if (!question) {
          return false;
        }
      }
      serviceCreator.create(false, new NullProgressMonitor());
    } catch (CoreException e) {
      throw new ExecutionException("Error generating API for class", e);
    } catch (Exception e) {
      MessageDialog.openInformation(Display.getDefault().getActiveShell(),
          "Error in Generating API", e.getMessage());
      return null;
    }
    return null;
  }
}
