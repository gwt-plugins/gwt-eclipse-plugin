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

import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.login.GoogleLoginPlugin;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A dialog that has an embedded browser for the user to sign in.
 */
public class LoginBrowser extends Dialog {

  public static final int BROWSER_ERROR = -1;
  private static final String DIALOG_TITLE = "Sign in to Google Services";
  private static final boolean IS_MAC_OS = Platform.OS_MACOSX.equals(Platform.getOS());
  private static final String LOGOUT_URL = "https://www.google.com/accounts/Logout";
  private static final String SUCCESS_CODE_PREFIX = "Success code=";
  
  private Browser browser;

  private final LocationAdapter browserLocationAdapter = new LocationAdapter() {

    // This is to prevent the user from navigating anywhere
    // by clicking on the links on the google login pages, which will
    // interrupt the login flow. Open up a real browser for these.
    @Override
    public void changing(LocationEvent event) {

      // Blacklist regex.
      String googlePrefix = "https?://www\\.google\\.com";
      String[] blacklist = {
          googlePrefix + "/?", googlePrefix + "/intl/en/privacy.*",
          googlePrefix + "/accounts/?", googlePrefix + "/support/accounts.*",
          googlePrefix + "/accounts/ManageAccount/?",
          googlePrefix + "/accounts/TOS.*",
          googlePrefix + "/accounts/recovery.*",
          googlePrefix + "/accounts/NewAccount.*"};

      for (String s : blacklist) {
        if (event.location.matches(s)) {
          event.doit = false;
          openBrowser(event.location);
          return;
        }
      }
    }
  };
  
  private final ProgressAdapter browserProgressListener = new ProgressAdapter() {
    @Override
    public void completed(ProgressEvent event) {

      /*
       * When we're done logging in, we set loggingOut to true and send the
       * browser to google's logout url to clear the login cookies. The next
       * time complete() is called, we should be logged out and forwarded to
       * either the corp "logout complete page" or the external login page.
       * This dance is because SWT's Browser cookie API doesn't work very well
       * to just clear the login cookies.
       */
      if (loggingOut) {
        asyncClose(OK);
        return;
      }

      /*
       * If we arrive to a logout url not programmatically (ie, loggingOut if
       * false), then the user is trying to log out. So, send them back to our
       * login url.
       * 
       * Redirecting on ManageAccount is for when the user may have logged in,
       * needed to enter an OTP, and then decided to click the "log in as
       * another user" link. Without this, they're bounced back to the login
       * page and then forwarded to the manage account page.
       */
      if (browser.getUrl().toLowerCase().contains("logout")
          || browser.getUrl().contains("ManageAccount")) {
        browser.setUrl(url);
        return;
      }
    }
  };

  private final TitleListener browserTitleListener = new TitleListener() {
    public void changed(TitleEvent event) {
      if (event.title.startsWith(SUCCESS_CODE_PREFIX)) {
        verificationCode = event.title.substring(SUCCESS_CODE_PREFIX.length());
        doLogoutAndClose(OK);
      }
    }
  };

  private boolean loggingOut = false;
  private final String message;
  private String verificationCode;

  private final String url;

  /**
   * @param parentShell The parents shell for this dialog.
   * @param message The message to be displayed at the top of the dialog. If
   *          null, then no message area is created and nothing is displayed.
   */
  public LoginBrowser(Shell parentShell, String url, String message) {
    super(parentShell);
    this.url = url;
    this.message = message;
  }

  /**
   * Asynchronously (on the ui thread) sets the return code for the dialog
   * and closes the dialog.
   */
  public void asyncClose(final int code) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        setReturnCode(code);
        close();
      }
    });
  }

  /**
   * @returns the verification code parsed from the redirect url after the user. 
   */
  public String getVerificationCode() {
    return verificationCode;
  }

  /**
   * It's possible that the user clicks cancel after they've logged in but
   * before they've clicked "allow" or "deny", so override cancel to logout and
   * then close.
   */
  @Override
  protected void cancelPressed() {
    if (IS_MAC_OS) {
      super.cancelPressed();
    } else {
      doLogoutAndClose(CANCEL);
    }
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(DIALOG_TITLE);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.CANCEL_ID,
        IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected Control createDialogArea(Composite parent) {

    if (message != null && message.length() != 0) {
      Composite titleArea = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.marginWidth = 0;
      layout.marginLeft = 13;
      titleArea.setLayout(layout);

      Label messageLabel = new Label(titleArea, SWT.NONE);
      messageLabel.setText(message);

      Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
      separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    try {
      browser = new Browser(parent, SWT.NONE);
    } catch (Throwable e) {
      GoogleLoginPlugin.logWarning("Could not instantiate embedded browser for user sign in: "
          + e.getMessage());
      throw new RuntimeException(e);
    }

    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = 660;
    gd.widthHint = 1060;
    browser.setLayoutData(gd);

    Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    browser.addProgressListener(browserProgressListener);

    // prevent the user from going places that would break the login flow
    browser.addLocationListener(browserLocationAdapter);

    // When the verification code is issues, the title changes to the format:
    // Success code=<token>
    browser.addTitleListener(browserTitleListener);

    browser.setUrl(url);

    return parent;
  }
  
  @Override
  protected boolean isResizable() {
    return true;
  }

  /**
   * Send the browser to the logout page, but wait until the progress listener
   * sees that the page is done until closing the dialog, but hide the browser
   * so that the user doesn't see the pages change.
   */
  private void doLogoutAndClose(int code) {
    loggingOut = true;

    // Logging you out of the embedded safari will log you out of
    // the external safari, see
    // http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=9
    if (IS_MAC_OS) {
      asyncClose(code);
    } else {
      browser.setVisible(false);
      browser.setUrl(LOGOUT_URL);
    }
  }

  private void openBrowser(String url) {
    try {
      BrowserUtilities.launchBrowserAndHandleExceptions(url);
    } catch (Exception e) {
      GoogleLoginPlugin.logError("Could not open external browser", e);
    }
  }
}
