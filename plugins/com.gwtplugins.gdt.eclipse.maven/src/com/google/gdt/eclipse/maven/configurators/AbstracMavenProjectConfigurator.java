/******************************************************************************
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

import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.maven.Activator;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

/**
 * An abstract Maven project configurator that should be extended by configurators that wish to
 * configure a GWT project to work with a particular SDK.
 * <p>
 * This configurator is used to perform general setup for any Maven project imported via m2Eclipse.
 * <p>
 * NOTE: Do not access this class from outside of the configurators package. All classes in the
 * configurators package have dependencies on plugins that may or may not be present in the user's
 * installation. As long as these classes are only invoked through m2Eclipe's extension points,
 * other parts of this plugin can be used without requiring the m2Eclipse dependencies.
 */
public abstract class AbstracMavenProjectConfigurator extends AbstractProjectConfigurator {
  /**
   * Optional callback interface for callers to get notifications before or after adding natures to
   * a project. Useful in cases where callers want to execute code before or after applying the
   * nature.
   */
  public class NatureCallback {
    /**
     * Invoked after the nature is added.
     */
    protected void afterAddingNature() {}

    /**
     * Invoked before the nature is added.
     */
    protected void beforeAddingNature() {}
  }

  protected static final String MAVEN_ECLIPSE_PLUGIN_ID = "org.apache.maven.plugins:maven-eclipse-plugin";
  protected static final String MAVEN_GWT_PLUGIN_ID1 = "org.codehaus.mojo:gwt-maven-plugin";
  protected static final String MAVEN_GWT_PLUGIN_ID2 = "net.ltgt.gwt.maven:gwt-maven-plugin";
  protected static final String MAVEN_WAR_PLUGIN = "org.apache.maven.plugins:maven-war-plugin";

  /**
   * {@inheritDoc} In the case of a non-GWT project, we do nothing.
   */
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    Activator.log("AbstractMavenProjectConfigurator.configure request=" + request);
    // Sometimes M2Eclipse calls this method with request == null. Why?
    if (request != null) {
      MavenProject mavenProject = request.mavenProject();
      Activator.log("AbstractMavenProjectConfigurator.configure mavenProject=" + mavenProject
          + " getGWtMavenPlugin=" + getGwtMavenPlugin(mavenProject));

      if (mavenProject != null && getGwtMavenPlugin(mavenProject) != null) {
        IProject project = request.mavenProjectFacade().getProject();

        // Make sure it is a java project, GWT Maven Plugin 2 gwt-app will not auto configure as one
        NatureUtils.addNature(project, JavaCore.NATURE_ID);

        doConfigure(mavenProject, project, request, monitor);
      }
    }
  }

  /**
   * Adds or removes a given nature to the given Eclipse project, but only if that nature is defined
   * in the Maven project. Invokes the {@link NatureCallback} methods before and after adding the
   * nature.
   *
   * @param project the Eclipse project
   * @param mavenProject the Maven project's description
   * @param natureId the nature to be added
   * @param addNature whether to add or remove the nature
   * @param callback callback to invoke when installing the nature
   * @param monitor a progress monitor
   * @return {@code true} if the nature was added
   * @throws CoreException on project update errors
   */
  protected boolean configureNature(IProject project, MavenProject mavenProject, String natureId,
      boolean addNature, final NatureCallback callback, IProgressMonitor monitor)
      throws CoreException {
    if (hasProjectNature(mavenProject, natureId)) {
      if (!NatureUtils.hasNature(project, natureId) && addNature) {
        if (callback != null) {
          callback.beforeAddingNature();
        }

        // Apply nature
        NatureUtils.addNature(project, natureId);

        if (callback != null) {
          callback.afterAddingNature();
        }
      }
      return true;
    }
    NatureUtils.removeNature(project, natureId);
    return false;
  }

  /**
   * Configures a given Eclipse project from a Maven project description.
   *
   * @param project the Eclipse project
   * @param mavenProject the Maven project's description
   * @param request the project configuration request
   * @param monitor a progress monitor
   * @throws CoreException on project update errors
   */
  protected abstract void doConfigure(MavenProject mavenProject, IProject project,
      ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  /**
   * Searches the Maven pom.xml for the given project nature.
   *
   * @param mavenProject a description of the Maven project
   * @param natureId the nature to check
   * @return {@code true} if the project
   */
  protected boolean hasProjectNature(MavenProject mavenProject, String natureId) {
    if (natureId == GWTNature.NATURE_ID || getGwtMavenPlugin(mavenProject) != null) {
      return true;
    }
    // The use of the maven-eclipse-plugin is deprecated. The following code is
    // only for backward compatibility.
    Plugin plugin = getEclipsePlugin(mavenProject);
    if (plugin != null) {
      Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
      if (configuration != null) {
        Xpp3Dom additionalBuildCommands = configuration.getChild("additionalProjectnatures");
        if (additionalBuildCommands != null) {
          for (Xpp3Dom projectNature : additionalBuildCommands.getChildren("projectnature")) {
            if (projectNature != null && natureId.equals(projectNature.getValue())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private Plugin getEclipsePlugin(MavenProject mavenProject) {
    return mavenProject.getPlugin(MAVEN_ECLIPSE_PLUGIN_ID);
  }

  /**
   * Get the first or second GWT Maven plugin.
   *
   * @param mavenProject
   * @return GWT maven plugin one or two
   */
  protected Plugin getGwtMavenPlugin(MavenProject mavenProject) {
    if (getGwtMavenPlugin2(mavenProject) != null) {
      return getGwtMavenPlugin2(mavenProject);
    }
    return getGwtMavenPlugin1(mavenProject);
  }

  protected Plugin getGwtMavenPlugin1(MavenProject mavenProject) {
    return mavenProject.getPlugin(MAVEN_GWT_PLUGIN_ID1);
  }

  protected Plugin getGwtMavenPlugin2(MavenProject mavenProject) {
    return mavenProject.getPlugin(MAVEN_GWT_PLUGIN_ID2);
  }

  /**
   * Get the War plugin.
   *
   * @param mavenProject
   * @return the war plugin.
   */
  protected Plugin getWarPlugin(MavenProject mavenProject) {
    return mavenProject.getPlugin(MAVEN_WAR_PLUGIN);
  }

  /**
   * Returns true if the first GWT Maven plugin exists.
   *
   * @param mavenProject
   * @return true if the first Maven plugin was used
   */
  protected boolean isGwtMavenPlugin1(MavenProject mavenProject) {
    return mavenProject.getPlugin(MAVEN_GWT_PLUGIN_ID1) != null;
  }

  /**
   * Returns true if the second GWT Maven plugin exists.
   *
   * @param mavenProject
   * @return true if the second GWT Maven plugin exists.
   */
  protected boolean isGwtMavenPlugin2(MavenProject mavenProject) {
    return mavenProject.getPlugin(MAVEN_GWT_PLUGIN_ID2) != null;
  }

}
