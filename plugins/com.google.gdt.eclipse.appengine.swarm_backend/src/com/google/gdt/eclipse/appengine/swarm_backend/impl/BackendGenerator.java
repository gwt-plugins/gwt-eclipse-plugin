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

package com.google.gdt.eclipse.appengine.swarm_backend.impl;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkContainer;
import com.google.gdt.eclipse.appengine.swarm.AppEngineSwarmPlugin;
import com.google.gdt.eclipse.appengine.swarm.IEndpointsActionCallback;
import com.google.gdt.eclipse.appengine.swarm.util.ConnectedProjectHandler;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmAnnotationUtils;
import com.google.gdt.eclipse.appengine.swarm.util.XmlUtil;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator;
import com.google.gdt.eclipse.appengine.swarm_backend.AppEngineSwarmBackendPlugin;
import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock.SdkSelection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Generates an App Engine backend project for a given Android project. The name
 * of the generated App Engine project is "androidProjectName-AppeEngine".
 */
public class BackendGenerator {

  private static final String GCM_PACKAGE_SUFFIX = "";

  private static final String GAE_PROJECT_NAME_SUFFIX = "-AppEngine";

  /**
   * Generates Web API for the java bean DeviceInfo.
   */
  public static void generateWebApi(IJavaProject appEngineJavaProject, IPath gaeSdkPath,
      String appId, SubMonitor monitor) throws Exception {

    SwarmServiceCreator serviceCreator = new SwarmServiceCreator();
    serviceCreator.setProject(appEngineJavaProject.getProject());
    serviceCreator.setAppId(appId);
    serviceCreator.setGaeSdkPath(gaeSdkPath);

    List<IType> entityList = new ArrayList<IType>();
    for (IPackageFragment pkgFragment : appEngineJavaProject.getPackageFragments()) {
      if (pkgFragment.getKind() != IPackageFragmentRoot.K_SOURCE) {
        continue;
      }
      for (ICompilationUnit cu : pkgFragment.getCompilationUnits()) {
        SwarmAnnotationUtils.collectSwarmTypes(entityList, cu);
      }
    }

    /*
     * TODO(appu): WARNING this is a hack to get MessageData not to create it's
     * own endpoint automatically. Why you ask, because MessageEndpoint does a
     * lot of custom stuff that isn't part of standard endpoint generation.
     */
    Iterator<IType> it = entityList.iterator();
    while (it.hasNext()) {
      IType entity = it.next();
      if (GCMSupport.APP_ENGINE_ENTITIES_TO_SKIP_ENDPOINT_GENERATION.contains(entity.getElementName())) {
        it.remove();
        break;
      }
    }
    serviceCreator.setEntities(entityList);

    // Using parent monitor; not spawning a new submonitor in this method, since
    // there's nothing we really want to report here
    serviceCreator.create(true, monitor);
  }

  public static String getAndroidBackendProjectName(String androidProjectName) {
    return androidProjectName.concat(GAE_PROJECT_NAME_SUFFIX);
  }

  public static String getAndroidBackendProjectPackage(String androidPackage) {
    return androidPackage + GCM_PACKAGE_SUFFIX;
  }
  private final IProject androidProject;
  private final boolean isNewlyCreatedAndroidProject;
  private String apiKey = null;
  private String projectNumber = null;

  private final SdkSelection<GaeSdk> sdkSelection;

  public BackendGenerator(IProject androidProject, boolean isNewlyCreatedAndroidProject,
      SdkSelection<GaeSdk> sdkSelection) {
    this.androidProject = androidProject;
    this.isNewlyCreatedAndroidProject = isNewlyCreatedAndroidProject;
    this.sdkSelection = sdkSelection;
  }

  /**
   * Constructor that allows injecting a projectNumber and ApiKey into the
   * generated code
   *
   * @param androidProject
   * @param isNewlyCreatedAndroidProject
   * @param projectNumber
   * @param apiKey
   */
  public BackendGenerator(IProject androidProject, boolean isNewlyCreatedAndroidProject,
      String projectNumber, String apiKey, SdkSelection<GaeSdk> sdkSelection) {
    this(androidProject, isNewlyCreatedAndroidProject, sdkSelection);
    this.apiKey = apiKey;
    this.projectNumber = projectNumber;
  }

  /**
   * Create a new instance of the BackendGenerator.
   *
   * @param androidProject The Android project for which to create a backend.
   */
  public BackendGenerator(IProject androidProject, SdkSelection<GaeSdk> sdkSelection) {
    this.androidProject = androidProject;
    isNewlyCreatedAndroidProject = false;
    this.sdkSelection = sdkSelection;
  }

  /**
   * Generate an App Engine Backend project for the given Android project.
   * Sample code will be generated based on whether
   * {@link #isGenerateCloudMessaging()} and/or
   * {@link #isGenerateSampleEndpoint()()} is set.
   *
   * This method invokes the operation in a Workspace Job that locks the entire
   * workspace. User feedback and options for cancellation are provided.
   *
   * TODO(rdayal): Add assertions to make sure that at least one of
   * isGenerateCloudMessaging/isGenerateSampleEndpoint is set.
   *
   * @throws CoreException
   * @throws InterruptedException
   */
  public void generateBackendProject() throws CoreException, InterruptedException {

    WorkspaceJob job = new WorkspaceJob("Generating App Engine Backend") {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {

        IStatus status = Status.OK_STATUS;
        try {
          monitor.beginTask("Generating App Engine Backend Project", 100);
          if (!createProject(getAndroidBackendProjectName(androidProject.getName()), monitor)) {
            status = new Status(IStatus.ERROR, AppEngineSwarmBackendPlugin.PLUGIN_ID,
                "An error occured during the project generation phase. See error log for details.");
          }
        } catch (ParserConfigurationException e) {
          AppEngineSwarmBackendPlugin.log(e);
          status = new Status(IStatus.ERROR, AppEngineSwarmBackendPlugin.PLUGIN_ID,
              "An error occured during the project generation phase. See error log for details.");
        } catch (SAXException e) {
          AppEngineSwarmBackendPlugin.log(e);
          status = new Status(IStatus.ERROR, AppEngineSwarmBackendPlugin.PLUGIN_ID,
              "An error occured during the project generation phase. See error log for details.");
        } catch (IOException e) {
          AppEngineSwarmBackendPlugin.log(e);
          status = new Status(IStatus.ERROR, AppEngineSwarmBackendPlugin.PLUGIN_ID,
              "An error occured during the project generation phase. See error log for details.");
        } finally {
          monitor.done();
        }

        return status;
      }
    };

    job.setRule(ResourcesPlugin.getWorkspace().getRoot());
    job.setUser(true);
    job.schedule();
  }

  /**
   * Generate an App Engine Backend project for the given Android project.
   * Sample code will be generated based on whether
   * {@link #isGenerateCloudMessaging()} and/or
   * {@link #isGenerateSampleEndpoint()()} is set.
   *
   * This method assumes that the caller is holding a lock on the workspace, and
   * is running in the context of some container that provides progress
   * monitoring.
   *
   * @throws CoreException
   * @throws InterruptedException
   */
  public void generateBackendProject(IProgressMonitor monitor) throws ParserConfigurationException,
      SAXException, IOException {
    createProject(getAndroidBackendProjectName(androidProject.getName()), monitor);
  }

  private boolean createProject(String backendProjectName, IProgressMonitor monitor)
      throws ParserConfigurationException, SAXException, IOException {
    boolean isSuccessful = false;

    SubMonitor submonitor = SubMonitor.convert(monitor, "Generating App Engine Backend", 100);

    // First, generate a simple App Engine Project

    IWebAppProjectCreator wapc = ProjectUtilities.createWebAppProjectCreator();
    String androidProjectPackageName = new XmlUtil().findAndroidPackage(androidProject);

    wapc.setProjectName(backendProjectName);
    wapc.setPackageName(androidProjectPackageName);
    wapc.setLocationURI(ResourcesPlugin.getWorkspace().getRoot().getLocationURI());
    wapc.setGenerateEmptyProject(true);
    wapc.setAppsMarketplaceSupported(false);

    wapc.addContainerPath(SdkClasspathContainer.computeContainerPath(GaeSdkContainer.CONTAINER_ID,
        sdkSelection.getSelectedSdk(), sdkSelection.isDefault()
            ? SdkClasspathContainer.Type.DEFAULT : SdkClasspathContainer.Type.NAMED));
    wapc.addNature(GaeNature.NATURE_ID);
    wapc.setIsGaeSdkFromEclipseDefault(true);

    try {
      wapc.create(submonitor.newChild(20));

      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      GaeProject gaeProject = GaeProject.create(ResourcesPlugin.getWorkspace().getRoot().getProject(
          backendProjectName));
      ConnectedProjectHandler.setConnectedProject(gaeProject.getProject(), androidProject);
      ConnectedProjectHandler.setConnectedProject(androidProject, gaeProject.getProject());

      /*
       * Remove GAE Notifier builder from project; we don't want to trigger the
       * re-generation of any of the Swarm service classes or client libraries
       * while we're constructing the project itself.
       */
      BuilderUtilities.removeBuilderFromProject(gaeProject.getProject(),
          GaeNature.GAE_PROJECT_CHANGE_NOTIFIER);

      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      GCMSupport gcmSupport = new GCMSupport(androidProject, gaeProject.getProject(),
          projectNumber, apiKey);

      // Add GCM templates for App Engine
      String appEnginePackageName = BackendGenerator.getAndroidBackendProjectPackage(androidProjectPackageName);
      gcmSupport.addAppEngineSupport(submonitor.newChild(10), appEnginePackageName);

      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      // We have to refresh the project after adding the GCM templates
      gaeProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, submonitor.newChild(1));

      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      /*
       * Now, generate the Swarm API and client libraries. This will copy the
       * endpoint library source over to the Android project, and add new
       * sourcepaths to the project.
       */
      generateWebApi(gaeProject, submonitor.newChild(40));

      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      // Perform a build so that we build the endpoint source.
      androidProject.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, submonitor.newChild(30));

      gcmSupport.addAndroidSupport(androidProjectPackageName, submonitor.newChild(30));

      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      androidProject.refreshLocal(IResource.DEPTH_INFINITE, submonitor.newChild(1));

      // Now, re-add the builder
      BuilderUtilities.addBuilderToProject(gaeProject.getProject(),
          GaeNature.GAE_PROJECT_CHANGE_NOTIFIER);

      isSuccessful = true;

      // Notify callbacks
      ExtensionQuery<IEndpointsActionCallback> extensionQuery = new ExtensionQuery<IEndpointsActionCallback>(
          AppEngineSwarmPlugin.PLUGIN_ID, "endpointscallback", "class");
      for (ExtensionQuery.Data<IEndpointsActionCallback> extensionData : extensionQuery.getData()) {
        if (isNewlyCreatedAndroidProject) {
          extensionData.getExtensionPointData().onGenerateAppEngineConnectedAndroidProject(
              gaeProject.getProject());
        } else {
          extensionData.getExtensionPointData().onGenerateAppEngineBackend(gaeProject.getProject());
        }
      }

    } catch (Exception e) {
      AppEngineSwarmBackendPlugin.log(e);
    } finally {
      monitor.done();
    }

    return isSuccessful;
  }

  /**
   * Generates Web API for the java bean DeviceInfo.
   */
  private void generateWebApi(GaeProject gaeProject, SubMonitor monitor) throws Exception {
    IPath gaeInstallationPath = gaeProject.getSdk().getInstallationPath();
    generateWebApi(gaeProject.getJavaProject(), gaeInstallationPath, gaeProject.getAppId(), monitor);
  }
}