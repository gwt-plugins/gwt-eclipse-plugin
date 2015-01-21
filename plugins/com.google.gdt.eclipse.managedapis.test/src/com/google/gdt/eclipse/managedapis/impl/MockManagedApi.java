/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.managedapis.ManagedApi;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.joda.time.LocalDate;

import java.net.URL;
import java.util.Set;

public class MockManagedApi implements ManagedApi {

  private final IFolder managedApiRootFolder;
  private final String managedApiName;
  
  public MockManagedApi(IFolder managedApiRootFolder, String managedApiName) {
    this.managedApiName = managedApiName;
    this.managedApiRootFolder = managedApiRootFolder;    
  }
  
  public String getIdentifier() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  public URL getDiscoveryLink() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getDisplayName() {
    // TODO Auto-generated method stub
    return null;
  }

  public URL getDocumentationLink() {
    // TODO Auto-generated method stub
    return null;
  }

  public URL getDownloadLink() {
    // TODO Auto-generated method stub
    return null;
  }

  public URL getIconLink(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  public Set<String> getIconLinkKeys() {
    // TODO Auto-generated method stub
    return null;
  }

  public String[] getLabels() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getName() {
    return managedApiName;
  }

  public String getPublisher() {
    // TODO Auto-generated method stub
    return null;
  }

  public Integer getRanking() {
    // TODO Auto-generated method stub
    return null;
  }

  public LocalDate getReleaseDate() {
    // TODO Auto-generated method stub
    return null;
  }

  public URL getReleaseNotesLink() {
    // TODO Auto-generated method stub
    return null;
  }

  public URL getTosLink() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getVersion() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean hasIconLinkKey(String arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean hasLabel(String arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  public Boolean isPreferred() {
    // TODO Auto-generated method stub
    return null;
  }

  public void delete(IProgressMonitor monitor) {
    // TODO Auto-generated method stub
    
  }

  public ImageDescriptor getClasspathContainerIconImageDescriptor() {
    // TODO Auto-generated method stub
    return null;
  }

  public IClasspathEntry[] getClasspathEntries() throws CoreException {
    // TODO Auto-generated method stub
    return null;
  }

  public IFolder getRootDirectory() {
    return managedApiRootFolder;
  }

  public boolean hasClasspathContainerIcon() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isDeleted() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isRevisionUpdateAvailable() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isUpdateAvailable() {
    // TODO Auto-generated method stub
    return false;
  }

  public void setRevisionUpdateAvailable(boolean revisionUpdateAvailable) {
    // TODO Auto-generated method stub
    
  }

  public void setUpdateAvailable() {
    // TODO Auto-generated method stub
    
  }

  public void unsetUpdateAvailable() {
    // TODO Auto-generated method stub
    
  }

}
