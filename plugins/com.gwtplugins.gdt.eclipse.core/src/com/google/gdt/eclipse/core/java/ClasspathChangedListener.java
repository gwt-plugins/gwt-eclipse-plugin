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
package com.google.gdt.eclipse.core.java;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;

/**
 * An {@link IElementChangedListener} that listens for classpath changes.
 */
public abstract class ClasspathChangedListener implements
    IElementChangedListener {

  public void elementChanged(ElementChangedEvent event) {
    for (IJavaElementDelta delta : event.getDelta().getChangedChildren()) {
      int flags = delta.getFlags();
      if ((flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0) {
        IJavaElement element = delta.getElement();
        if (element.getElementType() != IJavaElement.JAVA_PROJECT) {
          continue;
        }

        classpathChanged((IJavaProject) element);
      }
    }
  }

  protected abstract void classpathChanged(IJavaProject javaProject);
}
