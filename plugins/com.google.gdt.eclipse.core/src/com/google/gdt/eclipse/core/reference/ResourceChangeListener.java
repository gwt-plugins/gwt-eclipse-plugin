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
package com.google.gdt.eclipse.core.reference;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.core.reference.ReferenceManager.ReferenceChangeListener;
import com.google.gdt.eclipse.core.reference.location.ReferenceLocationType;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Listens for resource changes that affect references in the given reference
 * manager.
 */
class ResourceChangeListener implements IResourceChangeListener {

  private final ReferenceManager referenceManager;

  ResourceChangeListener(ReferenceManager referenceManager) {
    this.referenceManager = referenceManager;
  }

  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getDelta() == null) {
      return;
    }

    final Map<IResource, IResourceDelta> changedResources = new HashMap<IResource, IResourceDelta>();

    try {
      event.getDelta().accept(new IResourceDeltaVisitor() {
        public boolean visit(IResourceDelta delta) throws CoreException {
          IResource resource = delta.getResource();

          try {
            // Only track references within GPE projects
            boolean isRoot = resource.getType() == IResource.ROOT;
            if (!isRoot
                && !ProjectUtilities.isGpeProject(resource.getProject())) {
              return false;
            } else if (isRoot) {
              return true;
            }
          } catch (CoreException e) {
            CorePluginLog.logWarning(e, "Could not get natures on project "
                + resource.getProject().getName());
            return false;
          }
          
          if (!ResourceUtils.isRelevantResourceChange(delta)) {
            return true; // keep exploring the tree
          }
          
          // Reference managers do not track changes to .class files.
          if ("class".equals(resource.getProjectRelativePath().getFileExtension())) {
            return false;
          }          
          
          Set<IReference> references = referenceManager.getReferencesWithMatchingResource(
              resource, EnumSet.of(ReferenceLocationType.TARGET));
          if (references != null && references.size() > 0) {
            changedResources.put(resource, delta);
          }

          return true;
        }
      });
    } catch (CoreException e) {
      CorePluginLog.logError(e,
          "Could not update reference manager after resource change.");
      return;
    }

    if (changedResources.size() > 0) {
      callReferencedResourceChangeListeners(changedResources);
    }
  }

  /**
   * Starts tracking resource changes.
   */
  void start() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
        IResourceChangeEvent.POST_CHANGE);
  }

  /**
   * Stops tracking resource changes.
   */
  void stop() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  private void callReferencedResourceChangeListeners(
      Map<IResource, IResourceDelta> changedResources) {
    for (ReferenceChangeListener listener : referenceManager.getReferenceChangeListeners()) {
      listener.referencedResourceChanged(changedResources);
    }
  }

}
