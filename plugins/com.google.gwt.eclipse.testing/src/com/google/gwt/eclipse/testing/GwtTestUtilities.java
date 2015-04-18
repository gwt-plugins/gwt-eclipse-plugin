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
package com.google.gwt.eclipse.testing;

import com.google.gcp.eclipse.testing.TestEnvironmentUtil;
import com.google.gcp.eclipse.testing.TestUtil;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.suite.GdtPlugin;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;

/**
 * Utility methods for most tests.
 */
public class GwtTestUtilities {
  /**
   * Models a simple condition checker.
   */
  public interface Condition {
    boolean checkCondition() throws Exception;
  }

  public static void setUp() throws CoreException {
    // Need to wait until ProjectUtilities.setWebAppProjectCreatorFactory() is invoked.
    while (GdtPlugin.getDefault() == null) {
      TestUtil.delay(100);
    }
    setupWorkspaceVariables();
  }

  public static void waitForCondition(Condition condition, int timeoutMs)
      throws Exception {
    long absTimeout = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < absTimeout) {
      if (condition.checkCondition()) {
        return;
      }

      Thread.sleep(250);
    }

    throw new Exception("Condition never met.");
  }

  /**
   * Sets up workspace variables to point at the the {@code GWT_ROOT} and {@code GWT_TOOLS}
   * environment variables, which point at a local clone of the GWT git repository. If those
   * environment variables are not set, extracts a snapshot of the GWT source tree that
   * is bundled with this plug-in and sets the environment variables to point at it.
   */
  private static void setupWorkspaceVariables() throws CoreException {
    IPathVariableManager variableManager = ResourcesPlugin.getWorkspace().getPathVariableManager();

    String gwtRoot = System.getenv("GWT_ROOT");
    if (gwtRoot == null) {
      System.out.println("The GWT_ROOT environment variable is not set, using test bundle version");
      gwtRoot = TestEnvironmentUtil.installTestSdk(
          GwtTestingPlugin.getDefault().getBundle(),
          Path.fromPortableString("/resources/gwt-root.zip")).append("trunk").toOSString();
      TestEnvironmentUtil.updateEnvironmentVariable("GWT_ROOT", gwtRoot);
      System.out.println("The GWT_ROOT environment variable is now set");
    }
    IPath gwtRootPath = Path.fromOSString(gwtRoot);
    if (variableManager.getURIValue("GWT_ROOT") == null) {
      CorePluginLog.logInfo("Setting GWT_ROOT = " + gwtRootPath.toOSString());
      variableManager.setURIValue("GWT_ROOT", gwtRootPath.toFile().toURI());
    }

    String gwtTools = System.getenv("GWT_TOOLS");
    if (gwtTools == null) {
      System.out.println("The GWT_TOOLS environment variable is not set, using GWT_ROOT as a base");
      gwtTools = gwtRoot + "/tools";
      TestEnvironmentUtil.updateEnvironmentVariable("GWT_TOOLS", gwtTools);
    }
    IPath gwtToolsPath = Path.fromOSString(gwtTools);
    if (JavaCore.getClasspathVariable("GWT_TOOLS") == null) {
      CorePluginLog.logInfo("Setting GWT_TOOLS = " + gwtToolsPath.toOSString());
      JavaCore.setClasspathVariable("GWT_TOOLS", gwtToolsPath, null);
    }
  }
}
