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
package com.google.gdt.eclipse.suite.launch.ui;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.suite.launch.WebAppLaunchUtil;
import com.google.gdt.eclipse.suite.propertytesters.LaunchTargetTester;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;

/**
 * Launch shortcut for -noserver launches of Web Applications.
 */
public class WebAppNoServerLaunchShortcut extends WebAppLaunchShortcut {

  @Override
  protected void launch(IResource resource, String mode) {

    // assert that by the time we're in here, the PropertyTester agrees that we
    // should be here.
    assert (new LaunchTargetTester().test(resource, null, new Object[0], null));

    // Resolve to the actual resource (if it is linked)
    resource = ResourceUtils.resolveTargetResource(resource);

    try {
      String startupUrl = WebAppLaunchUtil.determineStartupURL(resource, true);
      if (startupUrl != null) {
        ILaunchConfiguration config = findOrCreateLaunchConfiguration(resource,
            startupUrl, true);

        assert (config != null);

        DebugUITools.launch(config, mode);
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    } catch (OperationCanceledException e) {
      // Abort launch
    }
  }

}
