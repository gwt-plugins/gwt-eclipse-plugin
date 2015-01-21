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
import com.google.appengine.eclipse.core.markers.AppEngineProblemType;
import com.google.gdt.eclipse.core.MarkerUtilities;
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
 * Notifies that Gae project has changed to all plugins implementing gaeProjectChange extension.
 * 
 * TODO(rdayal): We really need to fix this guy. This builder is way to aggressive. We should use a
 * JavaCompilationParticipant and look for changes in @Api-annotated files. Only then should we do
 * anything related to Cloud Endpoint updating. Also, it's not clear why this second builder was
 * introduced (next to GaeProjectValidator) and placed in this plugin (as opposed to the Cloud
 * Endpoints plugin).
 */
public class CloudEndpointsBuilder extends IncrementalProjectBuilder {

  // Once this builder is moved into the GaeProjectValidator, we won't need this separate
  // ID for the problem marker.
  public static final String PROBLEM_MARKER_ID = AppEngineCorePlugin.PLUGIN_ID
      + ".endpointsProblemMarker";

  private boolean appEngineWebXmlChanged = false;
  private boolean classFileChanged = false;

  @Override
  // Overrides an Eclipse API method with a raw parameter type
  protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args,
      IProgressMonitor monitor) throws CoreException {
    IResourceDelta delta = null;
    appEngineWebXmlChanged = false;
    classFileChanged = false;

    MarkerUtilities.clearMarkers(PROBLEM_MARKER_ID, getProject());

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
             * We want to pick up both class file changes, and changes to the appengine-web.xml file
             * (as changes in the app id will affect the endpoint URL that is burned in to the
             * generated client libraries.
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
      try {
        data.gaeProjectRebuilt(getProject(), appEngineWebXmlChanged, monitor);
      } catch (Exception e) {
        /*
         * If there were any exceptions that were reported as part of App Engine's API generation
         * process, they are InvocationTargetExceptions that are converted into
         * SwarmGenerationExceptions, which means that e.getMessage() will give us the right
         * message. We should fix the thrown exception types from GaeProjectChangeExcetion and
         * SwarmServiceCreator to clarify this. The tricky part is that the exception types will
         * have to be defined in this plugin, as the endpoints plugin depends on this one.
         */
        MarkerUtilities.createMarker(PROBLEM_MARKER_ID,
            AppEngineProblemType.CLOUD_ENDPOINTS_BUILD_PROBLEM, getProject(), e.getMessage());
      }
    }
    return null;
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    MarkerUtilities.clearMarkers(PROBLEM_MARKER_ID, getProject());
  }
}
