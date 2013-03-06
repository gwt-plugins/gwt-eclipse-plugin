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
package com.google.gwt.eclipse.core.validators;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.markers.ProjectStructureOrSdkProblemType;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.validators.AbstractProjectValidator;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.runtime.GWTProjectsRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.util.Map;

/**
 * Validator implemented as an IncrementalProjectbuilder which runs over
 * projects with the GWT nature. It validates that the project is using a valid
 * GWT SDK, and that if the project has the Web App nature, that it is using GWT
 * 1.6+.
 * 
 * If any problems are detected, the appropriate resources are decorated with
 * problem markers. Each of these problem markers has a quick fix available for
 * it.
 */
public class GWTProjectValidator extends AbstractProjectValidator {

  private static final String MIN_SDK_VERSION = "2.0.0";

  public static final String BUILDER_ID = GWTPlugin.PLUGIN_ID
      + ".gwtProjectValidator";

  public static final String GWT_SDK_TYPE_NAME = "GWT";

  public static final String PROBLEM_MARKER_ID = GWTPlugin.PLUGIN_ID
      + ".gwtProjectProblemMarker";

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
      return null;
    }

    GWTRuntime sdk = GWTRuntime.findSdkFor(javaProject);

    // If it's not a project that's part of a GWT runtime, it needs a GWT
    // runtime.
    if (!GWTProjectsRuntime.isGWTRuntimeProject(javaProject)) {
      if (!validateSdk(sdk)) {
        return null;
      }

      assert (sdk != null);
    }

    if (WebAppUtilities.isWebApp(getProject()) && !sdk.containsSCL()) {
      // If the project has a web app nature, it must use GWT 1.6+
      MarkerUtilities.createQuickFixMarker(PROBLEM_MARKER_ID,
          ProjectStructureOrSdkProblemType.WAR_WITH_PRE_GWT_16, null,
          getProject());
    }

    return null;
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    cleanImpl();
  }

  @Override
  protected void doAddInvalidSdkMarker(IJavaProject javaProject, Sdk sdk,
      String detailedErrorMessage)
      throws CoreException {
    addInvalidSdkMarker(PROBLEM_MARKER_ID, javaProject, sdk, GWT_SDK_TYPE_NAME,
        detailedErrorMessage);
  }

  @Override
  protected void doAddMissingSdkLibraryMarker(
      IResource closestEnclosingResourceToWebInfLib, File webAppClasspathFile)
      throws CoreException {
    addMissingSdkLibraryMarker(PROBLEM_MARKER_ID, GWT_SDK_TYPE_NAME,
        closestEnclosingResourceToWebInfLib, webAppClasspathFile);
  }

  @Override
  protected void doAddNoSdkMarker(IJavaProject javaProject)
      throws CoreException {
    addNoSdkMarker(PROBLEM_MARKER_ID, javaProject, GWT_SDK_TYPE_NAME);
  }

  @Override
  protected void doAddSdkFileSizeMismatchMarker(File webAppClasspathFile,
      IFile webInfLibFile) throws CoreException {
    addSdkFileSizeMismatchMarker(PROBLEM_MARKER_ID, GWT_SDK_TYPE_NAME,
        webAppClasspathFile, webInfLibFile);
  }

  @Override
  protected String getSupportedSdkVersion() {
    return MIN_SDK_VERSION;
  }

  private void cleanImpl() throws CoreException {
    MarkerUtilities.clearMarkers(PROBLEM_MARKER_ID, getProject());
  }
}
