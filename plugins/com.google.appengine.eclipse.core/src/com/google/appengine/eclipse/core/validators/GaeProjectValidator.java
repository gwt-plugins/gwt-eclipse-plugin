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
package com.google.appengine.eclipse.core.validators;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.markers.AppEngineProblemType;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.core.ProcessUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.validators.AbstractProjectValidator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall3;
import org.eclipse.jdt.launching.JavaRuntime;

import java.io.File;
import java.util.Map;

/**
 * Validator implemented as an IncrementalProjectbuilder which runs over
 * projects with the App Engine nature. It validates that the project is using a
 * valid App Engine SDK, and that the project has a
 * <WAR>/WEB-INF/appengine-web.xml file.
 * 
 * If any problems are detected, the appropriate resources are decorated with
 * problem markers. Each of these problem markers has a quick fix available for
 * it.
 */
public class GaeProjectValidator extends AbstractProjectValidator {

  private interface JSPResourceVisitor extends IResourceVisitor {
    boolean wasJspFound();
  }

  public static final String APP_ENGINE_SDK_TYPE_NAME = "App Engine";

  public static final String BUILDER_ID = AppEngineCorePlugin.PLUGIN_ID
      + ".projectValidator";

  public static final String PROBLEM_MARKER_ID = AppEngineCorePlugin.PLUGIN_ID
      + ".problemMarker";

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

    GaeSdk sdk = GaeSdk.findSdkFor(javaProject);
    if (!validateSdk(sdk)) {
      return null;
    }

    if (WebAppUtilities.isWebApp(getProject())) {

      if (!validateAppEngineWebXmlFileExists()) {
        return null;
      }

      IFolder warFolder = WebAppUtilities.getWarSrc(getProject());

      /*
       * If we've reached this point, we know that the
       * <WAR>/WEB-INF/appengine-web.xml file exists, which means that the war
       * folder must also exist.
       */
      assert (warFolder.exists());

      if (!validateUsingJDKifUsingJSPs(warFolder, javaProject)) {
        return null;
      }

      if (!validateUsingJava16OrNewer(javaProject)) {
        return null;
      }
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
    addInvalidSdkMarker(PROBLEM_MARKER_ID, javaProject, sdk,
        APP_ENGINE_SDK_TYPE_NAME, detailedErrorMessage);
  }

  @Override
  protected void doAddMissingSdkLibraryMarker(
      IResource closestEnclosingResourceToWebInfLib, File webAppClasspathFile)
      throws CoreException {
    addMissingSdkLibraryMarker(PROBLEM_MARKER_ID, APP_ENGINE_SDK_TYPE_NAME,
        closestEnclosingResourceToWebInfLib, webAppClasspathFile);
  }

  @Override
  protected void doAddNoSdkMarker(IJavaProject javaProject)
      throws CoreException {
    addNoSdkMarker(PROBLEM_MARKER_ID, javaProject, APP_ENGINE_SDK_TYPE_NAME);
  }

  @Override
  protected void doAddSdkFileSizeMismatchMarker(File webAppClasspathFile,
      IFile webInfLibFile) throws CoreException {
    addSdkFileSizeMismatchMarker(PROBLEM_MARKER_ID, APP_ENGINE_SDK_TYPE_NAME,
        webAppClasspathFile, webInfLibFile);
  }

  @Override
  protected String getSupportedSdkVersion() {
    // all GAE SDKs are supported.
    return null;
  }

  private void cleanImpl() throws CoreException {
    MarkerUtilities.clearMarkers(PROBLEM_MARKER_ID, getProject());
  }

  private boolean validateAppEngineWebXmlFileExists() throws CoreException {
    IFile appEngineWebXml = WebAppUtilities.getWebInfSrc(getProject()).getFile(
        "appengine-web.xml");

    if (!appEngineWebXml.exists()) {
      MarkerUtilities.createQuickFixMarker(
          PROBLEM_MARKER_ID,
          AppEngineProblemType.MISSING_APPENGINE_WEB_XML,
          null,
          ResourceUtils.findFirstEnclosingResourceThatExists(appEngineWebXml.getFullPath()));
      return false;
    }

    return true;
  }

  @SuppressWarnings("unchecked")
  private boolean validateUsingJava16OrNewer(IJavaProject javaProject)
      throws CoreException {
    IVMInstall3 vm = (IVMInstall3) JavaRuntime.getVMInstall(javaProject);
    if (vm == null) {
      // No VM is configured; wait until one is configured before warning user
      return true;
    }

    // This property maps to the JRE version, which is what App Engine checks
    // against
    String vmVersionSysProp = "java.specification.version";
    Map<String, String> sysPropValues = vm.evaluateSystemProperties(
        new String[] {vmVersionSysProp},
        null);
    String vmVersion = sysPropValues.get(vmVersionSysProp);
    if (StringUtilities.isEmpty(vmVersion)) {
      AppEngineCorePluginLog.logWarning("Checking JRE version but the system property is empty.");
      // Assume it passes
      return true;
    }
    
    String[] vmVersionComponents = vmVersion.split("\\.");
    if (vmVersionComponents.length >= 2) {
      if (vmVersionComponents[0].equals("1")) {
        try {
          int minorVersion = Integer.parseInt(vmVersionComponents[1]);
          if (minorVersion < 6) {
            MarkerUtilities.createQuickFixMarker(
                PROBLEM_MARKER_ID,
                AppEngineProblemType.JAVA15_DEPRECATED,
                null,
                javaProject.getProject());
            return false;
          }
        } catch (NumberFormatException e) {
          AppEngineCorePluginLog.logWarning(e, "Unexpected JRE version.");
        }
      }
    }

    return true;
  }

  private boolean validateUsingJDKifUsingJSPs(IFolder warFolder,
      IJavaProject javaProject) throws CoreException {

    boolean isProjectUsingJdk = ProcessUtilities.isUsingJDK(javaProject);

    if (isProjectUsingJdk) {
      // If the project is using a JDK, there is no point in doing
      // further checking - we know that JSPs in the project are not
      // a problem if it is using a JDK.
      return true;
    }

    JSPResourceVisitor jspResourcevisitor = new JSPResourceVisitor() {

      private boolean wasJSPFound = false;

      public boolean visit(IResource resource) throws CoreException {
        if (resource.getType() == IResource.FILE) {
          IFile file = (IFile) resource;
          if ("JSP".equalsIgnoreCase(file.getFileExtension())) {
            wasJSPFound = true;
            MarkerUtilities.createQuickFixMarker(PROBLEM_MARKER_ID,
                AppEngineProblemType.JSP_WITHOUT_JDK, null, resource);
          }
        }
        return true;
      }

      public boolean wasJspFound() {
        return wasJSPFound;
      }
    };

    warFolder.accept(jspResourcevisitor);

    return jspResourcevisitor.wasJspFound();
  }
}
