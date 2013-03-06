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
package com.google.gdt.eclipse.gph.util;

import com.google.gdt.eclipse.gph.model.GPHProject;

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

import java.util.List;

/**
 * A control used to display a GPHProject.
 * 
 */
public class ProjectViewer {
  private static Font BOLD_FONT;

  private static Font SUMMARY_FONT;

  private Composite mainComposite;

  private Label descriptionText;

  private Form form;

  private Composite labelsComposite;

  private Label licenseLabel;

  private Link moreInfoLink;

  private GPHProject project;

  private ScrolledComposite scrolledComposite;

  private Label summaryLabel;

  /**
   * 
   * @param parent
   */
  public ProjectViewer(final Composite parent) {
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
        BrowserHelper.openUrl(mainComposite.getShell(), e.text);
      }
    });

    Label spacer = new Label(bottomRow, SWT.NONE);
    spacer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    licenseLabel = new Label(bottomRow, SWT.NONE);
    licenseLabel.setText("");

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

  public GPHProject getProject() {
    return project;
  }

  public void setProject(GPHProject project) {
    this.project = project;

    if (project == null) {
      form.setText("");
      summaryLabel.setText("");

      moreInfoLink.setText("");
      licenseLabel.setText("");

      descriptionText.setText("");

      removeAllControls(labelsComposite);
    } else {
      form.setText(noNulls(project.getName()));
      summaryLabel.setText(noNulls(project.getSummary()));

      if (project.getHtmlLink() != null) {
        moreInfoLink.setText("<a href=\"" + project.getHtmlLink()
            + "\">more info...</a>");
      } else {
        moreInfoLink.setText("");
      }

      licenseLabel.setText(noNulls(project.getLicenseType()));

      String plainText = WikiUtils.convertToText(noNulls(project.getDescription()));

      descriptionText.setText(plainText);

      removeAllControls(labelsComposite);

      if (project.getRole() != null) {
        addLabel("Your role", project.getRole());
      }

      if (project.getLabels().size() > 0) {
        addLabel("Labels", createLabelString(project.getLabels()));
      }

      if (project.getDomain() != null) {
        addLabel("Domain", project.getDomain());
      }
    }

    // Magical incantations.
    labelsComposite.layout(true);

    descriptionText.getParent().layout(true);

    licenseLabel.getParent().layout(true);

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

  private String createLabelString(List<String> list) {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < list.size(); i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(list.get(i));
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
