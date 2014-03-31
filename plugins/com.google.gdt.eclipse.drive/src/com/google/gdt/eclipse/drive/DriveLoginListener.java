/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive;

import com.google.gdt.eclipse.drive.driveapi.DriveServiceFacade;
import com.google.gdt.eclipse.login.common.LoginListener;

/**
 * A listener for login-status-change events reported by the Google login plugin.
 * Installation of this listener is specified in plugin.xml.
 */
public class DriveLoginListener implements LoginListener {
  @Override
  public void statusChanged(boolean login) {
    if (DrivePlugin.getDefault() == null) {
      // The listener has apparently been called during workbench exit, after the DrivePlugin has
      // been stopped. Calling DrivePlugin.logInfo would throw NullPointerException.
      return;
    }
    DrivePlugin.logInfo("DriveLoginListener invoked to report " + (login ? "login" : "logout"));
    if (!login) {
      DriveServiceFacade.get().onLogout(); 
    }
  }
}
