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

import com.google.appengine.eclipse.core.resources.GaeProjectResources;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import java.io.InputStream;

/**
 * Adds GAE descriptor file ("appengine-web.xml") upon project creation.
 */
public final class AppEngineXmlCreateOperation extends GaeFileCreateOperation {

  public AppEngineXmlCreateOperation(IDataModel model) {
    super(model, new Path("WEB-INF/appengine-web.xml"));
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
        throw new ExecutionException("Cannot set App ID.", e);
      }
    }
    // setup version
    String appVersion = dataModel.getStringProperty(IGaeFacetConstants.GAE_PROPERTY_APP_VERSION);
    if (appVersion != null && appVersion.trim().length() > 0) {
      try {
        ProjectUtils.setAppVersion(project, appVersion, true);
      } catch (CoreException ce) {
        return StatusUtilities.newErrorStatus(ce, AppEnginePlugin.PLUGIN_ID);
      } catch (Throwable e) {
        throw new ExecutionException("Cannot set App Version.", e);
      }
    }
    return Status.OK_STATUS;
  }

  @Override
  protected InputStream getResourceContentsAsStream() throws CoreException {
    // TODO: use GWT? Possibly get GWT boolean from dataModel (provided by GWT facet)?
    return GaeProjectResources.createAppEngineWebXmlSource(false);
  }
}
