/**
 *
 */
package com.google.gwt.eclipse.testing;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.jobs.JobsUtilities;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Maven unit testing utilities.
 */
public class GwtMavenTestingUtilities {

  // Copied from Maven2Utils to avoid the extra dependency
  private static final String MAVEN2_NATURE_ID = "org.eclipse.m2e.core.maven2Nature";

  /**
   * Convert the standard Java project to a Maven project. This will remove the Default GWT sdk and
   * instead use a GWT Maven sdk distribution. Using the Maven classpath container will allow for
   * adding a specific GWT version easily.
   *
   * TODO Embue the WebAppCreator factory or create a Maven web app factory with Maven creation
   * options.
   */
  public static void createMavenProject(IProject project, String withGwtSdkVersion) throws Exception {
    // Remove the default GWT sdk container from classpath, instead use Maven
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] entriesWithGwtContainer = javaProject.getRawClasspath();
    IClasspathEntry[] entriesWithOutGwtContainer =
        new IClasspathEntry[entriesWithGwtContainer.length - 1];
    int b = 0;
    for (int a = 0; a < entriesWithGwtContainer.length; a++) {
      String path = entriesWithGwtContainer[a].getPath().toString();
      if (!path.contains(GWTRuntimeContainer.CONTAINER_ID)) {
        entriesWithOutGwtContainer[b] = entriesWithGwtContainer[a];
        b++;
      }
    }
    // Removing the GWT SDK classpath entry from project
    javaProject.setRawClasspath(entriesWithOutGwtContainer, new NullProgressMonitor());
    JobsUtilities.waitForIdle();

    // Provide a pom.xml for a bare-bones configuration to convert standard project to Maven nature
    URL url = GwtTestingPlugin.getDefault().getBundle().getResource("resources/pom.xml");
    InputStream pomxmlStream = url.openStream();
    pomxmlStream = changeGwtSdkVersionInPom(pomxmlStream, withGwtSdkVersion);
    ResourceUtils.createFile(project.getFullPath().append("pom.xml"), pomxmlStream);

    // Turn on the Maven nature
    NatureUtils.addNature(project, MAVEN2_NATURE_ID);
    JobsUtilities.waitForIdle();

    // Maven update project will add the Maven dependencies to the classpath
    IProjectConfigurationManager projectConfig = MavenPlugin.getProjectConfigurationManager();
    projectConfig.updateProjectConfiguration(project, new NullProgressMonitor());
    JobsUtilities.waitForIdle();
  }

  /**
   * Replace the pom.xml GWT version properties with provided GWT version.
   */
  private static InputStream changeGwtSdkVersionInPom(InputStream pomxmlStream, String withGwtSdkVersion)
      throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(pomxmlStream));
    StringBuffer modifiedPom = new StringBuffer();
    String line;
    while ((line = reader.readLine()) != null) {
      String replaceWith = "<gwt.version>" + withGwtSdkVersion + "</gwt.version>";
      line = line.replaceFirst("<gwt.version>.*?</gwt.version>", replaceWith);
      replaceWith = "<gwt.plugin.version>" + withGwtSdkVersion + "</gwt.plugin.version>";
      line = line.replaceFirst("<gwt.plugin.version>.*?</gwt.plugin.version>", replaceWith);
      modifiedPom.append(line + "\n");
    }

    return new ByteArrayInputStream(modifiedPom.toString().getBytes());
  }

}
