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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a GWT module resource in a JAR file.
 */
@SuppressWarnings("restriction")
public class ModuleJarResource extends AbstractModule {

  protected ModuleJarResource(IJarEntryResource jarResource) {
    super(jarResource);
  }

  public IJarEntryResource getJarEntryResource() {
    return (IJarEntryResource) storage;
  }

  public boolean isBinary() {
    return true;
  }

  @Override
  protected IDOMModel doGetModelForRead() throws IOException, CoreException {
    IModelManager modelManager = StructuredModelManager.getModelManager();
    InputStream moduleStream = storage.getContents();
    IDOMModel model = (IDOMModel) modelManager.getModelForRead(
        storage.getName(), moduleStream, null);
    moduleStream.close();
    return model;
  }

  @Override
  protected String doGetPackageName() {
    IPath modulePckgPath = storage.getFullPath().removeLastSegments(1).makeRelative();
    return modulePckgPath.toString().replace('/', '.');
  }

}
