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
package com.google.gdt.eclipse.managedapis;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * The eclipse project provides an interface to easily access methods on a
 * project that would otherwise violate LOD (Law of Demeter). This type is
 * represented as an interface to support testing -- as it provides a simple
 * type to mock.
 */
public interface EclipseProject {

  /**
   * The equivalent of calling getProject().getFolder(localPath) on an
   * IJavaProject.
   *
   * @param localPath the local path string
   * @return the IFolder within the current project as reference by the
   *         localpath.
   */
  IFolder getFolder(String localPath);

  IJavaProject getJavaProject();

  /**
   * The equivalent of calling getPath() on an IJavaProject.
   *
   * @return the root path of the project.
   */
  IPath getPath();

  /**
   * The equivalent of calling getProject().getPersistentProperty(qname) on an
   * IJavaProject.
   *
   * @param qname the key used to index the property.
   * @return the persisted property
   */
  String getPersistentProperty(QualifiedName qname);

  /**
   * Access the IProject for the EclipseProject;
   */
  IProject getProject();

  /**
   * The equivalent of calling getProject().setPersistentProperty(qname, value)
   * on an IJavaProject.
   *
   * @param qname the key used to index the property.
   * @param value the property to persist
   * @throws CoreException
   */
  void setPersistentProperty(QualifiedName qname, String value)
      throws CoreException;

  /**
   * The equivalent of calling getProject().setRawClasspath() on an
   * IJavaProject.
   *
   * @param entries an array representing the target state of the project
   *          classpath.
   * @param monitor progress monitor to track the change
   */
  void setRawClasspath(IClasspathEntry[] entries, IProgressMonitor monitor)
      throws JavaModelException;
}
