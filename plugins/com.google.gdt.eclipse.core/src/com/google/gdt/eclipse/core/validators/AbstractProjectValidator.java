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

import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.markers.ProjectStructureOrSdkProblemType;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.text.MessageFormat;

/**
 * Validator that delegates to template methods for reporting errors/warnings
 * associated with SDKs on a project.
 */
public abstract class AbstractProjectValidator extends
    IncrementalProjectBuilder {

  private static String UNSUPPORTED_SDK_MESSAGE = "Version {0} is not supported, must be {1} or later";

  protected static void addInvalidSdkMarker(String problemMarkerID,
      IJavaProject javaProject, Sdk sdk, String sdkTypeName,
      String detailedErrorMessage)
      throws CoreException {
    MarkerUtilities.createQuickFixMarker(problemMarkerID,
        ProjectStructureOrSdkProblemType.INVALID_SDK, null,
        javaProject.getProject(), sdkTypeName, sdk.getName(),
        detailedErrorMessage);
  }

  protected static void addMissingSdkLibraryMarker(String problemMarkerID,
      String sdkTypeName, IResource closestEnclosingResourceToWebInfLib,
      File webAppClasspathFile) throws CoreException {
    MarkerUtilities.createQuickFixMarker(
        problemMarkerID,
        ProjectStructureOrSdkProblemType.SDK_LIBRARY_MISSING_FROM_WEBINF_LIB,
        Path.fromOSString(webAppClasspathFile.getAbsolutePath()).toPortableString(),
        closestEnclosingResourceToWebInfLib, sdkTypeName,
        webAppClasspathFile.getName());
  }

  protected static void addNoSdkMarker(String problemMarkerID,
      IJavaProject javaProject, String sdkTypeName) throws CoreException {
    MarkerUtilities.createQuickFixMarker(problemMarkerID,
        ProjectStructureOrSdkProblemType.NO_SDK, sdkTypeName,
        javaProject.getProject(), javaProject.getElementName(), sdkTypeName);
  }

  protected static void addSdkFileSizeMismatchMarker(String problemMarkerID,
      String sdkTypeName, File webAppClasspathFile, IFile webInfLibFile)
      throws CoreException {
    MarkerUtilities.createQuickFixMarker(problemMarkerID,
        ProjectStructureOrSdkProblemType.SDK_FILE_SIZE_MISMATCH, null,
        webInfLibFile, webInfLibFile.getProjectRelativePath().toOSString(),
        sdkTypeName, webAppClasspathFile.getName());
  }

  protected abstract void doAddInvalidSdkMarker(IJavaProject javaProject,
      Sdk sdk, String detailedErrorMessage) throws CoreException;

  protected abstract void doAddMissingSdkLibraryMarker(
      IResource closestEnclosingResourceToWebInfLib, File webAppClasspathFile)
      throws CoreException;

  protected abstract void doAddNoSdkMarker(IJavaProject javaProject)
      throws CoreException;

  protected abstract void doAddSdkFileSizeMismatchMarker(
      File webAppClasspathFile, IFile webInfLibFile) throws CoreException;

  /**
   * Returns the minimum supported SDK version, or null if all versions are
   * supported.
   */
  protected abstract String getSupportedSdkVersion();

  /**
   * Given a Java project, validates that it has a classpath container with the
   * given ID on its build path, and the SDK that the container corresponds to
   * exists, is valid and supported.
   * 
   * If any problems are detected with finding or validating the SDK, error
   * markers with the given ID are created on the Java project's corresponding
   * IProject, and a value of false will be returned.
   * 
   * If the project has the Web Application Nature, then warning markers with
   * the given ID will be generated for each of the SDK's server classpath
   * libraries which are inconsistent with the contents of WEB-INF/lib. Even if
   * there are inconsistencies, a value of true will still be returned by this
   * method.
   * 
   * TODO: Ensure that only one SDK with the given containerID exists on the
   * project's build path; right now, this method looks for the first classpath
   * container entry that matches the containerID *
   * 
   * @return whether the classpath container refers to an existing and valid
   *         SDK.
   * @throws CoreException
   */
  protected boolean validateSdk(Sdk sdk) throws CoreException {

    IJavaProject javaProject = JavaCore.create(getProject());
    if (sdk == null) {
      doAddNoSdkMarker(javaProject);
      return false;
    } else {
      IStatus validationStatus = sdk.validate();
      if (!validationStatus.isOK()) {
        doAddInvalidSdkMarker(javaProject, sdk, validationStatus.getMessage());
        return false;
      }

      // Validate the SDK version.
      String supportedSdkVersion = getSupportedSdkVersion();
      if (supportedSdkVersion != null) {
        String sdkVersion = sdk.getVersion();
        if (!SdkUtils.isInternal(sdkVersion)
            && SdkUtils.compareVersionStrings(sdkVersion, supportedSdkVersion) < 0) {

          doAddInvalidSdkMarker(javaProject, sdk, MessageFormat.format(
              UNSUPPORTED_SDK_MESSAGE, sdkVersion, supportedSdkVersion));
          return false;
        }
      }
    }

    if (!WebAppUtilities.hasManagedWarOut(getProject())) {
      return true;
    }

    IFolder webInfLibFolder = WebAppUtilities.getWebInfLib(getProject());

    /*
     * Store a reference to the closest enclosing resource to the WEB-INF/lib
     * IFolder (which may be the WEB-INF/lib IFolder itself). We need to to do
     * this so that we can put an error marker on the appropriate resource in
     * the event that the WEB-INF/lib folder does not exist.
     */
    IResource closestEnclosingResourceToWebInfLib = null;
    if (!webInfLibFolder.exists()) {
      closestEnclosingResourceToWebInfLib = ResourceUtils.findFirstEnclosingResourceThatExists(webInfLibFolder.getFullPath());
    } else {
      closestEnclosingResourceToWebInfLib = webInfLibFolder;
    }

    for (File webAppClasspathFile : sdk.getWebAppClasspathFiles(getProject())) {

      IFile webInfLibFile = null;

      if (webInfLibFolder.exists()) {
        webInfLibFile = webInfLibFolder.getFile(webAppClasspathFile.getName());
      }

      if (webInfLibFile == null || !webInfLibFile.exists()) {
        doAddMissingSdkLibraryMarker(closestEnclosingResourceToWebInfLib,
            webAppClasspathFile);
      } else if (webAppClasspathFile.length() != webInfLibFile.getLocation().toFile().length()) {
        doAddSdkFileSizeMismatchMarker(webAppClasspathFile, webInfLibFile);
      }
    }

    /*
     * Even if an SDK file is missing from WEB-INF/lib, return true, because we
     * only flag this as a warning.
     */
    return true;
  }
}

