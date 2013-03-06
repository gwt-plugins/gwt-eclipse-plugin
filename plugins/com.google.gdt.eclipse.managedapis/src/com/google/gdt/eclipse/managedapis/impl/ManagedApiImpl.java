/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.managedapis.EclipseProject;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiUtils;
import com.google.gdt.googleapi.core.ApiDirectoryListingJsonCodec;
import com.google.gdt.googleapi.core.ApiInfo;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.joda.time.LocalDate;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

/**
 * Represents an API installed locally in the current project.
 */
public class ManagedApiImpl implements ManagedApi {

  private static ApiDirectoryListingJsonCodec codec = new ApiDirectoryListingJsonCodec();

  private static final String URL_PROTOCOL_FILE = "file";

  public static ManagedApi createManagedApi(EclipseProject project,
      IFolder managedApiRootDirectory) {
    ManagedApiImpl managedApi = null;
    try {
      ManagedApiResourceVisitor visitor = ManagedApiUtils.scanManagedApiFiles(project,
          managedApiRootDirectory);
      IFile descriptorFile = visitor.getDescriptor();
      if (visitor.getDescriptor() != null) {
        InputStream managedApiDescriptorStream = descriptorFile.getContents();
        URL baseURL = descriptorFile.getRawLocationURI().toURL();
        ApiInfo apiInfo = codec.toApiInfo(managedApiDescriptorStream, baseURL);
        if (apiInfo == null) {
          ManagedApiLogger.error("Unable to decode API");
        } else {
          ImageDescriptor imageDescriptor = createClasspathContainerIconImageDescriptor(apiInfo);
          managedApi = new ManagedApiImpl(managedApiRootDirectory, apiInfo,
              visitor.getClasspathEntries(), imageDescriptor);
        }
      }
    } catch (MalformedURLException e) {
      ManagedApiLogger.error(e, "Unable to create URL for API descriptor");
    } catch (UnsupportedEncodingException e) {
      ManagedApiLogger.error(e,
          "Unable to create API descriptor due to encoding");
    } catch (CoreException e) {
      ManagedApiLogger.error(e, "Core exception");
    }
    return managedApi;
  }
  
  private static ImageDescriptor createClasspathContainerIconImageDescriptor(
      ApiInfo apiInfo) {
    ImageDescriptor imageDescriptor = null;
    URL iconLink = apiInfo.getIconLink(ManagedApiPlugin.ICON_KEY_CLASSPATH_CONTAINER);
    if (iconLink != null) {
      if (URL_PROTOCOL_FILE.equals(iconLink.getProtocol())) {
        File iconFile = null;
        try {
          iconFile = new File(iconLink.toURI());
        } catch (URISyntaxException e) {
          // Ignore
        }

        if (iconFile != null && iconFile.exists()) {
          imageDescriptor = ImageDescriptor.createFromURL(iconLink);
        } else {
          ManagedApiLogger.warn("Bad image reference for icon: "
              + iconLink.toExternalForm());
        }
      } else {
        imageDescriptor = ImageDescriptor.createFromURL(iconLink);
      }
    }
    return imageDescriptor;
  }

  /**
   * An IApiInfo data provider.
   */
  private final ApiInfo apiInfo;

  /**
   * Cached values for classpath entries found in the managedApiRootDirectory.
   */
  private final IClasspathEntry[] classpathEntries;

  /**
   * Delete flag: set if the ManagedApi is deleted. The delete removes the
   * underlying files for the API, so the ManagedApi object is dead once this
   * occurs.
   */
  private boolean deletedState = false;

  /**
   * Root directory that will hold files for this particular ManagedApi.
   */
  private final IFolder managedApiRootDirectory;

  private boolean updateAvailable;

  private boolean revisionUpdateAvailable;

  private ImageDescriptor iconImageDescriptor;

  private ManagedApiImpl(IFolder managedApiRootDirectory, ApiInfo apiInfo,
      IClasspathEntry[] classpathEntries, ImageDescriptor iconImageDescriptor) {
    this.managedApiRootDirectory = managedApiRootDirectory;
    this.apiInfo = apiInfo;
    this.classpathEntries = classpathEntries;
    this.iconImageDescriptor = iconImageDescriptor;
  }

  public void delete(IProgressMonitor monitor) {
    deletedState = true;
    if (managedApiRootDirectory.exists()) {
      try {
        managedApiRootDirectory.delete(true, monitor);
      } catch (CoreException e) {
        ManagedApiLogger.warn(e,
            "Failure to delete the directory for managed API "
                + getDisplayName());
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ManagedApiImpl other = (ManagedApiImpl) obj;
    if (apiInfo == null) {
      if (other.apiInfo != null)
        return false;
    } else if (!apiInfo.equals(other.apiInfo))
      return false;
    if (managedApiRootDirectory == null) {
      if (other.managedApiRootDirectory != null)
        return false;
    } else if (!managedApiRootDirectory.equals(other.managedApiRootDirectory)) {
      return false;
    } else if (updateAvailable != other.isUpdateAvailable()) {
      return false;
    } else if (revisionUpdateAvailable != other.isRevisionUpdateAvailable()) {
      return false;
    }
    return true;
  }

  public ImageDescriptor getClasspathContainerIconImageDescriptor() {
    return iconImageDescriptor;
  }

  public IClasspathEntry[] getClasspathEntries() {
    return classpathEntries;
  }

  public String getDescription() {
    return apiInfo.getDescription();
  }

  public URL getDiscoveryLink() {
    return apiInfo.getDiscoveryLink();
  }

  public String getDisplayName() {
    return apiInfo.getDisplayName();
  }

  public URL getDocumentationLink() {
    return apiInfo.getDocumentationLink();
  }

  public URL getDownloadLink() {
    return apiInfo.getDownloadLink();
  }

  public URL getIconLink(String key) {
    return apiInfo.getIconLink(key);
  }

  public Set<String> getIconLinkKeys() {
    return apiInfo.getIconLinkKeys();
  }

  public String getIdentifier() {
    return apiInfo.getIdentifier();
  }

  public String[] getLabels() {
    return apiInfo.getLabels();
  }

  public String getName() {
    return apiInfo.getName();
  }

  public String getPublisher() {
    return apiInfo.getPublisher();
  }

  public Integer getRanking() {
    return apiInfo.getRanking();
  }

  public LocalDate getReleaseDate() {
    return apiInfo.getReleaseDate();
  }

  public URL getReleaseNotesLink() {
    return apiInfo.getReleaseNotesLink();
  }

  public IFolder getRootDirectory() {
    return managedApiRootDirectory;
  }

  public URL getTosLink() {
    return apiInfo.getTosLink();
  }

  public String getVersion() {
    return apiInfo.getVersion();
  }

  public boolean hasClasspathContainerIcon() {
    return apiInfo.hasIconLinkKey(ManagedApiPlugin.ICON_KEY_CLASSPATH_CONTAINER);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((apiInfo == null) ? 0 : apiInfo.hashCode());
    result = prime
        * result
        + ((managedApiRootDirectory == null) ? 0
            : managedApiRootDirectory.hashCode());
    return result;
  }

  public boolean hasIconLinkKey(String key) {
    return apiInfo.hasIconLinkKey(key);
  }

  public boolean hasLabel(String label) {
    return apiInfo.hasLabel(label);
  }

  public boolean isDeleted() {
    return deletedState;
  }

  public Boolean isPreferred() {
    return apiInfo.isPreferred();
  }

  public boolean isRevisionUpdateAvailable() {
    return revisionUpdateAvailable;
  }

  public boolean isUpdateAvailable() {
    return updateAvailable;
  }

  public void setRevisionUpdateAvailable(boolean revisionUpdateAvailable) {
    this.revisionUpdateAvailable = revisionUpdateAvailable;
  }

  public void setUpdateAvailable() {
    this.updateAvailable = true;
  }

  public void unsetUpdateAvailable() {
    this.updateAvailable = false;
  }
}
