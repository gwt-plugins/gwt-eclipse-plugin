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
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gwt.eclipse.core.GWTPlugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * A GWT library containing the gwt-user and gwt-dev classes.
 * 
 * TODO: Move this and subtypes into the sdk package.
 */
public class GWTRuntimeContainer extends SdkClasspathContainer<GWTRuntime> {
  public static final String CONTAINER_ID = GWTPlugin.PLUGIN_ID
      + ".GWT_CONTAINER";
  public static final Path CONTAINER_PATH = new Path(CONTAINER_ID);

  public static IPath getDefaultRuntimePath() {
    // Default runtime just has one segment path with container ID
    return CONTAINER_PATH;
  }

  public static boolean isPathForGWTRuntimeContainer(IPath path) {
    return CONTAINER_PATH.isPrefixOf(path);
  }

  public GWTRuntimeContainer(IPath path, GWTRuntime runtime,
      IClasspathEntry[] classpathEntries, String description) {
    super(path, runtime, classpathEntries, description);
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    IClasspathEntry[] superEntries = super.getClasspathEntries();
    List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

    for (IClasspathEntry superEntry : superEntries) {
      entries.add(superEntry);
    }
    return entries.toArray(new IClasspathEntry[entries.size()]);
  }
}
