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
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.TestUtilities;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;

import junit.framework.TestCase;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/*
 * TODO: once our testing infrastructure includes older SDKs, add capability
 * tests for them.
 */
/**
 * Tests for the {@link GwtCapabilityChecker}.
 */
public class GwtCapabilityCheckerTest extends TestCase {

  public void testCapabilitiesOfGwtProjectsRuntime() throws CoreException {
    GwtRuntimeTestUtilities.importGwtSourceProjects();
    try {
      GWTRuntime gwtRuntime = GWTRuntime.getFactory().newInstance(
          "GWT Projects",
          ResourcesPlugin.getWorkspace().getRoot().getLocation());
      assertLatestGwtCapabilities(gwtRuntime);
    } finally {
      GwtRuntimeTestUtilities.removeGwtSourceProjects();
    }
  }

  public void testCapabilitiesOfLatestGwtSdk() throws Exception {
    GwtRuntimeTestUtilities.addDefaultRuntime();
    try {
      assertLatestGwtCapabilities(GWTPreferences.getDefaultRuntime());
    } finally {
      GwtRuntimeTestUtilities.removeDefaultRuntime();
    }
  }

  @Override
  protected void setUp() throws Exception {
    TestUtilities.setUp();
  }

  private void assertLatestGwtCapabilities(GWTRuntime gwtRuntime) {
    GwtCapabilityChecker checker = new GwtCapabilityChecker(gwtRuntime);
    assertTrue(checker.doesCompilerAllowMultipleModules());
  }

}
