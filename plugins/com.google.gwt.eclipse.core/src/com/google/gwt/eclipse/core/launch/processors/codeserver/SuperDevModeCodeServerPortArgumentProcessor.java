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
package com.google.gwt.eclipse.core.launch.processors.codeserver;

import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.launch.GWTLaunchAttributes;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * Handles the -codeServerPort [auto | <port>] arg.
 *
 * SDM Mode only. Do not use for Dev Mode.
 */
public class SuperDevModeCodeServerPortArgumentProcessor implements ILaunchConfigurationProcessor {

  public static final String SDM_CODE_SERVER_PORT_ARG = "-port";
  private static final String SDM_CODE_SERVER_PORT_ERROR = "-port must specify a valid port number";

  /**
   * Return either the default port or an override value.
   *
   * @param args Arguments used to start the code server.
   * @return the port.
   */
  public static String getPort(List<String> args) {
    int index = getArgIndex(args);

    // not in or last arg (and hence no value)
    if (index < 0 || index == args.size() - 1) {
      return (String) GWTLaunchAttributes.CODE_SERVER_PORT.getDefaultValue();
    }

    String port = args.get(index + 1);
    if (!validatePort(port)) {
      return (String) GWTLaunchAttributes.CODE_SERVER_PORT.getDefaultValue();
    }

    return port;
  }

  private static int getArgIndex(List<String> args) {
    return args.indexOf(SDM_CODE_SERVER_PORT_ARG);
  }

  private static boolean validatePort(String port) {
    try {
      int valueNum = Integer.parseInt(port);
      if (valueNum < 0 || valueNum > 65535) {
        return false;
      }
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  @Override
  public void update(ILaunchConfigurationWorkingCopy launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    // only gwt projects use -codeServerPort
    if (!GWTNature.isGWTProject(javaProject.getProject())) {
      return;
    }

    int insertionIndex =
        LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(programArgs,
            getArgIndex(programArgs), true);

    String port = GWTLaunchConfigurationWorkingCopy.getSdmCodeServerPort(launchConfig);

    if (!validatePort(port)) {
      port = GWTLaunchAttributes.CODE_SERVER_PORT.getDefaultValue().toString();
    }

    programArgs.add(insertionIndex, SDM_CODE_SERVER_PORT_ARG);
    programArgs.add(insertionIndex + 1, port);
  }

  @Override
  public String validate(ILaunchConfiguration launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    int index = getArgIndex(programArgs);

    if (index < 0) {
      return null;
    }

    // last thing on the list, and hence can't have a value
    if (index == programArgs.size() - 1) {
      return SDM_CODE_SERVER_PORT_ERROR;
    }

    String value = programArgs.get(index + 1);

    if (!validatePort(value)) {
      return SDM_CODE_SERVER_PORT_ERROR;
    }

    return null;
  }

}
