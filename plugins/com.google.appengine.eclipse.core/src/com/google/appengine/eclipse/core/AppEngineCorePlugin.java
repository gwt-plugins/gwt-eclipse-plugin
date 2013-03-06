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
package com.google.appengine.eclipse.core;

import com.google.appengine.eclipse.core.resources.GaeImages;
import com.google.appengine.eclipse.core.sdk.AppEngineBridge;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateWebInfFolderCommand;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.PluginProperties;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;
import com.google.gdt.eclipse.core.sdk.WebInfFolderUpdater;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * The activator class controls the plug-in life cycle.
 * 
 */
public class AppEngineCorePlugin extends AbstractGooglePlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = AppEngineCorePlugin.class.getPackage().getName();

  // TODO: Expose this via an accessor.
  public static final String SDK_DOWNLOAD_URL;

  // The shared instance
  private static AppEngineCorePlugin plugin;

  static {
    PluginProperties props = new PluginProperties(AppEngineCorePlugin.class);
    SDK_DOWNLOAD_URL = props.getProperty("gaeplugin.sdk_download_url",
        "http://code.google.com/appengine/downloads.html");
  }

  public static Shell getActiveWorkbenchShell() {
    IWorkbenchWindow window = getActiveWorkbenchWindow();
    if (window != null) {
      return window.getShell();
    }
    return null;
  }

  public static IWorkbenchWindow getActiveWorkbenchWindow() {
    return getDefault().getWorkbench().getActiveWorkbenchWindow();
  }

  /**
   * Returns the shared instance.
   * 
   * @return the shared instance
   */
  public static AppEngineCorePlugin getDefault() {
    return plugin;
  }

  public static String getVersion() {
    return (String) getDefault().getBundle().getHeaders().get(
        Constants.BUNDLE_VERSION);
  }

  private static void closeStreams(InputStream is, OutputStream os) {
    if (os != null) {
      try {
        os.close();
      } catch (IOException ioe) {
        AppEngineCorePluginLog.logError(ioe);
      }
    }

    if (is != null) {
      try {
        is.close();
      } catch (IOException ioe) {
        AppEngineCorePluginLog.logError(ioe);
      }
    }
  }

  /**
   * Reads the contents of appengine-api-proxy.jar from the root of the plugin's
   * jar, and makes a copy of the file in the plugin's state location.
   * 
   * @throws IOException
   */
  private static void extractAppEngineProxyJar() throws IOException {
    extractJar(AppEngineCorePlugin.getDefault(),
        AppEngineBridge.APPENGINE_PROXY_JAR_NAME,
        AppEngineBridge.APPENGINE_PROXY_JAR_NAME);
  }

  private static void extractAppEngineToolsApiJar() throws IOException {
    extractJar(AppEngineCorePlugin.getDefault(),
        "lib/" + AppEngineBridge.APPENGINE_TOOLS_JAR_NAME,
        AppEngineBridge.APPENGINE_TOOLS_JAR_NAME);
  }

  private final WebInfFolderUpdater webInfFolderUpdater = new WebInfFolderUpdater() {
    @Override
    protected Sdk getSdk(IJavaProject javaProject) {
      try {
        return GaeSdk.findSdkFor(javaProject);
      } catch (JavaModelException e) {
        AppEngineCorePluginLog.logError(e, "Could not update WEB-INF folder");
        return null;
      }
    }

    @Override
    protected UpdateWebInfFolderCommand getUpdateCommand(
        IJavaProject javaProject, Sdk sdk) {
      return new AppEngineUpdateWebInfFolderCommand(javaProject, sdk);
    }
  };

  /**
   * A bit of a hack to tell if we're running in development mode. If the
   * "proxy_bin" folder can be found at the plugin's root, then we're definitely
   * running in development mode.
   * 
   * This method is useful for determining whether or not to load the bridge
   * implementation from appengine-api-proxy.jar, or proxy_bin.
   */
  public boolean inDevelopmentMode() {
    return (FileLocator.find(AppEngineCorePlugin.getDefault().getBundle(),
        new Path("proxy_bin"), (Map<?, ?>) null) != null);
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;

    if (!inDevelopmentMode()) {
      /*
       * Extracts appengine-api-proxy.jar and appengine-tools-api.jar
       *  from the plugin jar, and copies it
       * into the plugin's state location, overwriting any existing copy that
       * may exist. We have to do this because URLClassLoaders are not able to
       * load classes from jars within jars.
       * 
       * If the jar cannot be extracted, an IOException will be thrown, which
       * will escape and prevent this plugin from loading.
       */
      extractAppEngineProxyJar();
    }
    extractAppEngineToolsApiJar();

    webInfFolderUpdater.start();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    webInfFolderUpdater.stop();

    plugin = null;
    super.stop(context);
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    reg.put(GaeImages.APP_ENGINE_DEPLOY_LARGE,
        imageDescriptorFromPath("icons/ae-deploy_90x79.png"));
  }
}
