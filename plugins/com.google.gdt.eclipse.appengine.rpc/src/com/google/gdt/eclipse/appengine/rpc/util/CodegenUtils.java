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
package com.google.gdt.eclipse.appengine.rpc.util;

import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * TODO: doc me.
 */
@SuppressWarnings("restriction")
public class CodegenUtils {

  public static IClasspathEntry[] addEntryToClasspath(
      IClasspathEntry[] entries, IClasspathEntry newEntry) {
    int n = entries.length;
    IClasspathEntry[] newEntries = new IClasspathEntry[n + 1];
    System.arraycopy(entries, 0, newEntries, 0, n);
    newEntries[n] = newEntry;
    return newEntries;
  }

  public static void applyEdit(ICompilationUnit cu, TextEdit edit,
      boolean save, IProgressMonitor monitor) throws CoreException {
    IFile file = (IFile) cu.getResource();
    if (!save || !file.exists()) {
      cu.applyTextEdit(edit, monitor);
    } else {
      if (monitor == null) {
        monitor = new NullProgressMonitor();
      }
      monitor.beginTask("Apply edits", 2); //$NON-NLS-1$
      try {
        IStatus status = Resources.makeCommittable(file, null);
        if (!status.isOK()) {
          AppEngineRPCPlugin.getLogger().logError(
              "Rpc Service generation: unable to apply edit to file " //$NON-NLS-1$
                  + cu.getElementName());
        }

        cu.applyTextEdit(edit, new SubProgressMonitor(monitor, 1));

        cu.save(new SubProgressMonitor(monitor, 1), true);
      } finally {
        monitor.done();
      }
    }
  }

  public static ImportRewrite createImportRewrite(CompilationUnit astRoot,
      boolean restoreExistingImports) {
    return CodeStyleConfiguration.createImportRewrite(astRoot,
        restoreExistingImports);
  }

  /**
   * Format the source
   */
  public static String format(String source, int formatType) {

    TextEdit textEdit = null;
    textEdit = ToolFactory.createCodeFormatter(null).format(formatType, source,
        0, source.length(), 0, null);

    String formattedContent;
    if (textEdit != null) {
      Document document = new Document(source);
      try {
        textEdit.apply(document);
      } catch (MalformedTreeException e) {
        AppEngineRPCPlugin.log(e);
      } catch (BadLocationException e) {
        AppEngineRPCPlugin.log(e);
      }
      formattedContent = document.get();
    } else {
      formattedContent = source;
    }

    return formattedContent;
  }

  /**
   * Removes the corresponding source folder from the class path entries if
   * found.
   * 
   * @param entries The class path entries to read. A copy will be returned.
   * @param folder The parent source folder to remove.
   * @return A new class path entries array.
   */
  public static IClasspathEntry[] removeSourceClasspath(
      IClasspathEntry[] entries, IContainer folder) {
    if (folder == null) {
      return entries;
    }
    IClasspathEntry source = JavaCore.newSourceEntry(folder.getFullPath());
    int n = entries.length;
    for (int i = n - 1; i >= 0; i--) {
      if (entries[i].equals(source)) {
        IClasspathEntry[] newEntries = new IClasspathEntry[n - 1];
        if (i > 0) {
          System.arraycopy(entries, 0, newEntries, 0, i);
        }
        if (i < n - 1) {
          System.arraycopy(entries, i + 1, newEntries, i, n - i - 1);
        }
        n--;
        entries = newEntries;
      }
    }
    return entries;
  }

  public static void setupSourceFolders(IJavaProject javaProject,
      IFolder sourceFolder, IProgressMonitor monitor) throws JavaModelException {
    IProject project = javaProject.getProject();
    // get the list of entries.
    IClasspathEntry[] entries = javaProject.getRawClasspath();

    // remove the project as a source folder (This is the default)
    entries = removeSourceClasspath(entries, project);

    // add the source folder
    // remove it first in case.
    entries = removeSourceClasspath(entries, sourceFolder);
    entries = addEntryToClasspath(entries,
        JavaCore.newSourceEntry(sourceFolder.getFullPath()));

    javaProject.setRawClasspath(entries, new SubProgressMonitor(monitor, 10));
  }

  /**
   * Adds the given folder to the project's class path.
   * 
   * @param javaProject The Java Project to update.
   * @param sourceFolder Template Parameters.
   * @param monitor An existing monitor.
   * @throws JavaModelException if the classpath could not be set.
   */
  public static void setupSourceFolders(IJavaProject javaProject,
      String[] sourceFolders, IProgressMonitor monitor)
      throws JavaModelException {
    IProject project = javaProject.getProject();
    // get the list of entries.
    IClasspathEntry[] entries = javaProject.getRawClasspath();

    // remove the project as a source folder (This is the default)
    entries = removeSourceClasspath(entries, project);

    // add the source folders.
    for (String sourceFolder : sourceFolders) {
      IFolder srcFolder = project.getFolder(sourceFolder);

      // remove it first in case.
      entries = removeSourceClasspath(entries, srcFolder);
      entries = addEntryToClasspath(entries,
          JavaCore.newSourceEntry(srcFolder.getFullPath()));
    }
    javaProject.setRawClasspath(entries, new SubProgressMonitor(monitor, 10));
  }

}
