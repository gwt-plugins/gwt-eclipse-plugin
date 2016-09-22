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

import com.google.gdt.eclipse.core.reference.location.IMatchable;
import com.google.gdt.eclipse.core.reference.location.IReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.ReferenceLocationType;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.ui.IMemento;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides reference tracking across arbitrary resources. Clients should add
 * references and become a {@link ReferenceChangeListener}. When a reference's
 * resources are modified, the client will received a callback for each
 * resource.
 * <p>
 * Typical usage is for a separate instance for each domain. For example,
 * UiBinder has its own instance, and other features should not try to piggyback
 * on its instance. This ensures client listeners only receive relevant
 * callbacks for the references they are interested in.
 * <p>
 * This class is fully thread-safe. The reference model (IReference and
 * IReferenceLocation implementations) are also thread-safe.
 */
public class ReferenceManager {

  /**
   * A listener that is called when a referenced resource is modified.
   */
  public interface ReferenceChangeListener {
    void referencedJavaElementChanged(
        Map<IJavaElement, IJavaElementDelta> changedElements);

    /**
     * Called when a referenced resource is modified.
     * <p>
     * Note: This is called during the
     * {@link org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)}
     * callback. Please read its javadoc, the same rules apply here (e.g. do not
     * hold on to the given {@link IResourceDelta}).
     * 
     * @param changedResources a map of changed resources and the corresponding
     *          {@link IResourceDelta}.
     */
    void referencedResourceChanged(
        Map<IResource, IResourceDelta> changedResources);
  }

  public static ReferenceManager load(IMemento memento)
      throws PersistenceException {
    ReferenceManager referenceManager = new ReferenceManager();
    for (IReference reference : ReferenceManagerPersister.load(memento)) {
      referenceManager.addReference(reference);
    }
    return referenceManager;
  }

  private final ElementChangeListener elementChangeListener;

  /**
   * The client listeners to be notified when a resource in a reference changes.
   * <p>
   * See {@link #references} for warnings on thread-safety.
   */
  private final List<ReferenceChangeListener> listeners;

  /**
   * The set of all references being tracked.
   * <p>
   * If using an iterator from this set, synchronize on this instance. Do not
   * let the lock escape from this class (e.g. do not callback clients while
   * holding the lock, do not return this instance, etc.)
   */
  private final Set<IReference> references;

  /**
   * The listener to track changes to resources in reference.
   */
  private final ResourceChangeListener resourceChangeListener;

  public ReferenceManager() {
    this.listeners = Collections.synchronizedList(new ArrayList<ReferenceChangeListener>());
    this.references = Collections.synchronizedSet(new HashSet<IReference>());
    this.resourceChangeListener = new ResourceChangeListener(this);
    this.elementChangeListener = new ElementChangeListener(this);
  }

  public void addReference(IReference reference) {
    references.add(reference);
  }

  public void addReferencedResourceChangeListener(
      ReferenceChangeListener listener) {
    listeners.add(listener);
  }

  public Set<IReference> getReferencesWithMatchingJavaElement(
      IJavaElement element, EnumSet<ReferenceLocationType> locationTypes) {
    return getReferences(element, locationTypes);
  }

  /**
   * Returns a set of references where each one's target reference location
   * matches the given resource.
   * 
   * @param resource the resource to be matched
   * @return a non-null set of matching references
   */
  public Set<IReference> getReferencesWithMatchingResource(IResource resource,
      EnumSet<ReferenceLocationType> locationTypes) {
    return getReferences(resource, locationTypes);
  }

  public void persist(IMemento memento) {
    HashSet<IReference> copiedReferences;

    synchronized (references) {
      copiedReferences = new HashSet<IReference>(references);
    }

    ReferenceManagerPersister.persist(copiedReferences, memento);
  }

  public void removeReference(IReference reference) {
    references.remove(reference);
  }

  public void removeReferencedResourceChangeListener(
      ReferenceChangeListener listener) {
    listeners.remove(listener);
  }

  public void removeReferences(
      Collection<? extends IReference> referencesToRemove) {
    for (IReference reference : referencesToRemove) {
      removeReference(reference);
    }
  }

  /**
   * Removes all references whose source is contained inside the given project.
   */
  public void removeSourceReferences(IProject project) {
    for (IReference reference : getSafeReferences()) {
      if (reference.getSourceProject().equals(project)) {
        removeReference(reference);
      }
    }
  }

  /**
   * Starts the reference manager. This includes listening for resource changes.
   */
  public void start() {
    resourceChangeListener.start();
    elementChangeListener.start();
  }

  /**
   * Stops the reference manager. This includes halting the listening of
   * resource changes.
   */
  public void stop() {
    resourceChangeListener.stop();
    elementChangeListener.stop();
  }

  /**
   * @return a copied list of reference change listeners
   */
  List<ReferenceChangeListener> getReferenceChangeListeners() {
    synchronized (listeners) {
      return new ArrayList<ReferenceChangeListener>(listeners);
    }
  }

  private Set<IReference> getReferences(Object elementToMatch,
      EnumSet<ReferenceLocationType> locationTypes) {
    Set<IReference> matchingReferences = new HashSet<IReference>();
    for (IReference reference : getSafeReferences()) {
      if (reference.getSourceProject().isOpen()) {
        IReferenceLocation targetLocation = reference.getTargetLocation();
        IReferenceLocation sourceLocation = reference.getSourceLocation();

        boolean targetLocationMatches = locationTypes.contains(ReferenceLocationType.TARGET)
            && (targetLocation instanceof IMatchable)
            && ((IMatchable) targetLocation).matches(elementToMatch);

        boolean sourceLocationMatches = locationTypes.contains(ReferenceLocationType.SOURCE)
            && (sourceLocation instanceof IMatchable)
            && ((IMatchable) sourceLocation).matches(elementToMatch);

        if (targetLocationMatches || sourceLocationMatches) {
          matchingReferences.add(reference);
        }
      }
    }
    return matchingReferences;
  }

  private List<IReference> getSafeReferences() {
    synchronized (references) {
      return new ArrayList<IReference>(references);
    }
  }

}
