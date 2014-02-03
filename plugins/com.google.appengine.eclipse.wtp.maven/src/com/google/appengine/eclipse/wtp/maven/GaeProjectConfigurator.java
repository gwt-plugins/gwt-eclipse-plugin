/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.maven;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.wtp.WTPProjectConfigurator;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * A {@link WTPProjectConfigurator} that adds GAE and JPA facets to GAE projects.
 */
@SuppressWarnings("restriction") // WTPProjectConfigurator
public class GaeProjectConfigurator extends WTPProjectConfigurator {
  
  private static final List<String> FILES_TO_REMOVE =
      ImmutableList.of("target", ".settings", ".classpath", ".project");

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    Model pom = request.getMavenProject().getModel();
    if (isGaeProject(pom)) {
      IProject eclipseProject = request.getProject();
      prepareCleanSlate(eclipseProject, monitor);
      IFacetedProject facetedProject = ProjectFacetsManager.create(eclipseProject);
      new GaeFacetManager().addGaeFacet(pom, facetedProject, monitor);
      new JpaFacetManager().addJpaFacet(facetedProject, monitor);
    }
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(
      IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    AppEngineMavenPlugin.logInfo("GaeProjectConfigurator.getBuildParticipant invoked");
    return super.getBuildParticipant(projectFacade, execution, executionMetadata);
  }
  
  private static boolean isGaeProject(Model pom) {
    List<Plugin> plugins = pom.getBuild().getPlugins();
    for (Plugin plugin : plugins) {
      if (Constants.APPENGINE_GROUP_ID.equals(plugin.getGroupId())
          && Constants.APPENGINE_MAVEN_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
        return true;
      }
    }
    return false;
  }
  
  private void prepareCleanSlate(IProject eclipseProject, IProgressMonitor monitor) {
    try {
      eclipseProject.refreshLocal(IResource.DEPTH_ONE, monitor);
      IResource[] topLevelFiles = eclipseProject.members();
      List<String> topLevelFileNames = Lists.newArrayListWithCapacity(topLevelFiles.length);
      for (IResource file : topLevelFiles) {
        topLevelFileNames.add(file.getName());
      }
      for (String fileName : FILES_TO_REMOVE) {
        if (topLevelFileNames.contains(fileName)) {
          Pattern filesToRenamePattern = Pattern.compile(Pattern.quote(fileName) + "(\\.old)*");
          PriorityQueue<String> filesToRename =
              new PriorityQueue<String>(
                  topLevelFileNames.size(),
                  // Comparator to order strings by descending length:
                  new Comparator<String>(){
                    @Override public int compare(String o1, String o2) {
                      return o2.length() - o1.length();
                    }
                  });
          for (String candidate : topLevelFileNames) {
            if (filesToRenamePattern.matcher(candidate).matches()) {
              filesToRename.add(candidate);
            }
          }
          // For a given file name "foo", we visit file names "foo", "foo.old", "foo.old.old", in
          // order of descending length, renaming "foo.old.old" to "foo.old.old.old", then renaming
          // "foo.old" to "foo.old.old", then renaming "foo" to "foo.old".
          while(!filesToRename.isEmpty()) {
            String oldName = filesToRename.poll();
            String newName = oldName + ".old";
            eclipseProject.getFile(oldName).move(new Path(newName), true, monitor);
          }
        }
      }
    } catch (CoreException e) {
      AppEngineMavenPlugin.logError(
          "Error renaming old generated files to prepare clean slate for new Maven project install",
          e);
    }
  }

}
