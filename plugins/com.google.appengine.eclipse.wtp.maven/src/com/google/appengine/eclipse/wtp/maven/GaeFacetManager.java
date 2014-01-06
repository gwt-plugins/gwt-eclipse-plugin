package com.google.appengine.eclipse.wtp.maven;

import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action.Type;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.internal.FacetedProjectWorkingCopy;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.appengine.eclipse.wtp.server.GaeServer;

/**
 * Provides a method for determining whether an AppEngine facet (for either war or ear packaging)
 * should be added to a given project, and if so, adding it.
 */
public class GaeFacetManager {
  
  private static final String APPENGINE_GROUP_ID = "com.google.appengine";
  private static final String APPENGINE_MAVEN_PLUGIN_ARTIFACT_ID = "appengine-maven-plugin";
  
  /**
   * Determines whether an AppEngine facet should be added to a given project, and if so, adds it.
   * If the Maven project depends on appengine-maven-plugin and has war packaging, and does not
   * already have the facet {@link IGaeFacetConstants.GAE_FACET_ID}, that facet is added;
   * otherwise, if the Maven project depends on appengine-maven-plugin and has ear packaging, and
   * does not already have the facet {@link IGaeFacetConstants.GAE_EAR_FACET_ID}, that facet is
   * added.
   * 
   * @param pom the Maven model for the project
   * @param eclipseProject the given project
   */
  public void addGaeFacetIfNeeded(Model pom, IProject eclipseProject) {
    if (!isGaeProject(pom)) {
      return;
    }
    try {
      IFacetedProject facetedProject = getFacetedProject(eclipseProject);
      IProjectFacet facetOfInterest = getFacetForPackaging(pom.getPackaging());
      if (!facetedProject.hasProjectFacet(facetOfInterest)) {
        ensureGaeRuntimeDefined();
        addFacetToProject(facetOfInterest, facetedProject);
      }
    } catch (EarlyExit e) {
      return;
    }
  }
  
  private static boolean isGaeProject(Model pom) {
    List<Plugin> plugins = pom.getBuild().getPlugins();
    for (Plugin plugin : plugins) {
      if (APPENGINE_GROUP_ID.equals(plugin.getGroupId())
          && APPENGINE_MAVEN_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
        return true;
      }
    }
    return false;
  }

  private static IFacetedProject getFacetedProject(IProject eclipseProject) throws EarlyExit {
    try {
      return ProjectFacetsManager.create(eclipseProject);
    } catch (CoreException e) {
      AppEngineMavenPlugin.logError(
          "Error obtaining IFacetedProject for Eclipse project " + eclipseProject.getName(), e);
      throw new EarlyExit();
    }
  }

  private static IProjectFacet getFacetForPackaging(String packaging) throws EarlyExit {
    String facetIdOfInterest;
    switch (packaging) {
      case "war":
        facetIdOfInterest = IGaeFacetConstants.GAE_FACET_ID;
        break;
      case "ear":
        facetIdOfInterest = IGaeFacetConstants.GAE_EAR_FACET_ID;
        break;
      default:
        AppEngineMavenPlugin.logError(
            "Unexpected packaging \"" + packaging + "\" for a project using "
                + APPENGINE_MAVEN_PLUGIN_ARTIFACT_ID,
            null);
        throw new EarlyExit();
    }
    return ProjectFacetsManager.getProjectFacet(facetIdOfInterest);
  }
  
  private static void ensureGaeRuntimeDefined() {
    // HACK: access GdtPreferences forces it container plug-in to start
    // which will implicitly invoke auto-register GAE SDK
//    GdtPreferences.getInstallationId();
    GaeSdk sdk = GaePreferences.getDefaultSdk();
    try {
      IServerType serverType = ServerCore.findServerType(GaeServer.SERVER_TYPE_ID);
      IRuntime[] runtimes = ServerCore.getRuntimes();
      for (IRuntime runtime : runtimes) {
        if (runtime != null && serverType.getRuntimeType().equals(runtime.getRuntimeType())) {
          return;
        }
      }
      // not found, create new
      IRuntimeWorkingCopy runtimeWorkingCopy = serverType.getRuntimeType().createRuntime(null, null);
      GaeRuntime runtime = (GaeRuntime) runtimeWorkingCopy.loadAdapter(GaeRuntime.class,
          new NullProgressMonitor());
      if (sdk != null) {
        // have sdk, initialize
        String location = sdk.getInstallationPath().toOSString();
        runtime.setGaeSdk(sdk);
        runtimeWorkingCopy.setLocation(new Path(location));
      }
      runtimeWorkingCopy.save(true, null);
    } catch (CoreException e) {
      AppEnginePlugin.logMessage(e);
    }
  }

  // See https://code.google.com/p/appengine-maven-plugin/source/browse/src/main/java/com/google/appengine/SdkResolver.java
  // Also see GaeRuntimeConfigurator
  private static void addFacetToProject(
      IProjectFacet facetOfInterest, IFacetedProject facetedProject) throws EarlyExit {
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    workingCopy.addProjectFacet(facetOfInterest.getDefaultVersion());
    suppressSampleAppGeneration(workingCopy);
    try {
      workingCopy.commitChanges(null);
    } catch (CoreException e) {
      AppEngineMavenPlugin.logError(
          "Error committing addition of " + facetOfInterest.getId() + " facet to project", e);
      throw new EarlyExit();
    }
  }
  
  // For now, we suppress generation of a sample app, because it erroneously expects the file
  // $project_root/target/generated-sources/appengine-endpoints/WEB-INF/web.xml to exist, and aborts
  // the facet installation upon discovering that it doesn't.
  // TODO(nhcohen): Remove the call on this method, and the method itself, when we fix the problem
  // with sample app generation.
  private static void suppressSampleAppGeneration(IFacetedProjectWorkingCopy fpwc) {
    for (Action action : ((IFacetedProjectWorkingCopy) fpwc).getProjectFacetActions()) {
      if (action.getType().equals(Type.INSTALL)) {
        IDataModel dm = ((IDataModel) action.getConfig());
        dm.setBooleanProperty(IGaeFacetConstants.GAE_PROPERTY_CREATE_SAMPLE, false);
      }
    }
  }

  @SuppressWarnings("serial")
  private static class EarlyExit extends Exception {
    public EarlyExit() { }
  }

}
