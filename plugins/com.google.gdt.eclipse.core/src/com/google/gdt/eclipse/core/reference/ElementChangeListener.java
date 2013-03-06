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

import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.core.java.JavaModelSearch.IJavaElementDeltaVisitor;
import com.google.gdt.eclipse.core.reference.ReferenceManager.ReferenceChangeListener;
import com.google.gdt.eclipse.core.reference.location.ReferenceLocationType;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Listens for changes to Java elements and upon matching references, triggers
 * callbacks to clients of the reference manager.
 */
class ElementChangeListener implements IElementChangedListener {

  private final ReferenceManager referenceManager;

  ElementChangeListener(ReferenceManager referenceManager) {
    this.referenceManager = referenceManager;
  }

  public void elementChanged(ElementChangedEvent event) {
    final Map<IJavaElement, IJavaElementDelta> changedElements = new HashMap<IJavaElement, IJavaElementDelta>();

    JavaModelSearch.visitJavaElementDelta(event.getDelta(),
        new IJavaElementDeltaVisitor() {
          public boolean visit(IJavaElementDelta delta) {
            IJavaElement element = delta.getElement();

            /*
             * We care about packages being only added or removed because if we
             * called the change listeners on a change to a package, any change
             * to any file in that package will cause all ui.xml files that
             * reference that package (say with xmlns urn imports) to be
             * revalidated. Some projects end up having hundreds of ui.xml files
             * referencing a package, and then saving any change in that package
             * ends up taking tens of seconds.
             */
            int type = element.getElementType();
            if (type == IJavaElement.PACKAGE_FRAGMENT
                && delta.getKind() == IJavaElementDelta.CHANGED) {
              return true;
            }

            Set<IReference> references = referenceManager.getReferencesWithMatchingJavaElement(
                element, EnumSet.of(ReferenceLocationType.TARGET));
            if (references != null && references.size() > 0) {
              changedElements.put(element, delta);
            }
            return true;
          }
        });

    if (changedElements.size() > 0) {
      callReferenceChangeListeners(changedElements);
    }
  }

  /**
   * Starts tracking element changes.
   */
  void start() {
    JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);
  }

  /**
   * Stops tracking element changes.
   */
  void stop() {
    JavaCore.removeElementChangedListener(this);
  }

  private void callReferenceChangeListeners(
      Map<IJavaElement, IJavaElementDelta> changedElements) {
    for (ReferenceChangeListener listener : referenceManager.getReferenceChangeListeners()) {
      listener.referencedJavaElementChanged(changedElements);
    }
  }

}
