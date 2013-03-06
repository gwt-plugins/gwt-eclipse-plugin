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
package com.google.gdt.eclipse.login.ui;

import com.google.gdt.eclipse.login.GoogleLogin;
import com.google.gdt.eclipse.login.GoogleLoginPlugin;
import com.google.gdt.eclipse.login.GoogleLoginPrefs;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import java.net.URL;

/**
 * 
 */
public class LoginTrimContribution extends WorkbenchWindowControlContribution {

  private interface RecursiveRunner {
    void runOnControl(Control c);
  }

  private static final String CLICK_TO_CONNECT_MSG = "Click to connect.";
  private static final String CLICK_TO_SIGN_IN_MSG = "Click to sign in";
  private static final String CLICK_TO_SIGN_OUT_MSG = "Click to sign out";
  private static final Image LOGGED_IN_ICON;
  private static final Image LOGGED_OUT_ICON;
  private static final String NOT_CONNECTED_MSG = "Not connected";
  private static final String SIGN_IN_MSG = "Signed in";
  private static final String SIGN_IN_TO_GOOGLE_MSG = "Sign in to Google...";
  private static final String SIGNED_IN_AS_MSG = "Signed in as ";
  
  static {
    LOGGED_IN_ICON = getIcon("google_logged_in.gif");
    LOGGED_OUT_ICON = getIcon("google_logged_out.gif");
  }

  private static Image getIcon(String name) {
    try {
      URL url = new URL(
          GoogleLoginPlugin.getDefault().getBundle().getEntry("/"), "icons/"
              + name);
      return ImageDescriptor.createFromURL(url).createImage();
    } catch (Exception e) {
      GoogleLoginPlugin.logError("Could not load icon '" + name + "'", e);
      return null;
    }
  }

  // only use this on the UI thread! >:|
  private boolean drawBorder = false;

  private MenuItem iconOnlyMenuItem;
  
  private Label loginImg;
  
  private Label loginMsg;

  private Composite loginTrim;

  private MenuItem logoutOnExitMenuItem;

  private final MouseAdapter mouseListener = new MouseAdapter() {
    @Override
    public void mouseUp(MouseEvent e) {
      if (e.button == 1) {
        if (GoogleLogin.getInstance().isConnected()) {
          if (GoogleLogin.getInstance().isLoggedIn()) {
            GoogleLogin.getInstance().logOut();
          } else {
            GoogleLogin.getInstance().logIn();
          }
        }
        updateUi();
        updateToolTips();
      }
    }
  };

  private final MouseTrackListener mouseTrackListener = new MouseTrackAdapter() {
    @Override
    public void mouseEnter(MouseEvent e) {
      updateToolTips();
      drawBorder = true;
      loginTrim.redraw();
    }

    @Override
    public void mouseExit(MouseEvent e) {
      drawBorder = false;
      loginTrim.redraw();
    }
  };

  private Menu rightClickMenu;
  
  public LoginTrimContribution() {
    GoogleLogin.getInstance().setLoginTrimContribution(this);
  }

  @Override
  public void dispose() {
    GoogleLogin.getInstance().setLoginTrimContribution(null);
  }

  public String getLoginMessage() {
    if (iconOnlyMenuItem.getSelection()) {
      return "";
    } else {

      if (!GoogleLogin.getInstance().isConnected()) {
        return NOT_CONNECTED_MSG;
      }

      if (GoogleLogin.getInstance().isLoggedIn()) {
        String email = GoogleLogin.getInstance().getEmail();
        if (email != null) {
          return email;
        } else {
          return SIGN_IN_MSG;
        }
      } else {
        return SIGN_IN_TO_GOOGLE_MSG;
      }
    }
  }

  public String getLoginToolTipText() {

    StringBuffer sb = new StringBuffer();
    if (GoogleLogin.getInstance().isConnected()) {
      if (GoogleLogin.getInstance().isLoggedIn()) {
        String email = GoogleLogin.getInstance().getEmail();
        if (email != null && email.length() != 0) {
          sb.append(SIGNED_IN_AS_MSG);
          sb.append(GoogleLogin.getInstance().getEmail());
          sb.append(". ");
        }
        sb.append(CLICK_TO_SIGN_OUT_MSG);
      } else {
        sb.append(CLICK_TO_SIGN_IN_MSG);
      }
    } else {
      sb.append(CLICK_TO_CONNECT_MSG);
    }

    return sb.toString();
  }

  public boolean getLogoutOnExitChecked() {
    return logoutOnExitMenuItem.getSelection();
  }

  public void updateUi() {

    if (!GoogleLogin.getInstance().isConnected()) {
      setLoginImage(LOGGED_OUT_ICON);
    } else if (GoogleLogin.getInstance().isLoggedIn()) {
      setLoginImage(LOGGED_IN_ICON);
    } else {
      setLoginImage(LOGGED_OUT_ICON);
    }

    String msg = getLoginMessage();

    if (!loginMsg.getText().equals(msg)) {
      loginMsg.setText(msg);

      /*
       * The following is the magical incantation to get SWT to resize a toolbar
       * after setting the text on a label
       */

      loginMsg.pack();
      Point original = loginTrim.getParent().getSize();

      if (original.y == 0) {
        // on startup, y will be zero, and if zero is passed, SWT will
        // hide the widget, so this just needs to be > 0
        original.y = 16;
      }

      // + 30 for the icon at the left and padding
      Point size = new Point(loginMsg.getSize().x + 30, original.y);

      loginTrim.getParent().setSize(size);
      loginTrim.setSize(size);
      loginTrim.getShell().layout();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.action.ControlContribution#createControl(org.eclipse.
   * swt.widgets.Composite)
   */
  @Override
  protected Control createControl(Composite parent) {

    loginTrim = createLoginTrim(parent);

    loginImg = new Label(loginTrim, SWT.NONE);

    loginMsg = new Label(loginTrim, SWT.NONE);

    createRightClickMenu();

    runOnControlRecursive(loginTrim, new RecursiveRunner() {
      public void runOnControl(Control c) {
        c.setToolTipText(getLoginToolTipText());
        c.addMouseListener(mouseListener);
        c.addMouseTrackListener(mouseTrackListener);
        c.setMenu(rightClickMenu);
      }
    });

    updateUi();
    updateToolTips();

    return loginTrim;
  }

  private Composite createLoginTrim(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    comp.addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent e) {
        if (drawBorder) {
          GC g = e.gc;
          g.setForeground(Display.getCurrent().getSystemColor(
              SWT.COLOR_DARK_GRAY));
          g.drawRectangle(0, 0, e.width - 1, e.height - 1);
        }
      }
    });

    RowLayout layout = new RowLayout(SWT.HORIZONTAL);
    layout.pack = true;
    layout.wrap = false;
    comp.setLayout(layout);
    return comp;
  }
  
  private void createRightClickMenu() {
    rightClickMenu = new Menu(loginTrim);

    iconOnlyMenuItem = new MenuItem(rightClickMenu, SWT.CHECK);
    iconOnlyMenuItem.setText("Icon only");
    iconOnlyMenuItem.setSelection(GoogleLoginPrefs.getIconOnlyPref());
    iconOnlyMenuItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        GoogleLoginPrefs.saveIconOnlyPref(
            iconOnlyMenuItem.getSelection());
        updateUi();
      }
    });

    logoutOnExitMenuItem = new MenuItem(rightClickMenu, SWT.CHECK);
    logoutOnExitMenuItem.setText("Sign out on exit");
    logoutOnExitMenuItem.setSelection(GoogleLoginPrefs.getLogoutOnExitPref());
    logoutOnExitMenuItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        GoogleLoginPrefs.saveLogoutOnExitPref(
            logoutOnExitMenuItem.getSelection());
      }
    });
  }
  
  private void runOnControlRecursive(Control c, RecursiveRunner rr) {
    rr.runOnControl(c);
    if (c instanceof Composite) {
      Composite composite = (Composite) c;
      for (Control child : composite.getChildren()) {
        rr.runOnControl(child);
      }
    }
  }

  private void setLoginImage(Image img) {
    if (loginImg.getImage() != img) {
      loginImg.setImage(img);
    }
  }

  private void updateToolTips() {
    final String tooltip = getLoginToolTipText();
    runOnControlRecursive(loginTrim, new RecursiveRunner() {
      public void runOnControl(Control c) {
        c.setToolTipText(tooltip);
      }
    });
  }

}
