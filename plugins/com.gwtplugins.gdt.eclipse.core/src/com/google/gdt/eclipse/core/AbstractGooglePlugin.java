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

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Other Google Eclipse Plugins inherit from this.
 */
public abstract class AbstractGooglePlugin extends AbstractUIPlugin {

  // TODO: to see if we can/should first check if the jar already exists
  protected static void extractJar(
      Plugin plugin, String pathInPluginJar, String pathInStateLocation)
      throws IOException {
    IPath pluginStateLocation = plugin.getStateLocation();
    File fileInStateLocation = pluginStateLocation.append(
        pathInStateLocation).toFile();
    fileInStateLocation.delete();

    FileOutputStream os = null;
    InputStream is = FileLocator.openStream(
        plugin.getBundle(), new Path(pathInPluginJar), false);

    try {
      os = new FileOutputStream(fileInStateLocation);

      byte[] buf = new byte[2048];
      int bytesRead = 0;
      while ((bytesRead = is.read(buf)) != -1) {
        os.write(buf, 0, bytesRead);
      }
    } finally {
      closeStreams(is, os);
    }
  }

  private static void closeStreams(InputStream is, OutputStream os)
      throws IOException {
    if (os != null) {
      os.close();
    }

    if (is != null) {
      is.close();
    }
  }
  
  private boolean imageRegistryCreated = false;

  public Image getImage(String imageId) {
    return getImageRegistry().get(imageId);
  }

  public ImageDescriptor getImageDescriptor(String imageId) {
    return getImageRegistry().getDescriptor(imageId);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    if (imageRegistryCreated) {
      getImageRegistry().dispose();
    }

    super.stop(context);
  }

  protected ImageDescriptor imageDescriptorFromPath(String imageFilePath) {
    return imageDescriptorFromPlugin(getPluginID(), imageFilePath);
  }

  /**
   * Subclasses that override this method must call
   * super.initializeImageRegistry as well.
   */
  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    imageRegistryCreated = true;
  }

  private String getPluginID() {
    return getBundle().getSymbolicName();
  }
}
