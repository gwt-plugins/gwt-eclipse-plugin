/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gcp.eclipse.testing;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

/**
 * Utilities for setting up {@link IProject}s in OSGi tests.
 */
public class ProjectTestUtil {
  private ProjectTestUtil() {} // Non-instantiatable utility class

  /** IDs for project natures */
  public static final String JAVA_NATURE = "org.eclipse.jdt.core.javanature";
  public static final String JAVA_BUILDER_ID = "org.eclipse.jdt.core.javabuilder";

  public static final String SOURCE_FOLDER = "src";

  /**
   * Enables or disables automatic building of projects.
   */
  public static void setAutoBuilding(boolean enable) throws CoreException {
    IWorkspaceDescription desc = ResourcesPlugin.getWorkspace().getDescription();
    if (desc.isAutoBuilding() != enable) {
      desc.setAutoBuilding(enable);
      ResourcesPlugin.getWorkspace().setDescription(desc);
    }
  }

  /**
   * Returns the {@link ICommand} for the specified builder, or {@code null} if the builder does not
   * exist.
   */
  public static ICommand getBuilder(IProject project, String builderId) throws CoreException {
    IProjectDescription description = project.getDescription();

    for (ICommand command : description.getBuildSpec()) {
      if (command.getBuilderName().equals(builderId)) {
        return command;
      }
    }
    return null;
  }

  /**
   * Adds the specified builder to an existing project.
   */
  public static void addBuilder(IProject project, String builderId) throws CoreException {
    Preconditions.checkArgument(project.exists());
    IProjectDescription description = project.getDescription();
    List<ICommand> commands = Lists.newArrayList(description.getBuildSpec());
    for (ICommand command : commands) {
      if (command.getBuilderName().equals(builderId)) {
        return;
      }
    }
    ICommand command = description.newCommand();
    command.setBuilderName(builderId);
    commands.add(command);
    description.setBuildSpec(commands.toArray(new ICommand[commands.size()]));
    project.setDescription(description, IResource.FORCE, null);
  }

  /**
   * Adds the nature defined by {@code natureId} to {@code project}.
   */
  public static void addNature(IProject project, String natureId) throws CoreException {
    Preconditions.checkArgument(project.exists());
    IProjectDescription description = project.getDescription();
    Set<String> natures = Sets.newHashSet(description.getNatureIds());
    natures.add(natureId);
    description.setNatureIds(natures.toArray(new String[natures.size()]));
    project.setDescription(description, null);
  }

  /**
   * Returns the absolute path to the root of the CitC or Piper client.
   */
  public static IPath getSourceLocation(IProject project) {
    return project.getLocation().append(SOURCE_FOLDER);
  }

  /**
   * Returns the working directory for the test.
   */
  private static IPath getWorkingDirectory() {
    return ResourcesPlugin.getWorkspace().getRoot().getLocation();
  }

  /**
   * Creates a file relative to the project's "src" folder. Files created this way become visible
   * after the next refresh operation.
   */
  public static void createFile(IProject project, String relativePath) throws IOException {
    IPath root = getSourceLocation(project);
    IPath path = Path.fromOSString(relativePath);
    Preconditions.checkArgument(!path.isAbsolute());
    PathTestUtil.touch(root.append(path));
  }

  /**
   * Creates a directory relative to the project's "src" folder. Directories created this way become
   * visible after the next refresh operation.
   */
  public static void createDirectory(IProject project, String relativePath) throws IOException {
    IPath root = getSourceLocation(project);
    IPath path = Path.fromOSString(relativePath);
    Preconditions.checkArgument(!path.isAbsolute());
    PathTestUtil.makeDir(root.append(path));
  }

  /**
   * Clears out existing projects and the client files that back them.
   */
  public static void cleanup() {
    // Clear out existing projects
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      // Close the project before deleting it to make sure all resource synchronization is done,
      // otherwise we can see errors from Guide's resource filtering.
      if (project.exists()) {
        try {
          project.close(null);
          project.delete(true, true, null);
        } catch (CoreException e) {
          e.printStackTrace();  // Log the error but don't flag a test failure.
        }
      }
    }

    // Clear out any backing files.
    File workingDirectory = getWorkingDirectory().toFile();
    if (workingDirectory.exists()) {
      for (File child : workingDirectory.listFiles()) {
        if (!child.getName().equals(".metadata")) {
          try {
            PathTestUtil.deleteRecursively(Path.fromOSString(child.getAbsolutePath()));
          } catch (IOException e) {
            e.printStackTrace();  // Log the error but don't flag a test failure.
          }
        }
      }
    }
  }

  /**
   * Creates and returns an instance of {@code NullProgressMonitor}.
   */
  public static NullProgressMonitor npm() {
    return new NullProgressMonitor();
  }

  /**
   * Creates a simple project.
   *
   * @param projectName the name of the project
   * @param natureIds an array of natures IDs to set on the project, or {@code null} if none should
   *        be set
   * @return the created project
   * @throws CoreException if the project is not created
   */
  public static IProject createSimpleProject(String projectName, String... natureIds)
      throws CoreException {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    if (project.exists()) {
      project.delete(true, true, npm());
    }
    project.create(npm());
    project.open(npm());
    if (natureIds != null) {
      IProjectDescription desc = project.getDescription();
      desc.setNatureIds(natureIds);
      project.setDescription(desc, npm());
    }
    return project;
  }

  /**
   * Creates a Java project with the given name and the specified raw classpath.
   */
  public static IJavaProject createProject(String projectName, IClasspathEntry[] rawClasspaths)
      throws CoreException {
    IProject project = createSimpleProject(projectName, JAVA_NATURE);
    IJavaProject javaProject = JavaCore.create(project);
    javaProject.setRawClasspath(rawClasspaths, npm());

    return javaProject;
  }

  /**
   * Creates a Java project with the given name and a "src" folder and the default JRE library on
   * its classpath.
   */
  public static IJavaProject createProject(String projectName) throws CoreException {
    IClasspathEntry[] classpath = new IClasspathEntry[] {
        JavaCore.newSourceEntry(Path.fromOSString("/" + projectName + "/" + SOURCE_FOLDER)),
        JavaCore.newContainerEntry(Path.fromOSString("org.eclipse.jdt.launching.JRE_CONTAINER"))};
    return ProjectTestUtil.createProject(projectName, classpath);
  }

  /**
   * Creates a META-INF/persistence.xml file for JPA persistence
   */
  public static void createPersistenceFile(IProject project) throws CoreException {
    String[] persistenceFileData = new String[] {
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<persistence version=\"2.0\" xmlns=\"http://java.sun.com/xml/ns/persistence\" ",
        "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ",
        "    xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence ",
        "    http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd\">",
        "  <persistence-unit name=\"JPA1\">",
        "  </persistence-unit>",
        "</persistence>"};
    IFolder srcFolder = project.getFolder("src");
    if (!srcFolder.exists()) {
      srcFolder.create(true, true, null);
    }
    IFolder metaFolder = srcFolder.getFolder("META-INF");
    if (!metaFolder.exists()) {
      metaFolder.create(true, true, null);
    }
    IFile persistenceXml = metaFolder.getFile("persistence.xml");
    if (!persistenceXml.exists()) {
      String data = Joiner.on('\n').join(persistenceFileData);
      InputStream is = new ByteArrayInputStream(Charset.forName("UTF-8").encode(data).array());
      persistenceXml.create(is, true, null);
    }
  }
}
