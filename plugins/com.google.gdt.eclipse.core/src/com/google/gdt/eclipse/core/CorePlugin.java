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
package com.google.gdt.eclipse.core;

import com.google.gdt.eclipse.core.projects.ProjectChangeTimestampTracker;
import com.google.gdt.eclipse.core.resources.CoreImages;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.BundleContext;

/**
 */
public class CorePlugin extends AbstractGooglePlugin {
  public static final String PLUGIN_ID = CorePlugin.class.getPackage().getName();

  private static CorePlugin plugin;

  /**
   * Returns the shared instance.
   * 
   * @return the shared instance
   */
  public static CorePlugin getDefault() {
    return plugin;
  }

  public CorePlugin() {
  }

  @Override
  public Image getImage(String imageId) {
    return getImageRegistry().get(imageId);
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;

    ProjectChangeTimestampTracker.INSTANCE.startTracking();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    ProjectChangeTimestampTracker.INSTANCE.stopTracking();

    plugin = null;
    super.stop(context);
  }

  protected String getPluginID() {
    return PLUGIN_ID;
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);

    reg.put(CoreImages.LIBRARY_ICON,
        imageDescriptorFromPath("icons/library_obj.gif"));

    reg.put(CoreImages.TERMINATE_ICON,
        imageDescriptorFromPath("icons/terminate_obj.gif"));

    ImageDescriptor errorOverlayDescriptor = imageDescriptorFromPath("icons/error_co.gif");
    reg.put(CoreImages.ERROR_OVERLAY, errorOverlayDescriptor);

    ImageDescriptor invalidSdkDescriptor = new DecorationOverlayIcon(
        getImage(CoreImages.LIBRARY_ICON), errorOverlayDescriptor,
        IDecoration.BOTTOM_LEFT);

    reg.put(CoreImages.INVALID_SDK_ICON, invalidSdkDescriptor);
  }
}
