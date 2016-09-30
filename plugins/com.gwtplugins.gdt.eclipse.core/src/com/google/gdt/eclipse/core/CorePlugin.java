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

import com.google.gdt.eclipse.core.markers.GdtProblemSeverities;
import com.google.gdt.eclipse.core.markers.ProjectStructureOrSdkProblemType;
import com.google.gdt.eclipse.core.projects.ProjectChangeTimestampTracker;
import com.google.gdt.eclipse.core.resources.CoreImages;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 */
public class CorePlugin extends AbstractGwtPlugin {
  public static final String PLUGIN_ID = "com.gwtplugins.gdt.eclipse.core";

  private static final Version MIN_GPE_JAVA_VERSION = new Version(1, 7, 0);

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

    // Make sure the executing JRE is up to date. This plugin and everything it depends on
    // are Java 1.5 compatible, so this check will run at workbench startup in any JRE at level
    // 1.5 or later. However, some other parts of GPE require MIN_GPE_JAVA_VERSION or later. If we
    // run on a version later than or equal to 1.5 but less than MIN_GPE_JAVA_VERSION, the code
    // below pops up an error dialog and then throws an exception so that GPE does not start, and
    // does not make any contributions to the UI.
    try {
      checkVersion("java.specification.version", MIN_GPE_JAVA_VERSION);
    } catch (final GpeVersionException e) {
      UIJob dialogJob =
          new UIJob("background-initiated question dialog"){
            @Override public IStatus runInUIThread(IProgressMonitor monitor) {
              Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
              MessageDialog.openError(
                  activeShell,
                  "Newer Java version needed to run Google Plugin for Eclipse",
                  e.getMessage());
              return Status.OK_STATUS;
            }
          };
      dialogJob.schedule();
      // TODO(nhcohen): Uncomment the following once we no longer support Indigo. Due to a bug in
      // Indigo, throwing this exception from CorePlugin.start() indirectly leads to the throwing of
      // an unhandled exception in the main Eclipse thread, so Eclipse cannot start.
      //
      // throw e; // Prevent plugin from starting up.
    }

    // Load problem severities
    GdtProblemSeverities.getInstance().addProblemTypeEnums(
        new Class<?>[] {ProjectStructureOrSdkProblemType.class});

    ProjectChangeTimestampTracker.INSTANCE.startTracking();
  }

  private static void checkVersion(String key, Version minVersion) throws GpeVersionException {
    String detectedVersionString = System.getProperty(key);
    if (key == null) {
      // Shouldn't happen
      throw new GpeVersionException(
          "Can't check Java version: System property \"" + key + "\" is not defined.");
    }
    Version detectedVersion;
    try {
      detectedVersion = new Version(detectedVersionString);
    } catch (IllegalArgumentException e) {
      throw new GpeVersionException(
          String.format(
              "Can't check JRE version: Value of %s property, \"%s\", has an unexpected form.",
              key,
              detectedVersionString),
          e);
    }
    if (detectedVersion.compareTo(minVersion) < 0) {
      throw new GpeVersionException(
          String.format(
              "JRE version is %s; version %s or later is needed to run Google Plugin for Eclipse.",
              detectedVersion,
              minVersion));
    }
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

    reg.put(CoreImages.LIBRARY_ICON, imageDescriptorFromPath("icons/library_obj.gif"));

    reg.put(CoreImages.TERMINATE_ICON, imageDescriptorFromPath("icons/terminate_obj.gif"));

    ImageDescriptor errorOverlayDescriptor = imageDescriptorFromPath("icons/error_co.gif");
    reg.put(CoreImages.ERROR_OVERLAY, errorOverlayDescriptor);

    ImageDescriptor invalidSdkDescriptor = new DecorationOverlayIcon(
        getImage(CoreImages.LIBRARY_ICON), errorOverlayDescriptor, IDecoration.BOTTOM_LEFT);

    reg.put(CoreImages.INVALID_SDK_ICON, invalidSdkDescriptor);
  }
}
