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
package com.google.gdt.eclipse.core;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchyChangedListener;
import org.eclipse.jdt.core.JavaModelException;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches full class hierarchies (super and subtypes) so they don't have to be
 * recomputed each time they're needed. The cached hierarchies are automatically
 * kept in-sync with changes to a type's super or subtype hierarchy.
 * 
 * TODO: Implement a cache sweep mechanism if we store "heavier" objects in the
 * cache and/or if a lot more clients use the cache.
 */
public class TypeHierarchyCache {

  private class TypeHierarchyUpdater implements ITypeHierarchyChangedListener {
    public void typeHierarchyChanged(ITypeHierarchy typeHierarchy) {
      try {
        IType type = typeHierarchy.getType();
        if (type.exists()) {
          typeHierarchy.refresh(null);
        } else {
          synchronized (hierarchies) {
            // Prune non-existent types from the cache
            hierarchies.remove(type);
          }
        }
      } catch (JavaModelException e) {
        CorePluginLog.logError(e, "Could not refresh the type hierarchy of "
            + typeHierarchy.getType().getElementName());
      }
    }
  }

  private final ITypeHierarchyChangedListener hierarchyUpdater = new TypeHierarchyUpdater();

  private Map<IType, ITypeHierarchy> hierarchies = new HashMap<IType, ITypeHierarchy>();

  public synchronized ITypeHierarchy getHierarchy(IType type)
      throws JavaModelException {
    // Return from the cache if available
    if (hierarchies.containsKey(type)) {
      return hierarchies.get(type);
    }
    // Create an auto-updating type hierarchy and cache it
    ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
    hierarchy.addTypeHierarchyChangedListener(hierarchyUpdater);
    hierarchies.put(type, hierarchy);

    return hierarchy;
  }
}
