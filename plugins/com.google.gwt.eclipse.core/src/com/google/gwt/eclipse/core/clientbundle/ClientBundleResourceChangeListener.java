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
package com.google.gwt.eclipse.core.clientbundle;

import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import java.util.Set;

/**
 * Watches for changes to resources which are actual or potential dependencies
 * of ClientBundle subtypes.
 */
public class ClientBundleResourceChangeListener implements
    IResourceChangeListener {

  private static ClientBundleResourceChangeListener INSTANCE;

  /**
   * Registers an instance of this listener with the workspace. Ignored if
   * another listener has already been registered.
   */
  public static synchronized void addToWorkspace() {
    if (INSTANCE == null) {
      INSTANCE = new ClientBundleResourceChangeListener();
      ResourcesPlugin.getWorkspace().addResourceChangeListener(INSTANCE,
          IResourceChangeEvent.POST_CHANGE);
    }
  }

  private static IPackageFragment getContainingPackageFragment(
      IResource resource) {
    IContainer container = resource.getParent();
    if (!(container instanceof IFolder)) {
      return null;
    }

    IJavaElement javaElement = JavaCore.create((IFolder) container);
    if (!(javaElement instanceof IPackageFragment)) {
      return null;
    }

    return (IPackageFragment) javaElement;
  }

  private static boolean isPossibleClientBundleResourceDelta(
      IResourceDelta delta) {
    IResource resource = delta.getResource();

    if (resource.isDerived()) {
      return false;
    }

    if (resource.getType() != IResource.FILE) {
      return false;
    }

    // We only care about additions/deletions. If the contents of a
    // resource file change, it doesn't affect our validation.
    if (delta.getKind() != IResourceDelta.ADDED
        && delta.getKind() != IResourceDelta.REMOVED) {
      return false;
    }

    // Ignore Java source changes, since we're only interested in resource files
    if ("java".equals(resource.getFileExtension())) {
      return false;
    }

    if (!GWTNature.isGWTProject(resource.getProject())) {
      return false;
    }

    IJavaProject javaProject = JavaCore.create(resource.getProject());
    if (!javaProject.isOnClasspath(resource)) {
      return false;
    }

    // All checks passed; it looks like a change we care about
    return true;
  }

  private static boolean shouldVisitResourceDeltaChildren(IResourceDelta delta) {
    return !delta.getResource().isDerived();
  }

  public void resourceChanged(IResourceChangeEvent event) {
    try {
      IResourceDelta delta = event.getDelta();
      if (delta != null) {
        visitResourceDelta(delta);
      }
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
    }
  }

  private void visitResourceDelta(IResourceDelta delta) throws CoreException {
    delta.accept(new IResourceDeltaVisitor() {
      public boolean visit(IResourceDelta delta) throws CoreException {
        if (!isPossibleClientBundleResourceDelta(delta)) {
          return shouldVisitResourceDeltaChildren(delta);
        }

        IResource resource = delta.getResource();
        IPackageFragment pckgFragment = getContainingPackageFragment(resource);
        if (pckgFragment == null) {
          // The package fragment can be null for files inside META-INF
          return true;
        }

        IPath resourceClasspathRelativePath = new Path(
            pckgFragment.getElementName().replace('.', '/')).append(resource.getName());

        Set<ICompilationUnit> dependentCompilationUnits = ClientBundleResourceDependencyIndex.getInstance().findDependentCompilationUnits(
            resourceClasspathRelativePath);

        for (ICompilationUnit cu : dependentCompilationUnits) {
          if (!cu.exists()) {
            // Prune missing compilation units. This might happen, for example,
            // if a compilation unit is renamed. The CU will be added to the
            // index under its new name automatically, but the old entry will
            // stick around until we see it here.
            // TODO: maybe create a rename participant for updating the index?
            ClientBundleResourceDependencyIndex.getInstance().remove(cu);
          }
        }

        // Don't schedule a job to revalidate an empty list of cu's (otherwise
        // we end up spamming the Eclipse job runner).
        if (dependentCompilationUnits.size() > 0) {
          BuilderUtilities.revalidateCompilationUnits(
              dependentCompilationUnits, "Validating ClientBundles");
        }

        return true;
      }
    });
  }

}
