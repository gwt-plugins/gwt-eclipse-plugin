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
package com.google.gdt.eclipse.appsmarketplace.sdk;

import com.google.gdt.eclipse.appsmarketplace.AppsMarketplacePlugin;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.sdk.AbstractSdk;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;

/**
 * Apps Marketplace has no sdk. This is almost a dummy implementation to play
 * along with the sdk pattern and availing of apis built for updating WEB-INF
 * folder etc.
 */
public final class AppsMarketplaceSdk extends AbstractSdk {
  public AppsMarketplaceSdk() {
    super("apps-marketplace", null);
  }

  /* (non-Javadoc)
   * @see com.google.gdt.eclipse.core.sdk.Sdk#getClasspathEntries()
   */
  public IClasspathEntry[] getClasspathEntries() {
    IClasspathEntry[] entries = new IClasspathEntry[1];
    String jarPath = AppsMarketplacePlugin.getDefault().getStateLocation() + "/"
        + AppsMarketplacePlugin.APPS_MARKETPLACE_JAR_NAME;
    entries[0] = JavaCore.newLibraryEntry(new Path(jarPath), null, null);
    return entries;
  }

  public IClasspathEntry[] getClasspathEntriesFromWebInfLib(IProject project) {
    // Fix classpath warning during project generation
    // http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=14
    IFolder webInfLibFolder = WebAppUtilities.getWebInfLib(project);
    IFile file = webInfLibFolder.getFile(
        AppsMarketplacePlugin.APPS_MARKETPLACE_JAR_NAME);
    if (file == null) {
      return getClasspathEntries();
    } else {
      IPath jarPath = file.getFullPath();
      IClasspathEntry[] entries = new IClasspathEntry[1];
      entries[0] = JavaCore.newLibraryEntry(jarPath, null, null);
      return entries;
    }
  }

  public String getVersion() {
    return "1.0";
  }

  public File[] getWebAppClasspathFiles(IProject project) {
    File[] files = new File[1];
    String jarPath = AppsMarketplacePlugin.getDefault().getStateLocation() + "/"
        + AppsMarketplacePlugin.APPS_MARKETPLACE_JAR_NAME;
    files[0] = Path.fromOSString(jarPath).toFile();
    return files;
  }

  public IStatus validate() {
    return Status.OK_STATUS;
  }
}

