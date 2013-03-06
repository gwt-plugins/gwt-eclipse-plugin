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
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand;
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;

import org.eclipse.jdt.core.IJavaProject;

/**
 * 
 */
public class GWTUpdateProjectSdkCommand extends
    UpdateProjectSdkCommand<GWTRuntime> {

  public static <T extends Sdk> UpdateType computeUpdateType(GWTRuntime oldSdk,
      GWTRuntime newSdk, boolean isDefault) {
    return computeUpdateType(oldSdk, newSdk, isDefault,
        GWTPreferences.getSdks(), GWTRuntimeContainer.CONTAINER_ID);
  }

  public GWTUpdateProjectSdkCommand(IJavaProject javaProject,
      GWTRuntime oldSdk, GWTRuntime newSdk, UpdateType updateType,
      UpdateWebInfFolderCommand updateWebInfFolderCommand) {
    super(javaProject, oldSdk, newSdk, updateType, updateWebInfFolderCommand);
  }

  @Override
  protected String getContainerId() {
    return GWTRuntimeContainer.CONTAINER_ID;
  }
}
