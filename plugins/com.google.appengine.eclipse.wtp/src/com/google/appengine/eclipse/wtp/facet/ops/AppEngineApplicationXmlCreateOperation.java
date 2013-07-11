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
package com.google.appengine.eclipse.wtp.facet.ops;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.gdt.eclipse.core.DynamicWebProjectUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Adds GAE EAR descriptor file ("appengine-application.xml") upon project creation.
 */
public final class AppEngineApplicationXmlCreateOperation extends GaeFileCreateOperation {

  public AppEngineApplicationXmlCreateOperation(IDataModel model) {
    super(model, new Path("META-INF/appengine-application.xml"));
  }

  @Override
  protected IStatus configureResource() throws ExecutionException {
    IDataModel dataModel = getDataModel();
    IProject project = ProjectUtils.getProject(dataModel);
    // setup app id
    String appId = dataModel.getStringProperty(IGaeFacetConstants.GAE_PROPERTY_APP_ID);
    if (appId != null && appId.trim().length() > 0) {
      try {
        ProjectUtils.setAppId(project, appId, true);
      } catch (CoreException ce) {
        return StatusUtilities.newErrorStatus(ce, AppEnginePlugin.PLUGIN_ID);
      } catch (Throwable e) {
        throw new ExecutionException("Cannot set Application ID.", e);
      }
    }
    return Status.OK_STATUS;
  }

  @Override
  protected void createResource() throws ExecutionException {
    IDataModel dataModel = getDataModel();
    IProject project = ProjectUtils.getProject(dataModel);
    IPath webContentFolder = DynamicWebProjectUtilities.getWebContentFolder(project);
    if (webContentFolder != null) {
      IFile file = project.getFile(webContentFolder.append(filePath));
      // create directories if needed
      IContainer parent = file.getParent();
      if (!parent.exists() && parent.getType() == IResource.FOLDER) {
        try {
          ((IFolder) parent).create(true, true, new NullProgressMonitor());
        } catch (CoreException e) {
          throw new ExecutionException("Cannot create folder: " + parent, e);
        }
      }
    }
    super.createResource();
  }

  @Override
  protected InputStream getResourceContentsAsStream() throws CoreException {
    try {
      // TODO(amitin): move to template.
      String defaultDesc = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n<appengine-application xmlns=\"http://appengine.google.com/ns/1.0\">\n<application></application>\n</appengine-application>";
      return new ByteInputStream(defaultDesc.getBytes("UTF-8"), defaultDesc.length());
    } catch (UnsupportedEncodingException e) {
      throw new CoreException(StatusUtilities.newErrorStatus(e, AppEnginePlugin.PLUGIN_ID));
    }
  }
}
