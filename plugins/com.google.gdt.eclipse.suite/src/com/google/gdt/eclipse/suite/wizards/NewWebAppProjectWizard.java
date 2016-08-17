/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.suite.wizards;

import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.resources.GdtImages;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.prefs.BackingStoreException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;

/**
 * Creates a new web application project.
 *
 * TODO: The progress monitors are not being used correctly.
 */
@SuppressWarnings("restriction")
public class NewWebAppProjectWizard extends NewElementWizard implements INewWizard {

  private IPath gwtSdkContainerPath;

  private URI locationURI;

  private NewWebAppProjectWizardPage newProjectWizardPage;

  private String packageName;

  private String projectName;

  private boolean useGWT;

  private boolean isGenerateEmptyProject;

  private boolean buildAnt;

  private boolean buildMaven;

  public NewWebAppProjectWizard() {
  }

  @Override
  public void addPages() {
    newProjectWizardPage = new NewWebAppProjectWizardPage();
    addPage(newProjectWizardPage);
  }

  @Override
  public IJavaElement getCreatedElement() {
    return null;
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    setHelpAvailable(false);
    setWindowTitle("New Web Application Project");
    setNeedsProgressMonitor(true);
    setDefaultPageImageDescriptor(GdtPlugin.getDefault().getImageDescriptor(GdtImages.GDT_NEW_PROJECT_LARGE));
  }

  /**
   * We initialize these members here so that finishPage can access them without triggering a SWT
   * InvalidThreadAccessException.
   */
  @Override
  public boolean performFinish() {
    projectName = newProjectWizardPage.getProjectName();
    useGWT = newProjectWizardPage.useGWT();
    gwtSdkContainerPath = newProjectWizardPage.getGWTSdkContainerPath();
    packageName = newProjectWizardPage.getPackage();
    locationURI = newProjectWizardPage.getCreationLocationURI();
    isGenerateEmptyProject = newProjectWizardPage.isGenerateEmptyProject();
    buildAnt = newProjectWizardPage.getBuildAnt();
    buildMaven = newProjectWizardPage.getBuildMaven();

    /**
     * HACK: We need to make sure that the DebugUITools plugin (and the DebugUIPlugin plugin) is loaded via the main
     * thread. before we call super.performFinish(). Otherwise, a race condition in Eclipse 3.5 occurs where
     * LaunchConfigurationManager.loadLaunchGroups() is called from two threads. The first call comes from our query for
     * launch groups in WebAppProjectCreator.createLaunchConfiguration() (which is part of a ModalContext runnable). The
     * second comes about due to the initialization of the DebugUIPlugin plugin (which we cause, by accessing classes in
     * this plugin through the DebugUITools plugin).
     */
    DebugUITools.getLaunchGroups();

    boolean finished = super.performFinish();
    if (finished) {
      // TODO: See JavaProjectWizard to see how to switch to Java perspective
      // and open new element
    }
    return finished;
  }

  @Override
  protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
    try {
      IWebAppProjectCreator wapc = ProjectUtilities.createWebAppProjectCreator();
      wapc.setProjectName(projectName);
      wapc.setPackageName(packageName);
      wapc.setLocationURI(locationURI);
      wapc.setGenerateEmptyProject(isGenerateEmptyProject);
      wapc.setBuildAnt(buildAnt);
      wapc.setBuildMaven(buildMaven);

      if (useGWT) {
        wapc.addContainerPath(gwtSdkContainerPath);
        wapc.addNature(GWTNature.NATURE_ID);
      }

      wapc.create(monitor);

    } catch (MalformedURLException e) {
      throw new CoreException(new Status(IStatus.ERROR, GdtPlugin.PLUGIN_ID, e.getMessage(), e));
    } catch (UnsupportedEncodingException e) {
      throw new CoreException(new Status(IStatus.ERROR, GdtPlugin.PLUGIN_ID, e.getMessage(), e));
    } catch (SdkException e) {
      throw new CoreException(new Status(IStatus.ERROR, GdtPlugin.PLUGIN_ID, e.getMessage(), e));
    } catch (ClassNotFoundException e) {
      throw new CoreException(new Status(IStatus.ERROR, GdtPlugin.PLUGIN_ID, e.getMessage(), e));
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, GdtPlugin.PLUGIN_ID, e.getMessage(), e));
    } catch (BackingStoreException e) {
      throw new CoreException(new Status(IStatus.ERROR, GdtPlugin.PLUGIN_ID, e.getMessage(), e));
    }
  }
}
