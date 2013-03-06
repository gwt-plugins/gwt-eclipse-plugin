/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract event listener identifies event types of importance to the Managed
 * API capability. The listener is tied to a specific ManagedApiProject (setting
 * the managedApiProject is mandatory). On identification of a specific event,
 * the elementChanged method calls the appropriate abstract method (see below).
 * 
 *  This class is intended to be subclassed and registered using
 * JavaCore.addElementChangedListener(listener). Note: be careful to ensure that
 * the removeElementChangedListener() is called when the listener is no longer
 * needed.
 */
public abstract class ManagedApiChangeListener
    implements IElementChangedListener {

  private ManagedApiProject managedApiProject;

  /**
   * Implements the standard elementChanged method defined by the
   * IElementChangedListener, this method identifies events of importance to a
   * specific ManagedApiProject and calls the appropriate abstract method on a
   * match.
   */
  public void elementChanged(ElementChangedEvent event) {
    if (managedApiProject == null) {
      throw new IllegalStateException(
          "managedApiProject must be set prior to registering the listener");
    }

    for (IJavaElementDelta delta : event.getDelta().getRemovedChildren()) {
      int flags = delta.getFlags();
      if (flags == 0) {
        IJavaElement element = delta.getElement();
        IJavaProject javaProject = (IJavaProject) element.getAdapter(
            IJavaProject.class);
        if (javaProject != null) {
          IProject project = javaProject.getProject();
          if (project.equals(managedApiProject.getJavaProject().getProject())) {
            managedApiProjectRemoved();
          }
        }
      }
    }

    for (IJavaElementDelta delta : event.getDelta().getChangedChildren()) {
      int flags = delta.getFlags();
      if ((flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0) {
        IJavaElement element = delta.getElement();
        if (element.getElementType() == IJavaElement.JAVA_PROJECT
            && element.equals(managedApiProject.getJavaProject())) {
          Set<ManagedApi> managedApiRemovalSet = new HashSet<ManagedApi>(
              delta.getChangedChildren().length);
          List<String> managedApiFolderNames = new ArrayList();
          for (IJavaElementDelta childDelta : delta.getChangedChildren()) {
            if ((childDelta.getFlags()
                & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) {
              IJavaElement childElement = childDelta.getElement();
              if (childElement.getElementType()
                  == IJavaElement.PACKAGE_FRAGMENT_ROOT && managedApiProject
                .isPackageFragmentRootInManagedApi(
                    (IPackageFragmentRoot) childElement)) {
                String managedApiFolderName = managedApiProject
                  .getPathRelativeToManagedApiRoot(
                      (IPackageFragmentRoot) childElement);
                if (!managedApiFolderNames.contains(managedApiFolderName)) {
                  managedApiFolderNames.add(managedApiFolderName);
                  managedApiRemovalSet.add(
                      managedApiProject.createManagedApi(managedApiFolderName));
                }
              }
            }
          }
          if (managedApiRemovalSet.size() > 0) {
            managedApiRemoved(managedApiRemovalSet.toArray(
                new ManagedApiImpl[managedApiRemovalSet.size()]));
          }
        }
      } else if ((flags & IJavaElementDelta.F_CLOSED) != 0) {
        IJavaElement element = delta.getElement();
        if (element.getElementType() == IJavaElement.JAVA_PROJECT
            && element.equals(managedApiProject.getJavaProject())) {
          managedApiProjectClosed();
        }
      }
    }
  }

  /**
   * Override to handle an event representing the close of a ManagedApiProject.
   */
  public abstract void managedApiProjectClosed();

  public abstract void managedApiProjectRemoved();

  /**
   * Override to handle an event representing the removal of some number of
   * ManagedApis from a ManagedApiProject.
   * 
   * @param removedManagedApis an array of ManagedApis being removed in the
   *          current operation.
   */
  public abstract void managedApiRemoved(ManagedApiImpl[] removedManagedApis);

  /**
   * Initialize the listener with a ManagedApiProject.
   * 
   * @param managedApiProject This listener should represent this particular
   *          ManagedApiProject.
   */
  public void setManagedApiProject(ManagedApiProject managedApiProject) {
    this.managedApiProject = managedApiProject;
  }
}
