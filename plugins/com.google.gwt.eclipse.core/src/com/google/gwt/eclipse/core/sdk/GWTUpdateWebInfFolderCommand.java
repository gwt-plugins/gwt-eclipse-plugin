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
package com.google.gwt.eclipse.core.sdk;

import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Updates the managed WAR output directory's WEB-INF/lib directory so it
 * contains the correct jars from the GWT SDK.
 */
public class GWTUpdateWebInfFolderCommand extends UpdateWebInfFolderCommand {
  public GWTUpdateWebInfFolderCommand(IJavaProject javaProject, Sdk sdk) {
    super(javaProject, sdk);
  }

  @Override
  protected List<String> computeWebInfLibFilesToRemove() throws CoreException {
    return Collections.singletonList("gwt-servlet.jar");
  }

  @Override
  protected void saveFilesCopiedToWebInfLib(List<File> webInfLibFiles)
      throws BackingStoreException {
    GWTProjectProperties.setFilesCopiedToWebInfLib(
        getJavaProject().getProject(), webInfLibFiles);
  }
}
