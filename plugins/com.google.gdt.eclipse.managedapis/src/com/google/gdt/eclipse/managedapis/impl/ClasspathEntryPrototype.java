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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathAttribute;
import org.eclipse.jdt.internal.core.ClasspathEntry;

/**
 * Represents a builder for ClasspathEntry used to cumulatively put together an
 * entry from a number of files (class-containing jar files, source, javadoc).
 * This type also includes a factory method to take these values and produce a
 * ClasspathEntry for inclusion in a Java project.
 */
@SuppressWarnings("restriction")
public class ClasspathEntryPrototype {

  private IFile classArchive;
  private IFile sourceArchive;
  private IFile javadocArchive;

  /**
   * Produce a ClasspathEntry from this prototype.
   * 
   * @param projectRoot The project root, used to determine relative paths.
   * @return The classpath entry or null if no class archive is defined.
   */
  public IClasspathEntry createClasspathEntry(IPath projectRoot) {
    IPath libRelativePath = (classArchive == null) ? null : makeRelative(
        classArchive.getFullPath(), projectRoot);
    IPath srcPath = (sourceArchive == null) ? null : makeRelative(
        sourceArchive.getFullPath(), projectRoot);
    IPath javadocPath = (javadocArchive == null) ? null : makeRelative(
        javadocArchive.getFullPath(), projectRoot);

    IClasspathAttribute[] extraAttributes = (null == javadocPath)
        ? ClasspathEntry.NO_EXTRA_ATTRIBUTES
        : new IClasspathAttribute[] {new ClasspathAttribute(
            IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
            "jar:platform:/resource" + javadocPath.toString() + "!/")};

    // create a new CPE_LIBRARY type of cp entry with an attached source
    // archive if it exists
    return (libRelativePath == null) ? null : JavaCore.newLibraryEntry(
        libRelativePath, srcPath, srcPath == null ? null : new Path("/"),
        ClasspathEntry.NO_ACCESS_RULES, extraAttributes, false);
  }

  public void setClassArchive(IFile classArchive) {
    this.classArchive = classArchive;
  }

  public void setJavadocArchive(IFile javadocArchive) {
    this.javadocArchive = javadocArchive;
  }

  public void setSourceArchive(IFile sourceArchive) {
    this.sourceArchive = sourceArchive;
  }

  private IPath makeRelative(IPath path, IPath base) {
    return path.removeFirstSegments(base.segmentCount() - 1).makeAbsolute();
  }
}
