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
package com.google.gwt.eclipse.core.clientbundle;

import com.google.gdt.eclipse.core.resources.CompilationUnitResourceDependencyIndex;
import com.google.gwt.eclipse.core.GWTPlugin;

import org.eclipse.core.runtime.IPath;

/**
 * Tracks ClientBundle subtypes' dependencies on resource files.
 */
public class ClientBundleResourceDependencyIndex extends
    CompilationUnitResourceDependencyIndex {

  private static ClientBundleResourceDependencyIndex INSTANCE;

  public static ClientBundleResourceDependencyIndex getInstance() {
    // Lazily load the index
    if (INSTANCE == null) {
      INSTANCE = new ClientBundleResourceDependencyIndex();
    }
    return INSTANCE;
  }

  public static void save() {
    if (INSTANCE != null) {
      INSTANCE.saveIndex();
    }
  }

  public ClientBundleResourceDependencyIndex() {
    super("clientBundleResources");
  }

  @Override
  protected IPath getIndexFileLocation() {
    // <workspace>/.metadata/.plugins/com.google.gwt.eclipse.plugin
    return GWTPlugin.getDefault().getStateLocation();
  }

}
