package com.google.gdt.eclipse.maven.configurators;

import com.google.gdt.eclipse.core.ReflectionUtilities;

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
      try {
        return ReflectionUtilities.invoke(request, "getMavenProject");
      } catch (ReflectiveOperationException ignore) { // try the m2e 2.x accessor method
        return ReflectionUtilities.invoke(request, "mavenProject");
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed getting MavenProject from ProjectConfigurationRequest", e);
    }
  }

  public static IMavenProjectFacade getMavenProjectFacade(ProjectConfigurationRequest request) {
    try {
      try {
        return ReflectionUtilities.invoke(request, "getMavenProjectFacade");
      } catch (NoSuchMethodException ignore) { // try the m2e 2.x accessor method
        return ReflectionUtilities.invoke(request, "mavenProjectFacade");
      }
    } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Failed getting IMavenProjectFacade from ProjectConfigurationRequest", e);
    }
  }
}
