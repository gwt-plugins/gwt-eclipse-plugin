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
package com.google.appengine.eclipse.core.sdk;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.ReflectionException;

/**
 * Updates the managed WAR output directory's WEB-INF/lib directory so it
 * contains the correct jars from the App Engine SDK.
 */
public class AppEngineUpdateWebInfFolderCommand extends
    UpdateWebInfFolderCommand {

  private static List<String> toFileNames(List<File> files) {
    List<String> fileNames = new ArrayList<String>();
    for (File file : files) {
      fileNames.add(file.getName());
    }
    return fileNames;
  }

  public AppEngineUpdateWebInfFolderCommand(IJavaProject javaProject, Sdk sdk) {
    super(javaProject, sdk);
  }

  @Override
  protected List<String> computeWebInfLibFilesToRemove() throws CoreException {
    IClasspathEntry[] classpathEntries = getJavaProject().getRawClasspath();
    IClasspathEntry classpathContainer = ClasspathUtilities.findClasspathEntryContainer(
        classpathEntries, GaeSdkContainer.CONTAINER_ID);
    if (classpathContainer != null) {
      // We were using containers, the assume memoized version is legit
      return GaeProjectProperties.getFileNamesCopiedToWebInfLib(getJavaProject().getProject());
    }

    // Assume that we are using raw classpaths
    IPath previousSdkInstallationPath = AppEngineUpdateProjectSdkCommand.computeAppEngineSdkInstallPath(getJavaProject());
    if (previousSdkInstallationPath == null) {
      return Collections.emptyList();
    }

    // Return a list of all possible files that the SDK could have added to WEB-INF.
    AppEngineBridge appEngineBridge = AppEngineBridgeFactory.getAppEngineBridge(previousSdkInstallationPath);
    if (appEngineBridge != null) {
      List<File> toReturn = new ArrayList<File>();
      toReturn.addAll(appEngineBridge.getUserLibFiles());
      if (sdk instanceof GaeSdk
          && (((GaeSdk) sdk).getCapabilities().contains(GaeSdkCapability.OPTIONAL_USER_LIB))) {
        try {
          toReturn.addAll(appEngineBridge.getLatestUserLibFiles(false));
          for (String version : appEngineBridge.getUserLibVersions("datanucleus")) {
            List<File> userLibFiles = appEngineBridge.getUserLibFiles("datanuclues", version);
            if (userLibFiles != null) {
              toReturn.addAll(userLibFiles);
            }
          }
          return toFileNames(toReturn);
        } catch (ReflectionException e) {
          AppEngineCorePluginLog.logError(e.getTargetException(), e.getLocalizedMessage());
        }
      }
      return toFileNames(appEngineBridge.getUserLibFiles());
    }

    return Collections.emptyList();
  }

  @Override
  protected void saveFilesCopiedToWebInfLib(List<File> webInfLibFiles)
      throws BackingStoreException {
    GaeProjectProperties.setFilesCopiedToWebInfLib(
        getJavaProject().getProject(), webInfLibFiles);
  }

}
