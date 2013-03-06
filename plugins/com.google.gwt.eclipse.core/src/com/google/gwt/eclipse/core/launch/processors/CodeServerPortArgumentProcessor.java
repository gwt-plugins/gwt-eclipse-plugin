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

import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
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
 */
public class CodeServerPortArgumentProcessor implements ILaunchConfigurationProcessor {

  public static final String CODE_SERVER_PORT_ARG = "-codeServerPort";
  
  private static final String CODE_SERVER_PORT_ERROR = "-codeServerPort must specify a valid port number or 'auto'"; 
  
  /**
   * Return either the default port or an override value.
   * 
   * @param args Arguments used to start the code server.
   * @return The port.
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
    return args.indexOf(CODE_SERVER_PORT_ARG);
  }

  private static boolean validatePort(String port) {
    if ("auto".equalsIgnoreCase(port)) {
      return true;
    }
    
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

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    
    // only gwt projects use -codeServerPort
    if (!GWTNature.isGWTProject(javaProject.getProject())) {
      return;
    }
    
    int index = getArgIndex(programArgs);
    
    String port = GWTLaunchConfigurationWorkingCopy.getCodeServerPort(launchConfig);
    boolean auto = GWTLaunchConfigurationWorkingCopy.getCodeServerPortAuto(launchConfig);
    
    if (auto || !validatePort(port)) {
      port = "auto";
    }
    
    if (index < 0) {
      programArgs.add(0, CODE_SERVER_PORT_ARG);
      programArgs.add(1, port);
    } else {
      if (index == programArgs.size() - 1) {
        programArgs.add(port);
      } else {
        String argValue = programArgs.get(index + 1);
        if (validatePort(argValue)) {
          programArgs.set(index + 1, port);
        } else {
          programArgs.add(index + 1, port);
        }
      }
    }
  }
  
  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    
    int index = getArgIndex(programArgs);
    
    if (index < 0) {
      return null;
    }
    
    // last thing on the list, and hence can't have a value
    if (index == programArgs.size() - 1) {
      return CODE_SERVER_PORT_ERROR;
    }
    
    String value = programArgs.get(index + 1);
    
    if (value.equals("auto")) {
      return null;
    }
    
    
    if (!validatePort(value)) {
      return CODE_SERVER_PORT_ERROR;
    }
    
    return null;
  }
  
}
