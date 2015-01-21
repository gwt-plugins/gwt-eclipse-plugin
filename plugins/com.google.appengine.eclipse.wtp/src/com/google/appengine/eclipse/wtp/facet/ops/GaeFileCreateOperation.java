/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.facet.ops;

import com.google.appengine.eclipse.wtp.utils.IOUtils;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.common.io.Files;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import java.io.InputStream;

/**
 * Create file resource operation.
 */
public class GaeFileCreateOperation extends GaeResourceCreateOperation {

  protected IPath filePath;

  public GaeFileCreateOperation(IDataModel model, IPath path) {
    super(model);
    filePath = path;
  }

  @Override
  protected void createResource() throws ExecutionException {
    IFile file = (IFile) resource;
    InputStream is = null;
    try {
      is = getResourceContentsAsStream();
      if (is != null) {
        // Protect against an imported project missing an expected directory:
        Files.createParentDirs(file.getLocation().toFile());
        file.refreshLocal(IResource.DEPTH_ONE, null);

        file.create(is, false, null);
      }
    } catch (Throwable e) {
      throw new ExecutionException("Cannot create resource: " + resource, e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  @Override
  protected IResource getResource() {
    IProject project = ProjectUtils.getProject(getDataModel());
    IVirtualComponent comp = ComponentCore.createComponent(project);
    IPath projectPath = comp.getRootFolder().getUnderlyingFolder().getProjectRelativePath().append(
        filePath);
    return comp.getProject().getFile(projectPath);
  }

  /**
   * @return the resource contents as {@link InputStream}.
   */
  protected InputStream getResourceContentsAsStream() throws CoreException {
    IFile file = (IFile) resource;
    return file.getContents();
  }
}
