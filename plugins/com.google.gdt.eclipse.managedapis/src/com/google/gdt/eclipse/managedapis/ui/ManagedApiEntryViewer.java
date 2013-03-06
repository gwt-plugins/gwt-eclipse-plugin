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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * A control used to display a {@link ManagedApiEntry}.
 */
public class ManagedApiEntryViewer {
  private static Font BOLD_FONT;

  private static Font SUMMARY_FONT;

  private Composite mainComposite;

  private Label descriptionText;

  private Form form;

  private Composite labelsComposite;

  private ManagedApiEntry managedApiEntry;

  private Link moreInfoLink;

  private Label publisherLabel;

  private ScrolledComposite scrolledComposite;

  private Label summaryLabel;

  /**
   * Create a new ManagedApiEntryViewer.
   * 
   * @param parent
   */
  public ManagedApiEntryViewer(final Composite parent) {
    mainComposite = new Composite(parent, SWT.BORDER);
    GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(mainComposite);

    // Set up the Forms UI.
    FormToolkit toolkit = new FormToolkit(mainComposite.getDisplay());
    form = toolkit.createForm(mainComposite);
    toolkit.decorateFormHeading(form);
    summaryLabel = toolkit.createLabel(form.getHead(), "", SWT.NONE);
    summaryLabel.setBackground(null);
    summaryLabel.setForeground(form.getForeground());
    summaryLabel.setFont(getSummaryFont(form.getFont(), summaryLabel.getFont()));
    form.setHeadClient(summaryLabel);
    form.setText("");

    form.setLayoutData(new GridData(GridData.FILL_BOTH));

    // A horizontal line between the description area and the bottom bar.
    Label separator = new Label(mainComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // Create a bottom bar (to hold the info link and the license label).
    Composite bottomRow = new Composite(mainComposite, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(3).margins(2, 2).applyTo(
        bottomRow);
    bottomRow.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    moreInfoLink = new Link(bottomRow, SWT.NONE);
    moreInfoLink.setText("");
    moreInfoLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtilities.launchBrowserAndHandleExceptions(e.text);
      }
    });

    Label spacer = new Label(bottomRow, SWT.NONE);
    spacer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    publisherLabel = new Label(bottomRow, SWT.NONE);
    publisherLabel.setText("");

    // Create the description area.
    form.getBody().setLayout(new FillLayout());
    scrolledComposite = new ScrolledComposite(form.getBody(), SWT.V_SCROLL);
    scrolledComposite.setBackground(parent.getDisplay().getSystemColor(
        SWT.COLOR_LIST_BACKGROUND));

    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setMinWidth(100);
    scrolledComposite.setMinHeight(100);
    scrolledComposite.setExpandVertical(true);
    scrolledComposite.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        updateScrolledComposite();
      }
    });

    final Composite bodyComposite = toolkit.createComposite(scrolledComposite);
    scrolledComposite.setContent(bodyComposite);
    GridLayoutFactory.fillDefaults().extendedMargins(5, 3, 5, 5).applyTo(
        bodyComposite);

    labelsComposite = toolkit.createComposite(bodyComposite);
    labelsComposite.setBackgroundMode(SWT.INHERIT_DEFAULT);
    GridLayoutFactory.fillDefaults().applyTo(labelsComposite);
    GridDataFactory.fillDefaults().applyTo(labelsComposite);

    descriptionText = new Label(bodyComposite, SWT.WRAP);
    descriptionText.setBackground(parent.getDisplay().getSystemColor(
        SWT.COLOR_LIST_BACKGROUND));

    GridDataFactory.fillDefaults().grab(true, true).applyTo(descriptionText);
  }

  public final Control getControl() {
    return mainComposite;
  }

  public ManagedApiEntry getManagedApiEntry() {
    return managedApiEntry;
  }

  public void setManagedApiEntry(ManagedApiEntry managedApiEntry) {
    this.managedApiEntry = managedApiEntry;

    if (managedApiEntry == null) {
      form.setText("");
      summaryLabel.setText("");

      moreInfoLink.setText("");
      publisherLabel.setText("");

      descriptionText.setText("");

      removeAllControls(labelsComposite);
    } else {
      if (managedApiEntry.isInstalled()) {
        if (managedApiEntry.isUpdateAvailable()) {
          form.setText(managedApiEntry.getDisplayName() + " - update available");
        } else {
          form.setText(managedApiEntry.getDisplayName() + " (installed)");
        }
      } else {
        form.setText(managedApiEntry.getDisplayName());
      }

      // Is the API id or the link better? Which will users know it as?
      // summaryLabel.setText(managedApiEntry.getDocumentationLink().toString());
      summaryLabel.setText(noNulls(managedApiEntry.getName()));

      if (managedApiEntry.getDocumentationLink() != null) {
        moreInfoLink.setText("<a href=\""
            + managedApiEntry.getDocumentationLink()
            + "\">API documentation</a>");
      } else {
        moreInfoLink.setText("");
      }

      publisherLabel.setText(getPublisherText(managedApiEntry));

      descriptionText.setText(noNulls(managedApiEntry.getDescription()));

      removeAllControls(labelsComposite);

      if (managedApiEntry.isInstalled() && managedApiEntry.isUpdateAvailable()) {
        addLabel("Available version", managedApiEntry.getDirectoryEntryVersion());
      } else {
        if (managedApiEntry.getDirectoryEntryVersion() != null) {
          addLabel("Version", managedApiEntry.getDirectoryEntryVersion());
        }
      }
      
      if (managedApiEntry.getDirectoryEntryLabels().length > 0) {
        addLabel("Labels",
            createLabelString(managedApiEntry.getDirectoryEntryLabels()));
      }
    }

    // Magical incantations.
    labelsComposite.layout(true);

    descriptionText.getParent().layout(true);

    publisherLabel.getParent().layout(true);

    mainComposite.layout(true);

    updateScrolledComposite();

    mainComposite.redraw();
  }

  private void addLabel(String title, String text) {
    Composite composite = new Composite(labelsComposite, SWT.NONE);
    composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
    GridLayoutFactory.fillDefaults().spacing(5, 0).numColumns(2).applyTo(
        composite);

    Label titleLabel = new Label(composite, SWT.NONE);
    titleLabel.setFont(getBoldFont(titleLabel.getFont()));
    titleLabel.setText(title);
    titleLabel.setBackground(null);

    Label textLabel = new Label(composite, SWT.NONE);
    textLabel.setText(text);
    titleLabel.setBackground(null);
  }

  private String createLabelString(String[] labels) {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < labels.length; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(labels[i]);
    }

    return builder.toString();
  }

  private Font getBoldFont(Font templateFont) {
    if (BOLD_FONT == null) {
      FontData fontData = templateFont.getFontData()[0];

      BOLD_FONT = new Font(templateFont.getDevice(), new FontData(
          fontData.getName(), fontData.getHeight(), SWT.BOLD));
    }

    return BOLD_FONT;
  }

  private String getPublisherText(ManagedApiEntry entry) {
    StringBuilder builder = new StringBuilder();

    if (entry.getPublisher() != null) {
      builder.append("published by " + entry.getPublisher());
    }

    return builder.toString();
  }

  private Font getSummaryFont(Font titleFont, Font normalFont) {
    if (SUMMARY_FONT == null) {
      FontData titleFontData = titleFont.getFontData()[0];
      FontData normalFontData = normalFont.getFontData()[0];

      SUMMARY_FONT = new Font(titleFont.getDevice(), new FontData(
          titleFontData.getName(), normalFontData.getHeight(), SWT.NORMAL));
    }

    return SUMMARY_FONT;
  }

  private String noNulls(String str) {
    if (str == null) {
      return "";
    } else {
      return str;
    }
  }

  private void removeAllControls(Composite composite) {
    for (Control control : composite.getChildren()) {
      control.dispose();
    }
  }

  private void updateScrolledComposite() {
    Rectangle clientArea = scrolledComposite.getClientArea();
    Control content = scrolledComposite.getContent();

    Point size = content.computeSize(clientArea.width, SWT.DEFAULT, true);

    scrolledComposite.setMinHeight(size.y);
    scrolledComposite.getVerticalBar().setIncrement(20);
    scrolledComposite.getVerticalBar().setPageIncrement(clientArea.height);
  }

}
