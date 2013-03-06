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
package com.google.gdt.eclipse.platform.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ModuleFile;
import org.eclipse.wst.server.core.util.PublishHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Publisher for WTP modules.
 */
// Added this annotation because of the use of the ServerPlugin class.
@SuppressWarnings("restriction")
public class WtpPublisher {

  private static final IPath[] PUBLISHING_IGNORE_PATHS = {new Path("WEB-INF").append("appengine-generated")};
  private static final IPath WEB_INF_LIB_PATH = new Path("WEB-INF").append("lib");

  /**
   * Publish the WST {@link IModule}s to the specified WAR directory. Note that
   * the war directory will be erased and then recreated based on the latest
   * module contents.
   */
  public static void publishModulesToWarDirectory(IProject project,
      IModule[] modules, IPath warDirectoryPath, boolean forceFullPublish, IProgressMonitor monitor)
      throws CoreException {

    File warDirectory = warDirectoryPath.toFile();

    warDirectory.mkdirs();

    // Use WTP's publishing mechanism to copy over any jst-type modules
    // over to the output war directory

    File tempFile = ServerPlugin.getInstance().getStateLocation().toFile();
    PublishHelper helper = new PublishHelper(tempFile);

    List<IStatus> publishResult = new ArrayList<IStatus>();

    for (IModule module : modules) {
      ModuleDelegate moduleDelegate = (ModuleDelegate) module.loadAdapter(
          ModuleDelegate.class, null);

      // Collect the general set of resources for the module
      ArrayList<IModuleResource> resources = new ArrayList<IModuleResource>(
          Arrays.asList(moduleDelegate.members()));

      // Allocate an ArrayList for "extra" resources, which are implied by
      // the general resources
      ArrayList<IModuleResource> extraResources = new ArrayList<IModuleResource>();

      IModule[] children = null;

      IModuleType moduleType = module.getModuleType();
      if (moduleType != null && "jst.web".equals(moduleType.getId())) {
        IWebModule webModule = (IWebModule) module.loadAdapter(
            IWebModule.class, null);
        if (webModule != null) {
          children = webModule.getModules();
        }
      }

      // Examine the child modules of the current module
      if (children != null) {
        for (IModule child : children) {
          ModuleDelegate childModuleDelegate = (ModuleDelegate) child.loadAdapter(
              ModuleDelegate.class, null);
          IModuleResource[] mr = childModuleDelegate.members();
          if ("jst.utility".equals(child.getModuleType().getId())) {
            /*
             * This type of module represents a dependent WTP "utility" project.
             * We need to take the resource for this module and package them up
             * into a jar.
             * 
             * Later, we'll copy the resulting jar over to the WEB-INF/lib
             * folder.
             */
            File jarFile = new File(tempFile, child.getName() + ".jar");
            IStatus[] status = helper.publishZip(mr,
                new Path(jarFile.getAbsolutePath()), monitor);
            merge(publishResult, status);
            extraResources.add(new ModuleFile(jarFile, jarFile.getName(),
                WEB_INF_LIB_PATH));
          } else if ("jst.appclient".equals(child.getModuleType().getId())) {
            /*
             * This is a strange type of module that represents those server
             * runtime libaries with a MainClass attribute defined in their
             * MANIFEST file. For some reason, WTP's publishing mechanism will
             * not copy such libraries over automatically; we need to do it
             * explicitly.
             */
            File file = (File) mr[0].getAdapter(File.class);
            extraResources.add(new ModuleFile(file, file.getName(),
                WEB_INF_LIB_PATH));
          }
        }
      }

      IStatus[] status;
      if (!forceFullPublish) {
        /*
         * First, publish over the main module, ignoring paths where the App
         * Engine Development server places artifacts that are useful from
         * session-to-session.
         */
        status = helper.publishSmart(
            resources.toArray(new IModuleResource[resources.size()]),
            warDirectoryPath, PUBLISHING_IGNORE_PATHS, monitor);
      } else {
        /*
         * For speed tracer
         */
        status = helper.publishFull(
            resources.toArray(new IModuleResource[resources.size()]),
            warDirectoryPath, monitor);
      }
      merge(publishResult, status);

      /*
       * If we have any resources that are the result of child modules, then do
       * a straight copy of them (i.e. no smart publishing - in fact, trying to
       * publish these resources using smartPublish will not work; the resources
       * have not been constructed in the form that smartPublish expects).
       */
      if (extraResources.size() > 0) {
        status = helper.publishFull(
            extraResources.toArray((new IModuleResource[extraResources.size()])),
            warDirectoryPath, monitor);
        merge(publishResult, status);
      }
    }

    if (publishResult.size() > 0) {
      throw new CoreException(new MultiStatus(
          "com.google.gdt.eclipse.platform.shared", 0,
          publishResult.toArray(new IStatus[publishResult.size()]), NLS.bind(
              "Publishing of ''{0}'' failed", project.getName()), null));
    }
  }

  private static void merge(List<IStatus> result, IStatus[] status) {
    if (result == null || status == null || status.length == 0) {
      return;
    }

    int size = status.length;
    for (int i = 0; i < size; i++) {
      result.add(status[i]);
    }
  }
}
