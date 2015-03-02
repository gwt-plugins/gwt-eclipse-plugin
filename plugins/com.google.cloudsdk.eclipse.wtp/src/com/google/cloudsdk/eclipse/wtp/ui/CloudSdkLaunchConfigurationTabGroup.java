/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloudsdk.eclipse.wtp.ui;

import com.google.cloudsdk.eclipse.wtp.server.CloudSdkServer;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.wst.server.ui.ServerLaunchConfigurationTab;

/**
 * Launch configuration tab group for Google Cloud SDK server.
 */
public class CloudSdkLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {
  @Override
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        new ServerLaunchConfigurationTab(new String[] {CloudSdkServer.SERVER_TYPE_ID}),
        new CloudSdkServerLaunchTab()};
    setTabs(tabs);
  }
}
