/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.managedapis.EclipseProject;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures the Proguard-related state for an Android project.
 * 
 * Specifically, any information about Proguard configuration in the
 * <code>project.properties</code> file, and the contents of the Proguard
 * configuration file for the Android Client API Library in the user's project
 * (usually called <code>proguard-google-api-client.txt</code>).
 * 
 * This class is useful for dealing with undo/redo operations.
 */
public class ProguardState {

  static final String PROJECT_PROPERTIES = "project.properties";

  private static final Pattern PROGUARD_CONFIG_PATTERN = Pattern.compile(
      "#??proguard\\.config=.*", Pattern.MULTILINE);

  /**
   * Given a project, capture the current state of the Proguard-related
   * information.
   * 
   * @param currentConfig The Proguard configuration information. Can be null
   *          if no Proguard information exists for the current project.
   * @param proj The project
   * @return The captured Proguard state for the project.
   */
  public static ProguardState createFromCurrent(ProguardConfig currentConfig,
      EclipseProject proj) {

    ProguardState currentState = new ProguardState(currentConfig, proj);
    IFile androidProjPropertiesFile = proj.getProject().getFile(
        PROJECT_PROPERTIES);

    try {
      currentState.androidProjPropertiesContents = ResourceUtils.readFileContents(androidProjPropertiesFile.getLocation());
    } catch (IOException ioe) {
      ManagedApiPlugin.getDefault().getLog().log(
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Could not find project.properties file for Android project.",
              ioe));
      return currentState;
    }

    if (currentConfig == null) {
      // No managed APIs right now...
      return currentState;
    }

    IPath apiProguardConfigFile = currentConfig.computeAbsSystemPathForProguardConfig();

    try {
      currentState.apiProguardConfigContents = ResourceUtils.readFileContents(apiProguardConfigFile);
    } catch (IOException e) {
      // No existing apiary Proguard config file; that's ok, we don't
      // need to log this exception
    }

    return currentState;
  }

  /**
   * Generate the new state of the Proguard-related information for a project
   * based on the current state of the project, and any Proguard configuration
   * information that is going to necessary after Managed APIs are
   * added/removed from the project.
   * 
   * @param oldState The old (current) state of the project's Proguard
   *          configuration. Cannot be null.
   * @param futureConfig The new Proguard configuration information. Can be
   *          null if all Managed APIs are going to be removed.
   * @param proj The project
   * @return The future Proguard state for the project. If
   *         <code>futureConfig</code> is <code>null</code> and the
   *         <code>oldState</code> has Proguard information, it will be
   *         preserved.
   */
  public static ProguardState createForFuture(ProguardState oldState,
      ProguardConfig futureConfig, EclipseProject proj) {
    ProguardState newState = new ProguardState(futureConfig, proj);

    // If all of the managed APIs are being removed in the future..
    if (newState.config == null) {
      newState.androidProjPropertiesContents = oldState.androidProjPropertiesContents;
      return newState;
    }

    newState.androidProjPropertiesContents = generateNewAndroidProjProperties(
        oldState.androidProjPropertiesContents, newState.config);
    try {
      newState.apiProguardConfigContents = ResourceUtils.readFileContents(ResourceUtils.resolveToAbsoluteFileSystemPath(newState.config.getWorkspaceRelPathForApiProguardFile()));
    } catch (IOException e) {
      ManagedApiPlugin.getDefault().getLog().log(
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Could not find API Proguard configuration file.", e));
    }

    return newState;
  }

  private static String generateNewAndroidProjProperties(
      String currentContents, ProguardConfig newState) {

    StringBuffer androidProjProps = new StringBuffer(currentContents);

    if (androidProjProps.length() == 0) {
      return "";
    }

    Matcher m = PROGUARD_CONFIG_PATTERN.matcher(androidProjProps);

    if (!m.find()) {
      return androidProjProps.toString();
    }

    String newProguardConfigLine = generateNewProguardConfigLine(m.group(),
        newState);

    if (newProguardConfigLine == null) {
      // nothing to do
      return androidProjProps.toString();
    }

    androidProjProps.replace(m.start(), m.end(), newProguardConfigLine);

    return androidProjProps.toString();
  }

  private static String generateNewProguardConfigLine(
      String oldProguardConfigLine, ProguardConfig config) {

    StringBuffer buf = new StringBuffer(oldProguardConfigLine);

    assert (buf.length() > 0);

    // need to uncomment the proguard config line
    if (buf.charAt(0) == '#') {
      buf.deleteCharAt(0);
    }

    IPath newApiProguardFilePath = config.computeProjRelativePathForProguardConfig();

    // Don't append the file if it's already there..
    if (buf.indexOf(newApiProguardFilePath.toString()) != -1) {
      return buf.toString();
    }

    // Now, we need to append the path of the proguard config file to the end
    // of the line

    if (buf.charAt(buf.length() - 1) != ':') {
      buf.append(':');
    }

    buf.append(newApiProguardFilePath);

    return buf.toString();
  }

  // Visible for testing
  String androidProjPropertiesContents = null;
  String apiProguardConfigContents = null;

  private ProguardConfig config;
  private EclipseProject project;

  private ProguardState(ProguardConfig config, EclipseProject project) {
    this.config = config;
    this.project = project;
  }

  /**
   * Apply the state to the project.
   * 
   * Any Proguard information captured about the
   * <code>project.properties</code> file will be written.
   * 
   * If no information has been captured about the Proguard Config file for
   * the Android Client Library, then we leave it alone, even if it exists.
   * 
   * Note: This method modifies resources in the Eclipse workspace.
   * 
   * @throws IOException
   * @throws CoreException
   */
  public void apply() throws IOException, CoreException {

    // Deal with Android project.properties file

    if (androidProjPropertiesContents == null) {
      // There should never be a case when we're deleting/removing
      // the contents of the project.properties file. Bail.
      return;
    }

    IFile androidProjPropertiesFile = project.getProject().getFile(
        PROJECT_PROPERTIES);

    ResourceUtils.writeToFile(
        androidProjPropertiesFile.getLocation().toFile(),
        androidProjPropertiesContents);

    // Deal with proguard configuration file
    if (config != null && apiProguardConfigContents != null) {
      IPath proguardAbsFileSystemPath = config.computeAbsSystemPathForProguardConfig();
      // Write the new contents to the proguard config file
      ResourceUtils.createFolderStructure(
          project.getProject(),
          config.computeProjRelativePathForProguardConfig().removeLastSegments(
              1));
      ResourceUtils.writeToFile(proguardAbsFileSystemPath.toFile(),
          apiProguardConfigContents);
    }

    project.getProject().refreshLocal(IResource.DEPTH_INFINITE,
        new NullProgressMonitor());

  }
}