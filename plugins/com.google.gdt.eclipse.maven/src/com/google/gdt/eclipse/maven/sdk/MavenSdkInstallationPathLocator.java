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
package com.google.gdt.eclipse.maven.sdk;

import com.google.appengine.eclipse.core.sdk.GaeSdk.ISdkPath;
import com.google.appengine.eclipse.core.sdk.GaeSdk.ProjectBoundSdk;
import com.google.gdt.eclipse.maven.MavenUtils;
import com.google.gdt.eclipse.maven.MavenUtils.MavenInfo;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;

/**
 * Used to locate a full GAE SDK in the user's local Maven repository, based on
 * the path to a GAE jar.
 */
public class MavenSdkInstallationPathLocator implements ISdkPath {

  private static final String MAVEN_APP_ENGINE_GROUP_ID = "com.google.appengine";
  private static final String MAVEN_APP_ENGINE_SDK_ARTIFACT_ID = "appengine-java-sdk";

  private static IPath getExpandedAppEngineSdkPathInMavenRepo(
      IPath appEngineLibPath) {
    if (appEngineLibPath == null) {
      return null;
    }

    MavenInfo appEngineLibMavenInfo = MavenInfo.create(appEngineLibPath,
        MAVEN_APP_ENGINE_GROUP_ID);

    if (appEngineLibMavenInfo == null) {
      return null;
    }

    IPath expandedMavenSdkPath = appEngineLibMavenInfo.getRepositoryPath();
    expandedMavenSdkPath = expandedMavenSdkPath.append(MAVEN_APP_ENGINE_GROUP_ID.replace(
        '.', '/'));
    expandedMavenSdkPath = expandedMavenSdkPath.append(MAVEN_APP_ENGINE_SDK_ARTIFACT_ID);
    expandedMavenSdkPath = expandedMavenSdkPath.append(appEngineLibMavenInfo.getVersion());
    expandedMavenSdkPath = expandedMavenSdkPath.append(MAVEN_APP_ENGINE_SDK_ARTIFACT_ID
        + '-' + appEngineLibMavenInfo.getVersion());
    expandedMavenSdkPath = expandedMavenSdkPath.addTrailingSeparator();

    return expandedMavenSdkPath;
  }

  public IPath getSdkInstallationPath(IProject project) {
    if (!MavenUtils.hasMavenNature(project)) {
      return null;
    }

    IPath pathToNonOrmLibInPartialSdk =
        new ProjectBoundSdk(JavaCore.create(project)).getInstallationPath();
    IPath expandedMavenSdkPath =
        getExpandedAppEngineSdkPathInMavenRepo(pathToNonOrmLibInPartialSdk);

    if (expandedMavenSdkPath == null) {
      return null;
    }

    File expandedMavenSdkDir = expandedMavenSdkPath.toFile();
    if (!expandedMavenSdkDir.exists() || !expandedMavenSdkDir.isDirectory()) {
      return null;
    }

    return expandedMavenSdkPath;
  }
}
