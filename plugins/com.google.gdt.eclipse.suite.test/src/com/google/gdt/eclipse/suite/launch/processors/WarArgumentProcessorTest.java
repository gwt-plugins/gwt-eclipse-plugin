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
package com.google.gdt.eclipse.suite.launch.processors;

import com.google.appengine.eclipse.core.projects.GaeEnablingProjectCreationParticipant;
import com.google.gcp.eclipse.testing.GaeProjectTestUtil;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorTestingHelper;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gwt.eclipse.core.launch.processors.GwtLaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.projects.GwtEnablingProjectCreationParticipant;
import com.google.gwt.eclipse.testing.GwtRuntimeTestUtilities;
import com.google.gwt.eclipse.testing.GwtTestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.List;

/**
 * Tests the {@link WarArgumentProcessor}.
 */
public class WarArgumentProcessorTest extends TestCase {

  private final LaunchConfigurationProcessorTestingHelper helper = new LaunchConfigurationProcessorTestingHelper();

  @Override
  public void setUp() throws Exception {
    GwtTestUtilities.setUp();

    GaeProjectTestUtil.addDefaultSdk();
    GwtRuntimeTestUtilities.addDefaultRuntime();

    helper.setUp(WarArgumentProcessorTest.class.getSimpleName(),
        new GwtEnablingProjectCreationParticipant(),
        new GaeEnablingProjectCreationParticipant());
  }


  @Override
  protected void tearDown() throws Exception {
    helper.tearDown();
  }

  public void testWarArgPresenceForNonWebAppProject() throws Exception {
    // Get rid of the "web app"-ness
    WebAppProjectProperties.setWarSrcDir(helper.getProject(), new Path(""));
    assertFalse(WebAppUtilities.isWebApp(helper.getProject()));

    // Ensure the WarArgumentProcessor keeps the "-war", since the main
    // type is still one that uses it
    List<String> args = LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
    assertTrue(args.indexOf("-war") >= 0);
    new WarArgumentProcessor().update(helper.getLaunchConfig(),
        JavaCore.create(helper.getProject()), args, null);
    assertTrue(args.indexOf("-war") >= 0);

    // Drop to GWTShell main type, which does not use it, ensure the arg is
    // removed
    helper.getLaunchConfig().setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
        GwtLaunchConfigurationProcessorUtilities.GWT_SHELL_MAIN_TYPE);
    assertTrue(args.indexOf("-war") >= 0);
    new WarArgumentProcessor().update(helper.getLaunchConfig(),
        JavaCore.create(helper.getProject()), args, null);
    assertFalse(args.indexOf("-war") >= 0);
  }

}
