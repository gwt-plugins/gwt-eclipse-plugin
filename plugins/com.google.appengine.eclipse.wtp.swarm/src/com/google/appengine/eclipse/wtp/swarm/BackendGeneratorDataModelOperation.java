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
package com.google.appengine.eclipse.wtp.swarm;

import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.jpa.SynchronizeClassesRunner;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.appengine.eclipse.wtp.wizards.GaeFacetWizardPage;
import com.google.gdt.eclipse.appengine.swarm.util.ConnectedProjectHandler;
import com.google.gdt.eclipse.appengine.swarm_backend.impl.BackendGenerator;
import com.google.gdt.eclipse.appengine.swarm_backend.impl.GCMSupport;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jpt.jpa.core.internal.facet.JpaFacetDataModelProperties;
import org.eclipse.jst.common.project.facet.core.libprov.ILibraryProvider;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryInstallDelegate;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderFramework;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

import java.util.Collection;

/**
 * A {@link IDataModelOperation} for backend generation.
 */
@SuppressWarnings("restriction")
public final class BackendGeneratorDataModelOperation extends AbstractDataModelOperation {
  private static final String GAE_LIBRARY_PROVIDER_ID = "com.google.appengine.eclipse.wtp.jpa.GaeLibraryProvider";
  private static final String FACET_JPT_JPA_ID = "jpt.jpa";
  private static final String FACET_JST_JAVA_ID = "jst.java";
  private static final String FACET_JST_WEB_ID = "jst.web";

  public BackendGeneratorDataModelOperation(IDataModel model) {
    super(model);
  }

  @Override
  public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    try {
      IDataModel dataModel = getDataModel();
      IFacetedProjectWorkingCopy facetedProject = (IFacetedProjectWorkingCopy) dataModel.getProperty(IFacetDataModelProperties.FACETED_PROJECT_WORKING_COPY);
      IRuntime runtime = (IRuntime) dataModel.getProperty(BackendGeneratorDataModelProvider.SELECTED_RUNTIME);
      facetedProject.setPrimaryRuntime(runtime);
      // add Java facet
      IProjectFacet javaFacet = ProjectFacetsManager.getProjectFacet(FACET_JST_JAVA_ID);
      facetedProject.addProjectFacet(javaFacet.getDefaultVersion());
      // add Web facet
      IProjectFacet webFacet = ProjectFacetsManager.getProjectFacet(FACET_JST_WEB_ID);
      IProjectFacetVersion version = webFacet.getLatestSupportedVersion(runtime);
      facetedProject.addProjectFacet(version);
      // add JPA facet
      IProjectFacet jpaFacet = ProjectFacetsManager.getProjectFacet(FACET_JPT_JPA_ID);
      if (jpaFacet != null) {
        IProjectFacetVersion jpaFacetVersion = jpaFacet.getVersion("2.0");
        if (jpaFacetVersion != null) {
          facetedProject.addProjectFacet(jpaFacetVersion);
          Action jpaFacetAction = facetedProject.getProjectFacetAction(jpaFacet);
          IDataModel jpaDataModel = (IDataModel) jpaFacetAction.getConfig();
          // do configure
          configureJpaFacet(jpaDataModel);
        } else {
          return StatusUtilities.newErrorStatus("Required JPA facet 2.0 is not found",
              AppEngineSwarmPlugin.PLUGIN_ID);
        }
      } else {
        return StatusUtilities.newErrorStatus("Required JPA facet is missing",
            AppEngineSwarmPlugin.PLUGIN_ID);
      }
      // add App Engine facet
      IProjectFacet gaeFacet = ProjectFacetsManager.getProjectFacet(IGaeFacetConstants.GAE_FACET_ID);
      facetedProject.addProjectFacet(gaeFacet.getDefaultVersion());
      Action gaeFacetAction = facetedProject.getProjectFacetAction(gaeFacet);
      IDataModel gaeCurrentDataModel = (IDataModel) gaeFacetAction.getConfig();
      IDataModel gaeConfiguredDataModel = (IDataModel) dataModel.getProperty(BackendGeneratorDataModelProvider.GAE_FACET_INSTALL_DM);
      // copy configured DM to action config data model.
      // another option is to execute default operation of source DM, but this requires commit
      // project working copy before.
      configureGaeFacet(gaeCurrentDataModel, gaeConfiguredDataModel);
      // commit
      facetedProject.commitChanges(monitor);
      facetedProject.dispose();

      // create contents for both projects
      doGenerateContents(monitor);
      // sync classes with persistence.xml
      syncClasses();
      return Status.OK_STATUS;
    } catch (CoreException e) {
      return e.getStatus();
    } catch (Exception e) {
      return StatusUtilities.newErrorStatus(e, AppEngineSwarmPlugin.PLUGIN_ID);
    }
  }

  /**
   * Copies properties filled using {@link GaeFacetWizardPage} into Action config data model for
   * installing GAE facet.
   */
  private void configureGaeFacet(IDataModel targetDataModel, IDataModel sourceDataModel) {
    @SuppressWarnings("unchecked")
    Collection<String> properties = sourceDataModel.getBaseProperties();
    for (String property : properties) {
      if (sourceDataModel.isPropertySet(property)) {
        Object value = sourceDataModel.getProperty(property);
        targetDataModel.setProperty(property, value);
      }
    }
  }

  /**
   * Makes JPA facet use GAE library provider.
   */
  private void configureJpaFacet(IDataModel jpaDataModel) {
    Object runtime = jpaDataModel.getProperty(JpaFacetDataModelProperties.RUNTIME);
    if (runtime == null) {
      runtime = getDataModel().getProperty(BackendGeneratorDataModelProvider.SELECTED_RUNTIME);
      jpaDataModel.setProperty(JpaFacetDataModelProperties.RUNTIME, runtime);
    }
    // install libprov
    ILibraryProvider provider = LibraryProviderFramework.getProvider(GAE_LIBRARY_PROVIDER_ID);
    LibraryInstallDelegate delegate = (LibraryInstallDelegate) jpaDataModel.getProperty(JpaFacetDataModelProperties.LIBRARY_PROVIDER_DELEGATE);
    delegate.setLibraryProvider(provider);
  }

  /**
   * Does creating connected projects resources and configuring them.
   */
  private void doGenerateContents(IProgressMonitor monitor) throws Exception {
    SubMonitor submonitor = SubMonitor.convert(monitor, "Generating App Engine Backend Contents",
        100);
    // prepare
    IDataModel dataModel = getDataModel();
    IProject appEngineProject = ProjectUtils.getProject(dataModel);
    try {
      // disable builder
      appEngineProject.setSessionProperty(CloudEndpointsUtils.PROP_DISABLE_ENDPOINTS_BUILDER, "true");

      IRuntime runtime = (IRuntime) dataModel.getProperty(BackendGeneratorDataModelProvider.SELECTED_RUNTIME);
      String projectNumber = dataModel.getStringProperty(BackendGeneratorDataModelProvider.SCM_PROJECT_NUMBER);
      String apiKey = dataModel.getStringProperty(BackendGeneratorDataModelProvider.SCM_API_KEY);
      IProject androidProject = ProjectUtils.getProject(dataModel,
          BackendGeneratorDataModelProvider.ANDROID_PROJECT_NAME);
      String androidProjectPackageName = dataModel.getStringProperty(BackendGeneratorDataModelProvider.ANDROID_PACKAGE_NAME);
      IDataModel gaeConfiguredDataModel = (IDataModel) dataModel.getProperty(BackendGeneratorDataModelProvider.GAE_FACET_INSTALL_DM);
      String appEnginePackageName = gaeConfiguredDataModel.getStringProperty(IGaeFacetConstants.GAE_PROPERTY_PACKAGE);
      // bind projects together
      ConnectedProjectHandler.setConnectedProject(appEngineProject, androidProject);
      ConnectedProjectHandler.setConnectedProject(androidProject, appEngineProject);
      // prepare generators
      GCMSupport gcmSupport = new GCMSupport(androidProject, appEngineProject, projectNumber,
          apiKey);
      // add GCM templates for App Engine
      IJavaProject appEngineJavaProject = JavaCore.create(appEngineProject);
      ensureAppEnginePackage(appEngineJavaProject, appEnginePackageName);
      gcmSupport.addAppEngineSupport(submonitor.newChild(10), appEnginePackageName);
      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      // we have to refresh the project after adding the GCM templates
      appEngineProject.refreshLocal(IResource.DEPTH_INFINITE, submonitor.newChild(1));
      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      /*
       * Now, generate the Swarm API and client libraries. This will copy the endpoint library
       * source over to the Android project, and add new sourcepaths to the project.
       */
      String appId = ProjectUtils.getAppId(appEngineProject);
      IPath gaeSdkPath = ProjectUtils.getGaeSdkLocation(runtime);
      if (gaeSdkPath == null) {
        throw new CoreException(StatusUtilities.newErrorStatus(
            "No Google App Engine SDK found for runtime: " + runtime.getName(),
            AppEngineSwarmPlugin.PLUGIN_ID));
      }
      try {
        BackendGenerator.generateWebApi(appEngineJavaProject, gaeSdkPath, appId,
            submonitor.newChild(40));
      } catch (Exception e) {
        throw new CoreException(StatusUtilities.newErrorStatus(e, AppEngineSwarmPlugin.PLUGIN_ID));
      }
      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      // perform a build so that we build the endpoint source.
      androidProject.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, submonitor.newChild(30));
      // android turn
      gcmSupport.addAndroidSupport(androidProjectPackageName, submonitor.newChild(30));
      if (submonitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      // do refresh
      androidProject.refreshLocal(IResource.DEPTH_INFINITE, submonitor.newChild(1));
      // TODO(amitin): notify callbacks?
    } finally {
      // enable builder
      appEngineProject.setSessionProperty(CloudEndpointsUtils.PROP_DISABLE_ENDPOINTS_BUILDER, null);
    }
  }

  /**
   * Ensures that the given package exists in app engine project.
   */
  private void ensureAppEnginePackage(IJavaProject appEngineJavaProject, String appEnginePackageName)
      throws CoreException {
    IFolder sourceFolder = appEngineJavaProject.getProject().getFolder("src");
    if (!sourceFolder.exists()) {
      sourceFolder.create(false, true, null);
    }
    appEngineJavaProject.getPackageFragmentRoot(sourceFolder).createPackageFragment(
        appEnginePackageName, false, null);
  }

  /**
   * @return 'persistence.xml' resource.
   */
  private IFile getPersistenceXml(IProject project) {
    // using JPA/JPT is more proper way to get persistence.xml resource file, but unfortunately
    // Eclipse people constantly change JPA/JPT interfaces, so use simple resource get.
    return project.getFile("src/META-INF/persistence.xml");
  }

  /**
   * Adds generated classes into 'persistence.xml'.
   */
  private void syncClasses() {
    final IFile persistenceXml = getPersistenceXml(ProjectUtils.getProject(getDataModel()));
    if (!persistenceXml.exists()) {
      return;
    }
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        SynchronizeClassesRunner runner = new SynchronizeClassesRunner();
        runner.syncClasses(persistenceXml, new NullProgressMonitor());
      }
    });
  }
}