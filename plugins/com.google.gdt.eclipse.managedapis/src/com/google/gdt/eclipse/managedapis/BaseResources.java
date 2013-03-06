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
package com.google.gdt.eclipse.managedapis;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A base method for defining access to SWT resources used by the plugin.
 */
public abstract class BaseResources implements Resources {
  private RGB disabledColorDescriptor;
  private Display display;
  private FontDescriptor h2FontDescriptor;
  private boolean initialized = false;
  private LocalResourceManager resourceManager;

  public BaseResources(Display display) {
    this.display = display;
    doInitResources();
  }

  public Image getAPIDefaultIcon32Image() throws MalformedURLException {
    URL infoIconUrl = getURLForLocalPath(ManagedApiConstants.ICON_32_GOOGLE_API_DEFAULT);
    return getImageForUrl(infoIconUrl);
  }

  /**
   * Returns an Image object for the resource at the referenced href.
   * 
   * @throws MalformedURLException
   */
  public Image getAPIIconForUrl(URL url) throws MalformedURLException {
    ensureInitialized("Unable to load ResourceManager from non-UI thread.");
    return getImageForUrl(url);
  }

  public Color getColorDisabled() {
    ensureInitialized("Unable to load ResourceManager from non-UI thread.");
    return resourceManager.createColor(disabledColorDescriptor);
  }

  public Display getDisplay() {
    return display;
  }

  /**
   * Provides method to access the 16x16 icon used for the classpath container.
   */
  public ImageDescriptor getGoogleAPIContainerIcon16ImageDescriptor()
      throws MalformedURLException {
    URL updateAvailableImageUrl = getURLForLocalPath(ManagedApiConstants.ICON_16_GOOGLE_API_CONTAINER);
    return ImageDescriptor.createFromURL(updateAvailableImageUrl);
  }

  public Image getInfoIcon16Image() throws MalformedURLException {
    URL infoIconUrl = getURLForLocalPath(ManagedApiConstants.ICON_16_MESSAGE_INFO);
    return getImageForUrl(infoIconUrl);
  }

  public ImageDescriptor getManagedApiImportIcon() throws MalformedURLException {
    URL updateAvailableImageUrl = getURLForLocalPath(ManagedApiConstants.ICON_WIZARD_MANAGED_API_IMPORT);
    return ImageDescriptor.createFromURL(updateAvailableImageUrl);
  }

  public LocalResourceManager getResourceManager() {
    ensureInitialized("Unable to load ResourceManager from non-UI thread.");
    return resourceManager;
  }

  public Font getSmallHeaderFont() {
    ensureInitialized("Unable to load font from non-UI thread.");
    return resourceManager.createFont(h2FontDescriptor);
  }

  /**
   * Provides method to access the 16x16 icon used to overlay the classpath
   * container and JARs to denote the availability of an update.
   */
  public ImageDescriptor getUpdateAvailableOverlay16ImageDescriptor()
      throws MalformedURLException {
    URL updateAvailableImageUrl = getURLForLocalPath(ManagedApiConstants.OVERLAY_16_UPDATE_AVAILABLE);
    return ImageDescriptor.createFromURL(updateAvailableImageUrl);
  }

  public abstract URL getURLForLocalPath(String string)
      throws MalformedURLException;

  /**
   * Method produces a font descriptor for the specified style and size. This
   * method should only be called from a UI thread; the doInitResources() method
   * ensures that this constraint is met.
   */
  private FontDescriptor createFontDescriptor(int style, float heightMultiplier) {
    Font baseFont = JFaceResources.getDialogFont();
    FontData[] fontData = baseFont.getFontData();
    FontData[] newFontData = new FontData[fontData.length];
    for (int i = 0; i < newFontData.length; i++) {
      newFontData[i] = new FontData(fontData[i].getName(),
          (int) (fontData[i].getHeight() * heightMultiplier),
          fontData[i].getStyle() | style);
    }
    return FontDescriptor.createFrom(newFontData);
  }

  private void doInitResources() {
    if (null != Display.getCurrent()) {
      resourceManager = new LocalResourceManager(
          JFaceResources.getResources(display));
      disabledColorDescriptor = new RGB(0x69, 0x69, 0x69);
      h2FontDescriptor = createFontDescriptor(SWT.BOLD, 1.25f);
      initialized = true;
    }
  }

  private void ensureInitialized(String errorMsg) {
    if (!initialized) {
      doInitResources();
      if (!initialized) {
        throw new SWTException(errorMsg);
      }
    }
  }

  private Image getImageForUrl(URL url) {
    Image image = null;
    try {
      ensureInitialized("Unable to load image from non-UI thread.");
      ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
      image = resourceManager.createImage(descriptor);
    } catch (SWTException e) {
      ManagedApiLogger.warn(e, "Load Image exception:");
    }
    return image;
  }
}
