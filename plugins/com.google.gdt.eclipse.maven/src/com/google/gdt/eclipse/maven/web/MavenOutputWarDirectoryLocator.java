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
package com.google.gdt.eclipse.maven.web;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities.IOutputWarDirectoryLocator;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gdt.eclipse.maven.MavenUtils;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

/**
 * Output war directory provider for Maven-based projects. Though right now this
 * is only true for Maven projects imported via STS, we expect that the property
 * corresponding to
 * {@link WebAppProjectProperties#getLastUsedWarOutLocation(org.eclipse.core.resources.IProject)}
 * will have been set at import time.
 * 
 * Since we know that the property was set correctly (as it was done
 * automatically at import time), we'll use this extension point to avoid the
 * unnecessary confirmation at launch, GWT compilation, and deployment time.
 */
public class MavenOutputWarDirectoryLocator implements
    IOutputWarDirectoryLocator {

  public IFolder getOutputWarDirectory(IProject project) {    
    if (!MavenUtils.hasMavenNature(project)) {
      return null;
    }
    
    if (WebAppUtilities.isWebApp(project)) {
      IPath lastUsedWarOutLocation = WebAppProjectProperties.getLastUsedWarOutLocation(project);
      if (lastUsedWarOutLocation != null) {
        IResource lastUsedWarOutResource = ResourceUtils.getResource(lastUsedWarOutLocation);
        if (lastUsedWarOutResource != null
            && lastUsedWarOutResource.getType() == IResource.FOLDER) {
          return (IFolder) lastUsedWarOutResource;
        }
      }
    }
    return null;
  }
}
