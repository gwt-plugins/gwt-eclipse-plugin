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
import com.google.appengine.eclipse.core.sdk.GaeSdkTestUtilities;
import com.google.gdt.eclipse.core.TestUtilities;
import com.google.gdt.eclipse.core.ClasspathUtilities.ClassFinder;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorTestingHelper;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.projects.GwtEnablingProjectCreationParticipant;
import com.google.gwt.eclipse.core.runtime.GwtRuntimeTestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Tests the {@link MainTypeProcessor}.
 */
public class MainTypeProcessorTest extends TestCase {

  private final LaunchConfigurationProcessorTestingHelper helper = new LaunchConfigurationProcessorTestingHelper();

  @Override
  public void setUp() throws Exception {
    TestUtilities.setUp();

    GaeSdkTestUtilities.addDefaultSdk();
    GwtRuntimeTestUtilities.addDefaultRuntime();

    helper.setUp(MainTypeProcessorTest.class.getSimpleName(),
        new GwtEnablingProjectCreationParticipant(),
        new GaeEnablingProjectCreationParticipant());
  }

  public void testGaeOnly() throws Exception {
    GWTNature.removeNatureFromProject(helper.getProject());
    new MainTypeProcessor(new ClassFinder()).update(helper.getLaunchConfig(),
        JavaCore.create(helper.getProject()), null, null);
    assertEquals(
        LaunchConfigurationProcessorUtilities.getMainTypeName(helper.getLaunchConfig()),
        MainTypeProcessor.MainType.GAE_APP_SERVER.mainTypeName);
  }

  public void testGwtCapabilityDetection() throws Exception {
    // Ensure it chooses dev mode if the class is around
    new MainTypeProcessor(new ClassFinder() {
      @Override
      public boolean exists(ClassLoader classLoader, String className) {
        // GWT 2.0+ include all classes
        return true;
      }
    }).update(helper.getLaunchConfig(), JavaCore.create(helper.getProject()), null, null);
    assertEquals(
        LaunchConfigurationProcessorUtilities.getMainTypeName(helper.getLaunchConfig()),
        MainTypeProcessor.MainType.GWT_DEV_MODE.mainTypeName);

    // Ensure it picks hosted mode when dev mode is not around
    new MainTypeProcessor(new ClassFinder() {
      @Override
      public boolean exists(ClassLoader classLoader, String className) {
        return !className.equals(MainTypeProcessor.MainType.GWT_DEV_MODE.mainTypeName);
      }
    }).update(helper.getLaunchConfig(), JavaCore.create(helper.getProject()), null, null);
    assertEquals(
        LaunchConfigurationProcessorUtilities.getMainTypeName(helper.getLaunchConfig()),
        MainTypeProcessor.MainType.GWT_HOSTED_MODE.mainTypeName);

    // Ensure it falls back to shell
    new MainTypeProcessor(new ClassFinder() {
      @Override
      public boolean exists(ClassLoader classLoader, String className) {
        return !className.equals(MainTypeProcessor.MainType.GWT_DEV_MODE.mainTypeName)
            && !className.equals(MainTypeProcessor.MainType.GWT_HOSTED_MODE.mainTypeName);
      }
    }).update(helper.getLaunchConfig(), JavaCore.create(helper.getProject()), null, null);
    assertEquals(
        LaunchConfigurationProcessorUtilities.getMainTypeName(helper.getLaunchConfig()),
        MainTypeProcessor.MainType.GWT_SHELL.mainTypeName);
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

  public void testNoWar() throws Exception {
    // Ensure shell is used when there isn't a WAR dir
    WebAppProjectProperties.setWarSrcDir(helper.getProject(), new Path(""));
    new MainTypeProcessor(new ClassFinder()).update(helper.getLaunchConfig(),
        JavaCore.create(helper.getProject()), null, null);
    assertEquals(
        LaunchConfigurationProcessorUtilities.getMainTypeName(helper.getLaunchConfig()),
        MainTypeProcessor.MainType.GWT_SHELL.mainTypeName);
  }

  @Override
  protected void tearDown() throws Exception {
    helper.tearDown();
  }

}
