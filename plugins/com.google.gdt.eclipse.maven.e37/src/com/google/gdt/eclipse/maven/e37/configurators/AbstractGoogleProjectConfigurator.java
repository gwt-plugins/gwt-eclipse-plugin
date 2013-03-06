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
package com.google.gdt.eclipse.maven.e37.configurators;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

/**
 * An abstract Maven project configurator that should be extended by
 * configurators that wish to configure a project to work with a particular SDK.
 *
 * This configurator is used to perform general setup for any Maven project
 * imported via m2Eclipse.
 *
 * NOTE: Do not access this class from outside of the configurators package. All
 * classes in the configurators package have dependencies on plugins that may or
 * may not be present in the user's installation. As long as these classes are
 * only invoked through m2Eclipe's extension points, other parts of this plugin
 * can be used without requiring the m2Eclipse dependencies.
 */
public abstract class AbstractGoogleProjectConfigurator extends
    AbstractProjectConfigurator {

  /**
   * Optional callback interface for callers to get notifications before or
   * after adding natures to a project.
   * <p>
   * Useful in cases where callers want to execute code before applying the
   * nature.
   */
  public interface INatureCallback {

    void afterAddingNature();

    void beforeAddingNature();
  }

  /**
   * Simple no-op adapter for {@link INatureCallback}.
   */
  public class NatureCallbackAdapter implements INatureCallback {

    public void afterAddingNature() {
    }

    public void beforeAddingNature() {
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void configure(ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    // Sometimes M2Eclipse cals this method with request == null. Why?
    if (request != null) {
      MavenProject mavenProject = request.getMavenProject();
      IProject project = request.getProject();

      doConfigure(mavenProject, project, request, monitor);
    }
  }

  protected boolean configureNature(IProject project,
      MavenProject mavenProject, String natureId, boolean addNature,
      final INatureCallback callback, IProgressMonitor monitor)
      throws CoreException {
    if (hasProjectNature(mavenProject, project, natureId)) {
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

  protected boolean configureNature(IProject project,
      MavenProject mavenProject, String natureId, boolean addNature,
      IProgressMonitor monitor) throws CoreException {
    return configureNature(project, mavenProject, natureId, addNature, null,
        monitor);
  }

  protected abstract void doConfigure(MavenProject mavenProject,
      IProject project, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException;

  /**
   * Searches the Maven pom.xml for the given project nature.
   */
  protected boolean hasProjectNature(MavenProject mavenProject,
      IProject project, String natureId) {
    if ((natureId == GWTNature.NATURE_ID) && (getGwtMavenPlugin(mavenProject) != null)) {
      return true;
    }
    if ((natureId == GaeNature.NATURE_ID) && (getGaeMavenPlugin(mavenProject) != null)) {
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
            if (projectNature != null
                && natureId.equals(projectNature.getValue())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private Plugin getEclipsePlugin(MavenProject mavenProject) {
    return mavenProject.getPlugin("org.apache.maven.plugins:maven-eclipse-plugin");
  }

  private Plugin getGaeMavenPlugin(MavenProject mavenProject) {
    return mavenProject.getPlugin("net.kindleit:maven-gae-plugin");
  }

  private Plugin getGwtMavenPlugin(MavenProject mavenProject) {
    return mavenProject.getPlugin("org.codehaus.mojo:gwt-maven-plugin");
  }

}

