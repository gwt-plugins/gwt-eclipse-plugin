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
package com.google.gwt.eclipse.core.preferences.ui;

import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.ui.AddSdkDialog;
import com.google.gdt.eclipse.core.ui.SdkTable;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.sdk.ui.AddGwtSdkDialog;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Page to manage preferences for GWT that apply across the workspace.
 */
public class GwtPreferencePage extends PreferencePage implements
    IWorkbenchPreferencePage {

  public static final String ID = GwtPreferencePage.class.getName();

  private SdkSet<GWTRuntime> sdkSet;

  /**
   *  
   */
  public GwtPreferencePage() {
  }

  /**
   * @param title
   */
  public GwtPreferencePage(String title) {
    super(title);
  }

  /**
   * @param title
   * @param image
   */
  public GwtPreferencePage(String title, ImageDescriptor image) {
    super(title, image);
    // TODO Auto-generated constructor stub
  }

  public void init(IWorkbench workbench) {
    // TODO Auto-generated method stub
  }

  @Override
  public boolean performOk() {
    assert (sdkSet != null);
    GWTPreferences.setSdks(sdkSet);
    // FIXME: Modify the default IClasspathContainers
    return super.performOk();
  }

  @Override
  protected Control createContents(Composite parent) {
    noDefaultAndApplyButton();

    sdkSet = GWTPreferences.getSdks();

    return new SdkTable<GWTRuntime>(parent, SWT.NONE, sdkSet, null, this) {
      @Override
      protected IStatus doAddSdk() {
        AddSdkDialog<GWTRuntime> addGaeSdkDialog = new AddGwtSdkDialog(
            getShell(), sdkSet, GWTPlugin.PLUGIN_ID,
            "Add Google Web Toolkit SDK", GWTRuntime.getFactory());
        if (addGaeSdkDialog.open() == Window.OK) {
          GWTRuntime newSdk = addGaeSdkDialog.getSdk();
          if (newSdk != null) {
            sdkSet.add(newSdk);
          }

          return Status.OK_STATUS;
        }

        return Status.CANCEL_STATUS;
      }

      @Override
      protected IStatus doDownloadSdk() {
        MessageDialog dialog = new MessageDialog(
            GWTPlugin.getActiveWorkbenchShell(), "Google Eclipse Plugin", null,
            "Would you like to open the Google Web Toolkit download page in your "
                + "web browser?\n\nFrom there, you can "
                + "download the latest GWT SDK and extract it to the"
                + " location of your choice. Add it to Eclipse"
                + " with the \"Add...\" button.", MessageDialog.QUESTION,
            new String[] {"Open Browser", IDialogConstants.CANCEL_LABEL}, 0);

        if (dialog.open() == Window.OK) {
          if (BrowserUtilities.launchBrowserAndHandleExceptions(GWTPlugin.SDK_DOWNLOAD_URL) == null) {
            return Status.CANCEL_STATUS;
          }
        } else {
          return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
      }
    };
  }
}
