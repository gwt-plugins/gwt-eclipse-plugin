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
package com.google.gdt.eclipse.core;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;

/**
 * Utility methods for most tests.
 */
public class TestUtilities {

  /**
   * Models a simple condition checker.
   */
  public interface Condition {
    boolean checkCondition() throws Exception;
  }

  public static void setUp() throws CoreException {
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

  private static String getEnvironmentVariable(String variableName) {
    String variableValue = System.getenv(variableName);
    if (variableValue == null) {
      throw new RuntimeException("The " + variableName + " variable is not set");
    }

    return variableValue;
  }

  private static void setupWorkspaceVariables() throws CoreException {
    IPathVariableManager variableManager = ResourcesPlugin.getWorkspace().getPathVariableManager();

    IPath gwtRootPath = new Path(getEnvironmentVariable("GWT_ROOT"));
    if (variableManager.getValue("GWT_ROOT") == null) {
      CorePluginLog.logInfo("Setting GWT_ROOT = " + gwtRootPath.toOSString());
      variableManager.setValue("GWT_ROOT", gwtRootPath);
    }

    IPath gwtToolsPath = new Path(getEnvironmentVariable("GWT_TOOLS"));
    if (JavaCore.getClasspathVariable("GWT_TOOLS") == null) {
      CorePluginLog.logInfo("Setting GWT_TOOLS = " + gwtToolsPath.toOSString());
      JavaCore.setClasspathVariable("GWT_TOOLS", gwtToolsPath, null);
    }
  }
}
