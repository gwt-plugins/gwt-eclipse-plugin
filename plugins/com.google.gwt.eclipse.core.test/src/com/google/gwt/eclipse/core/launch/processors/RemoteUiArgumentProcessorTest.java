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
package com.google.gwt.eclipse.core.launch.processors;

import com.google.gdt.eclipse.core.TestUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorTestingHelper;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gwt.eclipse.core.projects.GwtEnablingProjectCreationParticipant;
import com.google.gwt.eclipse.core.runtime.GwtRuntimeTestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.List;

/**
 * Tests the {@link RemoteUiArgumentProcessor}.
 */
public class RemoteUiArgumentProcessorTest extends TestCase {
  private final LaunchConfigurationProcessorTestingHelper helper = new LaunchConfigurationProcessorTestingHelper();

  @Override
  public void setUp() throws Exception {
    TestUtilities.setUp();

    GwtRuntimeTestUtilities.addDefaultRuntime();

    helper.setUp(RemoteUiArgumentProcessorTest.class.getSimpleName(),
        new GwtEnablingProjectCreationParticipant());
  }

  public void testGwtShellSupportsRemoteUi() throws Exception {
    List<String> args = LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
    
    // Get rid of the "web app"-ness
    WebAppProjectProperties.setWarSrcDir(helper.getProject(), new Path(""));
    assertFalse(WebAppUtilities.isWebApp(helper.getProject()));

    // Change to GWTShell main type
    helper.getLaunchConfig().setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
        GwtLaunchConfigurationProcessorUtilities.GWT_SHELL_MAIN_TYPE);

    // Ensure the remote UI arg remains on the command-line
    assertTrue(args.indexOf("-remoteUI") >= 0);
    new RemoteUiArgumentProcessor().update(helper.getLaunchConfig(),
        JavaCore.create(helper.getProject()), args, null);
    assertTrue(args.indexOf("-remoteUI") >= 0);
  }

  @Override
  protected void tearDown() throws Exception {
    helper.tearDown();
  }

}
