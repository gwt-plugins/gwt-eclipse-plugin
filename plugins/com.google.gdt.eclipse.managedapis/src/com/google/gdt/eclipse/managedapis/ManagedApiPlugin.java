/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis;

import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.extensiontypes.IManagedApiProjectStateTest;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectory;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectoryFactory;
import com.google.gdt.eclipse.managedapis.extensiontypes.IManagedApiProjectInitializationCallback;
import com.google.gdt.eclipse.managedapis.impl.IconCache;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiChecker;
import com.google.gdt.eclipse.managedapis.impl.RemoteApiDirectory;
import com.google.gdt.eclipse.managedapis.platform.PluginResources;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Entrypoint for the ManagedApi plugin and container of contants used by the
 * plugin as a whole.
 */
public class ManagedApiPlugin extends AbstractGooglePlugin {

  public static final boolean SINGLE_CLASSPATH_CONTAINER = false;

  public static final String DEPENDENCIES_FOLDER_NAME = "dependencies";

  public static final String PLUGIN_ID = "com.google.gdt.eclipse.managedapis";

  public static final String API_CONTAINER_PATH_ID = PLUGIN_ID
      + ".MANAGED_API_CONTAINER";

  public static final Path API_CONTAINER_PATH = new Path(API_CONTAINER_PATH_ID);

  public static final
      String COPY_CLASSPATH_ENTRIES_TARGET_PATH_KEY = "COPY_CLASSPATH_ENTRIES_TARGET_PATH";

  public static final
      String DEFAULT_MANAGED_API_DIRECTORY_HREF = "http://api-directory.googleapis.com/5935";

  public static final
      String DEFAULT_MANAGED_API_ICON_PREFETCH_HREF = "http://api-directory.googleapis.com/icons";

  public static final String ICON_KEY_CLASSPATH_CONTAINER = "x16";

  public static final String ICON_KEY_API_IMPORT = "x32";

  public static final QualifiedName MANAGED_API_FLAG_QNAME = new QualifiedName(
      PLUGIN_ID, "PROJECT_FLAG");

  public static final String
      MANAGED_API_ROOT_FOLDER_DEFAULT_PATH = ".google_apis";
  
  public static final String SWARM_LIB_FOLDER_NAME = "endpoint-libs";

  public static final String
      MANAGED_API_ROOT_FOLDER_PATH_KEY = "MANAGED_API_ROOT_FOLDER_PATH";

  public static final QualifiedName
      MANAGED_API_SESSION_KEY_QNAME = new QualifiedName(
          PLUGIN_ID, "MANAGED_API_SESSION_KEY");
  
  // Latest in the 1.14 series
  public static final String API_CLIENT_LANG_VERSION = "1.14.x";

  public static final long API_DIRECTORY_CACHE_TTL = 4 * 60 * 60 * 1000; // 4HRS

  public static final long CHECK_MANAGED_APIS_FREQ = 2 * 60 * 60 * 1000;// 2 HRS

  public static final boolean DO_DELETES = false;

  private static ManagedApiPlugin plugin;

  /**
   * Provide access to updated set of initialization callbacks configured for
   * this workspace.
   * 
   * @return the callbacks
   */
  public static IManagedApiProjectInitializationCallback[] findProjectInitializationCallbacks() {
    IManagedApiProjectInitializationCallback[] callbacks;
    getDefault().extensionsLock.lock();
    try {
      callbacks = getDefault().projectInitializationCallbacks;
    } finally {
      getDefault().extensionsLock.unlock();
    }
    return callbacks;
  }

  public static ManagedApiPlugin getDefault() {
    return plugin;
  }

  public static String getManagedApiDirectoryHref() {
    String overrideHref = System.getenv("MANAGED_API_DIRECTORY_HREF");
    if (overrideHref == null) {
      return DEFAULT_MANAGED_API_DIRECTORY_HREF;
    } else {
      return overrideHref;
    }
  }

  public static String getManagedApiIconBundleHref() {
    String overrideHref = System.getenv("MANAGED_API_ICON_PREFETCH_HREF");
    if (overrideHref == null) {
      return DEFAULT_MANAGED_API_ICON_PREFETCH_HREF;
    } else {
      return overrideHref;
    }
  }

  public static boolean isManagedApiClasspathContainer(IPath containerPath) {
    return containerPath.segment(0).equals(API_CONTAINER_PATH_ID);
  }

  public static boolean isValidToAddManagedApiProjectState(IProject project) {
    try {
      if (NatureUtils.hasNature(project, JavaCore.NATURE_ID)) {
        boolean isValid = true;
        getDefault().extensionsLock.lock();
        try {
          for (IManagedApiProjectStateTest tester :
              getDefault().managedApiProjectStateTests) {
            isValid &= tester.isValidToAddManagedApiProjectState(project);
          }
        } finally {
          getDefault().extensionsLock.unlock();
        }
        return isValid;
      }
    } catch (CoreException e) {
      ManagedApiLogger.info(e,
          "Caught core exception while testing project nature for project "
          + project.getProject().getName());
    }
    return false;
  }

  private RemoteApiDirectory apiDirectory = new RemoteApiDirectory(
      getManagedApiDirectoryHref());

  /**
   * Access to this array of extensions should be protected by the
   * extensionsLock.
   */
  private IManagedApiProjectStateTest[] managedApiProjectStateTests = null;

  /**
   * Access to this array of extensions should be protected by the
   * extensionsLock.
   */
  private IManagedApiProjectInitializationCallback[]
      projectInitializationCallbacks = null;

  private ReentrantLock extensionsLock = new ReentrantLock();

  private ApiDirectoryFactory apiDirectoryFactory = new ApiDirectoryFactory() {
    public ApiDirectory buildApiDirectory() {
      return apiDirectory;
    }
  };

  private ManagedApiChecker checker;

  private IconCache iconCache = null;

  private IRegistryChangeListener registryChangeListener;

  public ApiDirectoryFactory getApiDirectoryFactory() {
    return apiDirectoryFactory;
  }

  public IconCache getIconCache() {
    return iconCache;
  }

  public Resources getResources() {
    return new PluginResources(getWorkbench().getDisplay());
  }

  public void setIconCache(IconCache iconCache) {
    this.iconCache = iconCache;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;

    loadExtensions();

    registryChangeListener = new IRegistryChangeListener() {
      public void registryChanged(IRegistryChangeEvent event) {
        loadExtensions();
      }
    };

    Platform.getExtensionRegistry()
        .addRegistryChangeListener(registryChangeListener);

    checker = new ManagedApiChecker(apiDirectoryFactory,
        CHECK_MANAGED_APIS_FREQ,
        "Google Managed APIs Plugin: check APIs for updates", this);
    checker.startChecking();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
    if (checker != null) {
      checker.stopChecking();
    }

    if (registryChangeListener != null) {
      Platform.getExtensionRegistry()
          .removeRegistryChangeListener(registryChangeListener);
    }
  }

  private void loadExtensions() {
    ExtensionQuery<IManagedApiProjectStateTest> testExtQuery = new ExtensionQuery<IManagedApiProjectStateTest>(
        ManagedApiPlugin.PLUGIN_ID, "managedApiProjectStateTest", "class");
    List<ExtensionQuery.Data<IManagedApiProjectStateTest>> testExtensions = testExtQuery.getData();
    List<IManagedApiProjectStateTest> tests = new ArrayList<
        IManagedApiProjectStateTest>();

    for (ExtensionQuery.Data<IManagedApiProjectStateTest> testExtension :
        testExtensions) {
      if (testExtension.getExtensionPointData() != null) {
        tests.add(testExtension.getExtensionPointData());
      }
    }

    ExtensionQuery<IManagedApiProjectInitializationCallback>
        projectInitCallbackExtQuery = new ExtensionQuery<
        IManagedApiProjectInitializationCallback>(ManagedApiPlugin.PLUGIN_ID,
        "managedApiProjectInitializationCallback", "class");
    List<ExtensionQuery.Data<IManagedApiProjectInitializationCallback>>
        projectInitCallbackExtensions = projectInitCallbackExtQuery.getData();
    List<IManagedApiProjectInitializationCallback> initCallbacks = new ArrayList<IManagedApiProjectInitializationCallback>();

    for (ExtensionQuery.Data<IManagedApiProjectInitializationCallback>
        testCallback : projectInitCallbackExtensions) {
      if (testCallback.getExtensionPointData() != null) {
        initCallbacks.add(testCallback.getExtensionPointData());
      }
    }

    try {
      extensionsLock.lock();
      managedApiProjectStateTests = tests.toArray(
          new IManagedApiProjectStateTest[tests.size()]);
      projectInitializationCallbacks = initCallbacks.toArray(
          new IManagedApiProjectInitializationCallback[initCallbacks.size()]);
    } finally {
      extensionsLock.unlock();
    }
  }
}
