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
package com.google.gwt.eclipse.core.nature;

import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.core.validators.WebAppProjectValidator;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.util.Util;
import com.google.gwt.eclipse.core.validators.GWTProjectValidator;
import com.google.gwt.eclipse.core.validators.java.JavaCompilationParticipant;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Identifies a Java project as a GWT project.
 */
public class GWTNature implements IProjectNature {

  public static final String NATURE_ID = GWTPlugin.PLUGIN_ID + ".gwtNature";

  // TODO: do this in background (WorkspaceJob)?
  public static void addNatureToProject(IProject project) throws CoreException {
    NatureUtils.addNature(project, GWTNature.NATURE_ID);
  }

  public static boolean isGWTProject(IProject project) {
    try {
      return project.isAccessible() && project.hasNature(GWTNature.NATURE_ID);
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
    }
    return false;
  }

  // TODO: do this in background (WorkspaceJob)?
  public static void removeNatureFromProject(IProject project)
      throws CoreException {
    if (!project.hasNature(GWTNature.NATURE_ID)) {
      return;
    }

    // Clear any GWT build artifacts.
    JavaCompilationParticipant.cleanBuildArtifacts(project);
    MarkerUtilities.clearMarkers(GWTProjectValidator.PROBLEM_MARKER_ID, project);

    // Remove the GWT Nature ID from the natures in the project description
    NatureUtils.removeNature(project, GWTNature.NATURE_ID);
  }

  private static void resetDefaultEditors() {
    // To ensure that the GWT Java Editor is the default for any Java files
    // inside this GWT project, make sure no editors are declared default for
    // the .java file extension. The GWT Java Editor will be the default
    // because of its association with content type "GWT Java Source File"
    Util.resetDefaultEditor("java");

    // Do a similar thing for xml files within the GWT project, in order to
    // ensure
    // that the GWT Module Editor opens for GWT Module files.
    Util.resetDefaultEditor("xml");
  }

  private IProject project;

  public void configure() throws CoreException {
    BuilderUtilities.addBuilderToProject(project,
        WebAppProjectValidator.BUILDER_ID);
    BuilderUtilities.addBuilderToProject(project,
        GWTProjectValidator.BUILDER_ID);
    resetDefaultEditors();
  }

  public void deconfigure() throws CoreException {
    BuilderUtilities.removeBuilderFromProject(project,
        GWTProjectValidator.BUILDER_ID);
    WebAppProjectValidator.removeBuilderIfNoGwtOrAppEngineNature(project);
  }

  public IProject getProject() {
    return project;
  }

  public void setProject(IProject project) {
    this.project = project;
  }
}
