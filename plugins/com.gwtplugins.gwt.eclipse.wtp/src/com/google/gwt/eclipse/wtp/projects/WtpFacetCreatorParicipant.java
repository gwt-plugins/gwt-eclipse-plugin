package com.google.gwt.eclipse.wtp.projects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator;
import com.google.gwt.eclipse.core.runtime.GwtSdk;
import com.google.gwt.eclipse.wtp.GwtWtpPlugin;
import com.google.gwt.eclipse.wtp.utils.GwtFacetUtils;

public class WtpFacetCreatorParicipant implements IWebAppProjectCreator.Participant {

  private IJavaProject javaProject;

  @Override
  public void updateWebAppProjectCreator(IWebAppProjectCreator webAppProjectCreator) {
    javaProject = webAppProjectCreator.getCreatedJavaProject();
    if (javaProject == null) {
      return;
    }

    runJob(webAppProjectCreator);
  }

  private void runJob(final IWebAppProjectCreator webAppProjectCreator) {
    Job job = new Job("Install GWT Facet for Project") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        installGwtFacet(webAppProjectCreator, monitor);
        
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  private void installGwtFacet(IWebAppProjectCreator webAppProjectCreator, IProgressMonitor monitor) {
    IProject project = webAppProjectCreator.getCreatedJavaProject().getProject();
    
    IFacetedProject facetedProject = null;
    try {
      facetedProject = ProjectFacetsManager.create(project);
    } catch (CoreException e) {
      GwtWtpPlugin.logError("Could not faceted project", e);
      return;
    }
    
    if (facetedProject == null) {
      return;
    }
    
    IProjectFacet gwtFacet = GwtFacetUtils.getGwtFacet();
    
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    workingCopy.addProjectFacet(gwtFacet.getDefaultVersion());
    try {
      workingCopy.commitChanges(monitor);
    } catch (CoreException e) {
      GwtWtpPlugin.logError("Could not commit facet to project.", e);
    }
  }

}