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

import com.google.appengine.eclipse.core.launch.processors.HrdArgumentProcessor;
import com.google.appengine.eclipse.core.launch.processors.JavaAgentArgumentProcessor;
import com.google.appengine.eclipse.core.launch.processors.GoogleCloudSqlArgumentProcessor;
import com.google.appengine.eclipse.core.launch.processors.XBootclasspathArgumentProcessor;
import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.extensions.ExtensionQueryStringAttr;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gwt.eclipse.core.launch.processors.CodeServerPortArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.DGwtDevJarArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.LogLevelArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.ModuleArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.NoServerArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.RemoteUiArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.StartupUrlArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.XStartOnFirstThreadArgumentProcessor;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerLaunchConfiguration;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * TODO: consider splitting this into separate WebAppLaunchConfigurationUpdater
 * and SpeedTracerLaunchConfigurationUpdater.
 */
/**
 * Updates launch configurations by delegating to the many
 * {@link ILaunchConfigurationProcessor}s.
 */
public class LaunchConfigurationUpdater {

  /**
   * The types of launch configurations that the processors are capable of
   * handling.
   */
  public static final Set<String> APPLICABLE_LAUNCH_CONFIGURATION_TYPE_IDS = new HashSet<String>(
      Arrays.asList(new String[] {
          WebAppLaunchConfiguration.TYPE_ID,
          SpeedTracerLaunchConfiguration.TYPE_ID}));

  private static final List<ILaunchConfigurationProcessor> PROCESSORS = new ArrayList<ILaunchConfigurationProcessor>();

  static {
    // Ordering matters! E.g. startup URL depends on main type, so it must
    // appear after main type.
    ClasspathUtilities.ClassFinder classFinder = new ClasspathUtilities.ClassFinder();

    PROCESSORS.add(new ClasspathProviderProcessor());
    PROCESSORS.add(new MainTypeProcessor(classFinder));
    PROCESSORS.add(new ServerArgumentProcessor());
    PROCESSORS.add(new WarArgumentProcessor());
    PROCESSORS.add(new PortArgumentProcessor());
    PROCESSORS.add(new CodeServerPortArgumentProcessor());
    PROCESSORS.add(new NoServerArgumentProcessor());
    PROCESSORS.add(new LogLevelArgumentProcessor());
    PROCESSORS.add(new ModuleArgumentProcessor());
    PROCESSORS.add(new StartupUrlArgumentProcessor());
    PROCESSORS.add(new RemoteUiArgumentProcessor());
    PROCESSORS.add(new XStartOnFirstThreadArgumentProcessor());
    PROCESSORS.add(new JavaAgentArgumentProcessor());
    PROCESSORS.add(new XBootclasspathArgumentProcessor());
    PROCESSORS.add(new DGwtDevJarArgumentProcessor());
    PROCESSORS.add(new XmxArgumentProcessor());
    PROCESSORS.add(new HrdArgumentProcessor());
    PROCESSORS.add(new GoogleCloudSqlArgumentProcessor());
    addExternalProcessors();

    ExtensionQueryStringAttr extQuery = new ExtensionQueryStringAttr(
        GdtPlugin.PLUGIN_ID, "launchConfigurationType", "launchId");
    List<ExtensionQuery.Data<String>> launchIds = extQuery.getData();
    for (ExtensionQuery.Data<String> launchId : launchIds) {
      APPLICABLE_LAUNCH_CONFIGURATION_TYPE_IDS.add(launchId.getExtensionPointData().trim());
    }
  }

  private static void addExternalProcessors() {
    ExtensionQuery<ILaunchConfigurationProcessor> extQuery = new ExtensionQuery<ILaunchConfigurationProcessor>(
        GdtPlugin.PLUGIN_ID, "launchConfigVmArgProcessor", "class");

    List<ExtensionQuery.Data<ILaunchConfigurationProcessor>> launchConfigProcessors = extQuery.getData();
    for (ExtensionQuery.Data<ILaunchConfigurationProcessor> processor : launchConfigProcessors) {
      PROCESSORS.add(processor.getExtensionPointData());
    }
  }

  private final ILaunchConfiguration launchConfig;

  private final List<String> programArgs;

  private final List<String> vmArgs;

  private final IJavaProject javaProject;

  public LaunchConfigurationUpdater(ILaunchConfiguration launchConfig,
      IJavaProject javaProject) throws CoreException {
    this.launchConfig = launchConfig;
    this.javaProject = javaProject;

    programArgs = LaunchConfigurationProcessorUtilities.parseProgramArgs(launchConfig);
    vmArgs = LaunchConfigurationProcessorUtilities.parseVmArgs(launchConfig);
  }

  /**
   * Returns the launch configuration for this updater.
   */
  public ILaunchConfiguration getLaunchConfiguration() {
    return launchConfig;
  }

  /**
   * Updates the launch configuration by delegating to each
   * {@link ILaunchConfigurationProcessor}.
   * <p>
   * This method saves the launch configuration's working copy.
   * 
   * @throws CoreException
   */
  public void update() throws CoreException {
    ILaunchConfigurationWorkingCopy launchConfigWc = launchConfig.getWorkingCopy();

    for (ILaunchConfigurationProcessor processor : PROCESSORS) {
      try {
        processor.update(launchConfigWc, javaProject, programArgs, vmArgs);
      } catch (Throwable e) {
        GdtPlugin.getLogger().logError(e,
            "Launch configuration processor failed to run");
      }
    }

    launchConfigWc.setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
        LaunchConfigurationProcessorUtilities.createArgsString(programArgs));
    launchConfigWc.setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
        LaunchConfigurationProcessorUtilities.createArgsString(vmArgs));

    launchConfigWc.doSave();
  }

  public String validate() {
    for (ILaunchConfigurationProcessor processor : PROCESSORS) {
      String msg;
      try {
        msg = processor.validate(launchConfig, javaProject, programArgs, vmArgs);
        if (msg != null) {
          return msg;
        }
      } catch (CoreException e) {
        GdtPlugin.getLogger().logError(e,
            "Launch configuration processor failed to validate");
      }
    }

    return null;
  }
}

