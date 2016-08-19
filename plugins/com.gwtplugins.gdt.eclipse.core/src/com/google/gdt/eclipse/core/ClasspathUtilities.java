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
package com.google.gdt.eclipse.core;

import com.google.common.base.Predicate;
import com.google.gdt.eclipse.core.java.ClasspathChangedListener;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@SuppressWarnings("restriction")
public final class ClasspathUtilities {

  /*
   * Implemented as a class to allow unit tests to mock out different class
   * loading scenarios.
   */
  /**
   * Finds classes in a class loader.
   */
  public static class ClassFinder {
    public boolean exists(ClassLoader classLoader, String className) {
      return find(classLoader, className) != null;
    }

    /**
     * @return the {@link Class} object for the specified <code>className</code>
     *         using the given {@link ClassLoader}, or null.
     */
    public Class<?> find(ClassLoader classLoader, String className) {
      try {
        return Class.forName(className, false, classLoader);
      } catch (ClassNotFoundException e) {
        return null;
      }
    }
  }

  private static final IClasspathEntry[] NO_ENTRIES = new IClasspathEntry[0];

  /**
   * Finds all the classpath containers in the specified project that match the
   * provided container ID.
   * 
   * @param javaProject the project to query
   * @param containerId The container ID we are trying to match.
   * @return an array of matching classpath containers.
   */
  public static IClasspathEntry[] findClasspathContainersWithContainerId(
      IJavaProject javaProject, final String containerId)
      throws JavaModelException {

    Predicate<IClasspathEntry> matchPredicate = new Predicate<IClasspathEntry>() {
      public boolean apply(IClasspathEntry entry) {
        if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
          IPath containerPath = entry.getPath();
          if (containerPath.segmentCount() > 0
              && containerPath.segment(0).equals(containerId)) {
            return true;
          }
        }
        return false;
      }
    };

    IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
    int matchCount = 0;

    for (int i = 0; i < classpathEntries.length; i++) {
      if (matchPredicate.apply(classpathEntries[i])) {
        matchCount++;
      }
    }

    IClasspathEntry[] matchingClasspathEntries = new IClasspathEntry[matchCount];
    int matchingClasspathEntriesIdx = 0;
    for (int i = 0; i < classpathEntries.length; i++) {
      if (matchPredicate.apply(classpathEntries[i])) {
        matchingClasspathEntries[matchingClasspathEntriesIdx] = classpathEntries[i];
        matchingClasspathEntriesIdx++;
      }
    }

    return matchingClasspathEntries;
  }

  /**
   * Returns the {@link IClasspathEntry#CPE_CONTAINER} entry with the specified
   * container ID or <code>null</code> if one could not be found.
   * 
   * @param classpathEntries array of classpath entries
   * @param containerId container ID
   * @return {@link IClasspathEntry#CPE_CONTAINER} entry with the specified
   *         container ID or <code>null</code> if one could not be found
   */
  public static IClasspathEntry findClasspathEntryContainer(
      IClasspathEntry[] classpathEntries, String containerId) {
    int index = indexOfClasspathEntryContainer(classpathEntries, containerId);
    if (index >= 0) {
      return classpathEntries[index];
    }
    return null;
  }

  /**
   * Return the raw classpath entry on the project's classpath that contributes
   * the given type to the project.
   * 
   * @param javaProject The java project
   * @param fullyQualifiedName The fully-qualified type name
   * @return The raw classpath entry that contributes the type to the project,
   *         or <code>null</code> if no such classpath entry can be found.
   * @throws JavaModelException
   */
  public static IClasspathEntry findRawClasspathEntryFor(
      IJavaProject javaProject, String fullyQualifiedName)
      throws JavaModelException {
    IType type = javaProject.findType(fullyQualifiedName);
    if (type != null) {
      IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);

      JavaProject jProject = (JavaProject) javaProject;

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      for (IClasspathEntry rawClasspathEntry : rawClasspath) {
        IClasspathEntry[] resolvedClasspath = jProject.resolveClasspath(new IClasspathEntry[] {rawClasspathEntry});
        IPackageFragmentRoot[] computePackageFragmentRoots = jProject.computePackageFragmentRoots(
            resolvedClasspath, true, null);
        if (Arrays.asList(computePackageFragmentRoots).contains(
            packageFragmentRoot)) {
          return rawClasspathEntry;
        }
      }

      return packageFragmentRoot.getRawClasspathEntry();
    }

    return null;
  }

  /**
   * Returns a String (in the format of the JVM classpath argument) which
   * contains the given classpath entries.
   * 
   * @param classpathEntries array of runtime classpath entries
   * @return flattened String of the given classpath entries in the format
   *         suitable for passing as a JVM argument
   */
  public static String flattenToClasspathString(
      List<IRuntimeClasspathEntry> classpathEntries) {
    StringBuilder sb = new StringBuilder();
    boolean needsSeparator = false;
    for (IRuntimeClasspathEntry r : classpathEntries) {
      if (needsSeparator) {
        sb.append(File.pathSeparatorChar);
      }
      needsSeparator = true;
      sb.append(r.getLocation());
    }

    return sb.toString();
  }

  public static IClasspathEntry getNullableRawClasspathEntryForPackageFragmentRoot(
      IPackageFragmentRoot root) throws JavaModelException {

    IClasspathEntry rawEntry = null;
    {
      JavaProject project = (JavaProject) root.getJavaProject();
      // force the reverse rawEntry cache to be populated
      project.getResolvedClasspath(true);
      @SuppressWarnings("rawtypes")
      Map rootPathToRawEntries = project.getPerProjectInfo().rootPathToRawEntries;
      if (rootPathToRawEntries != null) {
        rawEntry = (IClasspathEntry) rootPathToRawEntries.get(root.getPath());
      }
    }
    return rawEntry;
  }

  /**
   * Determines whether a ClasspathContainer exists on the provided project that
   * matches the provided container ID.
   * 
   * @param javaProject the project to query
   * @param containerId The container ID we are trying to match.
   * @return whether at least one classpath container exists that matches the
   *         provided ID.
   */
  public static boolean includesClasspathContainerWithContainerId(
      IJavaProject javaProject, String containerId) throws JavaModelException {
    IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
    int indexOfClasspathEntryContainer = ClasspathUtilities.indexOfClasspathEntryContainer(
        classpathEntries, containerId);
    return indexOfClasspathEntryContainer >= 0;
  }

  /**
   * Returns the first index of the specified
   * {@link IClasspathEntry#CPE_CONTAINER} entry with the specified container ID
   * or -1 if one could not be found.
   * 
   * @param classpathEntries array of classpath entries
   * @param containerId container ID
   * @return index of the specified {@link IClasspathEntry#CPE_CONTAINER} entry
   *         with the specified container ID or -1
   */
  public static int indexOfClasspathEntryContainer(
      IClasspathEntry[] classpathEntries, String containerId) {
    for (int i = 0; i < classpathEntries.length; ++i) {
      IClasspathEntry classpathEntry = classpathEntries[i];
      if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
        // Skip anything that is not a container
        continue;
      }

      IPath containerPath = classpathEntry.getPath();
      if (containerPath.segmentCount() > 0
          && containerPath.segment(0).equals(containerId)) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Replace an {@link IClasspathEntry#CPE_CONTAINER} entry with the given
   * container ID, with its corresponding resolved classpath entries.
   * 
   * @param javaProject java project
   * @param containerId container ID to replace
   * @return true if a container was replaced
   * 
   * @throws JavaModelException
   */
  public static boolean replaceContainerWithClasspathEntries(
      IJavaProject javaProject, String containerId) throws JavaModelException {
    IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
    int containerIndex = ClasspathUtilities.indexOfClasspathEntryContainer(
        classpathEntries, containerId);
    if (containerIndex != -1) {
      List<IClasspathEntry> newClasspathEntries = new ArrayList<IClasspathEntry>(
          Arrays.asList(classpathEntries));
      IClasspathEntry classpathContainerEntry = newClasspathEntries.remove(containerIndex);
      IClasspathContainer classpathContainer = JavaCore.getClasspathContainer(
          classpathContainerEntry.getPath(), javaProject);
      if (classpathContainer != null) {
        newClasspathEntries.addAll(containerIndex,
            Arrays.asList(classpathContainer.getClasspathEntries()));
        ClasspathUtilities.setRawClasspath(javaProject, newClasspathEntries);
        return true;
      }
    }
    return false;
  }

  /**
   * Replaces a project's classpath container entry with a new one or appends it
   * to the classpath if none were found.
   * 
   * @param javaProject The target project
   * @param containerId the identifier of the classpath container type
   * @param newContainerPath the path for the new classpath. Note: this path
   *          should be partial, not including the initial segment which should
   *          in all cases be the value in containerId
   * @throws JavaModelException thrown by the call to getRawClasspath()
   */
  public static void replaceOrAppendContainer(IJavaProject javaProject,
      String containerId, IPath newContainerPath, IProgressMonitor monitor)
      throws JavaModelException {
    IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
    int indexOfClasspathEntryContainer = ClasspathUtilities.indexOfClasspathEntryContainer(
        classpathEntries, containerId);
    IClasspathEntry newContainer = JavaCore.newContainerEntry(new Path(
        containerId).append(newContainerPath));
    List<IClasspathEntry> newClasspathEntries = new ArrayList<IClasspathEntry>(
        Arrays.asList(classpathEntries));
    if (indexOfClasspathEntryContainer >= 0) {
      // Replace the entry
      newClasspathEntries.set(indexOfClasspathEntryContainer, newContainer);
    } else {
      // Append the entry
      newClasspathEntries.add(newContainer);
    }

    javaProject.setRawClasspath(
        newClasspathEntries.toArray(new IClasspathEntry[newClasspathEntries.size()]),
        monitor);
  }

  /**
   * Use this method to set the raw classpath of an IJavaProject. This method
   * should be used in favor of IJavaProject.setRawClasspath(IJavaProject,
   * IClasspathEntry [], IProgressMonitor) due to an odd interaction with
   * certain source control systems, such as Perforce. See the following URL for
   * more information: <a
   * href="http://bugs.eclipse.org/bugs/show_bug.cgi?id=243692"
   * >http://bugs.eclipse.org/bugs/show_bug.cgi?id=243692</a>
   * 
   * <p>
   * Note that this method is asynchronous, so the caller will regain control
   * immediately, and the raw classpath will be set at some future time. Right
   * now, there is no way to tell the caller when the operation has completed.
   * If this becomes a concern in the future, a callback parameter can be
   * introduced.
   * </p>
   * 
   * <p>
   * This method does not accept an IProgressMonitor, unlike the equivalent
   * method in IJavaProject, because there is an implicit progress monitor
   * provided when running the setRawClasspath operation as a task. In the
   * future, this method could be modified to accept a user-specified progress
   * monitor.
   * </p>
   * 
   * NOTE: If you are already running in a job, you probably don't want to call
   * this method.
   */
  public static void setRawClasspath(final IJavaProject javaProject,
      final IClasspathEntry[] newClasspathEntries) {

    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException,
          OperationCanceledException {
        javaProject.setRawClasspath(newClasspathEntries, monitor);
      }
    };

    WorkbenchRunnableAdapter op = new WorkbenchRunnableAdapter(runnable);
    op.runAsUserJob("Updating classpath of Java project '"
        + javaProject.getProject().getName() + "'", null);
  }

  /**
   * Use this method to set the raw classpath of an IJavaProject. This method
   * should be used in favor of IJavaProject.setRawClasspath(IJavaProject,
   * IClasspathEntry [], IProgressMonitor) due to an odd interaction with
   * certain source control systems, such as Perforce. See the following URL for
   * more information: <a
   * href="http://bugs.eclipse.org/bugs/show_bug.cgi?id=243692"
   * >http://bugs.eclipse.org/bugs/show_bug.cgi?id=243692</a>
   * 
   * <p>
   * Note that this method is asynchronous, so the caller will regain control
   * immediately, and the raw classpath will be set at some future time. Right
   * now, there is no way to tell the caller when the operation has completed.
   * If this becomes a concern in the future, a callback parameter can be
   * introduced.
   * </p>
   * 
   * <p>
   * This method does not accept an IProgressMonitor, unlike the equivalent
   * method in IJavaProject, because there is an implicit progress monitor
   * provided when running the setRawClasspath operation as a task. In the
   * future, this method could be modified to accept a user-specified progress
   * monitor.
   * </p>
   */
  public static void setRawClasspath(final IJavaProject javaProject,
      List<IClasspathEntry> classpathEntries) {
    setRawClasspath(javaProject, classpathEntries.toArray(NO_ENTRIES));
  }

  /**
   * Waits indefinitely until the given classpath entries are on the given
   * project's raw classpath.
   * 
   * @throws JavaModelException
   * @throws InterruptedException
   */
  public static void waitUntilEntriesAreOnClasspath(
      final IJavaProject javaProject,
      final List<IClasspathEntry> classpathEntries) throws JavaModelException,
      InterruptedException {

    // Used to notify when we are finished -- either all entries are on the
    // classpath or the anon class had an exception
    final Object finished = new Object();
    final JavaModelException[] anonClassException = new JavaModelException[1];

    ClasspathChangedListener listener = new ClasspathChangedListener() {
      @Override
      protected void classpathChanged(IJavaProject curJavaProject) {
        synchronized (finished) {
          try {
            if (curJavaProject.equals(javaProject)
                && areEntriesOnClasspath(javaProject, classpathEntries)) {
              finished.notifyAll();
            }
          } catch (JavaModelException e) {
            anonClassException[0] = e;
            finished.notifyAll();
          }
        }
      }
    };

    synchronized (finished) {
      JavaCore.addElementChangedListener(listener);

      try {
        // We're in a state where either the entries already exist on the
        // classpath, or we have a callback queued up (because of the
        // synchronization on finished) that will notify us when they are added.
        while (!areEntriesOnClasspath(javaProject, classpathEntries)) {
          finished.wait();
        }

        if (anonClassException[0] != null) {
          throw anonClassException[0];
        }
      } finally {
        JavaCore.removeElementChangedListener(listener);
      }
    }
  }

  private static boolean areEntriesOnClasspath(IJavaProject javaProject,
      List<IClasspathEntry> classpathEntries) throws JavaModelException {
    List<IClasspathEntry> projectClasspath = Arrays.asList(javaProject.getRawClasspath());

    for (IClasspathEntry entry : classpathEntries) {
      if (projectClasspath.indexOf(entry) == -1) {
        return false;
      }
    }

    return true;
  }

  private ClasspathUtilities() {
  }
}
