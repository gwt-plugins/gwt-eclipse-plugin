/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gwt.eclipse.core.launch.processors.codeserver;

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.launch.processors.GwtLaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import java.util.List;

/**
 * Handles the -src [src/] arg.
 * 
 * SDM mode only.
 */
public class SdmSrcArgumentProcessor implements ILaunchConfigurationProcessor {

  public static final String SRC_ARG = "-src";

  private static int getArgIndex(List<String> args) {
    return args.indexOf(SRC_ARG);
  }

  /**
   * Only update this argument when in GWT SDM mode.
   */
  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    // only gwt projects use -src
    if (!GWTNature.isGWTProject(javaProject.getProject())) {
      return;
    }

    // only gwt projects with SDM mode
    if (!GwtLaunchConfigurationProcessorUtilities.isSdmMode(launchConfig)) {
      return;
    }

    int srcArgIndex = getArgIndex(programArgs);

    // Prefer the existing value, fallback on the precanned one
    String srcDirs = null;
    if (srcArgIndex >= 0) {
      srcDirs = LaunchConfigurationProcessorUtilities.getArgValue(programArgs,
          srcArgIndex + 1);
    }

    // default directories, is retrieved from classpaths
    if (StringUtilities.isEmpty(srcDirs)) {
      srcDirs = getSrcDirectories(javaProject);
    }

    int insertionIndex = LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
        programArgs, srcArgIndex, true);

    // add the args to the list
    programArgs.add(insertionIndex, SRC_ARG);
    programArgs.add(insertionIndex + 1, srcDirs);
  }

  /**
   * Only update this argument when in GWT SDM mode.
   */
  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    return null;
  }

  /**
   * Get the class path entries that are the source.
   * 
   * @param javaProject the java project.
   * @param entry classpath entry value.
   * @return
   */
  private String getPathIfDir(IJavaProject javaProject, IClasspathEntry entry) {
    IPath p = entry.getPath();

    String projectName = javaProject.getProject().getName();

    String path = null;
    // src directories don't have an output
    // cpe source are src,test directories
    if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE
        && (entry.getOutputLocation() == null || (entry.getOutputLocation() != null && !entry.getOutputLocation().lastSegment().toString().equals(
            "test-classes")))) {
      String dir = p.toString();
      // if the base segment has the project name,
      // lets remove that so its relative to project
      if (dir.contains(projectName)) {
        IPath relative = p.removeFirstSegments(1);
        path = relative.toString();
      }
    }

    return path;
  }

  /**
   * Get a comma delimited list of the source directories relative to project.
   * 
   * @param javaProject The java project.
   * @return csv of the source directories.
   */
  private String getSrcDirectories(IJavaProject javaProject) {
    String src = "";

    IClasspathEntry[] cp = null;
    try {
      cp = javaProject.getRawClasspath();
    } catch (JavaModelException e) {
      e.printStackTrace();
    }

    // TODO maybe do a constant or some better error
    // Something had to go wrong with class path entries
    if (cp == null || cp.length == 0) {
      return "REPLACE_ME_WITH_src_dir";
    }

    for (int i = 0; i < cp.length; i++) {
      String path = getPathIfDir(javaProject, cp[i]);

      if (path != null && src.length() == 0) {
        src = path;
      } else if (path != null && src.length() > 0) {
        src = src + " -src " + path;
      }
    }

    // Something had to go wrong with the classpath entries, no source paths.
    if (src.length() == 0) {
      src = "REPLACE_ME_WITH_src_dir";
    }

    return src;
  }

}
