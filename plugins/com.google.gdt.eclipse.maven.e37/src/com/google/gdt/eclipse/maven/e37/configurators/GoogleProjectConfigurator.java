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
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gdt.eclipse.maven.Activator;
import com.google.gdt.eclipse.maven.sdk.GWTMavenRuntime;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
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

import java.util.Arrays;
import java.util.List;

/**
 * M2Eclipse project configuration extension that configures a project to get
 * the Google GWT/GAE project nature.
 *
 * NOTE: Do not access this class from outside of the configurators package. All
 * classes in the configurators package have dependencies on plugins that may or
 * may not be present in the user's installation. As long as these classes are
 * only invoked through m2Eclipe's extension points, other parts of this plugin
 * can be used without requiring the m2Eclipse dependencies.
 */
public class GoogleProjectConfigurator extends
    AbstractGoogleProjectConfigurator {

  private static final List<String> GAE_UNPACK_GOAL = Arrays.asList(new String[] {"net.kindleit:maven-gae-plugin:unpack"});

  @Override
  protected void doConfigure(final MavenProject mavenProject, IProject project,
      ProjectConfigurationRequest request, final IProgressMonitor monitor)
      throws CoreException {

    final IMaven maven = MavenPlugin.getDefault().getMaven();

    boolean configureGaeNatureSuccess = configureNature(project, mavenProject,
        GaeNature.NATURE_ID, true, new NatureCallbackAdapter() {

          @Override
          public void beforeAddingNature() {
            try {
              DefaultMavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
              executionRequest.setBaseDirectory(mavenProject.getBasedir());
              executionRequest.setLocalRepository(maven.getLocalRepository());
              executionRequest.setRemoteRepositories(mavenProject.getRemoteArtifactRepositories());
              executionRequest.setPluginArtifactRepositories(mavenProject.getPluginArtifactRepositories());
              executionRequest.setPom(mavenProject.getFile());
              executionRequest.setGoals(GAE_UNPACK_GOAL);

              MavenExecutionResult result = maven.execute(executionRequest,
                  monitor);
              if (result.hasExceptions()) {
                Activator.getDefault().getLog().log(
                    new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Error configuring project",
                        result.getExceptions().get(0)));
              }
            } catch (CoreException e) {
              Activator.getDefault().getLog().log(
                  new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                      "Error configuring project", e));
            }
          }
        }, monitor);

    boolean configureGWTNatureSuccess = configureNature(project, mavenProject,
        GWTNature.NATURE_ID, true, new NatureCallbackAdapter() {

          @Override
          public void beforeAddingNature() {

            // Get the GWT version from the project pom
            String gwtVersion = null;
            List<Dependency> dependencies = mavenProject.getDependencies();
            for (Dependency dependency : dependencies) {
              if (GWTMavenRuntime.MAVEN_GWT_GROUP_ID.equals(dependency.getGroupId())
                  && (GWTMavenRuntime.MAVEN_GWT_USER_ARTIFACT_ID.equals(dependency.getArtifactId()) || GWTMavenRuntime.MAVEN_GWT_SERVLET_ARTIFACT_ID.equals(dependency.getArtifactId()))) {
                gwtVersion = dependency.getVersion();
                break;
              }
            }

            // Check that the pom.xml has GWT dependencies
            if (!StringUtilities.isEmpty(gwtVersion)) {
              try {
                /*
                 * Download and install the gwt-dev.jar into the local
                 * repository.
                 */
                maven.resolve(GWTMavenRuntime.MAVEN_GWT_GROUP_ID,
                    GWTMavenRuntime.MAVEN_GWT_DEV_JAR_ARTIFACT_ID, gwtVersion,
                    "jar", null, mavenProject.getRemoteArtifactRepositories(),
                    monitor);
              } catch (CoreException e) {
                Activator.getDefault().getLog().log(
                    new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Error configuring project", e));
              }
            }
          }
        }, monitor);

    if (configureGWTNatureSuccess || configureGaeNatureSuccess) {
      try {
        // Add GWT Web Application configuration parameters
        WebAppProjectProperties.setWarSrcDir(project, new Path(
            "src/main/webapp"));
        WebAppProjectProperties.setWarSrcDirIsOutput(project, false);

        String artifactId = mavenProject.getArtifactId();
        String version = mavenProject.getVersion();
        IPath location = (project.getRawLocation() != null
            ? project.getRawLocation() : project.getLocation());
        if (location != null && artifactId != null && version != null) {
          WebAppProjectProperties.setLastUsedWarOutLocation(project,
              location.append("target").append(artifactId + "-" + version));
        }
      } catch (BackingStoreException be) {
        Activator.getDefault().getLog().log(
            new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                "Error configuring project", be));
      }
    }
  }

}
