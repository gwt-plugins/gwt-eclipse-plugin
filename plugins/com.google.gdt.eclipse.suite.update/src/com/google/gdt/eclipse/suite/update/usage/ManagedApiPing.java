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
package com.google.gdt.eclipse.suite.update.usage;

import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.ManagedApiProjectObserver;
import com.google.gdt.eclipse.managedapis.extensiontypes.IManagedApiProjectInitializationCallback;
import com.google.gdt.eclipse.managedapis.impl.ApiPlatformType;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Sends a ping to the update site when adding an api is complete.
 *
 * TODO: discuss impact of moving this and the UpdateSitePingManager into GDT Core.
 * TODO(rdayal): See if we can consolidate the identifiers that we're using to for
 * each platform with the ApiPlatformType class.
 */
public class ManagedApiPing implements IManagedApiProjectInitializationCallback {

  private static final String UNKNOWN_PLATFORM = "Unknown";
  private static final String ANDROID_ENVIRONMENT = "Android";
  private static final String APPENGINE_ENVIRONMENT = "AppEngine";
  private String apiPlatform;

  @Override
  public void onInitialization(ManagedApiProject project) {
    apiPlatform = UNKNOWN_PLATFORM;
    try {
      if (WebAppUtilities.hasManagedWarOut(project.getProject())) {
        apiPlatform = APPENGINE_ENVIRONMENT;
      } else if (ApiPlatformType.getAndroidPlatformType(project.getProject()) != null) {
        apiPlatform = ANDROID_ENVIRONMENT;
      }
    } catch (JavaModelException e) {
      GdtExtPlugin.getLogger().logError(e);
    }
    project.registerManagedApiProjectObserver(new ManagedApiProjectObserver() {
      @Override public void addManagedApis(ManagedApi[] api) {
        for (ManagedApi ma : api) {
          GdtExtPlugin.getAnalyticsPingManager().sendAPIAddPing(
              ma.getName() + "_" + ma.getVersion(), apiPlatform);
        }
      }

      @Override
      public void changeCopyToDirectory(IFolder originalFolder,
          IFolder newFolder) {
      }

      @Override
      public void refreshManagedApis(ManagedApi[] api) {
      }

      @Override
      public void removeManagedApis(ManagedApi[] api) {
      }
    });
  }
}
