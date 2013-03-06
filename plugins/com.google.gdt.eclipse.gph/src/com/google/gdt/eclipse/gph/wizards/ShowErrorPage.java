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
package com.google.gdt.eclipse.gph.wizards;

import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A general error page for wizards.
 */
public class ShowErrorPage extends AbstractWizardPage {
  private static Font BOLD_FONT;

  private static final String DEFAULT_MESSAGE = "Unable to import the project.";

  private IStatus status;

  public ShowErrorPage(IWizard wizard, IStatus status) {
    super("showErrorPage");

    this.status = status;

    setTitle(wizard.getWindowTitle());
    setDescription(DEFAULT_MESSAGE);
  }

  public ShowErrorPage(IWizard wizard, String message) {
    this(wizard, new Status(IStatus.ERROR, ProjectHostingUIPlugin.PLUGIN_ID,
        message));
  }

  public ShowErrorPage(IWizard wizard, Throwable exception) {
    this(wizard, new Status(IStatus.ERROR, ProjectHostingUIPlugin.PLUGIN_ID,
        exception.getMessage(), exception));
  }

  @Override
  protected Control createPageContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().margins(10, 10).numColumns(2).applyTo(
        composite);

    Label iconLabel = new Label(composite, SWT.NONE);
    iconLabel.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR));

    Label titleLabel = new Label(composite, SWT.NONE);
    titleLabel.setText(status.getMessage());
    titleLabel.setFont(getBoldFont(titleLabel.getFont()));
    GridDataFactory.fillDefaults().grab(true, false).applyTo(titleLabel);

    // spacer
    new Label(composite, SWT.NONE);

    if (status.isMultiStatus() || status.getException() != null) {
      final Link link = new Link(composite, SWT.NONE);
      link.setText("<a>show details...</a>");
      link.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          replaceWithDetails(link);
        }
      });
      GridDataFactory.fillDefaults().grab(true, true).applyTo(link);
    }
    
    return composite;
  }

  protected void replaceWithDetails(Control control) {
    Composite parent = control.getParent();

    control.setVisible(false);
    GridDataFactory.swtDefaults().exclude(true).applyTo(control);

    TextViewer viewer = new TextViewer(parent, SWT.READ_ONLY | SWT.WRAP
        | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    viewer.setDocument(new Document(getDetailsText()));
    viewer.getControl().setBackground(parent.getBackground());
    GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer.getControl());

    parent.layout();
  }

  private Font getBoldFont(Font templateFont) {
    if (BOLD_FONT == null) {
      FontData fontData = templateFont.getFontData()[0];

      BOLD_FONT = new Font(templateFont.getDevice(), new FontData(
          fontData.getName(), fontData.getHeight(), SWT.BOLD));
    }

    return BOLD_FONT;
  }

  private String getDetailsText() {
    StringWriter stringWriter = new StringWriter();

    PrintWriter writer = new PrintWriter(stringWriter);

    print(status, writer);

    return stringWriter.toString().trim();
  }

  private void print(IStatus status, PrintWriter writer) {
    writer.println(status.toString());

    if (status.getException() != null) {
      writer.println();
      status.getException().printStackTrace(writer);
    }

    if (status.isMultiStatus()) {
      for (IStatus child : status.getChildren()) {
        writer.println();
        print(child, writer);
      }
    } else {
      if (status.getException() != null) {
        writer.println();
        status.getException().printStackTrace(writer);

        if (status.getException().getCause() != null
            && status.getException().getCause() != status.getException()) {
          writer.println();
          status.getException().getCause().printStackTrace(writer);
        }
      }
    }
  }

}
