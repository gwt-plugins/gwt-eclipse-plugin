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
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Scan the files in a managed API file structure and register resources.
 */
public class ManagedApiResourceVisitor implements IResourceVisitor {

  /**
   * Extensions that specifically represent a jar file.
   */
  private static final String[] JAR_EXTENSIONS = new String[] {".jar"};

  /**
   * Filename suffix values (prior to the extension) that denote a javadoc
   * archive.
   */
  private static final String[] JAVADOC_SUFFIXES = new String[] {
      "-doc", "-docs", "-javadoc", "-javadocs"};

  /**
   * Filename suffix values (prior to the extension) that denote a source
   * archive.
   */
  private static final String[] SOURCE_SUFFIXES = new String[] {
      "-src", "-source", "-sources"};

  private static String getBasename(String name, String[] suffixes) {
    String basename = null;
    for (String extension : suffixes) {
      if (name.endsWith(extension)) {
        basename = name.substring(0, name.length() - extension.length());
      }
      if (null != basename) {
        break;
      }
    }
    return basename;
  }

  private static String getClassArchiveBaseReference(IResource resource) {
    String resourceName = resource.getName();
    return getBasename(resourceName, JAR_EXTENSIONS);
  }

  private static String getJavadocArchiveBaseReference(IResource resource) {
    String resourceName = resource.getName();
    String basename = getBasename(resourceName, JAR_EXTENSIONS);
    return basename == null ? null : getBasename(basename, JAVADOC_SUFFIXES);
  }

  private static String getSourceArchiveBaseReference(IResource resource) {
    String resourceName = resource.getName();
    String basename = getBasename(resourceName, JAR_EXTENSIONS);
    return basename == null ? null : getBasename(basename, SOURCE_SUFFIXES);
  }

  private static boolean isClassArchive(IResource resource) {
    String resourceName = resource.getName();
    String basename = getBasename(resourceName, JAR_EXTENSIONS);
    return resource.getType() == IResource.FILE
        && (basename != null)
        && ((getBasename(basename, JAVADOC_SUFFIXES) == null) && (getBasename(
            basename, SOURCE_SUFFIXES) == null));
  }

  private static boolean isDescriptor(IResource resource) {
    String resourceName = resource.getName();
    return resource.getType() == IResource.FILE
        && ManagedApiJsonClasses.DESCRIPTOR_FILENAME.equalsIgnoreCase(resourceName);
  }

  private static boolean isJavadocArchive(IResource resource) {
    String resourceName = resource.getName();
    String basename = getBasename(resourceName, JAR_EXTENSIONS);
    return resource.getType() == IResource.FILE && (basename != null)
        && ((getBasename(basename, JAVADOC_SUFFIXES) != null));
  }

  private static boolean isSourceArchive(IResource resource) {
    String resourceName = resource.getName();
    String basename = getBasename(resourceName, JAR_EXTENSIONS);
    return resource.getType() == IResource.FILE && (basename != null)
        && ((getBasename(basename, SOURCE_SUFFIXES) != null));
  }

  private IFile descriptorFile;

  private EclipseProject project;

  private Map<String, ClasspathEntryPrototype> prototypeMap = new HashMap<String, ClasspathEntryPrototype>();

  private IFolder rootDir;

  public IClasspathEntry[] getClasspathEntries() {
    ArrayList<IClasspathEntry> entryList = new ArrayList<IClasspathEntry>();
    for (ClasspathEntryPrototype prototype : prototypeMap.values()) {
      IClasspathEntry entry = prototype.createClasspathEntry(project.getPath());
      if (entry != null) {
        entryList.add(entry);
      }
    }
    IClasspathEntry[] entryArray = new IClasspathEntry[entryList.size()];
    return entryList.toArray(entryArray);
  }

  public IFile getDescriptor() {
    return descriptorFile;
  }

  public EclipseProject getProject() {
    return project;
  }

  public IFolder getRootDir() {
    return rootDir;
  }

  public void setProject(EclipseProject project) {
    this.project = project;
  }

  public void setRootDir(IFolder rootDir) {
    this.rootDir = rootDir;
  }

  public boolean visit(IResource resource) throws CoreException {
    if (rootDir.equals(resource)) {
      return true;
    } else {
      String resourceName = resource.getName();
      // exclude hidden files
      if (resourceName.length() == 0 || resourceName.charAt(0) == '.') {
        return false;
      } else if (isDescriptor(resource)) {
        this.descriptorFile = (IFile) resource;
        return false;
      } else if (isClassArchive(resource)) {
        ClasspathEntryPrototype prototype = getClasspathEntryPrototype(getClassArchiveBaseReference(resource));
        prototype.setClassArchive((IFile) resource);
        return false;
      } else if (isSourceArchive(resource)) {
        ClasspathEntryPrototype prototype = getClasspathEntryPrototype(getSourceArchiveBaseReference(resource));
        prototype.setSourceArchive((IFile) resource);
        return false;
      } else if (isJavadocArchive(resource)) {
        ClasspathEntryPrototype prototype = getClasspathEntryPrototype(getJavadocArchiveBaseReference(resource));
        prototype.setJavadocArchive((IFile) resource);
        return false;
      }
      // do dive into directories
      return true;
    }
  }

  private ClasspathEntryPrototype getClasspathEntryPrototype(
      String baseReference) {
    ClasspathEntryPrototype proto = prototypeMap.get(baseReference);
    if (proto == null) {
      proto = new ClasspathEntryPrototype();
      prototypeMap.put(baseReference, proto);
    }
    return proto;
  }
}