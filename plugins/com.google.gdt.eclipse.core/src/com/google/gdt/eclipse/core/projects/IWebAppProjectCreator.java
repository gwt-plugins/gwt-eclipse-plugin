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
package com.google.gdt.eclipse.core.projects;

import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;

/**
 * Creates web app projects populated with files relevant to the selected
 * natures.
 * 
 * Add an SDK's nature and container path to enable the project for that SDK.
 */
public interface IWebAppProjectCreator {
  
  /**
   * Interface for instantiators of IWebAppProjectCreator.
   */
  public interface Factory {
    IWebAppProjectCreator create();
  }

  /**
   * Participant during the web app project creation.
   * 
   * This allows more specific plugins to enable specific features on the
   * project, for example gwt.core can enable GWT and add a runtime on the
   * project being created.
   */
  public interface Participant {
    void updateWebAppProjectCreator(IWebAppProjectCreator creator);
  }

  void addContainerPath(IPath containerPath);

  void addFile(IPath path, InputStream inputStream);

  void addFile(IPath path, String content) throws UnsupportedEncodingException;

  void addNature(String natureId);

  /**
   * Creates the project per the current configuration. Note that the caller must have a workspace
   * lock in order to successfully execute this method.
   * 
   * @throws BackingStoreException
   * @throws IOException
   */
  void create(IProgressMonitor monitor) throws CoreException,
      MalformedURLException, SdkException, ClassNotFoundException,
      UnsupportedEncodingException, FileNotFoundException,
      BackingStoreException, IOException;

  /**
   * Set the appId field.
   * 
   * @param appId
   */
  void setAppId(String appId);

  /**
   * Set the isAppsMarketplaceSupported field. If true, support for the Apps Market Place plugin
   * will be added on project creation.
   * 
   * @param isAppsMarketplaceSupported
   */
  void setAppsMarketplaceSupported(boolean isAppsMarketplaceSupported);

  /**
   * Set the isGenerateEmptyProject field.
   * 
   * @param generateEmptyProject
   */
  void setGenerateEmptyProject(boolean generateEmptyProject);

  /**
   * Set the isUseGaeSdkFromDefault field. isUseGaeSdkFromDefault links to the default Google App
   * Engine SDK for the workspace.
   * 
   * @param gaeSdkIsEclipseDefault
   */
  void setIsGaeSdkFromEclipseDefault(boolean gaeSdkIsEclipseDefault);

  /**
   * Sets the isNewAppId flag which is true when a user is creating a new web application with a new
   * App Id and false when the web application is created using an existing App Id.
   * 
   * @param isNewAppId
   */
  void setIsNewAppId(boolean isNewAppId);

  void setLocationURI(URI locationURI);

  void setPackageName(String packageName);
  
  void setProjectName(String projectName);

  void setTemplates(String... templates);

  void setTemplateSources(String... sources);
}
