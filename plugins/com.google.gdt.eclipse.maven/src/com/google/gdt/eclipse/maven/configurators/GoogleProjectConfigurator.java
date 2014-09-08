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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.osgi.service.prefs.BackingStoreException;

import java.util.List;

/**
 * M2Eclipse project configuration extension that configures a project to get the Google GWT project
 * nature.
 * <p>
 * NOTE: Do not access this class from outside of the configurators package. All classes in the
 * configurators package have dependencies on plugins that may or may not be present in the user's
 * installation. As long as these classes are only invoked through m2Eclipe's extension points,
 * other parts of this plug-in can be used without requiring the m2Eclipse dependencies.
 */
public class GoogleProjectConfigurator extends AbstractGoogleProjectConfigurator {
  /**
   * These properties may be pulled from the gwt-maven-plugin configuration of the pom to be used in
   * configuring the Google Web Application settings for the Eclipse project. If not present,
   * defaults are used.
   */
  private static final String ECLIPSE_LAUNCH_SRC_DIR_PROPERTY_KEY = "eclipseLaunchFromWarDir";
  private static final boolean ECLIPSE_LAUNCH_SRC_DIR_DEFAULT = false;
  private static final String WAR_SRC_DIR_PROPERTY_KEY = "warSourceDirectory";
  private static final String WAR_SRC_DIR_DEFAULT = "src/main/webapp";

  /**
   * @param config gwt-maven-maven config DOM
   * @return the {@link #ECLIPSE_LAUNCH_SRC_DIR_PROPERTY_KEY} value from the config if it exists,
   *         {@link #ECLIPSE_LAUNCH_SRC_DIR_DEFAULT} otherwise or if config is null
   */
  private static final boolean getLaunchFromHere(Xpp3Dom config) {
    if (config == null) {
      return ECLIPSE_LAUNCH_SRC_DIR_DEFAULT;
    }
    for (Xpp3Dom child : config.getChildren()) {
      if (child != null && ECLIPSE_LAUNCH_SRC_DIR_PROPERTY_KEY.equals(child.getName())) {
        return child.getValue() == null ? ECLIPSE_LAUNCH_SRC_DIR_DEFAULT : Boolean
            .parseBoolean(child.getValue().trim());
      }
    }
    return ECLIPSE_LAUNCH_SRC_DIR_DEFAULT;
  }

  /**
   * @param config gwt-maven-maven config DOM
   * @return the {@link #WAR_SRC_DIR_PROPERTY_KEY} value from the config if it exists,
   *         {@link #WAR_SRC_DIR_DEFAULT} otherwise or if config is null
   */
  private static final String getWarSrcDir(Xpp3Dom config) {
    if (config == null) {
      return WAR_SRC_DIR_DEFAULT;
    }
    for (Xpp3Dom child : config.getChildren()) {
      if (child != null && WAR_SRC_DIR_PROPERTY_KEY.equals(child.getName())) {
        return child.getValue() == null ? WAR_SRC_DIR_DEFAULT : child.getValue().trim();
      }
    }
    return WAR_SRC_DIR_DEFAULT;
  }

  @Override
  protected void doConfigure(final MavenProject mavenProject, IProject project,
      ProjectConfigurationRequest unused, final IProgressMonitor monitor) throws CoreException {
    final IMaven maven = MavenPlugin.getMaven();

    boolean configureGWTNatureSuccess =
        configureNature(project, mavenProject, GWTNature.NATURE_ID, true, new NatureCallback() {
          @Override
          protected void beforeAddingNature() {
            // Get the GWT version from the project pom
            String gwtVersion = null;
            List<Dependency> dependencies = mavenProject.getDependencies();
            for (Dependency dependency : dependencies) {
              boolean hasGwtGroupId =
                  GWTMavenRuntime.MAVEN_GWT_GROUP_ID.equals(dependency.getGroupId());
              boolean hasGwtUserArtivactId =
                  GWTMavenRuntime.MAVEN_GWT_USER_ARTIFACT_ID.equals(dependency.getArtifactId());
              boolean hasGwtUserServletId =
                  GWTMavenRuntime.MAVEN_GWT_SERVLET_ARTIFACT_ID.equals(dependency.getArtifactId());
              if (hasGwtGroupId && (hasGwtUserArtivactId || hasGwtUserServletId)) {
                gwtVersion = dependency.getVersion();
                break;
              }
            }

            // Check that the pom.xml has GWT dependencies
            if (!StringUtilities.isEmpty(gwtVersion)) {
              try {
                /*
                 * Download and install the gwt-dev.jar into the local repository.
                 */
                maven.resolve(GWTMavenRuntime.MAVEN_GWT_GROUP_ID,
                    GWTMavenRuntime.MAVEN_GWT_DEV_JAR_ARTIFACT_ID, gwtVersion, "jar", null,
                    mavenProject.getRemoteArtifactRepositories(), monitor);
              } catch (CoreException e) {
                Activator
                    .getDefault()
                    .getLog()
                    .log(
                        new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error configuring project",
                            e));
              }
            }
          }
        }, monitor);

    // retrieve gwt-maven-plugin configuration if it exists
    Plugin gwtPlugin = mavenProject.getPlugin(MAVEN_GWT_PLUGIN_ID);
    Xpp3Dom config = gwtPlugin == null ? null : (Xpp3Dom) gwtPlugin.getConfiguration();

    if (configureGWTNatureSuccess) {
      try {
        // Add GWT Web Application configuration parameters
        WebAppProjectProperties.setWarSrcDir(project, new Path(getWarSrcDir(config)));
        WebAppProjectProperties.setWarSrcDirIsOutput(project, getLaunchFromHere(config));

        String artifactId = mavenProject.getArtifactId();
        String version = mavenProject.getVersion();
        IPath location =
            (project.getRawLocation() != null ? project.getRawLocation() : project.getLocation());
        if (location != null && artifactId != null && version != null) {
          WebAppProjectProperties.setLastUsedWarOutLocation(project, location.append("target")
              .append(artifactId + "-" + version));
        }
      } catch (BackingStoreException be) {
        Activator.getDefault().getLog()
            .log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error configuring project", be));
      }
    }
  }
}
