/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.core.orm.enhancement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.console.MessageConsole;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.ProcessUtilities;
import com.google.gdt.eclipse.core.console.MessageConsoleUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.extensions.ExtensionQueryWithElement;
import com.google.gdt.eclipse.maven.MavenUtils;

/**
 * Job that performs a datanucleus enhancement on a set <code>.class</code> files.
 */
public class EnhancerJob extends WorkspaceJob {
  
  private static final Pattern DATANUCLEUS_JAR_PATTERN =
      Pattern.compile(
          "datanucleus/datanucleus-(?<part>[a-z\\-]+)/(?<version>[^/]+)/datanucleus-\\k<part>-\\k<version>\\.jar$");
  
  private static final Set<String> DATANUCLEUS_PARTS_PACKAGED_WITH_APP_ENGINE =
      ImmutableSet.of("api-jdo", "api-jpa", "appengine", "core");

  /**
   * Interface for extension points to provide a classpath for the Datanucleus Enhancer.
   */
  public interface IEnhancerJobCpFinder {
    List<String> buildClasspath(IJavaProject javaProject);
  }
  /**
   * Interface for extension points to provide a Datanucleus version to be used with Datanucleus
   * Enhancer.
   */
  public interface IEnhancerJobDatanucleusVersionProvider {
    String getVersion(IJavaProject javaProject);
  }

  private static final String NAME = "DataNucleus Enhancer";

  private static final String MAINCLASS = "com.google.appengine.tools.enhancer.Enhance";

  private static final String DATANUCLEUS_VERSION_ARG = "-enhancerVersion";

  private static List<String> buildClasspath(IJavaProject javaProject) throws CoreException {

    ExtensionQuery<EnhancerJob.IEnhancerJobCpFinder> extQuery = new ExtensionQueryWithElement<EnhancerJob.IEnhancerJobCpFinder>(
        AppEngineCorePlugin.PLUGIN_ID, "enhancerJobExtension", "enhancerJobCpFinder", "class");
    List<ExtensionQuery.Data<EnhancerJob.IEnhancerJobCpFinder>> enhancerJobCpFinders = extQuery.getData();
    for (ExtensionQuery.Data<EnhancerJob.IEnhancerJobCpFinder> enhancerJobCpFinder : enhancerJobCpFinders) {
      List<String> extensionPointEnhancerCp = enhancerJobCpFinder.getExtensionPointData().buildClasspath(
          javaProject);
      if (extensionPointEnhancerCp != null) {
        return extensionPointEnhancerCp;
      }
    }

    List<String> unfilteredClasspath = LaunchUtilities.getDefaultClasspath(javaProject);
    // If this classpath contains certain enhancer JARs, they must be removed from the classpath
    // that will be used for invoking the enhancer, because conflicting JARs from the Maven
    // repository are added to the classpath by some other mechanism that we do not understand and
    // possibly cannot control.
    return MavenUtils.hasMavenNature(javaProject.getProject()) ?
        removeDatanucleusJars(unfilteredClasspath) : unfilteredClasspath;
  }
  
  private static final List<String> removeDatanucleusJars(List<String> input) {
    List<String> result = Lists.newArrayListWithCapacity(input.size());
    for (String classpathItem : input) {
      Matcher m = DATANUCLEUS_JAR_PATTERN.matcher(classpathItem);
      boolean matches = m.find();
      if (!(matches && DATANUCLEUS_PARTS_PACKAGED_WITH_APP_ENGINE.contains(m.group("part")))) {
        result.add(classpathItem);
      }
    }
    return result;
  }

  /**
   * Traverses for enhancerJobExtension extensions to determine Datanucleus version used in
   * <code>javaProject</code>. Extension implementors are responsible to return a proper version
   * only for Java project they can understand. Must return <code>null</code> in other cases.
   */
  private static String getDatanucleusVersion(IJavaProject javaProject) throws CoreException {
    ExtensionQuery<EnhancerJob.IEnhancerJobDatanucleusVersionProvider> extQuery = new ExtensionQueryWithElement<EnhancerJob.IEnhancerJobDatanucleusVersionProvider>(
        AppEngineCorePlugin.PLUGIN_ID, "enhancerJobExtension", "datanucleusVersionProvider",
        "class");
    List<ExtensionQuery.Data<EnhancerJob.IEnhancerJobDatanucleusVersionProvider>> providers = extQuery.getData();
    for (ExtensionQuery.Data<EnhancerJob.IEnhancerJobDatanucleusVersionProvider> provider : providers) {
      String version = provider.getExtensionPointData().getVersion(javaProject);
      if (version != null) {
        return version;
      }
    }
    return null;
  }

  private final IJavaProject javaProject;

  private final Set<String> pathsToEnhance;

  public EnhancerJob(IJavaProject javaProject, Set<String> pathsToEnhance) {
    super(NAME);
    this.javaProject = javaProject;
    this.pathsToEnhance = pathsToEnhance;
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

    List<String> classpath = buildClasspath(javaProject);

    try {
      List<String> commands = new ArrayList<String>();
      // Add the path to the java executable
      commands.add(ProcessUtilities.computeJavaExecutableFullyQualifiedPath(javaProject));

      // Add the classpath
      commands.add("-cp");
      commands.add(ProcessUtilities.buildClasspathString(classpath));

      // Add the main class
      commands.add(MAINCLASS);

      // Add the input files
      commands.addAll(pathsToEnhance);

      // Add the datanucleus version number if it is not empty.
      String datanucleusVersion = getDatanucleusVersion(javaProject);
      if (datanucleusVersion != null && !datanucleusVersion.isEmpty()) {
        commands.add(DATANUCLEUS_VERSION_ARG);
        commands.add(datanucleusVersion);
      }

      MessageConsole messageConsole = MessageConsoleUtilities.getMessageConsole(
          javaProject.getElementName() + " - Datanucleus Enhancement", null);

      IPath projectLocation = javaProject.getProject().getLocation();

      int exitCode =
          ProcessUtilities.launchProcessAndActivateOnError(
              commands, projectLocation.toFile(), messageConsole);
      
      if (exitCode != 0) {
        AppEngineCorePluginLog.logError(
            "Datanucleus enhancer terminated with exit code " + exitCode
            + ".\nNavigate to the console view for \"Datanucleus Enhancer\" for more information");
      }

      refreshEnhancedResources(monitor);

    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, AppEngineCorePlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    } catch (InterruptedException e) {
      throw new CoreException(new Status(IStatus.ERROR, AppEngineCorePlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    }
    return Status.OK_STATUS;
  }

  /**
   * After enhancement is done we have to refresh possibly changed resources. This won't trigger
   * another build.
   */
  private void refreshEnhancedResources(IProgressMonitor monitor) throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for (String pathString : pathsToEnhance) {
      IFile file = root.getFileForLocation(new Path(pathString));
      if (file != null && !file.isSynchronized(IResource.DEPTH_INFINITE)) {
        file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      }
    }
  }
}
