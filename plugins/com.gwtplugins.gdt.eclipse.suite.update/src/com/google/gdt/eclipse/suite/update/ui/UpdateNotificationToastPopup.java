/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.update.ui;

import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.resources.GdtImages;

import org.eclipse.jface.notifications.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link AbstractNotificationPopup} for the lower portion of the status bar.
 */
@SuppressWarnings("restriction")
public class UpdateNotificationToastPopup extends AbstractNotificationPopup {
  /**
   * This method is called whenever the notification area in the trim
   * contribution is visible and is clicked on.
   */
  public interface NotificationControlSelectedHandler {
    void onNotificationControlSelected();
  }

  private List<NotificationControlSelectedHandler> handlers = new ArrayList<NotificationControlSelectedHandler>();

  public UpdateNotificationToastPopup(Display display) {
    super(display);
  }

  public void addNotificationControlSelectedHandler(
      NotificationControlSelectedHandler handler) {
    handlers.add(handler);
  }

  public void removeNotificationControlSelectedHandler(
      NotificationControlSelectedHandler handler) {
    handlers.remove(handler);
  }

  @Override
  protected void createContentArea(Composite parent) {
    Composite googleUpdatesAvailablePanel = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    googleUpdatesAvailablePanel.setLayout(layout);

    MouseListener mouseListener = new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        fireNotificationControlSelected();
      }
    };

    googleUpdatesAvailablePanel.addMouseListener(mouseListener);

    Link updatesAvailableLink = new Link(googleUpdatesAvailablePanel, SWT.NONE);
    updatesAvailableLink.setText("<a href=\"#\">GWT updates available...</a>");
    updatesAvailableLink.setToolTipText("Updates area available for the GWT Plugin. Click here for more information.");
    updatesAvailableLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fireNotificationControlSelected();
      }
    });
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return GdtPlugin.getDefault().getImage(GdtImages.GDT_ICON);
  }

  @Override
  protected String getPopupShellTitle() {
    // Return a custom title
    return "GWT Plugin";
  }

  private void fireNotificationControlSelected() {
    for (NotificationControlSelectedHandler handler : handlers) {
      handler.onNotificationControlSelected();
    }
  }

}
