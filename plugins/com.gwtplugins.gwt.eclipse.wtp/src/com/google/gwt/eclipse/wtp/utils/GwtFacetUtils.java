package com.google.gwt.eclipse.wtp.utils;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gwt.eclipse.wtp.GwtWtpPlugin;
import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

public class GwtFacetUtils {
  
  /**
   * Returns if this project has a GWT facet.
   *
   * @param project
   * @return if the project has a GWT facet.
   */
  public static boolean hasGwtFacet(IProject project) {
    boolean hasFacet = false;
    try {
      hasFacet = FacetedProjectFramework.hasProjectFacet(project, "com.gwtplugins.gwt.facet");
    } catch (CoreException e) {
      CorePluginLog.logInfo("hasGetFacet: Error, can't figure GWT facet.", e);
    }

    return hasFacet;
  }
  
  /**
   * Returns if this project has a Old GWT facet. 
   *
   * @param project
   * @return if the project has a GWT facet.
   */
  public static boolean hasOldGwtFacet(IProject project) {
    boolean hasFacet = false;
    try {
      hasFacet = FacetedProjectFramework.hasProjectFacet(project, "com.google.gwt.facet");
    } catch (CoreException e) {
      CorePluginLog.logInfo("hasGetFacet: Error, can't figure GWT facet.", e);
    }

    return hasFacet;
  }

  /**
   * Does any of the projects added to the server runtime have a GWT Facet? 
   * 
   * TODO this will return the first GWT Facet. A multi GWT faceted project will fail. Add preference to turn one on or off.
   * 
   * @param server wtp runtime server
   * @return returns true if a GWT facet was found
   */
  public static boolean hasGwtFacet(IServer server) {
    IModule[] modules = getModules(server);

    for (IModule module : modules) {
      IProject project = module.getProject();

      IFacetedProject facetedProject = null;
      try {
        facetedProject = ProjectFacetsManager.create(project);
      } catch (CoreException e) {
        e.printStackTrace();
        continue;
      }

      if (facetedProject != null) {
        boolean hasFacet;
        try {
          hasFacet = FacetedProjectFramework.hasProjectFacet(facetedProject.getProject(),
              IGwtFacetConstants.GWT_PLUGINS_FACET_ID);
        } catch (CoreException e) {
          e.printStackTrace();
          continue;
        }
        if (hasFacet) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns the GWT Faceted project if is one in the server modules. 
   * 
   * TODO this will return the first GWT Facet. A multi GWT faceted project will fail. Add preference to turn one on or off. 
   * 
   * @param server wtp runtime server
   * @return returns true if a GWT facet was found
   */
  public static IFacetedProject getGwtFacetedProject(IServer server) {
    // Multi-module project
    IModule[] modules = getModules(server);

    for (IModule module : modules) {
      IProject project = module.getProject();

      IFacetedProject facetedProject = null;
      try {
        facetedProject = ProjectFacetsManager.create(project);
      } catch (CoreException e) {
        e.printStackTrace();
        continue;
      }

      if (facetedProject != null) {
        boolean hasFacet;
        try {
          hasFacet  = FacetedProjectFramework.hasProjectFacet(facetedProject.getProject(),
              IGwtFacetConstants.GWT_PLUGINS_FACET_ID);
        } catch (CoreException e) {
          e.printStackTrace();
          continue;
        }
        if (hasFacet) {
          return facetedProject;
        }
      }
    }

    return null;
  }
  
  /**
   * Get the modules added in the server.
   * @return 
   */
  public static IModule[] getModules(IServer server) {
 // Multi-module project
    IModule[] modules = server.getChildModules(server.getModules(), new NullProgressMonitor());
    if (modules == null || modules.length == 0) { // does it have multi-modules?
      // So it doesn't have multi-modules, lets use the root as the module.
      modules = server.getModules();
    }
    
    // Just in case
    if (modules == null || modules.length == 0) {
      GwtWtpPlugin.logMessage("Could not find GWT Faceted project from the Server runtime. Add a GWT Facet to the server modules.");
      return null;
    }
    
    return modules;
  }
  
}
