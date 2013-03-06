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
package com.google.gdt.eclipse.core.ui.controls.support;

import com.google.gdt.eclipse.core.ui.controls.BaseSelectableControl;
import com.google.gdt.eclipse.core.ui.controls.SelectableControl;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

/**
 * NOTE: This code is only for demonstration.
 */
public class ToolkitControl extends BaseSelectableControl implements
    SelectableControl {
  private static Font BOLD_FONT;

  private static Font getBoldFont(Font font) {
    if (BOLD_FONT == null) {
      FontData data = font.getFontData()[0];
      data.setStyle(SWT.BOLD);
      BOLD_FONT = new Font(Display.getDefault(), data);
    }

    return BOLD_FONT;
  }

  private WBToolkit wbToolkit;
  private Color defaultBackgroundColor;
  private Color selectedBackgroundColor;

  /**
   * @param parent
   * @param style
   */
  public ToolkitControl(Composite parent, WBToolkit wbToolkit) {
    super(parent, SWT.NONE);

    this.wbToolkit = wbToolkit;

    setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    setBackground(parent.getBackground());
    defaultBackgroundColor = parent.getBackground();
    selectedBackgroundColor = Display.getDefault().getSystemColor(
        SWT.COLOR_LIST_SELECTION);

    initControl();
  }

  public void setDefaultBackground(Color background) {
    this.defaultBackgroundColor = background;
    updateBackground();
  }

  @Override
  public void setSelected(boolean selected) {
    super.setSelected(selected);
    updateBackground();
  }

  public void setSelectedBackground(Color background) {
    this.selectedBackgroundColor = background;
    updateBackground();
  }

  public void updateBackground() {
    Color background;

    if (isSelected()) {
      background = selectedBackgroundColor;
    } else {
      background = defaultBackgroundColor;
    }

    updateBackground(this, background);
  }

  protected WBToolkit getToolkit() {
    return wbToolkit;
  }

  private void initControl() {
    GridLayoutFactory.fillDefaults().numColumns(3).spacing(3, 2).applyTo(this);
    GridLayout layout = (GridLayout) this.getLayout();
    layout.marginLeft = 7;
    layout.marginTop = 2;
    layout.marginBottom = 2;

    Label iconLabel = new Label(this, SWT.NULL);
    iconLabel.setBackground(getBackground());
    GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.BEGINNING).span(1, 2).applyTo(
        iconLabel);
    listenTo(iconLabel);

    Link nameLabel = new Link(this, SWT.NONE);
    nameLabel.setText(wbToolkit.getName());
    nameLabel.setBackground(getBackground());
    nameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    nameLabel.setFont(getBoldFont(getFont()));
    listenTo(nameLabel);

    Label providerLabel = new Label(this, SWT.NONE);
    providerLabel.setText(wbToolkit.getProviderDescription());
    providerLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    providerLabel.setBackground(getBackground());
    providerLabel.setForeground(Display.getDefault().getSystemColor(
        SWT.COLOR_GRAY));
    listenTo(providerLabel);

    Label summaryLabel = new Label(this, SWT.WRAP);
    String description = stripCRLF(wbToolkit.getDescription());
    summaryLabel.setText(description);
    summaryLabel.setBackground(getBackground());
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1).hint(100,
        SWT.DEFAULT).applyTo(summaryLabel);
    listenTo(summaryLabel);

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
