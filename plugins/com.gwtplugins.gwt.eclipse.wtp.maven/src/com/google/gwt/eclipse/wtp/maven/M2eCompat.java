package com.google.gwt.eclipse.wtp.maven;

import org.apache.maven.project.MavenProject;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

/**
 * @author Luca Piccarreta
 *
 * Adaptation layer for m2e 1.x / 2.x API
 */
public class M2eCompat {
  public static MavenProject getMavenProject(ProjectConfigurationRequest request) {
    try {
      java.lang.reflect.Method m;
      try {
          m = ProjectConfigurationRequest.class.getMethod("getMavenProject");
      } catch (NoSuchMethodException ignore) { // try the m2e 2.x accessor method
          m = ProjectConfigurationRequest.class.getMethod("mavenProject");
      }
      return (MavenProject) m.invoke(request);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed getting MavenProject from ProjectConfigurationRequest", e);
    }
  }

  public static IMavenProjectFacade getMavenProjectFacade(ProjectConfigurationRequest request) {
    try {
      java.lang.reflect.Method m;
      try {
          m = ProjectConfigurationRequest.class.getMethod("getMavenProjectFacade");
      } catch (NoSuchMethodException ignore) { // try the m2e 2.x accessor method
          m = ProjectConfigurationRequest.class.getMethod("mavenProjectFacade");
      }
      return (IMavenProjectFacade) m.invoke(request);
    } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Failed getting IMavenProjectFacade from ProjectConfigurationRequest", e);
    }
  }
}
