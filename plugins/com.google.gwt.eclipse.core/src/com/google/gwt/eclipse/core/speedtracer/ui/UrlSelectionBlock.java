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
package com.google.gwt.eclipse.core.speedtracer.ui;

import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.launch.ILaunchShortcutStrategy;
import com.google.gdt.eclipse.core.ui.TextSelectionBlock;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.ILaunchShortcutStrategyProvider;
import com.google.gwt.eclipse.core.launch.LegacyGWTLaunchShortcutStrategy;
import com.google.gwt.eclipse.core.launch.WebAppLaunchShortcutStrategy;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;

import java.util.List;

/*
 * TODO: GWTSettingsTab.UrlSelectionBlock could be replaced by this. Create
 * better javadoc for contract, move to gdt.core.ui
 */
/**
 * Text for the URL, and allows selecting a resource instead of manually
 * entering the URL.
 */
public class UrlSelectionBlock extends TextSelectionBlock {

  interface Listener {
    void urlChanged(String url, IStatus status);
  }

  private final Listener listener;

  private String url;

  private IProject project;

  public UrlSelectionBlock(Listener listener) {
    super("URL:", "&Browse...");
    this.listener = listener;
  }

  public String getUrl() {
    return url;
  }

  public void set(String url, IProject project) {
    this.url = url;
    this.project = project;

    updateControls();
  }

  @Override
  protected void initializeControls() {
    updateControls();
  }

  @Override
  protected String onButtonClicked() {
    if (project == null) {
      MessageDialog.openError(getShell(), "No project found",
          "Please make sure that this launch configuration has a valid project assigned.");
      return null;
    }

    ILaunchShortcutStrategy strategy = null;

    ExtensionQuery<ILaunchShortcutStrategyProvider> extQuery = new ExtensionQuery<ILaunchShortcutStrategyProvider>(
        GWTPlugin.PLUGIN_ID, "launchShortcutStrategy", "class");
    List<ExtensionQuery.Data<ILaunchShortcutStrategyProvider>> strategyProviderInfos = extQuery.getData();

    for (ExtensionQuery.Data<ILaunchShortcutStrategyProvider> data : strategyProviderInfos) {
      strategy = data.getExtensionPointData().getStrategy(project);
      break;
    }

    if (WebAppUtilities.isWebApp(project)) {
      strategy = new WebAppLaunchShortcutStrategy();
    } else {
      strategy = new LegacyGWTLaunchShortcutStrategy();
    }

    return strategy.getUrlFromUser(project, false);
  }

  @Override
  protected void onTextModified(IStatus status) {
    listener.urlChanged(url, status);
  }

  @Override
  protected IStatus validate() {
    url = null;

    String currentUrl = getText();
    if (currentUrl.length() == 0) {
      return StatusUtilities.newErrorStatus("Enter a URL to profile.",
          GWTPlugin.PLUGIN_ID);
    }

    url = currentUrl;

    return StatusUtilities.OK_STATUS;
  }

  private void updateControls() {
    setText(url);
  }

}
