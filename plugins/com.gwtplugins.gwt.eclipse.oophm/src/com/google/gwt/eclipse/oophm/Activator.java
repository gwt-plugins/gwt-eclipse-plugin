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
package com.google.gwt.eclipse.oophm;

import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gwt.eclipse.oophm.launch.RemoteUIServer;
import com.google.gwt.eclipse.oophm.launch.TerminatedLaunchListener;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModel;
import com.google.gwt.eclipse.oophm.views.hierarchical.WebAppLaunchViewActivator;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractGooglePlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.gwt.eclipse.oophm";

  private static ILaunchesListener2 listener = null;

  // The shared instance
  private static Activator plugin;

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static Activator getDefault() {
    return plugin;
  }

  private static void addLaunchListener() {
    listener = new TerminatedLaunchListener();
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(listener);
  }

  /**
   * Returns the image with the specified Java image decorated added in the
   * lower left quadrant of the image.
   */
  private static ImageDescriptor decorateImageDescriptor(Image baseImage,
      ImageDescriptor overlayDescriptor) {
    return new DecorationOverlayIcon(baseImage, overlayDescriptor,
        IDecoration.BOTTOM_LEFT);
  }

  private static void removeLaunchListener() {
    if (listener != null) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(listener);
      listener = null;
    }
  }

  /**
   * The constructor
   */
  public Activator() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
   * )
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    addLaunchListener();

    WebAppDebugModel.getInstance().addWebAppDebugModelListener(
        WebAppLaunchViewActivator.getInstance());
    
    BrowserUtilities.ensure32BitIe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
   * )
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    removeLaunchListener();
    RemoteUIServer.getInstance().stop();
    WebAppDebugModel.getInstance().removeWebAppDebugModelListener(
        WebAppLaunchViewActivator.getInstance());
    plugin = null;
    super.stop(context);
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);

    ImageDescriptor errorOverlayDescriptor = imageDescriptorFromPath("icons/error_co.gif");
    reg.put(DevModeImages.ERROR_OVERLAY, errorOverlayDescriptor);

    ImageDescriptor warningOverlayDescriptor = imageDescriptorFromPath("icons/warning_co.gif");
    reg.put(DevModeImages.WARNING_OVERLAY, warningOverlayDescriptor);

    reg.put(DevModeImages.CLEAR_LOG,
        imageDescriptorFromPath("icons/clear_co.gif"));
    reg.put(DevModeImages.CLEAR_TERMINATED_LAUNCHES,
        imageDescriptorFromPath("icons/rem_all_co.gif"));
    reg.put(DevModeImages.COLLAPSE_ALL,
        imageDescriptorFromPath("icons/collapseall.gif"));

    reg.put(DevModeImages.GDT_ICON,
        imageDescriptorFromPath("icons/gdt_16x16.png"));

    Image gdtImage = getImage(DevModeImages.GDT_ICON);

    reg.put(DevModeImages.GDT_ICON_ERROR, decorateImageDescriptor(gdtImage,
        errorOverlayDescriptor));

    reg.put(DevModeImages.GDT_ICON_WARNING, decorateImageDescriptor(gdtImage,
        warningOverlayDescriptor));

    reg.put(DevModeImages.GDT_ICON_TERMINATED,
        imageDescriptorFromPath("icons/gdt_16x16_terminated.png"));

    reg.put(DevModeImages.LOG_ITEM_DEBUG,
        imageDescriptorFromPath("icons/log-item-debug.png"));
    reg.put(DevModeImages.LOG_ITEM_ERROR,
        imageDescriptorFromPath("icons/log-item-error.png"));
    reg.put(DevModeImages.LOG_ITEM_INFO,
        imageDescriptorFromPath("icons/log-item-info.png"));
    reg.put(DevModeImages.LOG_ITEM_SPAM,
        imageDescriptorFromPath("icons/log-item-spam.png"));
    reg.put(DevModeImages.LOG_ITEM_TRACE,
        imageDescriptorFromPath("icons/log-item-trace.png"));
    reg.put(DevModeImages.LOG_ITEM_WARNING,
        imageDescriptorFromPath("icons/log-item-warning.png"));
    reg.put(DevModeImages.RELOAD_WEB_SERVER,
        imageDescriptorFromPath("icons/refresh.gif"));
    reg.put(DevModeImages.TERMINATE_LAUNCH,
        imageDescriptorFromPath("icons/terminatedlaunch_obj.gif"));
    reg.put(DevModeImages.WEB_BROWSER,
        imageDescriptorFromPath("icons/internal_browser.gif"));

    Image webBrowserImage = getImage(DevModeImages.WEB_BROWSER);

    reg.put(DevModeImages.WEB_BROWSER_ERROR, decorateImageDescriptor(
        webBrowserImage, errorOverlayDescriptor));

    reg.put(DevModeImages.WEB_BROWSER_WARNING, decorateImageDescriptor(
        webBrowserImage, warningOverlayDescriptor));

    reg.put(DevModeImages.WEB_BROWSER_TERMINATED,
        imageDescriptorFromPath("icons/internal_browser_terminated.gif"));

    reg.put(DevModeImages.SPEED_TRACER_ICON,
        imageDescriptorFromPath("icons/speed-tracer_small.png"));
    reg.put(DevModeImages.SPEED_TRACER_ICON_TERMINATED,
        imageDescriptorFromPath("icons/speed-tracer_small_terminated.png"));
  }
}
