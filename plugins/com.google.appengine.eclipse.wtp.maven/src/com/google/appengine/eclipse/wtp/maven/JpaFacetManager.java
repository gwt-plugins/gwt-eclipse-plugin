package com.google.appengine.eclipse.wtp.maven;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class JpaFacetManager {
  
  public void addJpaFacet(IFacetedProject facetedProject, IProgressMonitor monitor) {
    IProjectFacet jpaFacet = ProjectFacetsManager.getProjectFacet(Constants.JPA_FACET_ID);
    if (!facetedProject.hasProjectFacet(jpaFacet)) {
      try {
        IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
        workingCopy.addProjectFacet(jpaFacet.getDefaultVersion());
        workingCopy.commitChanges(monitor);
      } catch (CoreException e) {
        AppEngineMavenPlugin.logError("Error committing addition of JPA facet to project", e);
      }
    }
  }

}
