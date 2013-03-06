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

import com.google.gdt.eclipse.managedapis.EclipseProject;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiUtils;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.tools.ant.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import java.io.FileReader;
import java.io.IOException;

/**
 * Information about the Proguard configuration file for the Android API Client
 * Library (usually called <code>proguard-google-api-client.txt</code>), which
 * should be present for a Managed API added to an Android project.
 */
public class ProguardConfig {

  /**
   * Gson Serialization class to read Proguard configuration information from
   * <code>descriptor.json</code file.
   */
  public static class Info {

    private String proguardConfig;

    public String getProguardConfig() {
      return proguardConfig;
    }

    public void setProguardConfig(String proguardConfig) {
      this.proguardConfig = proguardConfig;
    }
  }

  /* Need to use a special naming policy due to the proguard-config field name. */
  private static final Gson GSON_CODEC = new GsonBuilder().setFieldNamingPolicy(
      FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();

  private static final IPath PROJ_REL_PROGUARD_CONFIG_ROOT = Path.EMPTY;

  private static final Info EMPTY_VALUE = new Info();

  private final EclipseProject project;
  private Info proguardInfo = EMPTY_VALUE;
  private IPath descriptorRootFolderWorkspaceRelPath = Path.EMPTY;

  /**
   * Construct a new instance.
   * 
   * @param api A managed API that's part of an Android project.
   * @param eProject The Android project.
   */
  public ProguardConfig(ManagedApi api, EclipseProject eProject) {
    this.project = eProject;
    initProguardInfo(api);
  }

  /**
   * Get the workspace-relative path of the API Proguard configuration file, or
   * <code>null</code> if there isn't one.
   */
  public IPath getWorkspaceRelPathForApiProguardFile() {
    if (!hasProguardConfig()) {
      return null;
    }

    return descriptorRootFolderWorkspaceRelPath.append(proguardInfo.getProguardConfig());
  }

  /**
   * Returns <code>true</code> if the Android project has a Managed API with a
   * Proguard configuration file.
   */
  public boolean hasProguardConfig() {
    String proguardConfig = proguardInfo.getProguardConfig();
    if (proguardConfig != null && proguardConfig.length() > 0) {
      return true;
    }

    return false;
  }

  /**
   * Returns the absolute file system path of the API Proguard configuration
   * file, or <code>null</code> if there isn't one.
   */
  public IPath computeAbsSystemPathForProguardConfig() {
    if (!hasProguardConfig()) {
      return null;
    }

    IPath proguardFileProjRelPath = PROJ_REL_PROGUARD_CONFIG_ROOT.append(proguardInfo.getProguardConfig());
    IPath proguardAbsFileSystemPath = project.getProject().getLocation().append(
        proguardFileProjRelPath);

    return proguardAbsFileSystemPath;
  }

  /**
   * Get the project-relative path of the API Proguard configuration file, or
   * <code>null</code> if there isn't one.
   */
  public IPath computeProjRelativePathForProguardConfig() {
    if (!hasProguardConfig()) {
      return null;
    }

    return PROJ_REL_PROGUARD_CONFIG_ROOT.append(proguardInfo.getProguardConfig());
  }

  private void initProguardInfo(ManagedApi managedApi) {
    try {
      IFile localDescriptorFile = ManagedApiUtils.scanManagedApiFiles(project,
          managedApi.getRootDirectory()).getDescriptor();

      if (localDescriptorFile != null) {
        String localDescriptorContent = FileUtils.readFully(new FileReader(
            localDescriptorFile.getLocation().toFile()));
        descriptorRootFolderWorkspaceRelPath = localDescriptorFile.getParent().getFullPath();
        proguardInfo = GSON_CODEC.fromJson(localDescriptorContent, Info.class);
      }

    } catch (CoreException ce) {
      ManagedApiPlugin.getDefault().getLog().log(
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Problem accessing project", ce));
    } catch (IOException ioe) {
      ManagedApiPlugin.getDefault().getLog().log(
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Problem reading descriptor file", ioe));
    }
  }
}
