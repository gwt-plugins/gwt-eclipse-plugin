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
package com.google.gdt.eclipse.core.java;

import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Provides methods for working with resources based on classpath-relative
 * paths.
 */
public class ClasspathResourceUtilities {

  /**
   * Gets the classpath relative path of a given resource.
   * 
   * Note: This method does not consider build path exclusion/inclusion filters.
   * 
   * @param resource
   * @param javaProject
   * @return the classpath relative path, or null if it could not be determined
   * @throws JavaModelException
   * @see JavaModelSearch#isValidElement(org.eclipse.jdt.core.IJavaElement)
   */
  public static IPath getClasspathRelativePathOfResource(IResource resource,
      IJavaProject javaProject) throws JavaModelException {

    IPackageFragmentRoot root = getPackageFragmentRootForResource(resource,
        javaProject);
    if (root == null) {
      return null;
    }

    IResource rootResource = root.getResource();
    IPath rootProjectPath = rootResource.getProjectRelativePath();
    IPath resourceProjectPath = resource.getProjectRelativePath();

    if (rootProjectPath.isPrefixOf(resourceProjectPath)) {
      return resourceProjectPath.removeFirstSegments(rootProjectPath.segmentCount());
    } else {
      return null;
    }
  }

  /**
   * Determines whether the given path is a source file (not in a JAR).
   * 
   * @throws JavaModelException
   */
  public static boolean isFile(IPath classpathRelativePath,
      IJavaProject javaProject) throws JavaModelException {
    return ClasspathResourceUtilities.resolveFile(classpathRelativePath,
        javaProject) instanceof IFile;
  }

  /**
   * Determines whether the given file is on the given package fragment. The
   * file can be a Java file, class file, or a non-Java resource.
   * 
   * @param fileName the file name
   * @param pckgFragment the package fragment to search
   * @return whether the given file is on the given package fragment
   * @throws JavaModelException
   */
  public static boolean isFileOnPackageFragment(String fileName,
      IPackageFragment pckgFragment) throws JavaModelException {
    return resolveFileOnPackageFragment(fileName, pckgFragment) != null;
  }

  /**
   * Determines whether the given path is in a JAR.
   * 
   * @throws JavaModelException
   */
  public static boolean isInJar(IPath classpathRelativePath,
      IJavaProject javaProject) throws JavaModelException {
    return ClasspathResourceUtilities.resolveFile(classpathRelativePath,
        javaProject) instanceof IJarEntryResource;
  }

  /**
   * Determines if a resource is available on a project's classpath. This method
   * searches both source paths and JAR/ZIP files.
   */
  public static boolean isResourceOnClasspath(IJavaProject javaProject,
      IPath resourcePath) throws JavaModelException {
    String pckg = JavaUtilities.getPackageNameFromPath(resourcePath.removeLastSegments(1));
    String fileName = resourcePath.lastSegment();

    for (IPackageFragment pckgFragment : JavaModelSearch.getPackageFragments(
        javaProject, pckg)) {
      if (ClasspathResourceUtilities.isFileOnPackageFragment(fileName,
          pckgFragment)) {
        return true;
      }
    }

    // Not there
    return false;
  }

  public static IStorage resolveFile(IPath classpathRelativePath,
      IJavaProject javaProject) throws JavaModelException {
    String fileName = classpathRelativePath.lastSegment();
    String packageName = JavaUtilities.getPackageNameFromPath(classpathRelativePath.removeLastSegments(1));

    for (IPackageFragment packageFragment : JavaModelSearch.getPackageFragments(
        javaProject, packageName)) {
      IStorage file = resolveFileOnPackageFragment(fileName, packageFragment);
      if (file != null) {
        return file;
      }
    }

    return null;
  }

  /**
   * Returns the given file or JAR entry if it is on the given package fragment.
   * The file can be a Java file, class file, or a non-Java resource.
   * <p>
   * This method returns null for .java files or .class files inside JARs.
   * 
   * @param fileName the file name
   * @param pckgFragment the package fragment to search
   * @return the file as an IResource or IJarEntryResource, or null
   * @throws JavaModelException
   */
  public static IStorage resolveFileOnPackageFragment(String fileName,
      IPackageFragment pckgFragment) throws JavaModelException {

    boolean isJavaFile = JavaCore.isJavaLikeFileName(fileName);
    boolean isClassFile = ResourceUtils.endsWith(fileName, ".class");

    // Check the non-Java resources first
    Object[] nonJavaResources = pckgFragment.getNonJavaResources();
    for (Object nonJavaResource : nonJavaResources) {
      if (nonJavaResource instanceof IFile) {
        IFile file = (IFile) nonJavaResource;
        String resFileName = file.getName();

        if (ResourceUtils.areFilenamesEqual(resFileName, fileName)) {
          // Java source files that have been excluded from the build path
          // show up as non-Java resources, but we'll ignore them since
          // they're not available on the classpath.
          if (!JavaCore.isJavaLikeFileName(resFileName)) {
            return file;
          } else {
            return null;
          }
        }
      }

      // JAR resources are not IResource's, so we need to handle them
      // differently
      if (nonJavaResource instanceof IJarEntryResource) {
        IJarEntryResource jarEntry = (IJarEntryResource) nonJavaResource;
        if (jarEntry.isFile()
            && ResourceUtils.areFilenamesEqual(jarEntry.getName(), fileName)) {
          return jarEntry;
        }
      }
    }

    // If we're looking for a .java or .class file, we can use the regular
    // Java Model methods.

    if (isJavaFile) {
      ICompilationUnit cu = pckgFragment.getCompilationUnit(fileName);
      if (cu.exists()) {
        return (IFile) cu.getCorrespondingResource();
      }
    }

    if (isClassFile) {
      IClassFile cf = pckgFragment.getClassFile(fileName);
      if (cf.exists()) {
        return (IFile) cf.getCorrespondingResource();
      }
    }

    return null;
  }

  private static IPackageFragmentRoot getPackageFragmentRootForResource(
      IResource resource, IJavaProject javaProject) throws JavaModelException {

    while (resource != null) {
      IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(resource);
      /*
       * Not using JavaModelSearch.isValidElement since checking for exclusions
       * from the buildpath is the caller's responsibility.
       */
      if (root != null && root.exists()) {
        return root;
      }

      resource = resource.getParent();
    }

    return null;
  }
}
