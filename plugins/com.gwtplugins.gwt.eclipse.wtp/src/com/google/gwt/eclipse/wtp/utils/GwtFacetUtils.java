package com.google.gwt.eclipse.wtp.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

public class GwtFacetUtils {

  /**
   * Does any of the projects added to the server runtime have a GWT Facet? 
   * 
   * TODO this will return the first GWT Facet. A multi GWT faceted project will fail. Add preference to turn one on or off.
   * 
   * @param server wtp runtime server
   * @return returns true if a GWT facet was found
   */
  public static boolean hasGwtFacet(IServer server) {
    IModule[] modules = server.getChildModules(server.getModules(), new NullProgressMonitor());
    if (modules == null || modules.length == 0) {
      return false;
    }

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
              IGwtFacetConstants.GWT_FACET_ID);
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
    IModule[] modules = server.getChildModules(server.getModules(), new NullProgressMonitor());
    if (modules == null || modules.length == 0) {
      return null;
    }

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
              IGwtFacetConstants.GWT_FACET_ID);
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
  
}
