/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.jpa.libprov;

import com.google.appengine.eclipse.core.sdk.AppEngineBridge;
import com.google.appengine.eclipse.core.sdk.AppEngineBridgeFactory;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponentType;

import java.io.File;
import java.util.List;

/**
 * Classpath container for Datanucleus libs.
 */
public final class DatanucleusClasspathContainer implements IClasspathContainer {
  public static final String CONTAINER_ID = AppEnginePlugin.PLUGIN_ID + ".DATANUCLEUS_CONTAINER";
  public static final Path CONTAINER_PATH = new Path(CONTAINER_ID);

  private IClasspathEntry[] entries;
  private IPath containerPath;
  private IJavaProject project;

  public DatanucleusClasspathContainer(IPath containerPath, IJavaProject project) {
    this.containerPath = containerPath;
    this.project = project;
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    if (entries == null) {
      entries = new IClasspathEntry[0];
      try {
        IFacetedProject facetedProject = ProjectFacetsManager.create(project.getProject());
        IRuntime primaryRuntime = facetedProject.getPrimaryRuntime();
        if (primaryRuntime == null) {
          AppEnginePlugin.logMessage("JPA install: no runtime.");
          return entries;
        }
        IPath sdkPath = getSdkPath(primaryRuntime);
        AppEngineBridge appEngineBridge = AppEngineBridgeFactory.getAppEngineBridge(sdkPath);
        if (appEngineBridge == null) {
          AppEnginePlugin.logMessage("JPA install: no GAE SDK found.");
          return entries;
        }
        List<File> userLibFiles = appEngineBridge.getUserLibFiles("datanucleus", "v2");
        entries = GaeSdk.getClasspathEntries(userLibFiles, sdkPath);
      } catch (Throwable e) {
        AppEnginePlugin.logMessage(e);
        return entries;
      }
    }
    return entries;
  }

  @Override
  public String getDescription() {
    return "Datanucleus Libraries";
  }

  @Override
  public int getKind() {
    return K_APPLICATION;
  }

  @Override
  public IPath getPath() {
    return containerPath;
  }

  /**
   * Searches for {@link GaeSdk} location.
   */
  private IPath getSdkPath(IRuntime primaryRuntime) {
    for (IRuntimeComponent component : primaryRuntime.getRuntimeComponents()) {
      IRuntimeComponentType type = component.getRuntimeComponentType();
      if (GaeRuntime.GAE_RUNTIME_ID.equals(type.getId())) {
        return new Path(component.getProperty("location"));
      }
    }
    return null;
  }
}
