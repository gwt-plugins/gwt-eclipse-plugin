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
package com.google.gdt.eclipse.gph.subclipse;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.tigris.subversion.clientadapter.Activator;
import org.tigris.subversion.clientadapter.ISVNClientWrapper;
import org.tigris.subversion.subclipse.core.SVNClientManager;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

import java.io.IOException;

/**
 * TODO: doc me.
 */
public class SubclipseProduct {

  private static final String SVNKIT_CLIENT_INTERFACE = "svnkit";
  private static final String JAVAHL_CLIENT_INTERFACE = "javahl";

  public static void configureSVNKit() throws IOException {
    IPreferenceStore store = getPreferenceStore();
    store.setValue(ISVNUIConstants.PREF_SVNINTERFACE, SVNKIT_CLIENT_INTERFACE);
    if (store.needsSaving() && store instanceof IPersistentPreferenceStore) {
      ((IPersistentPreferenceStore) store).save();
    }
  }

  /**
   * Check if subclipse is configured.
   * 
   * @return <code>true</code> if subclipse is configured, <code>false</code>
   *         otherwise
   */
  public static boolean isConfigured() {
    String svnClientInterface = getSVNClientInterface();
    if (JAVAHL_CLIENT_INTERFACE.equals(svnClientInterface)
        || svnClientInterface == null) {
      // issue: if we attempt to get the adapter and it's not configured we'll
      // get a pop-up
      // we'd like to sidestep this...
      // simplified algorithm: if ANY wrappers can't load (this will be javahl);
      // then setup pure java client
      return !anyWrapperLoadErrors();
    }
    return true;
  }

  private static boolean anyWrapperLoadErrors() {
    ISVNClientWrapper[] wrappers = Activator.getDefault().getAllClientWrappers();
    for (ISVNClientWrapper wrapper : wrappers) {
      String loadErrors = wrapper.getLoadErrors();
      if (loadErrors != null && loadErrors.length() > 0) {
        return true;
      }
    }
    return false;
  }

  private static IPreferenceStore getPreferenceStore() {
    return SVNUIPlugin.getPlugin().getPreferenceStore();
  }

  private static String getSVNClientInterface() {
    return getSVNClientManager().getSvnClientInterface();
  }

  private static SVNClientManager getSVNClientManager() {
    return SVNProviderPlugin.getPlugin().getSVNClientManager();
  }

}
