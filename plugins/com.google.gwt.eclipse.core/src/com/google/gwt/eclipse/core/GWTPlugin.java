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
package com.google.gwt.eclipse.core;

import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.PluginProperties;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;
import com.google.gdt.eclipse.core.sdk.WebInfFolderUpdater;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResourceChangeListener;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResourceDependencyIndex;
import com.google.gwt.eclipse.core.launch.SpeedTracerLaunchListener;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.sdk.GWTUpdateWebInfFolderCommand;
import com.google.gwt.eclipse.core.search.JavaRefIndex;
import com.google.gwt.eclipse.core.speedtracer.SourceViewerServer;
import com.google.gwt.eclipse.core.uibinder.model.reference.UiBinderReferenceManager;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.net.BindException;

/**
 * The activator class controls the plug-in life cycle.
 */
public class GWTPlugin extends AbstractGooglePlugin {

  // Plug-in ID
  public static final String PLUGIN_ID = GWTPlugin.class.getPackage().getName();

  // TODO: Expose this via an accessor.
  public static final String SDK_DOWNLOAD_URL;

  // Shared instance
  private static GWTPlugin plugin;

  static {
    PluginProperties props = new PluginProperties(GWTPlugin.class);
    SDK_DOWNLOAD_URL = props.getProperty("gwtplugin.sdk_download_url",
        "http://code.google.com/webtoolkit/download.html");
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

  public static GWTPlugin getDefault() {
    return plugin;
  }

  public static String getName() {
    return (String) getDefault().getBundle().getHeaders().get(
        Constants.BUNDLE_NAME);
  }

  public static String getVersion() {
    return (String) getDefault().getBundle().getHeaders().get(
        Constants.BUNDLE_VERSION);
  }
  
  private final WebInfFolderUpdater webInfFolderUpdater = new WebInfFolderUpdater() {
    @Override
    protected Sdk getSdk(IJavaProject javaProject) {
      try {
        return GWTRuntime.findSdkFor(javaProject);
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e, "Could not update WEB-INF folder");
        return null;
      }
    }

    @Override
    protected UpdateWebInfFolderCommand getUpdateCommand(
        IJavaProject javaProject, Sdk sdk) {
      return new GWTUpdateWebInfFolderCommand(javaProject, sdk);
    }
  };

  public GWTPlugin() {
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    addLaunchListener();

    try {
      SourceViewerServer.INSTANCE.start();
    } catch (BindException be) {
      // Ignore this exception - it's already been logged.
      
    } catch (Throwable e) {
      GWTPluginLog.logError(e, "Could not start source viewer server.");
    }

    ClientBundleResourceChangeListener.addToWorkspace();
    UiBinderReferenceManager.INSTANCE.start();

    webInfFolderUpdater.start();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    webInfFolderUpdater.stop();

    UiBinderReferenceManager.INSTANCE.stop();
    ClientBundleResourceDependencyIndex.save();
    JavaRefIndex.save();
    
    removeLaunchListener();

    try {
      SourceViewerServer.INSTANCE.stop();
    } catch (Throwable e) {
      GWTPluginLog.logError(e, "Could not stop source viewer server.");
    }

    plugin.savePluginPreferences();

    plugin = null;

    super.stop(context);
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);

    reg.put(GWTImages.GWT_ICON, imageDescriptorFromPath("icons/gwt_small.png"));
    reg.put(GWTImages.GWT_LOGO, imageDescriptorFromPath("icons/gwt_large.png"));
    reg.put(GWTImages.JAVA_ICON, imageDescriptorFromPath("icons/jcu_obj.gif"));

    reg.put(GWTImages.JSNI_DEFAULT_METHOD_SMALL,
        imageDescriptorFromPath("icons/methdef_obj.gif"));
    reg.put(GWTImages.JSNI_PRIVATE_METHOD_SMALL,
        imageDescriptorFromPath("icons/methpri_obj.gif"));
    reg.put(GWTImages.JSNI_PROTECTED_METHOD_SMALL,
        imageDescriptorFromPath("icons/methpro_obj.gif"));
    reg.put(GWTImages.JSNI_PUBLIC_METHOD_SMALL,
        imageDescriptorFromPath("icons/methpub_obj.gif"));

    reg.put(GWTImages.NEW_ASYNC_INTERFACE_LARGE,
        imageDescriptorFromPath("icons/gwt-new-asyncinterface_large.png"));
    reg.put(GWTImages.NEW_ASYNC_INTERFACE_SMALL,
        imageDescriptorFromPath("icons/gwt-new-asyncinterface_small.png"));

    reg.put(GWTImages.EDITOR_SELECTION_INFO,
        imageDescriptorFromPath("icons/wordassist_co.gif"));
    reg.put(GWTImages.NEW_ENTRY_POINT_LARGE,
        imageDescriptorFromPath("icons/gwt-new-entrypoint_large.png"));
    reg.put(GWTImages.NEW_ENTRY_POINT_SMALL,
        imageDescriptorFromPath("icons/gwt-new-entrypoint_small.png"));

    reg.put(GWTImages.NEW_HOST_PAGE_LARGE,
        imageDescriptorFromPath("icons/gwt-new-hostpage_large.png"));
    reg.put(GWTImages.NEW_HOST_PAGE_SMALL,
        imageDescriptorFromPath("icons/gwt-new-hostpage_small.png"));
    reg.put(GWTImages.NEW_MODULE_LARGE,
        imageDescriptorFromPath("icons/gwt-new-module_large.png"));
    reg.put(GWTImages.NEW_MODULE_SMALL,
        imageDescriptorFromPath("icons/gwt-new-module_small.png"));
    reg.put(GWTImages.MODULE_ICON,
        imageDescriptorFromPath("icons/gwt-module-file_small.gif"));
    reg.put(GWTImages.GWT_COMPILE_LARGE,
        imageDescriptorFromPath("icons/gwt-compile_large.png"));
    reg.put(GWTImages.NEW_CLIENT_BUNDLE_LARGE,
        imageDescriptorFromPath("icons/gwt-new-clientbundle_large.png"));
    reg.put(GWTImages.NEW_CLIENT_BUNDLE_SMALL,
        imageDescriptorFromPath("icons/gwt-new-clientbundle_small.png"));
    reg.put(GWTImages.NEW_UI_BINDER_LARGE,
        imageDescriptorFromPath("icons/gwt-new-uibinder_large.png"));
    reg.put(GWTImages.NEW_UI_BINDER_SMALL,
        imageDescriptorFromPath("icons/gwt-new-uibinder_small.png"));
    reg.put(GWTImages.SPEED_TRACER_LARGE,
        imageDescriptorFromPath("icons/speed-tracer_large.png"));
    reg.put(GWTImages.SPEED_TRACER_SMALL,
        imageDescriptorFromPath("icons/speed-tracer_small.png"));
  }

  private void addLaunchListener() {
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(SpeedTracerLaunchListener.INSTANCE);
  }

  private void removeLaunchListener() {
    DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(SpeedTracerLaunchListener.INSTANCE);
  }
  
}
