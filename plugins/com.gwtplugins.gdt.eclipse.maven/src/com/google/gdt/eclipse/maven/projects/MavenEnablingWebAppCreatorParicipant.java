/**
 *
 */
package com.google.gdt.eclipse.maven.projects;

import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator;
import com.google.gdt.eclipse.maven.Activator;
import com.google.gdt.eclipse.maven.MavenUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;

import java.util.List;

public class MavenEnablingWebAppCreatorParicipant implements IWebAppProjectCreator.Participant {

  private List<IJavaProject> javaProjects;

  @Override
  public void updateWebAppProjectCreator(IWebAppProjectCreator webAppProjectCreator) {
    boolean buildMaven = webAppProjectCreator.isBuildMaven();
    if (!buildMaven) {
      return;
    }

    javaProjects = webAppProjectCreator.getCreatedJavaProjects();
    if (javaProjects == null) {
      return;
    }

    runJob();
  }

  private void runJob() {
    Job job = new Job("Importing Maven Project") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        // Turn on the Maven nature
        try {
          for(int i=0;i<javaProjects.size();i++)
          {
            NatureUtils.addNature(javaProjects.get(i).getProject(), MavenUtils.MAVEN2_NATURE_ID);
          }
        } catch (CoreException e1) {
          e1.printStackTrace();
          return Status.CANCEL_STATUS;
        }

        Activator.log("MavenEnablingWebAppCreatorParicipant: Turning on Maven Nature");

        // Maven update project will add the Maven dependencies to the classpath
        IProjectConfigurationManager projectConfig = MavenPlugin.getProjectConfigurationManager();
        try {
          for(int i=0;i<javaProjects.size();i++)
          {
            projectConfig.updateProjectConfiguration(javaProjects.get(i).getProject(), monitor);
          }
        } catch (CoreException e) {
          // TODO(${user}): Auto-generated catch block
          e.printStackTrace();
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

}
