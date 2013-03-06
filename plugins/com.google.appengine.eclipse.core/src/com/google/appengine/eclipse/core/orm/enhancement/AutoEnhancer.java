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
package com.google.appengine.eclipse.core.orm.enhancement;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.JavaUtilities;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs enhancement of classes in response to build events. This builder
 * supports incremental and full builds. It also checks for project settings
 * that limit the number of classes that it will try to enhance.
 * 
 * NOTE: This builder assumes that the Java builder has already executed by the
 * time this builder kicks off.
 */
@SuppressWarnings("restriction")
public class AutoEnhancer extends IncrementalProjectBuilder {

  /**
   * Determine the path of the Java source file that produced the given class
   * file path, assuming the class file was generated in the given output
   * location and the source file lives in the given source folder.
   */
  private static IPath computeJavaFilePath(IPath classFilePath,
      IPath outputLocation, IPath srcFolderPath) {
    IPath classpathRelativePath = classFilePath.removeFirstSegments(outputLocation.segmentCount());
    String name = classFilePath.lastSegment();
    int enclosingTypeSeparatorPos = name.indexOf('$');
    if (enclosingTypeSeparatorPos != -1) {
      name = name.substring(0, enclosingTypeSeparatorPos);
    } else {
      assert (name.indexOf('.') > 0);
      name = name.substring(0, name.indexOf('.'));
    }
    name = name + ".java";

    classpathRelativePath = classpathRelativePath.removeLastSegments(1).append(
        name);
    IPath javaFilePath = srcFolderPath.append(classpathRelativePath);

    // Only return the computed Java file path if we can find the resource
    if (ResourcesPlugin.getWorkspace().getRoot().findMember(javaFilePath) != null) {
      return javaFilePath;
    }

    return null;
  }

  private static IPath getOutputLocation(IClasspathEntry classpathEntry,
      IPath defaultOutputLocation) {
    IPath outputFolder = classpathEntry.getOutputLocation();
    if (outputFolder == null) {
      outputFolder = defaultOutputLocation;
    }

    return outputFolder;
  }

  private char[][] inclusionPatterns;

  private IJavaProject javaProject;

  @SuppressWarnings("unchecked")
  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
      throws CoreException {
    IProject project = getProject();
    if (project == null || !project.isAccessible()) {
      return null;
    }

    assert (GaeNature.isGaeProject(project));

    javaProject = JavaCore.create(project);
    if (!javaProject.exists()) {
      return null;
    }

    GaeSdk sdk = GaeSdk.findSdkFor(javaProject);
    if (sdk == null || !sdk.validate().isOK()) {
      // Don't enhance GAE projects that have missing or invalid SDKs
      return null;
    }

    List<IPath> inclusionPatternsList = GaeProjectProperties.getOrmEnhancementInclusionPatterns(javaProject.getProject());
    if (inclusionPatternsList.isEmpty()) {
      return null;
    } else {
      inclusionPatterns = new char[inclusionPatternsList.size()][];
      for (int i = 0; i < inclusionPatternsList.size(); ++i) {
        inclusionPatterns[i] = inclusionPatternsList.get(i).toString().toCharArray();
      }
    }

    Set<String> pathsToEnhance;
    IResourceDelta delta = getDelta(getProject());
    if (kind == IncrementalProjectBuilder.FULL_BUILD || delta == null) {
      pathsToEnhance = computeEnhancementPathsForFullBuild();
    } else {
      pathsToEnhance = computeEnhancementPathsForIncrementalBuild(delta);
    }

    if (!pathsToEnhance.isEmpty()) {
      Job job = new EnhancerJob(javaProject, pathsToEnhance);
      job.setPriority(Job.SHORT);
      job.setRule(ResourcesPlugin.getWorkspace().getRoot());
      job.schedule();
    }

    return null;
  }

  private String computeEnhancementPathFromClassFile(IResource classFile) {
    return classFile.getLocation().toOSString();
  }

  private Set<String> computeEnhancementPathsForFullBuild()
      throws CoreException {
    Set<IPath> scannedOutputLocations = new HashSet<IPath>();
    final Set<String> pathsToEnhance = new HashSet<String>();

    for (final IClasspathEntry classpathEntry : javaProject.getRawClasspath()) {
      if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
        // Skip any classpath entry that is not a source entry
        continue;
      }

      final IPath outputLocation = getOutputLocation(classpathEntry,
          javaProject.getOutputLocation());

      if (!scannedOutputLocations.add(outputLocation)) {
        // Skip the output folder if we've already scanned it
        continue;
      }

      IFolder outputFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(
          outputLocation);
      if (outputFolder.exists()) {
        outputFolder.accept(new IResourceProxyVisitor() {
          public boolean visit(IResourceProxy proxy) throws CoreException {
            if (proxy.getType() != IResource.FILE) {
              return true;
            }

            if (JavaUtilities.isClassFileName(proxy.getName())) {
              IResource classFile = proxy.requestResource();
              IPath javaFilePath = computeJavaFilePath(classFile.getFullPath());
              if (shouldEnhanceClass(javaFilePath)) {
                pathsToEnhance.add(computeEnhancementPathFromClassFile(classFile));
              }
            }

            return false;
          }
        }, 0);
      }
    }

    return pathsToEnhance;
  }

  private Set<String> computeEnhancementPathsForIncrementalBuild(
      IResourceDelta delta) throws CoreException {
    final Set<String> pathsToEnhance = new HashSet<String>();
    IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
      public boolean visit(IResourceDelta delta) throws CoreException {
        // We get deltas for removed resources when the user changes the build
        // output location and selects Yes when asked whether the old output
        // location should be deleted. We don't want to enhance stale output.
        if (delta.getKind() == IResourceDelta.REMOVED) {
          return false;
        }

        IResource resource = delta.getResource();
        if (resource == null) {
          return true;
        }

        if (resource.getType() != IResource.FILE) {
          return true;
        }

        if (JavaUtilities.isClassFileName(resource.getName())) {
          IPath javaFilePath = computeJavaFilePath(resource.getFullPath());
          if (shouldEnhanceClass(javaFilePath)) {
            String enhancementPath = computeEnhancementPathFromClassFile(resource);
            pathsToEnhance.add(enhancementPath);
          }
        }

        return false;
      }
    };
    delta.accept(visitor);
    return pathsToEnhance;
  }

  /**
   * Determines the path of the Java source file that produced the given class
   * file path. We don't know which source folder contains the original Java
   * source, so we'll just try each of them until we find it.
   * 
   * @return the path of the Java source file, or <code>null</code> if a
   *         corresponding Java source file could not be found under any of the
   *         project's source folders.
   */
  private IPath computeJavaFilePath(IPath classFilePath)
      throws JavaModelException {
    for (IClasspathEntry classpathEntry : javaProject.getRawClasspath()) {
      if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
        // Skip any classpath entry that is not a source entry
        continue;
      }

      IPath outputLocation = getOutputLocation(classpathEntry,
          javaProject.getOutputLocation());

      IPath javaFilePath = computeJavaFilePath(classFilePath, outputLocation,
          classpathEntry.getPath());
      if (javaFilePath != null
          && ResourcesPlugin.getWorkspace().getRoot().findMember(javaFilePath) != null) {
        return javaFilePath;
      }
    }

    AppEngineCorePluginLog.logWarning("Could not find Java source for class file "
        + classFilePath.toString());
    return null;
  }

  /**
   * Answers whether the class file for a particular Java source file should be
   * enhanced. Right now we just check the Java file path against the defined
   * ORM inclusion patterns, but in the future we could possibly look at ORM
   * annotations in the file, or use some other more specific heuristic.
   */
  private boolean shouldEnhanceClass(IPath javaFilePath) {
    if (javaFilePath == null) {
      // If we can't find a Java source file, go ahead and enhance anyway
      return true;
    }

    // NOTE: inclusionPatterns cannot be null, or this method will automatically
    // return false and cause us to include *all* classes, instead of none.
    boolean shouldEnhance = !Util.isExcluded(
        javaFilePath.removeFirstSegments(1).makeRelative(), inclusionPatterns,
        null, false);
    return shouldEnhance;
  }

}