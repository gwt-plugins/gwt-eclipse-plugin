/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.managedapis;

import com.google.api.client.util.Maps;
import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.java.ClasspathChangedListener;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper for hooking managed API.
 * 
 * @deprecated first implementations, doesn't work for multiple APIs, not used any more. Could be
 *             useful to setup "org.eclipse.jst.component.nondependency" attribute.
 *
 * <pre>
 * classpathentry kind="con" path="com.google.gdt.eclipse.managedapis.MANAGED_API_CONTAINER/urlshortener-v1r16lv1.15.0-rc"
 *   attributes
 *     attribute name="org.eclipse.jst.component.nondependency" value=""/
 *   /attributes
 * /classpathentry
 * </pre>
 */
@Deprecated
final class ManagedApiHelper extends ClasspathChangedListener {
  private static final ManagedApiHelper INSTANCE = new ManagedApiHelper();

  /**
   * Adds a class path listener to get notified of changes.
   */
  public static void add() {
    JavaCore.addElementChangedListener(INSTANCE);
  }

  /**
   * Removes class path listener.
   */
  public static void remove() {
    JavaCore.addElementChangedListener(INSTANCE);
  }

  /**
   * Not instantiable.
   */
  private ManagedApiHelper() {
  }

  @Override
  protected void classpathChanged(final IJavaProject javaProject) {
    Job job = new WorkspaceJob("Adding Runtime Path to Managed API Classpath Container") {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        setupRuntimePath(javaProject);
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  private void setupRuntimePath(IJavaProject javaProject) throws CoreException {
    // get all managed containers
    IClasspathEntry[] entries = ClasspathUtilities.findClasspathContainersWithContainerId(
        javaProject, ManagedApiPlugin.API_CONTAINER_PATH_ID);
    Map<IPath, IClasspathEntry> modified = Maps.newHashMap();
    for (IClasspathEntry entry : entries) {
      IPath runtimePath = ClasspathDependencyUtil.getRuntimePath(entry);
      // fix entries without runtime path
      // FIXME(amitin): bad things is that we add runtime path even if user doesn't want it.
      if (runtimePath == null) {
        IVirtualComponent virtualComponent = ComponentCore.createComponent(javaProject.getProject());
        boolean isWebApp = JavaEEProjectUtilities.isDynamicWebProject(javaProject.getProject());
        if (virtualComponent == null) {
          runtimePath = ClasspathDependencyUtil.getDefaultRuntimePath(isWebApp,
              ClasspathDependencyUtil.isClassFolderEntry(entry));
        } else {
          runtimePath = ClasspathDependencyUtil.getDefaultRuntimePath(virtualComponent, entry);
        }
        IClasspathEntry newEntry = ClasspathDependencyUtil.modifyDependencyPath(entry, runtimePath);
        modified.put(entry.getPath(), newEntry);
      }
    }
    // setup classpath only if changed
    if (!modified.isEmpty()) {
      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      List<IClasspathEntry> newClasspath = new ArrayList<IClasspathEntry>();
      for (IClasspathEntry entry : rawClasspath) {
        IPath path = entry.getPath();
        if (modified.containsKey(path)) {
          newClasspath.add(modified.get(path));
        } else {
          newClasspath.add(entry);
        }
      }
      javaProject.setRawClasspath(newClasspath.toArray(new IClasspathEntry[newClasspath.size()]),
          null);
    }
  }
}