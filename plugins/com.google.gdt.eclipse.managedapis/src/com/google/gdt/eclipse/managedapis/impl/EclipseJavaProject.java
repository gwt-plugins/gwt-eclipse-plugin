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
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.managedapis.EclipseProject;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;

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
 * TODO: doc me.
 */
public class EclipseJavaProject implements EclipseProject {

  private IJavaProject project;

  public EclipseJavaProject(IJavaProject project) {
    super();
    this.project = project;
  }

  public IFolder getFolder(String localPath) {
    return project.getProject().getFolder(localPath);
  }

  public IJavaProject getJavaProject() {
    return project;
  }

  public IPath getPath() {
    return project.getPath();
  }

  public String getPersistentProperty(QualifiedName qname) {
    String property = null;
    try {
      property = project.getProject().getPersistentProperty(qname);
    } catch (CoreException e) {
      ManagedApiLogger.error("failure reading project persistant property");
    }
    return property;
  }

  public IProject getProject() {
    return project.getProject();
  }

  public void setPersistentProperty(QualifiedName qname, String value)
      throws CoreException {
    project.getProject().setPersistentProperty(qname, value);
  }

  public void setRawClasspath(IClasspathEntry[] entries,
      IProgressMonitor monitor) throws JavaModelException {
    project.setRawClasspath(entries, monitor);
  }
}
