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

import com.google.gdt.eclipse.core.ClasspathUtilities.ClassFinder;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorTestingHelper;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.projects.GwtEnablingProjectCreationParticipant;
import com.google.gwt.eclipse.testing.GwtRuntimeTestUtilities;
import com.google.gwt.eclipse.testing.GwtTestUtilities;

import junit.framework.TestCase;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Tests the {@link MainTypeProcessor}.
 */
public class MainTypeProcessorTest extends TestCase {

  private final LaunchConfigurationProcessorTestingHelper helper = new LaunchConfigurationProcessorTestingHelper();

  @Override
  public void setUp() throws Exception {
    GwtTestUtilities.setUp();

    GwtRuntimeTestUtilities.addDefaultRuntime();

    helper.setUp(MainTypeProcessorTest.class.getSimpleName(),
        new GwtEnablingProjectCreationParticipant());
  }

  public void testManuallyChanged() throws Exception {
    // Ensure the processor does not overwrite manual changes
    String otherMainType = "example.OtherMainType";
    helper.getLaunchConfig().setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, otherMainType);
    assertEquals(
        LaunchConfigurationProcessorUtilities.getMainTypeName(helper.getLaunchConfig()),
        otherMainType);
    new MainTypeProcessor(new ClassFinder()).update(helper.getLaunchConfig(),
        JavaCore.create(helper.getProject()), null, null);
    assertEquals(
        LaunchConfigurationProcessorUtilities.getMainTypeName(helper.getLaunchConfig()),
        otherMainType);
  }

  @Override
  protected void tearDown() throws Exception {
    helper.tearDown();
  }

}
