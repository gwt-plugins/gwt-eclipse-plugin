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
package com.google.gwt.eclipse.oophm.views.hierarchical;

import com.google.gwt.eclipse.oophm.model.BrowserTab;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.Server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.PageBook;

import java.util.HashMap;
import java.util.Map;

/**
 * Displays content for a given element.
 */
class ContentPanel extends Composite {
  private final PageBook pageBook;
  private final Map<Object, Control> elements = new HashMap<Object, Control>();
  private final Composite EMPTY_PANEL;

  public ContentPanel(Composite parent, int style) {
    super(parent, style);

    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    setLayout(layout);
    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    setLayoutData(layoutData);

    pageBook = new PageBook(this, SWT.NONE);
    // pageBook.setLayout(new GridLayout());
    pageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    EMPTY_PANEL = new Composite(pageBook, SWT.NONE);
    EMPTY_PANEL.setLayout(new GridLayout());
  }

  public void setSelection(Object element) {
    Control control = null;

    if (element != null) {
      control = elements.get(element);
      if (control == null) {
        if (element instanceof LaunchConfiguration) {
          control = new LaunchConfigurationContent(pageBook,
              (LaunchConfiguration) element);
        } else {
          // TODO: Is it worth considering a "IHasLogs" interface?
          if (element instanceof BrowserTab) {
            control = new LogContent<BrowserTab>(pageBook,
                ((BrowserTab) element).getLog());
          } else if (element instanceof Server) {
            control = new LogContent<Server>(pageBook,
                ((Server) element).getLog());
          }
        }

        elements.put(element, control);
      }
    }

    if (control != null) {
      pageBook.showPage(control);
    } else {
      pageBook.showPage(EMPTY_PANEL);
    }
  }
}