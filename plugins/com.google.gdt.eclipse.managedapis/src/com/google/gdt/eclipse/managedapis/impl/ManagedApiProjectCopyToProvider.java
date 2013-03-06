/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.ManagedApiProjectObserver;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO: doc me.
 */
public class ManagedApiProjectCopyToProvider
    implements ManagedApiProjectObserver {

  private ManagedApiProject project;

  public ManagedApiProjectCopyToProvider(ManagedApiProject project) {
    this.project = project;
  }

  public void addManagedApis(ManagedApi[] apis) {
    try {
      IFolder targetFolder = project.getCopyToTargetDir();
      if (targetFolder != null) {
        // Copy new apis
        copyJarFiles(apis, targetFolder);
      }
    } catch (CoreException e) {
      ManagedApiLogger.warn(
          e, "Error accessing project attribute: CopyToTargetDir");
    }
  }

  public void changeCopyToDirectory(IFolder originalFolder, IFolder newFolder) {
    try {
      if (newFolder != null) {
        // Copy all apis
        ManagedApi[] managedApis = project.getManagedApis();
        copyJarFiles(managedApis, newFolder);
      }
      if (originalFolder != null) {
        // Remove all apis
        ManagedApi[] oldSet = project.getManagedApis();
        removeJarFiles(oldSet, new ManagedApi[0], originalFolder);
      }
    } catch (CoreException e) {
      ManagedApiLogger.warn(e, "Error copying JARs");
    }
  }

  public void refreshManagedApis(ManagedApi[] apis) {
    addManagedApis(apis);
  }

  /**
   * Remove JAR files from the target location when the managed APIs that
   * provide them are removed. This method takes an array of ManagedApis that
   * have been removed.
   */
  public void removeManagedApis(ManagedApi[] apisRemoved) {
    try {
      IFolder targetFolder = project.getCopyToTargetDir();
      if (targetFolder != null) {
        // Remove apis
        ManagedApi[] targetApis = project.getManagedApis();
        Set<ManagedApi> originalApiSet = new HashSet<ManagedApi>();
        originalApiSet.addAll(Arrays.asList(targetApis));
        originalApiSet.addAll(Arrays.asList(apisRemoved));
        removeJarFiles(originalApiSet.toArray(
            new ManagedApi[originalApiSet.size()]), targetApis, targetFolder);
      }
    } catch (CoreException e) {
      ManagedApiLogger.warn(
          e, "Error accessing project attribute: CopyToTargetDir");
    }
  }

  private IClasspathEntry[] collectClasspathEntries(ManagedApi[] apis)
      throws CoreException {
    // remove duplication by using the jar name as the key
    Map<String, IClasspathEntry> jarmap = new HashMap<String, IClasspathEntry>();
    for (ManagedApi api : apis) {
      IClasspathEntry[] classpathEntries = api.getClasspathEntries();
      for (IClasspathEntry entry : classpathEntries) {
        jarmap.put(entry.getPath().lastSegment(), entry);
      }
    }
    return jarmap.values().toArray(new IClasspathEntry[jarmap.size()]);
  }

  private void copyJarFiles(final ManagedApi[] apis, final IFolder targetFolder)
      throws CoreException {
    final IClasspathEntry[] classpathEntries = collectClasspathEntries(apis);

    Job copyApiJob = new Job("Copy jar files for added APIs") {
        @Override
      protected IStatus run(IProgressMonitor monitor) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        for (IClasspathEntry entry : classpathEntries) {
          IFile sourceFile = workspaceRoot.getFile(entry.getPath());
          IFile destFile = getTargetFile(targetFolder, entry);

          try {
            if (destFile.exists()) {
              continue;
            }

            if (sourceFile != null) {
              ResourceUtils.createFile(
                  destFile.getFullPath(), sourceFile.getContents(true));
            }
          } catch (CoreException e) {
            ManagedApiLogger.log(ManagedApiLogger.ERROR, MessageFormat.format(
                "CoreException while copying file {0} to {1}", sourceFile,
                destFile));
          }
        }
        return Status.OK_STATUS;
      }
    };
    copyApiJob.setSystem(true);
    copyApiJob.setRule(targetFolder);
    copyApiJob.schedule();
  }

  private IFile getTargetFile(IFolder targetFolder, IClasspathEntry entry) {
    IFile destFile = targetFolder.getFile(entry.getPath().lastSegment());
    return destFile;
  }

  private void removeJarFiles(
      ManagedApi[] oldApis, ManagedApi[] newApis, IFolder targetFolder)
      throws CoreException {
    final Set<IFile> removalSet = new HashSet<IFile>();

    IClasspathEntry[] oldClasspathEntries = collectClasspathEntries(oldApis);
    for (IClasspathEntry entry : oldClasspathEntries) {
      if (!entry.getPath()
          .toString().contains(ManagedApiPlugin.DEPENDENCIES_FOLDER_NAME)) {
        removalSet.add(getTargetFile(targetFolder, entry));
      }
    }

    IClasspathEntry[] newClasspathEntries = collectClasspathEntries(newApis);
    for (IClasspathEntry entry : newClasspathEntries) {
      removalSet.remove(getTargetFile(targetFolder, entry));
    }

    Job deleteApiJob = new Job("Delete jars for removed APIs") {
        @Override
      protected IStatus run(IProgressMonitor monitor) {
        for (IFile file : removalSet) {
          try {
            if (file.exists()) {
              file.delete(true, new NullProgressMonitor());
            }
          } catch (CoreException e) {
            ManagedApiLogger.log(ManagedApiLogger.ERROR, MessageFormat.format(
                "CoreException while deleting file {0}", file));
          }
        }
        return Status.OK_STATUS;
      }
    };
    deleteApiJob.setSystem(true);
    deleteApiJob.setRule(targetFolder);
    deleteApiJob.schedule();
  }
}
