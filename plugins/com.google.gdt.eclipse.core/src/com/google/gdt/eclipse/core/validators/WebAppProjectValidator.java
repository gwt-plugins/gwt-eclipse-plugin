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
package com.google.gdt.eclipse.core.validators;

import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.markers.ProjectStructureOrSdkProblemType;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.List;
import java.util.Map;

/**
 * Builder that performs validation on a project with the WebAppNature.
 */
public class WebAppProjectValidator extends IncrementalProjectBuilder {

  public static final String BUILDER_ID = CorePlugin.PLUGIN_ID
      + ".webAppProjectValidator";

  public static final String PROBLEM_MARKER_ID = CorePlugin.PLUGIN_ID
      + ".webAppProblemMarker";

  public static void removeBuilderIfNoGwtOrAppEngineNature(IProject project)
      throws CoreException {
    // Unfortunately, we can't access GWTNature or GaeNature from here, so we
    // have to hard-code the nature IDs (which are unlikely to change anyway).
    if (!ProjectUtilities.isGpeProject(project)) {
      BuilderUtilities.removeBuilderFromProject(project, BUILDER_ID);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
      throws CoreException {

    // Clear existing markers.
    MarkerUtilities.clearMarkers(PROBLEM_MARKER_ID, getProject());

    if (kind == CLEAN_BUILD) {
      cleanImpl();
      return null;
    }

    IJavaProject javaProject = JavaCore.create(getProject());

    if (!JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject)) {
      // TODO: This error will be flagged by the GDT Validator.
      return null;
    }

    if (!WebAppUtilities.isWebApp(getProject())) {
      return null;
    }

    if (!validateWarSrcDirectoryExists()) {
      return null;
    }

    if (!validateWebXmlFileExists()) {
      return null;
    }

    // These validations only apply when the project has a managed WAR output
    // directory
    if (WebAppUtilities.hasManagedWarOut(getProject())) {
      if (!validateOutputDirectory(javaProject)) {
        return null;
      }

      if (!validateBuildClasspath(javaProject)) {
        return null;
      }
    }

    return null;
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    cleanImpl();
  }

  private void cleanImpl() throws CoreException {
    MarkerUtilities.clearMarkers(PROBLEM_MARKER_ID, getProject());
  }

  private boolean validateBuildClasspath(IJavaProject javaProject)
      throws CoreException {
    IPath webInfLibFolderLocation = null;

    IFolder webInfLibFolder = WebAppUtilities.getWebInfOut(getProject()).getFolder(
        "lib");

    if (webInfLibFolder.exists()) {
      webInfLibFolderLocation = webInfLibFolder.getLocation();
    }

    IClasspathEntry[] rawClasspaths = javaProject.getRawClasspath();
    boolean isOk = true;
    List<IPath> excludedJars = WebAppProjectProperties.getJarsExcludedFromWebInfLib(javaProject.getProject());

    for (IClasspathEntry rawClasspath : rawClasspaths) {
      rawClasspath = JavaCore.getResolvedClasspathEntry(rawClasspath);
      if (rawClasspath.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
        IPath entryPath = ResourceUtils.resolveToAbsoluteFileSystemPath(rawClasspath.getPath());
        if (excludedJars.contains(entryPath)) {
          continue;
        }

        if (webInfLibFolderLocation == null
            || !webInfLibFolderLocation.isPrefixOf(entryPath)) {
          MarkerUtilities.createQuickFixMarker(PROBLEM_MARKER_ID,
              ProjectStructureOrSdkProblemType.JAR_OUTSIDE_WEBINF_LIB,
              entryPath.toPortableString(), javaProject.getProject(),
              entryPath.toOSString());
          isOk = false;
        }
      }
    }

    return isOk;
  }

  private boolean validateOutputDirectory(IJavaProject javaProject)
      throws CoreException {
    IPath expectedOutputDir = WebAppUtilities.getWebInfOut(getProject()).getFolder(
        "classes").getFullPath();

    if (!javaProject.getOutputLocation().equals(expectedOutputDir)) {
      MarkerUtilities.createQuickFixMarker(PROBLEM_MARKER_ID,
          ProjectStructureOrSdkProblemType.BUILD_OUTPUT_DIR_NOT_WEBINF_CLASSES,
          null, getProject(), expectedOutputDir.toString());
      return false;
    }

    return true;
  }

  private boolean validateWarSrcDirectoryExists() throws CoreException {
    IFolder warSrc = WebAppUtilities.getWarSrc(getProject());
    if (!warSrc.exists()) {
      MarkerUtilities.createMarker(PROBLEM_MARKER_ID,
          ProjectStructureOrSdkProblemType.MISSING_WAR_DIR, getProject(),
          warSrc.getFullPath().toString());
      return false;
    }
    return true;
  }

  private boolean validateWebXmlFileExists() throws CoreException {
    IFile webXmlFile = WebAppUtilities.getWebInfSrc(getProject()).getFile(
        "web.xml");

    if (!webXmlFile.exists()) {
      MarkerUtilities.createQuickFixMarker(
          PROBLEM_MARKER_ID,
          ProjectStructureOrSdkProblemType.MISSING_WEB_XML,
          null,
          ResourceUtils.findFirstEnclosingResourceThatExists(webXmlFile.getFullPath()));
      return false;
    }

    return true;
  }
}
