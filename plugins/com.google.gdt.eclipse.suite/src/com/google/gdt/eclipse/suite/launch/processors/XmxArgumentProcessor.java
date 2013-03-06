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

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.extensions.ExtensionQueryStringAttr;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.suite.GdtPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * Processes the "-Xmx" VM argument.
 */
public class XmxArgumentProcessor implements ILaunchConfigurationProcessor {

  private static final String ARG_XMX_PREFIX = "-Xmx";
  private final String defaultMemory;

  public XmxArgumentProcessor() {
    int mem = Integer.MIN_VALUE;

    ExtensionQueryStringAttr extQuery = new ExtensionQueryStringAttr(
        GdtPlugin.PLUGIN_ID, "xMxArgumentProcessorMem", "memory");

    List<ExtensionQuery.Data<String>> memoryDefaults = extQuery.getData();
    for (ExtensionQuery.Data<String> memoryDefault : memoryDefaults) {
      try {
        mem = Integer.parseInt(memoryDefault.getExtensionPointData());
        break;
      } catch (NumberFormatException nfe) {
        // Ignore
      }
    }

    if (mem > 0) {
      defaultMemory = mem + "m";
    } else {
      defaultMemory = "512m";
    }
  }

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    int xmxArgIndex = StringUtilities.indexOfThatStartsWith(vmArgs,
        ARG_XMX_PREFIX, 0);
    if (xmxArgIndex == -1) {
      vmArgs.add(0, ARG_XMX_PREFIX + defaultMemory);
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    return null;
  }

}
