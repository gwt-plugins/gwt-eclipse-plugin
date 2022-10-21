/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gdt.eclipse.maven.configurators;

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gdt.eclipse.maven.Activator;
import com.google.gdt.eclipse.maven.sdk.GWTMavenRuntime;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.osgi.service.prefs.BackingStoreException;

import java.util.List;

/**
 * M2Eclipse project configuration extension that configures a project to get the GWT project nature.
 * <p>
 * NOTE: Do not access this class from outside of the configurators package. All classes in the configurators package
 * have dependencies on plugins that may or may not be present in the user's installation. As long as these classes are
 * only invoked through m2Eclipe's extension points, other parts of this plug-in can be used without requiring the
 * m2Eclipse dependencies.
 */
public class MavenProjectConfigurator extends AbstractMavenProjectConfigurator {

  /**
   * These properties may be pulled from the gwt-maven-plugin configuration of the pom to be used in configuring the GWT
   * Web Application settings for the Eclipse project. If not present, defaults are used.
   */
  private static final String ECLIPSE_LAUNCH_SRC_DIR_PROPERTY_KEY = "eclipseLaunchFromWarDir";
  private static final boolean ECLIPSE_LAUNCH_SRC_DIR_DEFAULT = false;
  private static final String WAR_SRC_DIR_PROPERTY_KEY = "warSourceDirectory";
  private static final String WEB_APP_DIRECTORY = "webappDirectory";
  private static final String HOSTED_WEB_APP_DIRECTORY = "hostedWebapp"; // GWT Maven Plugin 1 only
  private static final String GWT_MAVEN_MODULENAME = "moduleName";
  private static final String GWT_MAVEN_MODULESHORTNAME = "moduleShortName";

  private static final String WAR_SRC_DIR_DEFAULT = "src/main/webapp";

  @Override
  protected void doConfigure(final MavenProject mavenProject, IProject project, ProjectConfigurationRequest unused,
      final IProgressMonitor monitor) throws CoreException {
    Activator.log("MavenProjectConfigurator.doConfigure() invoked");

    // configure the GWT Nature
    boolean hasGwtNature = configureNature(project, mavenProject, GWTNature.NATURE_ID, true, new NatureCallback() {
      @Override
      protected void beforeAddingNature() {
        configureGwtProject(mavenProject, monitor);
      }
    }, monitor);

    // retrieve gwt-maven-plugin configuration if it exists
    Plugin gwtMavenPlugin = getGwtMavenPlugin(mavenProject);
    Xpp3Dom mavenConfig = gwtMavenPlugin == null ? null : (Xpp3Dom) gwtMavenPlugin.getConfiguration();

    // Persist GWT nature settings
    if (!hasGwtNature) {
      Activator.log("MavenProjectConfigurator: Skipping Maven configuration because GWT nature is false. hasGWTNature="
          + hasGwtNature);
      // Exit no maven plugin found
      return;
    }

    try {
      persistGwtNatureSettings(project, mavenProject, mavenConfig);
    } catch (BackingStoreException exception) {
      Activator.logError("MavenProjectConfigurator: Problem configuring maven project.", exception);
    }
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(final IMavenProjectFacade projectFacade, MojoExecution execution,
      final IPluginExecutionMetadata executionMetadata) {
    Activator.log("MavenProjectConfigurator.getBuildParticipant for Maven invoked");

    // Run the execution generate-module for maven2 plugin
    // Don't run on war for gwt maven plugin1
    MojoExecutionBuildParticipant build = null;
    MavenProject mavenProject = projectFacade.getMavenProject();
    if (mavenProject != null && isGwtMavenPlugin2(mavenProject)) {
      Activator.log("MavenProjectConfigurator.getBuildParticipant adding build participant for generate-module.");
      build = new MojoExecutionBuildParticipant(execution, true, true);
    }

    return build;
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    super.mavenProjectChanged(event, monitor);
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    Activator.log("MavenProjectConfigurator.configure invoked");
    super.configure(request, monitor);
  }

  /**
   * Save the settings for the GWT nature in the application GWT preferences.
   *
   * @param project
   * @param mavenProject
   * @param mavenConfig
   * @throws BackingStoreException
   */
  private void persistGwtNatureSettings(IProject project, MavenProject mavenProject, Xpp3Dom mavenConfig)
      throws BackingStoreException {
    IPath warOutDir = getWarOutDir(project, mavenProject);

    WebAppProjectProperties.setWarSrcDir(project, getWarSrcDir(mavenProject, mavenConfig)); // src/main/webapp
    WebAppProjectProperties.setWarSrcDirIsOutput(project, getLaunchFromHere(mavenConfig)); // false

    // TODO the extension should be used, from WarArgProcessor
    WebAppProjectProperties.setLastUsedWarOutLocation(project, warOutDir);

    WebAppProjectProperties.setGwtMavenModuleName(project, getGwtModuleName(mavenProject));
    WebAppProjectProperties.setGwtMavenModuleShortName(project, getGwtModuleShortName(mavenProject));

    String message = "MavenProjectConfiguratior Maven: Success with setting up GWT Nature\n";
    message += "\tartifactId=" + mavenProject.getArtifactId() + "\n";
    message += "\tversion=" + mavenProject.getVersion() + "\n";
    message += "\twarOutDir=" + warOutDir;
    Activator.log(message);
  }

  /**
   * Get the GWT Maven plugin 2 <moduleName/>.
   *
   * @param mavenProject
   * @return the moduleName from configuration
   */
  private String getGwtModuleName(MavenProject mavenProject) {
    if (!isGwtMavenPlugin2(mavenProject)) {
      return null;
    }

    Plugin gwtPlugin2 = getGwtMavenPlugin2(mavenProject);
    if (gwtPlugin2 == null) {
      return null;
    }

    Xpp3Dom gwtPluginConfig = (Xpp3Dom) gwtPlugin2.getConfiguration();
    if (gwtPluginConfig == null) {
      return null;
    }

    String moduleName = null;
    for (Xpp3Dom child : gwtPluginConfig.getChildren()) {
      if (child != null && GWT_MAVEN_MODULENAME.equals(child.getName())) {
        moduleName = child.getValue().trim();
      }
    }
    return moduleName;
  }

  /**
   * Get the GWT Maven plugin 2 <moduleShort=Name/>.
   *
   * @param mavenProject
   * @return the moduleName from configuration
   */
  private String getGwtModuleShortName(MavenProject mavenProject) {
    if (!isGwtMavenPlugin2(mavenProject)) {
      return null;
    }

    Plugin gwtPlugin2 = getGwtMavenPlugin2(mavenProject);
    if (gwtPlugin2 == null) {
      return null;
    }

    Xpp3Dom gwtPluginConfig = (Xpp3Dom) gwtPlugin2.getConfiguration();
    if (gwtPluginConfig == null) {
      return null;
    }

    String moduleName = null;
    for (Xpp3Dom child : gwtPluginConfig.getChildren()) {
      if (child != null && GWT_MAVEN_MODULESHORTNAME.equals(child.getName())) {
        moduleName = child.getValue().trim();
      }
    }
    return moduleName;
  }

  /**
   * Get the war output directory.
   *
   * @param project
   * @param mavenProject
   * @return returns the war output path
   */
  private IPath getWarOutDir(IProject project, MavenProject mavenProject) {
    String artifactId = mavenProject.getArtifactId();
    String version = mavenProject.getVersion();
    IPath locationOfProject = (project.getRawLocation() != null ? project.getRawLocation() : project.getLocation());

    IPath warOut = null;

    // Default directory target/artifact-version
    if (locationOfProject != null && artifactId != null && version != null) {
      warOut = locationOfProject.append("target").append(artifactId + "-" + version);
    }

    // Get the GWT Maven plugin 1 <hostedWebapp/> directory
    if (isGwtMavenPlugin1(mavenProject) && getGwtMavenPluginHostedWebAppDirectory(mavenProject) != null) {
      warOut = getGwtMavenPluginHostedWebAppDirectory(mavenProject);
    }

    // Get the Gwt Maven plugin 1 <webappDirectory/>
    if (isGwtMavenPlugin2(mavenProject) && getGwtPlugin2WebAppDirectory(mavenProject) != null) {
      warOut = getGwtPlugin2WebAppDirectory(mavenProject);
    }

    // Get the maven war plugin <webappDirectory/>
    if (getMavenWarPluginWebAppDirectory(mavenProject) != null) {
      warOut = getMavenWarPluginWebAppDirectory(mavenProject);
    }

    // make the directory if it doesn't exist
    if (warOut != null) {
      warOut.toFile().mkdirs();
    }

    return warOut;
  }

  /**
   * Get the GWT Maven <hostedWebapp/> directory.
   *
   * @param mavenProject
   * @return the webapp directory
   */
  private IPath getGwtMavenPluginHostedWebAppDirectory(MavenProject mavenProject) {
    Plugin warPlugin = getGwtMavenPlugin(mavenProject);
    if (warPlugin == null) {
      return null;
    }

    Xpp3Dom warPluginConfig = (Xpp3Dom) warPlugin.getConfiguration();
    if (warPluginConfig == null) {
      return null;
    }

    IPath warOut = null;
    for (Xpp3Dom child : warPluginConfig.getChildren()) {
      if (child != null && HOSTED_WEB_APP_DIRECTORY.equals(child.getName())) {
        String path = child.getValue().trim();
        if (!path.isEmpty()) {
          warOut = new Path(path);
        }
      }
    }

    return warOut;
  }

  /**
   * Return the war out directory via the Maven war plugin <webappDirectory/>.
   *
   * @param mavenProject
   * @return path to web app direcotry
   */
  protected IPath getMavenWarPluginWebAppDirectory(MavenProject mavenProject) {
    Plugin warPlugin = getWarPlugin(mavenProject);
    if (warPlugin == null) {
      return null;
    }

    Xpp3Dom warPluginConfig = (Xpp3Dom) warPlugin.getConfiguration();
    if (warPluginConfig == null) {
      return null;
    }

    IPath warOut = null;
    for (Xpp3Dom child : warPluginConfig.getChildren()) {
      if (child != null && WEB_APP_DIRECTORY.equals(child.getName())) {
        String path = child.getValue().trim();
        if (!path.isEmpty()) {
          warOut = new Path(path);
        }
      }
    }

    return warOut;
  }

  /**
   * Returns the Gwt Maven Plugin 2 web app directory <webappDirectory/>.
   *
   * @param mavenProject
   * @return path to web app direcotry
   */
  protected IPath getGwtPlugin2WebAppDirectory(MavenProject mavenProject) {
    Plugin warPlugin = getGwtMavenPlugin2(mavenProject);
    if (warPlugin == null) {
      return null;
    }

    Xpp3Dom warPluginConfig = (Xpp3Dom) warPlugin.getConfiguration();
    if (warPluginConfig == null) {
      return null;
    }

    IPath warOut = null;
    for (Xpp3Dom child : warPluginConfig.getChildren()) {
      if (child != null && WEB_APP_DIRECTORY.equals(child.getName())) {
        String path = child.getValue().trim();
        if (!path.isEmpty()) {
          warOut = new Path(path);
        }
      }
    }

    return warOut;
  }

  /**
   * Configure the GWT Nature.
   *
   * @param mavenProject
   * @param monitor
   */
  protected void configureGwtProject(MavenProject mavenProject, IProgressMonitor monitor) {
    // Get the GWT version from the project pom
    String gwtVersion = null;
    List<Dependency> dependencies = mavenProject.getDependencies();
    for (Dependency dependency : dependencies) {
      boolean hasGwtGroupId = GWTMavenRuntime.MAVEN_GWT_GROUP_ID.equals(dependency.getGroupId());
      boolean hasGwtUserArtivactId = GWTMavenRuntime.MAVEN_GWT_USER_ARTIFACT_ID.equals(dependency.getArtifactId());
      boolean hasGwtUserServletId = GWTMavenRuntime.MAVEN_GWT_SERVLET_ARTIFACT_ID.equals(dependency.getArtifactId());
      if (hasGwtGroupId && (hasGwtUserArtivactId || hasGwtUserServletId)) {
        gwtVersion = dependency.getVersion();
        Activator.log("MavenProjectConfigurator, Maven: Setting up Gwt Project. hasGwtGroupId=" + hasGwtGroupId
            + " hasGwtUser=" + hasGwtUserArtivactId + " hasGwtUserServletId=" + hasGwtUserServletId);
        break;
      }
    }

    Activator.log("MavenProjectConfigurator, Maven: gwtVersion=" + gwtVersion);

    resolveGwtDevJar(mavenProject, monitor, gwtVersion);
  }

  private void resolveGwtDevJar(MavenProject mavenProject, IProgressMonitor monitor, String gwtVersion) {
    // Check that the pom.xml has GWT dependencies
    if (gwtVersion != null && !StringUtilities.isEmpty(gwtVersion)) {
      try {
        // Download and install the gwt-dev.jar into the local repository.
        maven.resolve(GWTMavenRuntime.MAVEN_GWT_GROUP_ID, GWTMavenRuntime.MAVEN_GWT_DEV_JAR_ARTIFACT_ID, gwtVersion,
            "jar", null, mavenProject.getRemoteArtifactRepositories(), monitor);
      } catch (CoreException exception) {
        Activator.logError(
            "MavenProjectConfigurator: Problem configuring the maven project because it could not download the "
                + "gwt-dev.jar which is used to determine the version.",
            exception);
      }
    }
  }

  /**
   * Returns the war source directory such as src/main/webapp
   *
   * @param mavenProject
   *
   * @param config
   *          gwt-maven-maven config DOM
   * @return the {@link #WAR_SRC_DIR_PROPERTY_KEY} value from the config if it exists, {@link #WAR_SRC_DIR_DEFAULT}
   *         otherwise or if config is null
   */
  private static final IPath getWarSrcDir(MavenProject mavenProject, Xpp3Dom config) {
    String spath = WAR_SRC_DIR_DEFAULT;

    if (config != null) {
      for (Xpp3Dom child : config.getChildren()) {
        if (child != null && WAR_SRC_DIR_PROPERTY_KEY.equals(child.getName())) {
          spath = child.getValue() == null ? WAR_SRC_DIR_DEFAULT : child.getValue().trim();
        }
      }
    }

    IPath path = null;
    if (spath != null) {
      path = new Path(spath);

      String basePath = mavenProject.getBasedir().toPath().toAbsolutePath().toString();
      String fullPath = basePath + "/" + spath;
      java.io.File fullPathFile = new java.io.File(fullPath);
      if (!fullPathFile.exists()) {
        path = null;
      }
    }

    return path;
  }

  /**
   * Get the project launch locaiton
   *
   * @param config
   *          gwt-maven-maven config DOM
   * @return the {@link #ECLIPSE_LAUNCH_SRC_DIR_PROPERTY_KEY} value from the config if it exists,
   *         {@link #ECLIPSE_LAUNCH_SRC_DIR_DEFAULT} otherwise or if config is null
   */
  private final boolean getLaunchFromHere(Xpp3Dom config) {
    if (config == null) {
      return ECLIPSE_LAUNCH_SRC_DIR_DEFAULT;
    }
    for (Xpp3Dom child : config.getChildren()) {
      if (child != null && ECLIPSE_LAUNCH_SRC_DIR_PROPERTY_KEY.equals(child.getName())) {
        return child.getValue() == null ? ECLIPSE_LAUNCH_SRC_DIR_DEFAULT
            : Boolean.parseBoolean(child.getValue().trim());
      }
    }
    return ECLIPSE_LAUNCH_SRC_DIR_DEFAULT;
  }

}
