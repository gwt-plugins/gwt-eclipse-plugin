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
package com.google.appengine.eclipse.core.launch.processors;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.io.File;
import java.util.List;

/**
 * Adds a workaround to fix an issue with the jvm on Mac.
 * This will add the -Xbootclasspath/p vm argument and provide it with a jar
 * in the gae sdk that will override a broken class.
 */
public class XBootclasspathArgumentProcessor
    implements
      ILaunchConfigurationProcessor {

  private static final String BOOTCLASSPATH_PREFIX = "-Xbootclasspath/p:";

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    
    // only affects macs
    if (!Platform.OS_MACOSX.equals(Platform.getOS())) {
      return;
    }
    
    // only care about GAE projects
    if (!GaeNature.isGaeProject(javaProject.getProject())) {
      return;
    }

    int bootclasspathIndex = StringUtilities.indexOfThatStartsWith(vmArgs,
        BOOTCLASSPATH_PREFIX, 0);

    // check if it's already there
    if (bootclasspathIndex > -1) {
      return;
    }

    GaeSdk sdk = GaeSdk.findSdkFor(javaProject);
    if (sdk == null || !sdk.validate().isOK()) {
      return;
    }

    String jarPath = sdk.getInstallationPath().append("lib/override/appengine-dev-jdk-overrides.jar").toOSString();
    
    if (new File(jarPath).exists()) {
      vmArgs.add(BOOTCLASSPATH_PREFIX + jarPath);
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs) {
    return null;
  }

}
