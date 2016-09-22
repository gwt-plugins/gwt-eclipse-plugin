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

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.collections.ListUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Processes GWT startup URLs. These can either appear with the "-startupUrl"
 * argument or as unnamed, extra arguments at the end.
 */
public class StartupUrlArgumentProcessor implements
    ILaunchConfigurationProcessor {

  private static class StartupUrlParser {

    static StartupUrlParser parse(List<String> args,
        String persistedStartupUrl, ILaunchConfiguration config)
        throws CoreException {

      List<String> startupUrls = new ArrayList<String>();

      // Look for "-startupUrl" args
      List<Integer> startupUrlArgIndices = findStartupUrlArgIndices(args);
      for (Integer startupUrlArgIndex : startupUrlArgIndices) {
        String startupUrl = LaunchConfigurationProcessorUtilities.getArgValue(
            args, startupUrlArgIndex + 1);
        if (startupUrl != null) {
          startupUrls.add(startupUrl);
        }
      }

      int gwtShellStyleStartupUrlIndex = -1;
      if (startupUrls.isEmpty()) {
        // Look for GWTShell style startup URL if the other style did not match

        /*
         * Try to find the persisted startup URL in the args. This succeeds in
         * situations when the project configuration is changing (e.g. SDK
         * changes), but fails when the user is changing the value of the
         * startup URL.
         */
        if (!StringUtilities.isEmpty(persistedStartupUrl)) {
          gwtShellStyleStartupUrlIndex = args.lastIndexOf(
              persistedStartupUrl);
        }

        /*
         * If the above did not work (e.g. user changing value) and the main
         * type is GWTShell, try to find the last extra arg. This succeeds when
         * the user is changing values, but can lead to incorrect results when
         * the project configuration is changing (e.g. SDK changes) since the
         * last extra arg could be a different type (e.g. module or WAR).
         * Remember, the block above matches thus preventing this from executing
         * if the project configuration is changing.
         */
        if (gwtShellStyleStartupUrlIndex == -1
            && GwtLaunchConfigurationProcessorUtilities.isGwtShell(config)) {
          gwtShellStyleStartupUrlIndex = findStartupUrlFromGwtShellArgs(args);
        }

        // Assemble the list of startup URLs
        if (gwtShellStyleStartupUrlIndex >= 0) {
          startupUrls.add(args.get(gwtShellStyleStartupUrlIndex));
        }
      }

      return new StartupUrlParser(startupUrls, startupUrlArgIndices,
          gwtShellStyleStartupUrlIndex);
    }

    private static List<Integer> findStartupUrlArgIndices(List<String> args) {
      List<Integer> startupUrlArgIndices = new ArrayList<Integer>();

      int searchBeginIndex = 0;
      int startupUrlArgIndex;
      while ((startupUrlArgIndex = ListUtilities.indexOf(args,
          ARG_STARTUP_URL, searchBeginIndex)) != -1) {
        startupUrlArgIndices.add(startupUrlArgIndex);
        searchBeginIndex = startupUrlArgIndex + 1;
      }
      return startupUrlArgIndices;
    }

    public final List<String> startupUrls;

    public final List<Integer> startupUrlArgIndices;

    public final int gwtShellStyleStartupUrlIndex;

    public StartupUrlParser(List<String> startupUrls,
        List<Integer> startupUrlArgIndices, int gwtShellStartupUrlIndex) {
      this.startupUrls = startupUrls;
      this.startupUrlArgIndices = startupUrlArgIndices;
      this.gwtShellStyleStartupUrlIndex = gwtShellStartupUrlIndex;
    }
  }

  private static final String ARG_STARTUP_URL = "-startupUrl";

  public static String getStartupUrl(List<String> args,
      ILaunchConfiguration config) throws CoreException {
    String startupUrl = null;
    try {
      startupUrl = GWTLaunchConfiguration.getStartupUrl(config);
    } catch (CoreException e) {
      GWTPluginLog.logWarning(e, "Could not get persisted startup URL");
      // Continue since this method is called from user updates to the URL,
      // which can be parsed without the persisted startup URL
    }

    StartupUrlParser parser = StartupUrlParser.parse(args, startupUrl, config);
    return parser.startupUrls.isEmpty() ? null : parser.startupUrls.get(0);
  }

  private static int findStartupUrlFromGwtShellArgs(List<String> args) {
    // Keeping this here instead of static final to keep ugliness in this
    // method's scope
    HashMap<String, Integer> allGwtShellArgs = new HashMap<String, Integer>();
    allGwtShellArgs.put("-ea", 0);
    allGwtShellArgs.put("-noserver", 0);
    allGwtShellArgs.put("-port", 1);
    allGwtShellArgs.put("-whitelist", 1);
    allGwtShellArgs.put("-blacklist", 1);
    allGwtShellArgs.put("-logLevel", 1);
    allGwtShellArgs.put("-gen", 1);
    allGwtShellArgs.put("-out", 1);
    allGwtShellArgs.put("-style", 1);
    allGwtShellArgs.put("-logdir", 1);
    allGwtShellArgs.put("-codeServerPort", 1);
    allGwtShellArgs.put("-remoteUI", 1);

    for (int i = 0; i < args.size(); i++) {
      String arg = args.get(i);
      Integer argsToSkip = allGwtShellArgs.get(arg);
      if (argsToSkip == null && i == args.size() - 1) {
        // It's an unhandled arg at the end, this is our startup URL!
        return i;
      }

      if (argsToSkip != null) {
        i += argsToSkip;
      }
    }

    return -1;
  }

  private static void removeStartupUrlForGwtShell(List<String> args,
      StartupUrlParser parser) {
    if (parser.gwtShellStyleStartupUrlIndex != -1) {
      args.remove(parser.gwtShellStyleStartupUrlIndex);
    }
  }

  /**
   * @return the insertion index for new "-startupUrl" args
   */
  private static int removeStartupUrlsWithStartupUrlArg(List<String> args,
      StartupUrlParser parser, boolean removeOnlyFirst) {
    int insertionIndex = -1;

    for (Integer argIndex : parser.startupUrlArgIndices) {
      int curInsertionIndex = LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
          args, argIndex, true);
      if (insertionIndex == -1) {
        insertionIndex = curInsertionIndex;
      }

      if (removeOnlyFirst) {
        return insertionIndex;
      }
    }

    return insertionIndex == -1 ? 0 : insertionIndex;
  }

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    String persistedStartupUrl = GWTLaunchConfiguration.getStartupUrl(launchConfig);

    StartupUrlParser parser = StartupUrlParser.parse(programArgs,
        persistedStartupUrl, launchConfig);

    if (!GWTNature.isGWTProject(javaProject.getProject())) {
      removeStartupUrlForGwtShell(programArgs, parser);
      removeStartupUrlsWithStartupUrlArg(programArgs, parser, false);

    } else {
      if (GwtLaunchConfigurationProcessorUtilities.isGwtShell(launchConfig)) {
        removeStartupUrlsWithStartupUrlArg(programArgs, parser, false);
        removeStartupUrlForGwtShell(programArgs, parser);
        if (!StringUtilities.isEmpty(persistedStartupUrl)) {
          programArgs.add(persistedStartupUrl);
        }

      } else {
        removeStartupUrlForGwtShell(programArgs, parser);
        int insertionIndex = removeStartupUrlsWithStartupUrlArg(programArgs,
            parser, true);
        if (!StringUtilities.isEmpty(persistedStartupUrl)) {
          programArgs.add(insertionIndex, ARG_STARTUP_URL);
          programArgs.add(insertionIndex + 1, persistedStartupUrl);
        }
      }
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    return null;
  }
}
