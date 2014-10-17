/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.classpath;import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.wtp.GwtPlugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

/**
 * A container to store GWT libs which should be published into Web app lib folder.
 */
public final class GwtWtpClasspathContainer implements IClasspathContainer {

  public static final String CONTAINER_ID = GwtPlugin.PLUGIN_ID + ".GWT_WTP_CONTAINER";
  public static final IPath CONTAINER_PATH = new Path(CONTAINER_ID);

  private final IPath containerPath;
  private IClasspathEntry[] entries;
  private final IJavaProject project;

  public GwtWtpClasspathContainer(IPath containerPath, IJavaProject project) {
    this.containerPath = containerPath;
    this.project = project;
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    if (entries == null) {
      entries = new IClasspathEntry[0];
      try {
//        IPath sdkPath = ProjectUtils.getGaeSdkLocation(project.getProject());
//        if (sdkPath == null) {
//          GwtPlugin.logMessage("No SDK found.");
//          return entries;
//        }

//        AppEngineBridge appEngineBridge = AppEngineBridgeFactory.getAppEngineBridge(sdkPath);
//        if (appEngineBridge == null) {
//          AppEnginePlugin.logMessage("Cannot create App Engine Bridge.");
//          return entries;
//        }
        //List<File> userLibFiles = appEngineBridge.getLatestUserLibFiles(false);

        entries = GWTRuntime.findSdkFor(project).getClasspathEntries();
      } catch (Throwable e) {
        GwtPlugin.logMessage(e);
        return entries;
      }
    }
    return entries;
  }

  @Override
  public String getDescription() {
    return "GWT SDK library";
  }

  @Override
  public int getKind() {
    return K_APPLICATION;
  }

  @Override
  public IPath getPath() {
    return containerPath;
  }
}
