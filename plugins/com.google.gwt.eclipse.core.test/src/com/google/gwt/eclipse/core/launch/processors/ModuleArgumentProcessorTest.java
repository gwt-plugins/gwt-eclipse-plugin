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
 * Tests the {@link ModuleArgumentProcessor}.
 */
public class ModuleArgumentProcessorTest extends TestCase {
  private final LaunchConfigurationProcessorTestingHelper helper = new LaunchConfigurationProcessorTestingHelper();

  @Override
  public void setUp() throws Exception {
    TestUtilities.setUp();

    GwtRuntimeTestUtilities.addDefaultRuntime();

    helper.setUp(ModuleArgumentProcessorTest.class.getSimpleName(),
        new GwtEnablingProjectCreationParticipant());
  }

  public void testModulePresenceForNonWebAppProject() throws Exception {
    // Get the modules
    List<String> args = LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
    List<String> modules = ModuleArgumentProcessor.getModules(args,
        helper.getLaunchConfig(),
        helper.getProject());
    
    // Get rid of the "web app"-ness
    WebAppProjectProperties.setWarSrcDir(helper.getProject(), new Path(""));
    assertFalse(WebAppUtilities.isWebApp(helper.getProject()));

    // Ensure the modules still exist on the command-line, since the main
    // type is still one that uses them
    assertTrue(args.indexOf(modules.get(0)) >= 0);
    new ModuleArgumentProcessor().update(helper.getLaunchConfig(),
        JavaCore.create(helper.getProject()), args, null);
    assertTrue(args.indexOf(modules.get(0)) >= 0);

    // Drop to GWTShell main type, which does not use them, ensure they are
    // removed
    helper.getLaunchConfig().setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
        GwtLaunchConfigurationProcessorUtilities.GWT_SHELL_MAIN_TYPE);
    assertTrue(args.indexOf(modules.get(0)) >= 0);
    new ModuleArgumentProcessor().update(helper.getLaunchConfig(),
        JavaCore.create(helper.getProject()), args, null);
    assertFalse(args.indexOf(modules.get(0)) >= 0);
  }

  @Override
  protected void tearDown() throws Exception {
    helper.tearDown();
  }

}
