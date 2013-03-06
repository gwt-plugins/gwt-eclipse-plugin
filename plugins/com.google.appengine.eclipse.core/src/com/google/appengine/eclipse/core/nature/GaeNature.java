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
package com.google.appengine.eclipse.core.nature;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.validators.GaeProjectValidator;
import com.google.appengine.eclipse.core.validators.java.JavaCompilationParticipant;
import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.core.validators.WebAppProjectValidator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Identifies a Java project as a GAE project.
 */
public class GaeNature implements IProjectNature {

  public static final String CLASS_ENHANCER_BUILDER = AppEngineCorePlugin.PLUGIN_ID
      + ".enhancerbuilder";
  public static final String GAE_PROJECT_CHANGE_NOTIFIER =
      AppEngineCorePlugin.PLUGIN_ID + ".gaeProjectChangeNotifier";
  public static final String NATURE_ID = AppEngineCorePlugin.PLUGIN_ID
      + ".gaeNature";

  public static void addNatureToProject(IProject project) throws CoreException {
    NatureUtils.addNature(project, GaeNature.NATURE_ID);
  }

  public static boolean isGaeProject(IProject project) {
    try {
      return project.isAccessible() && project.hasNature(GaeNature.NATURE_ID);
    } catch (CoreException e) {
      AppEngineCorePluginLog.logError(e);
    }
    return false;
  }

  public static void removeNatureFromProject(IProject project)
      throws CoreException {
    if (!project.hasNature(GaeNature.NATURE_ID)) {
      return;
    }

    // Clear any GAE build artifacts.
    JavaCompilationParticipant.cleanBuildArtifacts(project);
    MarkerUtilities.clearMarkers(GaeProjectValidator.PROBLEM_MARKER_ID, project);

    NatureUtils.removeNature(project, GaeNature.NATURE_ID);
  }

  private IProject project;

  public void configure() throws CoreException {
    BuilderUtilities.addBuilderToProject(project,
        WebAppProjectValidator.BUILDER_ID);
    if (GaeProjectProperties.getGaeDatanucleusEnabled(project)) {
      BuilderUtilities.addBuilderToProject(project, CLASS_ENHANCER_BUILDER);
      BuilderUtilities.addBuilderToProject(
          project, GAE_PROJECT_CHANGE_NOTIFIER);
    }
    BuilderUtilities.addBuilderToProject(project,
        GaeProjectValidator.BUILDER_ID);
  }

  public void deconfigure() throws CoreException {
    BuilderUtilities.removeBuilderFromProject(project, CLASS_ENHANCER_BUILDER);
    BuilderUtilities.removeBuilderFromProject(
        project, GAE_PROJECT_CHANGE_NOTIFIER);
    BuilderUtilities.removeBuilderFromProject(project,
        GaeProjectValidator.BUILDER_ID);
    WebAppProjectValidator.removeBuilderIfNoGwtOrAppEngineNature(project);
  }

  public IProject getProject() {
    return project;
  }

  public void setProject(IProject project) {
    this.project = project;
  }
}
