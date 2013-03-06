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
package com.google.gdt.eclipse.platform.update;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.IProvHelpContextIds;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.dialogs.AvailableIUGroup;
import org.eclipse.equinox.internal.p2.ui.dialogs.AvailableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningWizardDialog;
import org.eclipse.equinox.internal.p2.ui.dialogs.RepositorySelectionGroup;
import org.eclipse.equinox.internal.p2.ui.sdk.InstallNewSoftwareHandler;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This is based off of the E35 version, but updated for E42 APIs.
 */
/**
 * Opens the P2 install new software wizard with defaults suitable for updating
 * for the Google Plugin for Eclipse.
 */
@SuppressWarnings("restriction")
public class UpdateDetailsPresenter implements IUpdateDetailsPresenter {

  /**
   * Derived from {@link InstallNewSoftwareHandler}.
   */
  private static class InstallUpdateHandler extends InstallNewSoftwareHandler {
    // These are taken from AvailableIUsPage
    private static final String DIALOG_SETTINGS_SECTION = "AvailableIUsPage"; //$NON-NLS-1$
    private static final String SHOW_LATEST_VERSIONS_ONLY = "ShowLatestVersionsOnly"; //$NON-NLS-1$
    private static final String HIDE_INSTALLED_IUS = "HideInstalledContent"; //$NON-NLS-1$
    private static final String RESOLVE_ALL = "ResolveInstallWithAllSites"; //$NON-NLS-1$

    private static final String[] SETTINGS_TO_SAVE = {
        SHOW_LATEST_VERSIONS_ONLY, HIDE_INSTALLED_IUS, RESOLVE_ALL};

    /**
     * Opens the wizard dialog.
     */
    public static void launch(String updateSiteUrl) throws Exception {
      InstallUpdateHandler handler = new InstallUpdateHandler(updateSiteUrl);
      handler.execute(null);
    }

    private final Map<String, Boolean> savedSettingValues = new HashMap<String, Boolean>();

    private final String gpeUpdateSiteUrl;

    private InstallUpdateHandler(String updateSiteUrl) {
      this.gpeUpdateSiteUrl = updateSiteUrl;
    }

    @Override
    protected void doExecute(LoadMetadataRepositoryJob job) {
      try {
        // Save the global settings that we will modify
        saveSettings();

        // Modify those settings
        prepareWizardSettings();

        // Boilerplate for opening the wizard
        InstallWizard wizard = new InstallWizard(getProvisioningUI(), null,
            null, job);
        WizardDialog dialog = new ProvisioningWizardDialog(getShell(), wizard);
        dialog.create();
        PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(),
            IProvHelpContextIds.INSTALL_WIZARD);

        // Prepare the wizard for GPE
        prepareWizard(wizard);

        // Open it
        dialog.open();

      } catch (Exception e) {
        throw new RuntimeException(e);

      } finally {
        // Restore the settings which we touched
        restoreSettings();
      }
    }

    private AvailableIUGroup findAvailableIuGroup(AvailableIUsPage page)
        throws NoSuchFieldException, IllegalAccessException {
      Field field = AvailableIUsPage.class.getDeclaredField("availableIUGroup");
      field.setAccessible(true);
      return (AvailableIUGroup) field.get(page);
    }

    /**
     * Finds the repository for the GPE update site (matching the update site
     * value that this plugin was compiled with.)
     */
    private URI findGpeRepoUri() throws Exception {
      URI gpeUpdateSiteUri = URI.create(gpeUpdateSiteUrl);
      Path gpeUpdateSitePath = getPath(gpeUpdateSiteUri);

      URI[] repos = getProvisioningUI().getRepositoryTracker().getKnownRepositories(
          getProvisioningUI().getSession());
      for (URI repo : repos) {
        if (repo != null && repo.getHost() != null
            && repo.getHost().equalsIgnoreCase(gpeUpdateSiteUri.getHost())
            && getPath(repo).equals(gpeUpdateSitePath)) {
          return repo;
        }
      }

      throw new Exception("Could not find GPE repository (" + gpeUpdateSiteUrl
          + ")");
    }

    private RepositorySelectionGroup findRepositorySelectionGroup(
        AvailableIUsPage page) throws NoSuchFieldException,
        IllegalAccessException {
      Field field = AvailableIUsPage.class.getDeclaredField("repoSelector");
      field.setAccessible(true);
      return (RepositorySelectionGroup) field.get(page);
    }

    private IDialogSettings getDialogSettings() {
      IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
      IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
      return section;
    }

    private Path getPath(URI uri) {
      String path = uri.getPath();
      return new Path(path != null ? path : "");
    }

    private void prepareWizard(InstallWizard wizard) throws Exception {
      AvailableIUsPage page = (AvailableIUsPage) wizard.getPage("AvailableSoftwarePage");
      if (page == null) {
        throw new Exception(
            "Could not find the available software page in the install wizard");
      }

      RepositorySelectionGroup repoSelector = findRepositorySelectionGroup(page);
      repoSelector.setRepositorySelection(AvailableIUGroup.AVAILABLE_SPECIFIED,
          findGpeRepoUri());

      setCheckedToAvailableUpdates(page);
    }

    /**
     * Sets the "hide installed", "resolve all update sites", and
     * "show latest versions only" checkboxes. Ideally, we would provide a
     * {@link org.eclipse.equinox.p2.ui.Policy} to the {@link InstallWizard},
     * but the P2 implementation gives higher priority to the user settings.
     * So, we save the user settings, set them to values suitable for
     * updating, show the dialog, and restore the user settings.
     */
    private void prepareWizardSettings() {
      IDialogSettings section = getDialogSettings();
      if (section != null) {
        section.put(HIDE_INSTALLED_IUS, true);
        section.put(RESOLVE_ALL, true);
        section.put(SHOW_LATEST_VERSIONS_ONLY, true);
      }
    }

    private void restoreSettings() {
      IDialogSettings section = getDialogSettings();
      if (section != null) {
        for (String setting : savedSettingValues.keySet()) {
          section.put(setting, savedSettingValues.get(setting).booleanValue());
        }
      }
    }

    private void saveSettings() {
      IDialogSettings section = getDialogSettings();
      if (section != null) {
        for (String setting : SETTINGS_TO_SAVE) {
          savedSettingValues.put(setting, section.getBoolean(setting));
        }
      }
    }

    /**
     * Places a checkmark next to all the available updates.
     */
    private void setCheckedToAvailableUpdates(AvailableIUsPage page)
        throws NoSuchFieldException, IllegalAccessException {
      AvailableIUGroup group = findAvailableIuGroup(page);
      TreeItem[] items = group.getTree().getItems();

      List<Object> elements = new ArrayList<Object>();
      for (TreeItem item : items) {
        elements.add(item.getData());
      }

      group.setChecked(elements.toArray());
    }
  }

  public void presentUpdateDetails(Shell shell, String updateSiteUrl, ILog log,
      String pluginId) {
    try {
      InstallUpdateHandler.launch(updateSiteUrl);
    } catch (Throwable t) {
      // Log, and fallback on dialog
      log.log(new Status(IStatus.WARNING, pluginId,
          "Could not open install wizard", t));

      MessageDialog.openInformation(
          shell,
          DEFAULT_DIALOG_TITLE,
          "An update is available for the Google Plugin for Eclipse.  Go to \"Help > Install New Software\" to install it.");
    }
  }
}
