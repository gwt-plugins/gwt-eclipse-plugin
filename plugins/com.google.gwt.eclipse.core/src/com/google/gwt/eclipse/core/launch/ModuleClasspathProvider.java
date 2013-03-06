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
package com.google.gwt.eclipse.core.launch;

import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.GWTProjectUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.runtime.GWTProjectsRuntime;
import com.google.gwt.eclipse.core.runtime.RuntimeClasspathEntryResolver;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardClasspathProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates the runtime classpath based on the module path.
 */
public class ModuleClasspathProvider extends StandardClasspathProvider {

  /**
   * Provides an id of a classpath provider (for the
   * org.eclipse.jdt.launching.classpathProvider extension point) for plugins
   * that want to override the default runtime classpath computation.
   */
  public interface IModuleClasspathProviderIdProvider {
    String getProviderId(IProject project);
  }

  // This is the provider ID of the ModuleClasspathProvider.
  private static final String PROVIDER_ID = GWTPlugin.PLUGIN_ID
      + ".moduleClasspathProvider";
  
  // This is the id of the moduleClasspathProvider extension point.
  private static final String EXTENSION_ID = "com.google.gwt.eclipse.core.moduleClasspathProvider";

  /**
   * Given a <code>project</code> instance, determines the provider id of a
   * classpath provider that is contributed to the environment via the
   * <code>org.eclipse.jdt.launching.classpathProvider<code> extension point.
   * 
   * The method will first query for implementers of the <code>com.google.gwt.eclipse.core.moduleClasspathProvider</code>
   * extension point, to see if an "external" classpath provider is available.
   * If so, it's provider id will be returned. If not, the id corresponding to
   * <code>ModuleClasspathProvider</code> will be returned.
   */
  public static String computeProviderId(IProject project) {
    List<ModuleClasspathProviderData> providers = Lists.newArrayList();
    if (project != null) {
      IExtensionRegistry registry = Platform.getExtensionRegistry();
      IConfigurationElement[] extensions = 
        registry.getConfigurationElementsFor(EXTENSION_ID);
      for (IConfigurationElement configurationElement : extensions) {
        ModuleClasspathProviderData entry = new ModuleClasspathProviderData(configurationElement);  
        if (entry.isProviderAvailable()) {
          providers.add(entry);
        }
      }
      
      // Sort by provider priority, highest to lowest.
      Collections.sort(providers);
      Collections.reverse(providers);

      for (ModuleClasspathProviderData provider : providers) {
        if (provider.getProvider().getProviderId(project) != null) {
          return provider.getProvider().getProviderId(project);
        }
      }
    }

    return PROVIDER_ID;
  }

  /**
   * Given a list of classpath entries, with all bootstrap entries before any
   * user entries, returns the index of the first user classpath entry.
   */
  private static int findIndexOfFirstUserEntry(
      List<IRuntimeClasspathEntry> entries) {
    for (int i = 0, size = entries.size(); i < size; i++) {
      if (entries.get(i).getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {
        return i;
      }
    }
    return entries.size();
  }

  @Override
  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(
      ILaunchConfiguration config) throws CoreException {
    IRuntimeClasspathEntry[] unresolvedClasspathEntries = super.computeUnresolvedClasspath(config);
    IJavaProject proj = JavaRuntime.getJavaProject(config);
    if (proj == null || !proj.getProject().hasNature(GWTNature.NATURE_ID)) {
      // Only GWT projects require source folders to be computed
      return unresolvedClasspathEntries;
    }

    /*
     * Figure out if we are supposed to be relying on the default classpath or
     * not. The default classpath is the one that is generated for a launch
     * configuration based on the launch configuration's project's build
     * classpath.
     * 
     * To determine whether or not to rely on the default classpath, we look at
     * the ATTR_DEFAULT_CLASSPATH attribute of the launch configuration. This
     * attribute is set whenever the user makes a change to the launch
     * configuration classpath using the add/remove buttons. From this point on,
     * Eclipse will respect the user's changes and will not replace their
     * entries with the classpath that it computes.
     * 
     * However, users can specify that they want to restore the behavior of
     * having Eclipse compute the classpath by clicking on the "Restore Default
     * Entries" button. This causes the ATTR_DEFAULT_ATTRIBUTE to be unset for a
     * launch configuration.
     */
    boolean useDefault = config.getAttribute(
        IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);

    if (!useDefault) {
      return unresolvedClasspathEntries;
    }

    /*
     * Compute the default classpath for the launch configuration. Note that all
     * of the entries for the default classpath DO NOT appear under the 'default
     * entries' section. This is because we are going to be adding the
     * GWT-related source paths to the classpath, and we want to give users the
     * opportunity to tweak them. If we add them to the 'default entries'
     * section, they will be unable to tweak them.
     * 
     * You might think that adding the source paths to the non-default section
     * would cause Eclipse to think that the user actually modified the
     * classpath, thereby causing the ATTR_DEFAULT_CLASSPATH attribute to be
     * changed. This is not the case; this attribute is only changed based on UI
     * interaction, so it is safe for us to add entries to the non-default
     * section programmatically.
     */
    ArrayList<IRuntimeClasspathEntry> defaultRuntimeClasspathEntries = new ArrayList<IRuntimeClasspathEntry>();
    defaultRuntimeClasspathEntries.addAll(Arrays.asList(unresolvedClasspathEntries));

    /*
     * Now, record the source folder(s) of each of the transitively required
     * projects.
     * 
     * Make sure that the source paths come before the default classpath entries
     * so users can override GWT functionality. They also must appear after any
     * existing bootstrap entries (e.g. the JRE), since that's the order the
     * JavaClasspathTab will expect them to be in when it goes to calculate
     * whether or not the configured classpath = default.
     */
    int srcPathsInsertionIndex = findIndexOfFirstUserEntry(defaultRuntimeClasspathEntries);

    try {
      defaultRuntimeClasspathEntries.addAll(
          srcPathsInsertionIndex,
          GWTProjectUtilities.getGWTSourceFolderPathsFromProjectAndDependencies(
              proj,
              GWTJUnitLaunchDelegate.isJUnitLaunchConfig(config.getType())));
    } catch (SdkException e) {
      GWTPluginLog.logError(e);
    }

    return defaultRuntimeClasspathEntries.toArray(new IRuntimeClasspathEntry[defaultRuntimeClasspathEntries.size()]);
  }

  @Override
  public IRuntimeClasspathEntry[] resolveClasspath(
      IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration)
      throws CoreException {

    IRuntimeClasspathEntry[] resolvedEntries = super.resolveClasspath(entries,
        configuration);

    /*
     * In the event that we're trying to compute the classpath for a launch
     * config that is associated with one of the GWT Runtime projects, we need
     * to manually call our RuntimeClasspathEntryResolver, because such projects
     * do not have a GWT SDK on the classpath.
     */
    IJavaProject proj = JavaRuntime.getJavaProject(configuration);
    if (GWTProjectsRuntime.isGWTRuntimeProject(proj)) {

      // Use a LinkedHashSet to prevent dupes
      Set<IRuntimeClasspathEntry> all = new LinkedHashSet<IRuntimeClasspathEntry>(
          entries.length);
      RuntimeClasspathEntryResolver resolver = new RuntimeClasspathEntryResolver();
      for (IRuntimeClasspathEntry resolvedEntry : resolvedEntries) {

        IRuntimeClasspathEntry[] gwtRuntimeResolvedEntries = resolver.resolveRuntimeClasspathEntry(
            resolvedEntry, configuration);

        if (gwtRuntimeResolvedEntries.length == 0) {
          // Nothing new for the GWT RuntimeClasspathEntryResolver to
          // contribute; preserve the original resolved entry.
          all.add(resolvedEntry);
        } else {
          all.addAll(Arrays.asList(gwtRuntimeResolvedEntries));
        }
      }
      resolvedEntries = all.toArray(new IRuntimeClasspathEntry[all.size()]);
    }
    return resolvedEntries;
  }

}
