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
package com.google.gdt.eclipse.core.resources;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.collections.ManyToManyIndex;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks dependencies of {@link ICompilationUnit}s which are non-Java
 * resources.
 */
public abstract class CompilationUnitResourceDependencyIndex {

  private static final String ATTR_COMPILATION_UNIT = "cu";

  private static final String ATTR_RESOURCE_PATH = "path";

  private static final String TAG_DEPENDENCIES = "deps";

  private static final String TAG_RESOURCE = "res";

  private static final String TAG_ROOT = "index";

  // Maps compilation units <=> resources they depend on.
  private final ManyToManyIndex<ICompilationUnit, IPath> index = new ManyToManyIndex<ICompilationUnit, IPath>();

  private final String indexName;

  protected CompilationUnitResourceDependencyIndex(String indexName) {
    this.indexName = indexName;
    loadIndex();
  }

  /**
   * Removes all compilation units in the specified project from the index. Note
   * that the index may still contain resources that live inside the project.
   */
  public void clear(IProject project) {
    // Copy the set so we don't get a ConcurrentModificationException
    Set<ICompilationUnit> compilationUnits = new HashSet<ICompilationUnit>(
        index.getAllLeftElements());
    for (ICompilationUnit cu : compilationUnits) {
      if (cu.getResource().getProject().equals(project)) {
        index.removeLeftElement(cu);
      }
    }
  }

  public Set<ICompilationUnit> findDependentCompilationUnits(IPath resourcePath) {
    return index.getLeftElements(normalizePath(resourcePath));
  }

  public void putResourcesForCompilationUnit(ICompilationUnit cu,
      Set<IPath> resourcePaths) {
    Set<IPath> normalizedPaths = new HashSet<IPath>();
    for (IPath resourcePath : resourcePaths) {
      normalizedPaths.add(normalizePath(resourcePath));
    }
    index.putLeftToManyRights(cu, normalizedPaths);
  }

  public void remove(ICompilationUnit cu) {
    index.removeLeftElement(cu);
  }

  public void saveIndex() {
    XMLMemento memento = XMLMemento.createWriteRoot(TAG_ROOT);
    persistIndex(memento);

    File indexFile = getIndexFile();
    FileWriter writer = null;
    try {
      try {
        writer = new FileWriter(indexFile);
        memento.save(writer);
      } finally {
        if (writer != null) {
          writer.close();
        }
      }
    } catch (IOException e) {
      CorePluginLog.logError(e, "Error saving index " + indexName);

      // Make sure we remove any partially-written file
      if (indexFile.exists()) {
        indexFile.delete();
      }
    }
  }

  protected abstract IPath getIndexFileLocation();

  protected IPath normalizePath(IPath path) {
    if (!ResourceUtils.isFilesystemCaseSensitive()) {
      return new Path(path.toString().toLowerCase());
    }
    return path;
  }

  private File getIndexFile() {
    return getIndexFileLocation().append(indexName).toFile();
  }

  private IPath getResourcePath(IMemento resNode) {
    String resourcePathString = resNode.getString(ATTR_RESOURCE_PATH);
    if (resourcePathString == null) {
      return null;
    }
    return new Path(resourcePathString);
  }

  private void loadCuDependencies(IMemento cuNode) {
    String cuHandle = cuNode.getString(ATTR_COMPILATION_UNIT);
    if (cuHandle == null) {
      CorePluginLog.logError("Loading index {0}: Missing attribute {1}",
          indexName, ATTR_COMPILATION_UNIT);
      return;
    }

    ICompilationUnit cu = (ICompilationUnit) JavaCore.create(cuHandle);
    // Verify the compilation unit still exists
    if (cu == null || !cu.exists()) {
      CorePluginLog.logError(
          "Loading index {0}: compilation unit no longer exists ({1})",
          indexName, cuHandle);
      return;
    }

    Set<IPath> resourcePaths = new HashSet<IPath>();
    for (IMemento resNode : cuNode.getChildren(TAG_RESOURCE)) {
      IPath resourcePath = getResourcePath(resNode);
      if (resourcePath == null) {
        CorePluginLog.logError("Loading index {0}: missing attribute {1}",
            indexName, ATTR_RESOURCE_PATH);
        continue;
      }

      resourcePaths.add(resourcePath);
    }

    // Now add these dependencies to the index
    index.putLeftToManyRights(cu, resourcePaths);
  }

  private void loadIndex() {
    FileReader reader = null;
    try {
      try {
        reader = new FileReader(getIndexFile());
        loadIndex(XMLMemento.createReadRoot(reader));
      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    } catch (FileNotFoundException e) {
      // Ignore this exception, which occurs when index does not yet exist
    } catch (Exception e) {
      CorePluginLog.logError(e, "Error loading index " + indexName);
    }
  }

  private void loadIndex(XMLMemento memento) {
    for (IMemento cuNode : memento.getChildren(TAG_DEPENDENCIES)) {
      loadCuDependencies(cuNode);
    }
  }

  private void persistIndex(XMLMemento memento) {
    for (ICompilationUnit cu : index.getAllLeftElements()) {
      IMemento cuNode = memento.createChild(TAG_DEPENDENCIES);
      cuNode.putString(ATTR_COMPILATION_UNIT, cu.getHandleIdentifier());

      for (IPath resourcePath : index.getRightElements(cu)) {
        IMemento resNode = cuNode.createChild(TAG_RESOURCE);
        resNode.putString(ATTR_RESOURCE_PATH, resourcePath.toString());
      }
    }
  }

}
