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

import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Updates the managed WAR output directory's WEB-INF/lib directory so it
 * contains the correct Apps Marketplace jars.
 */
public class AppsMarketplaceUpdateWebInfFolderCommand
    extends UpdateWebInfFolderCommand {

  public AppsMarketplaceUpdateWebInfFolderCommand(
      IJavaProject javaProject, Sdk sdk) {
    super(javaProject, sdk);
  }

  @Override
  protected List<String> computeWebInfLibFilesToRemove() throws CoreException {
    return Collections.<String> emptyList();
  }

  @Override
  protected void saveFilesCopiedToWebInfLib(List<File> webInfLibFiles)
      throws BackingStoreException {
  }

}
