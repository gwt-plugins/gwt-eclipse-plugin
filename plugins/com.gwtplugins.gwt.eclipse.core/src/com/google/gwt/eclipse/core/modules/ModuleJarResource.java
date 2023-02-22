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
package com.google.gwt.eclipse.core.modules;

import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import java.io.IOException;

/**
 * Represents a GWT module resource in a JAR file.
 */
@SuppressWarnings("restriction")
public class ModuleJarResource extends AbstractModule {

  IJarEntryResource storage;

  String qualifiedName = null;

  protected ModuleJarResource(IJarEntryResource jarResource) {
    this.storage = jarResource;
  }

  public IJarEntryResource getJarEntryResource() {
    return storage;
  }

  @Override
  public boolean isBinary() {
    return true;
  }

  /**
   * Returns the package name for the module.
   */
  @Override
  public String getQualifiedName() {
    // Cache the qualified name
    if (qualifiedName == null) {
      qualifiedName = Util.removeFileExtension(storage.getName());
      String modulePckg = doGetPackageName();
      if (modulePckg != null) {
        qualifiedName = modulePckg + "." + qualifiedName;
      }
    }
    return qualifiedName;
  }

  @Override
  protected IDOMModel doGetModelForRead() throws IOException, CoreException {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    IFile storageFile = ResourcesPlugin.getWorkspace().getRoot().getFile(storage.getFullPath());
    IDOMModel model = (IDOMModel) modelManager.getModelForRead(storageFile);
    return model;
  }

  private final String doGetPackageName() {
    IPath modulePckgPath = storage.getFullPath().removeLastSegments(1).makeRelative();
    return modulePckgPath.toString().replace('/', '.');
  }

}
