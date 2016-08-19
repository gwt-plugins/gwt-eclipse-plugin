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
package com.google.gwt.eclipse.oophm.model;

import com.google.gdt.eclipse.core.test.launch.MockILaunch;

import junit.framework.TestCase;

import org.eclipse.debug.core.ILaunch;

/**
 * Junit tests for {@link WebAppDebugModelTest}.
 */
public class WebAppDebugModelTest extends TestCase {

  public static WebAppDebugModel newWebAppDebugModel() {
    return new WebAppDebugModel();
  }

  public void testAddOrReturnExistingLaunchConfiguration() {
    WebAppDebugModel model = new WebAppDebugModel();
    ILaunch launch1 = new MockILaunch();
    ILaunch launch2 = new MockILaunch();
    ILaunch launch3 = new MockILaunch();

    // Test addition
    LaunchConfiguration launchConfig1 = model.addOrReturnExistingLaunchConfiguration(launch1, "1", null);
    LaunchConfiguration launchConfig2 = model.addOrReturnExistingLaunchConfiguration(launch2, "2", null);
    LaunchConfiguration launchConfig3 = model.addOrReturnExistingLaunchConfiguration(launch3, "3", null);
    assertNotSame(launchConfig1, launchConfig2);
    assertNotSame(launchConfig1, launchConfig3);
    assertNotSame(launchConfig2, launchConfig3);

    // Test existing
    LaunchConfiguration launchConfig1Dup = model.addOrReturnExistingLaunchConfiguration(launch1,
        "1", null);
    assertSame(launchConfig1, launchConfig1Dup);

    LaunchConfiguration launchConfig2Dup = model.addOrReturnExistingLaunchConfiguration(launch2,
        "2", null);
    assertSame(launchConfig2, launchConfig2Dup);

    LaunchConfiguration launchConfig3Dup = model.addOrReturnExistingLaunchConfiguration(launch3,
        "3", null);
    assertSame(launchConfig3, launchConfig3Dup);
  }
}
