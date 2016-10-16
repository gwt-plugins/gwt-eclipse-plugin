package com.google.gwt.eclipse.wtp.projects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;

import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator;
import com.google.gwt.eclipse.wtp.GwtWtpPlugin;
import com.google.gwt.eclipse.wtp.utils.GwtFacetUtils;

public class WtpFacetCreatorParicipant implements IWebAppProjectCreator.Participant {

  private IJavaProject javaProject;
  private IFacetedProjectListener projectFacetListener;
 
  @Override
  public void updateWebAppProjectCreator(IWebAppProjectCreator webAppProjectCreator) {
    // Just in case exit early
    javaProject = webAppProjectCreator.getCreatedJavaProject();
    if (javaProject == null) {
      GwtWtpPlugin.logError("Not installing the GWT Facet b/c it's not a Java project");
      return;
    }

    // wait till the faceted project is setup
    listenForFacetedProjectChange();
  }

  private void listenForFacetedProjectChange() {
    projectFacetListener = new IFacetedProjectListener() {
      @Override
      public void handleEvent(IFacetedProjectEvent event) {
        // prevent recursion and only do this once.
        FacetedProjectFramework.removeListener(projectFacetListener);
        
        IFacetedProject facetedProject = event.getProject();
        runJob(facetedProject);
      }
    };
    FacetedProjectFramework.addListener(projectFacetListener, IFacetedProjectEvent.Type.PROJECT_MODIFIED);
    
    
  }

  private void runJob(final IFacetedProject facetedProject) {
    Job job = new Job("Install GWT Facet for Project") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        installGwtFacet(facetedProject, monitor);

        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  private void installGwtFacet(IFacetedProject facetedProject, IProgressMonitor monitor) {
    if (facetedProject == null) {
      GwtWtpPlugin.logError("Project is not faceted yet.");
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