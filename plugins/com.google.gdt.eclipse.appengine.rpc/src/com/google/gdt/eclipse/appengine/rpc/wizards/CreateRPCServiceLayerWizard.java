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
package com.google.gdt.eclipse.appengine.rpc.wizards;

import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;
import com.google.gdt.eclipse.appengine.rpc.wizards.helpers.RpcServiceLayerCreator;
import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.core.update.internal.core.UpdateQueryBuilder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;

/**
 * TODO: doc me.
 */
@SuppressWarnings("restriction")
public class CreateRPCServiceLayerWizard extends NewElementWizard implements
    INewWizard {

  private ConfigureRPCServiceLayerWizardPage configureRPCPage;

  private IProject project;

  private RpcServiceLayerCreator serviceCreator;

  public CreateRPCServiceLayerWizard() {
  }

  @Override
  public void addPages() {
    configureRPCPage = new ConfigureRPCServiceLayerWizardPage(project);
    addPage(configureRPCPage);
  }

  @Override
  public boolean canFinish() {
    return configureRPCPage.isPageComplete();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
   */
  @Override
  public IJavaElement getCreatedElement() {
    return null;
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    project = getSelectedProject(selection.getFirstElement());
  }

  @Override
  public boolean performFinish() {

    serviceCreator = RpcServiceLayerCreator.createNewRpcServiceLayerCreator();
    serviceCreator.setEntities(configureRPCPage.getEntityTypes());
    serviceCreator.setGaeProjectSrc(configureRPCPage.getContainerRoot());
    serviceCreator.setServiceName(configureRPCPage.getServiceName());

    if (serviceCreator.serviceExists()) {
      boolean answer = MessageDialog.openQuestion(
          configureRPCPage.getShell(),
          "RPC Service exists",
          configureRPCPage.getServiceName()
              + " already exists. Would you like to overwrite the existing service?");
      if (!answer) {
        return false;
      }
    }
    
    UpdateQueryBuilder.incrementRPCLayerCount(project, false);
    
    boolean result = super.performFinish();
    if (result) {
      try {
        JavaUI.openInEditor(serviceCreator.getElement());
      } catch (PartInitException e) {
        AppEngineRPCPlugin.log(e);
      } catch (JavaModelException e) {
        AppEngineRPCPlugin.log(e);
      }
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse
   * .core.runtime.IProgressMonitor)
   */
  @Override
  protected void finishPage(IProgressMonitor monitor)
      throws InterruptedException, CoreException {
    serviceCreator.create(monitor);
  }

  private IProject getSelectedProject(Object selection) {
    if (selection == null) {
      return null;
    }
    IProject proj = AdapterUtilities.getAdapter(selection, IProject.class);
    if (proj != null) {
      return proj;
    }
    IJavaElement javaElement = AdapterUtilities.getAdapter(selection,
        IJavaElement.class);

    if (javaElement != null) {
      return javaElement.getJavaProject().getProject();
    }
    return null;
  }
}
