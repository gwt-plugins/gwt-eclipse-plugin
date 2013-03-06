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
package com.google.gdt.eclipse.suite.launch.processors;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.properties.GoogleCloudSqlProperties;
import com.google.appengine.eclipse.core.sdk.AppEngineBridge;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.common.collect.Sets;
import com.google.gwt.eclipse.core.launch.ModuleClasspathProvider;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import java.util.Arrays;
import java.util.Set;

/**
 * Generates the runtime classpath based Appengine project properties.
 */
public class AppengineClasspathProvider extends ModuleClasspathProvider {

  /**
   * Provides the id of a classpath provider (for the
   * org.eclipse.jdt.launching.classpathProvider extension point).
   */
  public static class AppengineClasspathProviderIdProvider implements
      ModuleClasspathProvider.IModuleClasspathProviderIdProvider {

    public String getProviderId(IProject project) {
      if (!GaeNature.isGaeProject(project)) {
        return null;
      }
      return "com.google.gdt.eclipse.suite.appengineClasspathProvider";
    }
  }

  @Override
  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(
      ILaunchConfiguration config) throws CoreException {
    IRuntimeClasspathEntry[] unresolvedClasspathEntries = super.computeUnresolvedClasspath(config);
    IJavaProject project = JavaRuntime.getJavaProject(config);
    if (project == null || !project.getProject().hasNature(GaeNature.NATURE_ID)) {
      return unresolvedClasspathEntries;
    }
    boolean useDefault = config.getAttribute(
        IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
    if (!useDefault) {
      return unresolvedClasspathEntries;
    }

    if (!GaeNature.isGaeProject(project.getProject())) {
      return unresolvedClasspathEntries;
    }

    if (!GoogleCloudSqlProperties.getGoogleCloudSqlEnabled(project.getProject())) {
      return unresolvedClasspathEntries;
    }

    IPath jdbcDriverPath = Path.fromOSString(GaeSdk.findSdkFor(project).getInstallationPath()
        + AppEngineBridge.APPENGINE_CLOUD_SQL_JAR_PATH_IN_SDK
        + AppEngineBridge.APPENGINE_CLOUD_SQL_JAR);
    
    if (!jdbcDriverPath.isAbsolute()) {
      return unresolvedClasspathEntries;
    }
    IRuntimeClasspathEntry jdbcJar = JavaRuntime.newArchiveRuntimeClasspathEntry(jdbcDriverPath);
    Set<IRuntimeClasspathEntry> updatedUnresolvedClasspathEntries = Sets.newLinkedHashSet(Arrays.asList(unresolvedClasspathEntries));
    updatedUnresolvedClasspathEntries.add(jdbcJar);
    return updatedUnresolvedClasspathEntries.toArray(new IRuntimeClasspathEntry[updatedUnresolvedClasspathEntries.size()]);
  }
}