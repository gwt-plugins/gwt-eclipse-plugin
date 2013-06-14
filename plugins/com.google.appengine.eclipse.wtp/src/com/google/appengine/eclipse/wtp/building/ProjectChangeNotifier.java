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
package com.google.appengine.eclipse.wtp.building;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.gdt.eclipse.core.DynamicWebProjectUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.j2ee.internal.J2EEConstants;

import java.io.File;
import java.util.Map;

/**
 * A builder providing touch to 'appengine-web.xml' in order GAE dev server to apply changes made in
 * java files.
 * 
 * Note: this is obsolete and it is not used for SDK versions >= 1.8.1.
 */
@SuppressWarnings("restriction")
public final class ProjectChangeNotifier extends IncrementalProjectBuilder {

  private boolean classFileChanged;
  public static final String BUILDER_ID = AppEnginePlugin.PLUGIN_ID
      + ".gaeWtpProjectChangeNotifier";

  /**
   * Default ctor
   */
  public ProjectChangeNotifier() {
  }

  @Override
  protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
      throws CoreException {
    if (kind != IncrementalProjectBuilder.FULL_BUILD) {
      IResourceDelta delta = getDelta(getProject());
      if (delta == null) {
        return null;
      }
      try {
        delta.accept(new IResourceDeltaVisitor() {
          @Override
          public boolean visit(IResourceDelta delta) {
            IPath resourcePath = delta.getResource().getRawLocation();
            if (resourcePath != null && resourcePath.toString().endsWith(".class")) {
              classFileChanged = true;
              return false;
            }
            return true;
          }
        });
      } catch (CoreException e) {
        AppEnginePlugin.logMessage(e);
      }
    } else {
      classFileChanged = true;
    }
    if (classFileChanged) {
      classFileChanged = false;
      touchAppengineFile();
    }
    return null;
  }

  /**
   * Touches 'appengine-web.xml' if it exists.
   */
  private void touchAppengineFile() {
    IProject project = getProject();
    IPath webContentFolder = DynamicWebProjectUtilities.getWebContentFolder(project);
    if (webContentFolder != null) {
      final IFile file = project.getFile(webContentFolder.append(J2EEConstants.WEB_INF).append(
          "appengine-web.xml"));
      Job job = new Job("Touching appengine-web.xml") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            if (file.exists()) {
              // IFile.touch() doesn't actually touch file in the file system.
              File javaioFile = file.getRawLocation().toFile();
              javaioFile.setLastModified(System.currentTimeMillis());
              file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            }
          } catch (Throwable e) {
            AppEnginePlugin.logMessage(e);
          }
          return Status.OK_STATUS;
        }
      };
      job.schedule();
    }
  }
}
