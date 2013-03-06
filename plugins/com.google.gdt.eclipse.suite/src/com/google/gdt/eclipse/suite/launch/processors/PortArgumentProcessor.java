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

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.processors.NoServerArgumentProcessor;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerLaunchConfiguration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes the port arguments for App Engine and GWT.
 */
public class PortArgumentProcessor implements ILaunchConfigurationProcessor {

  /**
   * Parses relevant port information from the given args.
   */
  public static class PortParser {

    public static PortParser parse(List<String> args) {
      int portArgIndex = getPortArgIndex(args);
      if (portArgIndex == -1) {
        return newNotPresentPortParser();
      }

      String portArg = args.get(portArgIndex);
      if (portArg.toLowerCase().startsWith(PortArgStyle.GAE_LONG.arg)) {
        String port = portArg.substring(PortArgStyle.GAE_LONG.arg.length());
        if (!isValidPort(port)) {
          return newNotPresentPortParser();
        }

        boolean isAuto = PortArgStyle.GAE_LONG.autoPortValue.equals(port);
        return new PortParser(true, portArgIndex, port, isAuto,
            PortArgStyle.GAE_LONG);

      } else {
        String port = LaunchConfigurationProcessorUtilities.getArgValue(args,
            portArgIndex + 1);
        if (!isValidPort(port)) {
          return newNotPresentPortParser();
        }

        PortArgStyle portArgStyle = portArg.equalsIgnoreCase(PortArgStyle.GWT.arg)
            ? PortArgStyle.GWT : PortArgStyle.GAE_SHORT;

        return new PortParser(true, portArgIndex, port,
            portArgStyle.autoPortValue.equals(port), portArgStyle);
      }
    }

    private static int getPortArgIndex(List<String> args) {
      int portArgIndex = args.indexOf(PortArgStyle.GWT.arg);
      if (portArgIndex != -1) {
        return portArgIndex;
      }

      portArgIndex = args.indexOf(PortArgStyle.GAE_SHORT.arg);
      if (portArgIndex != -1) {
        return portArgIndex;
      }

      return StringUtilities.indexOfThatStartsWith(args,
          PortArgStyle.GAE_LONG.arg, 0);
    }

    private static boolean isValidPort(String port) {
      if (StringUtilities.isEmpty(port)) {
        return false;
      }

      for (PortArgStyle style : PortArgStyle.values()) {
        if (style.autoPortValue.equals(port)) {
          return true;
        }
      }

      try {
        int portAsInt = Integer.parseInt(port);
        return portAsInt > 0 && portAsInt <= 65535;
      } catch (NumberFormatException e) {
        return false;
      }
    }

    private static PortParser newNotPresentPortParser() {
      return new PortParser(false, -1, null, false, null);
    }

    public final boolean isPresent;
    public final int portArgIndex;
    public final String port;
    public final boolean isAuto;

    public final PortArgStyle portArgStyle;

    private PortParser(boolean isPresent, int portArgIndex, String port,
        boolean isAuto, PortArgStyle portArgStyle) {
      this.isPresent = isPresent;
      this.portArgIndex = portArgIndex;
      this.port = port;
      this.isAuto = isAuto;
      this.portArgStyle = portArgStyle;
    }
  }

  private enum PortArgStyle {
    GAE_SHORT("-p", "0", true), GAE_LONG("--port=", "0", false), GWT("-port",
        "auto", true);

    private final String arg;
    private final String autoPortValue;
    private final boolean isPortASeparateArg;

    private PortArgStyle(String arg, String autoPortValue,
        boolean isPortASeparateArg) {
      this.arg = arg;
      this.autoPortValue = autoPortValue;
      this.isPortASeparateArg = isPortASeparateArg;
    }

    /**
     * @return list of arguments to specify port, or null if the given values
     *         are not valid (e.g. not auto but port is empty)
     */
    public List<String> getPortArgs(boolean isAuto, String port) {
      if (!isAuto && StringUtilities.isEmpty(port)
          || !PortParser.isValidPort(port)) {
        return null;
      }

      List<String> portArgs = new ArrayList<String>();
      if (isAuto) {
        port = autoPortValue;
      }

      if (isPortASeparateArg) {
        portArgs.add(arg);
        portArgs.add(port);
      } else {
        portArgs.add(arg + port);
      }

      return portArgs;
    }

    public boolean isGae() {
      return this == GAE_SHORT || this == GAE_LONG;
    }
  }

  private static final String INVALID_PORT_ARGUMENT_STYLE = "Argument style for specifying the port is not applicable to the current project";

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    boolean isAuto = WebAppLaunchConfigurationWorkingCopy.getAutoPortSelection(launchConfig);
    String port = WebAppLaunchConfigurationWorkingCopy.getServerPort(launchConfig);

    PortParser parser = PortParser.parse(programArgs);

    int insertionIndex = parser.isPresent
        ? LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
            programArgs, parser.portArgIndex,
            parser.portArgStyle.isPortASeparateArg) : 0;

    if (!NoServerArgumentProcessor.hasNoServerArg(programArgs)) {
      IProject project = javaProject.getProject();
      boolean isGwtProject = GWTNature.isGWTProject(project);
      boolean isGaeProject = GaeNature.isGaeProject(project);
      PortArgStyle portArgStyle = null;

      if (isGwtProject) {
        portArgStyle = PortArgStyle.GWT;
      } else if (isGaeProject) {
        // Prefer the style that existed in the args previously
        portArgStyle = parser.portArgStyle != null
            && parser.portArgStyle.isGae() ? parser.portArgStyle
            : PortArgStyle.GAE_LONG;
      } else {
        // This processor is not applicable for the given project
        return;
      }

      List<String> portArgs = portArgStyle.getPortArgs(isAuto, port);
      if (portArgs != null) {
        programArgs.addAll(insertionIndex, portArgs);
      }
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    // Only validate for main types we know about
    if (!MainTypeProcessor.isMainTypeFromSdk(launchConfig)) {
      return null;
    }

    /*
     * Speed Tracer launch configurations require a port to be defined. Parse
     * the current arguments and ensure the launch is not using automatic port
     * selection.
     */
    PortParser portParser = PortParser.parse(programArgs);
    if (SpeedTracerLaunchConfiguration.TYPE_ID.equals(launchConfig.getType().getIdentifier())
        && (!portParser.isPresent || portParser.isAuto)) {
      return "Please specify a port number (automatic port selection is not supported by Speed Tracer launch configurations)";
    }

    if (!portParser.isPresent) {
      return null;
    }

    IProject project = javaProject.getProject();
    boolean isGwtProject = GWTNature.isGWTProject(project);
    boolean isGaeProject = GaeNature.isGaeProject(project);
    if (isGwtProject && portParser.portArgStyle != PortArgStyle.GWT
        || isGaeProject && !isGwtProject && !portParser.portArgStyle.isGae()) {
      return INVALID_PORT_ARGUMENT_STYLE;
    }

    return null;
  }
}
