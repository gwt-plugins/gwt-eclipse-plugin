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

package com.google.appengine.eclipse.core.properties.ui;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.List;
import java.util.Map;

/**
 * Notifies that Gae project has changed to all plugins implementing
 * gaeProjectChange extension.
 * 
 * TODO(rdayal): We really need to fix this guy. This builder is way to
 * aggressive. We should use a JavaCompilationParticipant and look for changes
 * in @Api-annotated files. Only then should we do anything related to Cloud
 * Endpoint updating. Also, it's not clear why this second builder was
 * introduced (next to GaeProjectValidator) and placed in this plugin (as opposed
 * to the Cloud Endpoints plugin).
 */
public class CloudEndpointsBuilder extends IncrementalProjectBuilder {
  private boolean appEngineWebXmlChanged = false;
  private boolean classFileChanged = false;

  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) {
    IResourceDelta delta = null;
    appEngineWebXmlChanged = false;
    classFileChanged = false;

    if (kind != IncrementalProjectBuilder.FULL_BUILD) {
      delta = getDelta(getProject());
      if (delta == null) {
        return null;
      }
      try {
        delta.accept(new IResourceDeltaVisitor() {
          public boolean visit(IResourceDelta delta) {
            IPath resourcePath = delta.getResource().getRawLocation();
            /*
             * We want to pick up both class file changes, and changes to the
             * appengine-web.xml file (as changes in the app id will affect the
             * endpoint URL that is burned in to the generated client libraries.
             */
            if (resourcePath != null) {
              if (resourcePath.toString().endsWith(".class")) {
                classFileChanged = true;
                return false;
              } else if (resourcePath.toString().endsWith("appengine-web.xml")) {
                appEngineWebXmlChanged = true;
                return false;
              }
            }
            return true;
          }
        });
      } catch (CoreException e) {
        AppEngineCorePluginLog.logError(e);
      }
    } else {
      classFileChanged = true;
      appEngineWebXmlChanged = true;
    }
    if (!classFileChanged && !appEngineWebXmlChanged) {
      return null;
    }
    ExtensionQuery<GaeProjectChangeExtension> extQuery = new ExtensionQuery<GaeProjectChangeExtension>(
        AppEngineCorePlugin.PLUGIN_ID, "gaeProjectChange", "class");
    List<ExtensionQuery.Data<GaeProjectChangeExtension>> contributors = extQuery.getData();
    for (ExtensionQuery.Data<GaeProjectChangeExtension> c : contributors) {
      GaeProjectChangeExtension data = c.getExtensionPointData();
      data.gaeProjectRebuilt(getProject(), appEngineWebXmlChanged);
    }
    return null;
  }
}
