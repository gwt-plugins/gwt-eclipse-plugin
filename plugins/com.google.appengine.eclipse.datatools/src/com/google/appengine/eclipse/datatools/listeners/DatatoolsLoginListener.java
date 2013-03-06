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
package com.google.appengine.eclipse.datatools.listeners;

import com.google.appengine.eclipse.datatools.utils.DatatoolsUtils;
import com.google.gdt.eclipse.login.extensions.LoginListener;

import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.ProfileManager;

/**
 * Disconnects all Google Cloud SQL profiles when the user logs out.
 */
public class DatatoolsLoginListener implements LoginListener {

  public void statusChanged(boolean login) {
    // Disconnect all Google Cloud SQL profiles when the user logs out.
    if (!login) {
      ProfileManager profileManager = ProfileManager.getInstance();
      IConnectionProfile[] profiles = profileManager.getProfileByProviderID(
          DatatoolsUtils.DTP_GOOGLE_CLOUD_SQL_CONNECTION_PROFILE);
      for (IConnectionProfile profile : profiles) {
        if (profile.getConnectionState() == IConnectionProfile.CONNECTED_STATE) {
          profile.disconnect();
        }
      }
    }
  }
}