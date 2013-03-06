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
package com.google.gwt.eclipse.core.speedtracer;

import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationAttribute;
import com.google.gdt.eclipse.core.launch.ILaunchShortcutStrategy;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationAttributeUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.ILaunchShortcutStrategyProvider;
import com.google.gwt.eclipse.core.launch.LegacyGWTLaunchShortcutStrategy;
import com.google.gwt.eclipse.core.launch.ModuleClasspathProvider;
import com.google.gwt.eclipse.core.launch.WebAppLaunchShortcutStrategy;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for Speed Tracer launch configurations.
 */
public final class SpeedTracerLaunchConfiguration {

  /**
   * Speed Tracer-specific attributes. Use
   * {@link ILaunchConfigurationWorkingCopy#setAttribute} to set these.
   */
  public enum Attribute implements ILaunchConfigurationAttribute {
    URL(""), BROWSER(""), PERFORM_GWT_COMPILE(true);

    private final Object defaultValue;

    private Attribute(Object defaultValue) {
      this.defaultValue = defaultValue;
    }

    public Object getDefaultValue() {
      return defaultValue;
    }

    public String getQualifiedName() {
      return GWTPlugin.PLUGIN_ID + "." + name();
    }
  }

  public static String TYPE_ID = GWTPlugin.PLUGIN_ID
      + ".speedtracer.speedTracerLaunch";

  /*
   * TODO: a lot of this can be combined with the Web App launch config
   * management.
   */
  /**
   * This method may prompt the user.
   * 
   * @return an existing Speed Tracer launch configuration, or a creates a new
   *         one
   * @throw OperationCanceledException
   */
  public static ILaunchConfiguration findOrCreateLaunchConfiguration(
      IResource resource) throws CoreException, OperationCanceledException {

    /*
     * Implementation details: This code relies on the fact that generateUrl
     * will ask the user for an HTML/JSP resource if the given resource does not
     * cleanly map to one. (This is what WebAppLaunchShortcut ends up doing.)
     */
    String url = generateUrl(resource);
    if (url == null) {
      throw new OperationCanceledException();
    }

    ILaunchConfiguration config = findLaunchConfiguration(url,
        resource.getProject());
    if (config != null) {
      return config;
    }

    IProject project = resource.getProject();
    config = createLaunchConfiguration(computeName(resource), url, project);
    return config;
  }

  /**
   * Generates an appropriate URL based on whether the project is a web app.
   * 
   * @see ILaunchShortcutStrategy#generateUrl(IResource,boolean)
   */
  public static String generateUrl(IResource resource) throws CoreException {
    ILaunchShortcutStrategy strategy = null;
    IProject project = resource.getProject();

    ExtensionQuery<ILaunchShortcutStrategyProvider> extQuery = new ExtensionQuery<ILaunchShortcutStrategyProvider>(
        GWTPlugin.PLUGIN_ID, "launchShortcutStrategy", "class");
    List<ExtensionQuery.Data<ILaunchShortcutStrategyProvider>> strategyProviderInfos = extQuery.getData();

    for (ExtensionQuery.Data<ILaunchShortcutStrategyProvider> data : strategyProviderInfos) {
      strategy = data.getExtensionPointData().getStrategy(project);
      break;
    }

    if (strategy == null) {
      if (WebAppUtilities.isWebApp(project)) {
        strategy = new WebAppLaunchShortcutStrategy();
      } else {
        assert (GWTNature.isGWTProject(project));
        strategy = new LegacyGWTLaunchShortcutStrategy();
      }
    }

    return strategy.generateUrl(resource, false);
  }

  /**
   * Returns the absolute URL to the final page that will be profiled using
   * Speed Tracer
   */
  public static String getAbsoluteUrl(ILaunchConfiguration config)
      throws CoreException {
    int port;
    try {
      port = Integer.parseInt(WebAppLaunchConfiguration.getServerPort(config));
    } catch (NumberFormatException e) {
      throw new CoreException(
          new Status(
              IStatus.ERROR,
              GWTPlugin.PLUGIN_ID,
              "Could not determine server port, please make sure it is a valid port number.",
              e));
    }

    String url = LaunchConfigurationAttributeUtilities.getString(config,
        Attribute.URL);
    if (url == null) {
      return "http://localhost:" + port;
    }

    if (url.toLowerCase().startsWith("http")) {
      return url;
    }

    return new StringBuilder("http://localhost:").append(port).append('/').append(
        url).toString();
  }

  /**
   * @return a new Speed Tracer launch configuration.
   */
  static ILaunchConfiguration createLaunchConfiguration(String name,
      String url, IProject project) throws CoreException {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(TYPE_ID);

    ILaunchConfigurationWorkingCopy wc = type.newInstance(null, name);

    LaunchConfigurationUtilities.setProjectName(wc, project.getName());
    LaunchConfigurationAttributeUtilities.set(wc,
        SpeedTracerLaunchConfiguration.Attribute.URL, url);
    LaunchConfigurationAttributeUtilities.set(wc,
        SpeedTracerLaunchConfiguration.Attribute.BROWSER,
        BrowserUtilities.findChromeBrowserName());

    /*
     * Though it is a little aggressive to use this classpath provider (it adds
     * source directories to the classpath, which are not needed in the ST
     * case), it handles the more important cases, such as adding gwt-dev.jar
     * and appengine-tools-api.jar to the runtime classpath when they're
     * missing.
     */
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
        ModuleClasspathProvider.computeProviderId(project));

    // Link the launch configuration to the project. This will cause the
    // launch config to be deleted automatically if the project is deleted.
    wc.setMappedResources(new IResource[] {project});

    return wc.doSave();
  }

  /**
   * Finds a launch configuration matching the given data. If there are multiple
   * matches, the user may be prompted to select one.
   * 
   * @return an existing Speed Tracer launch configuration for the given
   *         resource, or null
   * 
   * @throw OperationCanceledException
   */
  static ILaunchConfiguration findLaunchConfiguration(String url,
      IProject project) throws CoreException, OperationCanceledException {

    List<ILaunchConfiguration> matchingConfigs = new ArrayList<ILaunchConfiguration>();
    List<ILaunchConfiguration> allConfigs = LaunchConfigurationUtilities.getLaunchConfigurations(
        project, SpeedTracerLaunchConfiguration.TYPE_ID);
    for (ILaunchConfiguration config : allConfigs) {
      String configUrl = LaunchConfigurationAttributeUtilities.getString(
          config, Attribute.URL);
      if (url.equals(configUrl)) {
        matchingConfigs.add(config);
      }
    }

    switch (matchingConfigs.size()) {
      case 0:
        return null;

      case 1:
        return matchingConfigs.get(0);

      default:
        ILaunchConfiguration chosenConfig = LaunchConfigurationUtilities.chooseConfiguration(
            matchingConfigs, Display.getDefault().getActiveShell());
        if (chosenConfig != null) {
          return chosenConfig;
        } else {
          throw new OperationCanceledException();
        }
    }
  }

  private static String computeName(IResource resource) {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    return manager.generateUniqueLaunchConfigurationNameFrom(resource.getName()
        + " using Speed Tracer");
  }

  private SpeedTracerLaunchConfiguration() {
  }

}
