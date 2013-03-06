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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.ui.controls.BaseSelectableControl;
import com.google.gdt.eclipse.core.ui.controls.SelectableControl;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.Messages;
import com.google.gdt.eclipse.managedapis.Resources;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.impl.IconCache;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents an item in the ApiListingViewer. Notably, this item subclasses
 * Composite rather than Item. This class wraps (immutably) a Data element,
 * handles rendering of children elements (e.g. icon, description, etc.), and
 * handles event propagation for mouse events to ensure click (and double-click)
 * events can be captured at a componentlevel.
 */
public class ApiListingItem extends BaseSelectableControl implements SelectableControl {

  private static final int IMAGE_SIZE = 48;
  private static final String PREFERRED_TEXT = "\nPreferred";

  private static Font BOLD_FONT;

  private Resources resources;

  private Color defaultBackgroundColor;
  private Color selectedBackgroundColor;

  /**
   * Create a new instance of the receiver with the specified parent, style and
   * info object/
   */
  public ApiListingItem(Composite parent, ManagedApiEntry entry, Resources resources) {
    super(parent, SWT.NONE);

    assert null != entry : "Listing can not be null";
    super.setData(entry);

    assert null != resources : "Listing can not be null";
    this.resources = resources;

    setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    setBackground(parent.getBackground());
    defaultBackgroundColor = parent.getBackground();
    selectedBackgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
    initControl(entry);
  }

  @Override
  public ManagedApiEntry getData() {
    return (ManagedApiEntry) super.getData();
  }

  @Override
  public void setBackground(Color color) {
    super.setBackground(color);
    Control[] children = getChildren();
    for (Control child : children) {
      child.setBackground(color);
    }
  }

  /**
   * Do not call. The data is an invariant in a ListItem.
   * 
   * @param data
   * @throws IllegalArgumentException Do not call.
   */
  @Override
  public void setData(Object data) {
    throw new IllegalArgumentException();
  }

  public void setDefaultBackgroundColor(Color defaultBackgroundColor) {
    this.defaultBackgroundColor = defaultBackgroundColor;
  }

  @Override
  public void setSelected(boolean selected) {
    boolean oldSelectedStatus = isSelected();
    super.setSelected(selected);
    if (oldSelectedStatus != isSelected()) {
      getData().setSelected(selected);
      updateBackground();
    }
  }

  public void setSelectedBackgroundColor(Color selectedBackgroundColor) {
    this.selectedBackgroundColor = selectedBackgroundColor;
  }

  public void updateBackground() {
    if (isSelected()) {
      updateBackground(this, selectedBackgroundColor);
    } else {
      updateBackground(this, defaultBackgroundColor);
    }
  }

  private Label createDescriptionLabel(Composite parent, ManagedApiEntry entry) {
    Label descriptionLabel = new Label(parent, SWT.WRAP);
    descriptionLabel.setBackground(null);
    StringBuffer descriptionLabelText = new StringBuffer();
    descriptionLabelText.append(stripCRLF(entry.getDescription()));

    descriptionLabel.setText(descriptionLabelText.toString());

    if (entry.isInstalled() && !entry.isUpdateAvailable()) {
      descriptionLabel.setForeground(
          parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
    }

    listenTo(descriptionLabel);

    return descriptionLabel;
  }

  private Image createGrayImage(Image image) {
    ImageDescriptor descriptor = ImageDescriptor.createWithFlags(
        ImageDescriptor.createFromImage(image), SWT.IMAGE_GRAY);

    return resources.getResourceManager().createImage(descriptor);
  }

  private Label createIconComponent(Composite parent, ManagedApiEntry entry) {
    Label iconLabel = new Label(parent, SWT.NONE);
    GridDataFactory.swtDefaults()
        .align(SWT.CENTER, SWT.BEGINNING)
        .hint(IMAGE_SIZE, SWT.DEFAULT)
        .minSize(IMAGE_SIZE, SWT.DEFAULT)
        .indent(0, 3)
        .applyTo(iconLabel);

    try {
      Image apiIcon = null;
      URL iconLink = entry.getIconLink(ManagedApiPlugin.ICON_KEY_API_IMPORT);
      if (iconLink != null) {
        IconCache iconCache = ManagedApiPlugin.getDefault().getIconCache();
        if (iconCache != null) {
          URL localURL = iconCache.getLocalImageURL(iconLink.toExternalForm());
          if (localURL != null) {
            apiIcon = resources.getAPIIconForUrl(iconLink);
          }
        }

        try {
          apiIcon = resources.getAPIIconForUrl(
              entry.getIconLink(ManagedApiPlugin.ICON_KEY_API_IMPORT));
        } catch (DeviceResourceException e) {
          ManagedApiLogger.warn("Unable to load icon for "
              + entry.getIconLink(ManagedApiPlugin.ICON_KEY_API_IMPORT));
        } catch (MalformedURLException e) {
          ManagedApiLogger.warn(Messages.MalformedUrl + " for API Icon");
        }
      }

      Image image = apiIcon != null
          ? apiIcon : resources.getAPIDefaultIcon32Image();

      if (entry.isInstalled()) {
        if (entry.isUpdateAvailable()) {
          iconLabel.setImage(createUpdateAvailableImage(image));
        } else {
          iconLabel.setImage(createGrayImage(image));
        }
      } else {
        iconLabel.setImage(image);
      }
    } catch (MalformedURLException e) {
      ManagedApiLogger.warn(Messages.MalformedUrl + " while API Icon");
    }

    listenTo(iconLabel);

    return iconLabel;
  }

  private Label createTitleLabel(Composite parent, ManagedApiEntry entry) {
    Label nameLabel = new Label(parent, SWT.NONE);
    nameLabel.setFont(getBoldFont(nameLabel.getFont()));

    if (entry.isInstalled()) {
      if (entry.isUpdateAvailable()) {
        nameLabel.setText(entry.getDisplayName() + " (update available)");
      } else {
        nameLabel.setText(entry.getDisplayName() + " (installed)");
      }
    } else {
      nameLabel.setText(entry.getDisplayName());
    }

    listenTo(nameLabel);

    return nameLabel;
  }

  private Image createUpdateAvailableImage(Image image) {
    try {
      ImageDescriptor overlayIcon = resources.getUpdateAvailableOverlay16ImageDescriptor();

      DecorationOverlayIcon overlayDescriptor = new DecorationOverlayIcon(
          image, overlayIcon, IDecoration.TOP_LEFT);

      return resources.getResourceManager().createImage(overlayDescriptor);
    } catch (MalformedURLException e) {
      return image;
    }
  }

  private Label createVersionLabel(Composite parent, ManagedApiEntry entry) {
    Label versionLabel = new Label(parent, SWT.NONE);
    versionLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_GRAY));

    String text;

    if (entry.hasDirectoryEntry()) {
      text = entry.getDirectoryEntryVersion();
      if (entry.getDirectoryEntry().isPreferred()) {
        text += PREFERRED_TEXT;
      }
    } else {
      text = entry.getInstalledVersion();
      if (entry.getInstalled().isPreferred()) {
        text += PREFERRED_TEXT;
      }
    }

    if (!text.startsWith("v")) {
      text = "v" + text;
    }

    versionLabel.setText(text);

    listenTo(versionLabel);

    return versionLabel;
  }

  private Font getBoldFont(Font templateFont) {
    if (BOLD_FONT == null) {
      FontData fontData = templateFont.getFontData()[0];

      BOLD_FONT = new Font(templateFont.getDevice(), new FontData(
          fontData.getName(), fontData.getHeight(), SWT.BOLD));
    }

    return BOLD_FONT;
  }

  private void initControl(ManagedApiEntry entry) {
    GridLayoutFactory.fillDefaults()
        .numColumns(3)
        .equalWidth(false)
        .extendedMargins(2, 0, 2, 2)
        .spacing(2, 0)
        .applyTo(this);

    Label iconLabel = createIconComponent(this, entry);
    ((GridData) iconLabel.getLayoutData()).verticalSpan = 2;

    Label nameLabel = createTitleLabel(this, entry);
    GridDataFactory.fillDefaults()
        .grab(true, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(nameLabel);

    Label versionLabel = createVersionLabel(this, entry);
    versionLabel.setAlignment(SWT.RIGHT);
    GridDataFactory.fillDefaults()
        .grab(true, false).align(SWT.END, SWT.CENTER).applyTo(versionLabel);

    Label description = createDescriptionLabel(this, entry);
    // TODO: I'd like ellipses at the end of truncated labels -
    int lineHeight = description.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
    GridDataFactory.fillDefaults()
        .span(2, 1)
        .grab(true, false)
        .align(SWT.FILL, SWT.FILL)
        .hint(100, lineHeight * 3)
        .applyTo(description);

    updateBackground();

    listenTo(this);
  }

  private void updateBackground(Composite composite, Color background) {
    composite.setBackground(background);

    for (Control control : composite.getChildren()) {
      if (control instanceof Composite) {
        updateBackground((Composite) control, background);
      } else {
        control.setBackground(background);
      }
    }
  }

}
