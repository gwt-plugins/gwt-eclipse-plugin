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

import com.google.gdt.eclipse.core.natures.NatureUtils;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Utility methods related to builders.
 */
@SuppressWarnings("restriction")
public class BuilderUtilities {

  /**
   * Adds a builder with the specified ID to the project if the project does not
   * already have the builder.
   * 
   * @param project project to add the builder to
   * @param builderId id of the builder to add
   * @throws CoreException
   */
  public static void addBuilderToProject(IProject project, String builderId)
      throws CoreException {

    if (hasBuilder(project, builderId)) {
      return;
    }

    IProjectDescription description = project.getDescription();
    List<ICommand> builders = new ArrayList<ICommand>(
        Arrays.asList(description.getBuildSpec()));

    ICommand newBuilder = description.newCommand();
    newBuilder.setBuilderName(builderId);
    builders.add(newBuilder);

    description.setBuildSpec(builders.toArray(new ICommand[builders.size()]));
    project.setDescription(description, null);
  }

  /**
   * Returns <code>true</code> if the project's build specification has the
   * given builder.
   */
  public static boolean hasBuilder(IProject project, String builderId)
      throws CoreException {
    for (ICommand builder : project.getDescription().getBuildSpec()) {
      if (builder.getBuilderName().equals(builderId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes a builder with the specified ID from the project if the project has
   * said builder.
   * 
   * @param project project to remove the builder from
   * @param builderId id of the builder to remove
   * @throws CoreException
   */
  public static void removeBuilderFromProject(IProject project, String builderId)
      throws CoreException {
    IProjectDescription description = project.getDescription();
    List<ICommand> builders = new ArrayList<ICommand>(
        Arrays.asList(description.getBuildSpec()));

    Iterator<ICommand> iter = builders.iterator();
    while (iter.hasNext()) {
      ICommand curBuilder = iter.next();
      if (curBuilder.getBuilderName().equals(builderId)) {
        iter.remove();
      }
    }

    description.setBuildSpec(builders.toArray(new ICommand[builders.size()]));
    project.setDescription(description, null);
  }

  /**
   * Forces re-validation of a set of compilation units by the JDT Java Builder.
   * 
   * @param cus the compilation units to re-validate
   * @param description a brief description of the external job that forcing the
   *          re-validation. This shows up in the Eclipse status bar while
   *          re-validation is taking place.
   */
  public static void revalidateCompilationUnits(
      final Set<ICompilationUnit> cus, String description) {
    WorkspaceJob revalidateJob = new WorkspaceJob(description) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor)
          throws CoreException {
        final IWorkingCopyManager wcManager = JavaPlugin.getDefault().getWorkingCopyManager();

        for (ICompilationUnit cu : cus) {
          if (!cu.getResource().exists()) {
            CorePluginLog.logWarning(MessageFormat.format(
                "Cannot revalidate non-existent compilation unit {0}",
                cu.getElementName()));
            continue;
          }

          final IEditorPart editorPart = getOpenEditor(cu);

          /*
           * If the .java file is open in an editor (without unsaved changes),
           * make a "null" edit by inserting an empty string at the top of the
           * file and then tell the editor to save. If incremental building is
           * enabled, this will trigger a re-validation of the file.
           */
          if (editorPart != null && !editorPart.isDirty()) {
            // Need to do the editor stuff from the UI thread
            Display.getDefault().asyncExec(new Runnable() {
              public void run() {
                try {
                  // Get the working copy open in the editor
                  ICompilationUnit wc = wcManager.getWorkingCopy(editorPart.getEditorInput());
                  wc.getBuffer().replace(0, 0, "");
                  editorPart.doSave(new NullProgressMonitor());
                } catch (JavaModelException e) {
                  CorePluginLog.logError(e);
                }
              }
            });
          } else {
            /*
             * If the .java file is not currently open, or if it's open with
             * unsaved changes, trigger re-validation by touching the underlying
             * resource.
             */
            cu.getResource().touch(null);
          }
        }
        return StatusUtilities.OK_STATUS;
      }
    };
    revalidateJob.schedule();
  }

  /**
   * Schedules a full rebuild on a project.
   * 
   * @param project the project to rebuild
   */
  public static void scheduleRebuild(final IProject project) {
    WorkspaceJob buildJob = new WorkspaceJob("Building " + project.getName()) {
      @Override
      public boolean belongsTo(Object family) {
        return ResourcesPlugin.FAMILY_MANUAL_BUILD.equals(family);
      }

      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor)
          throws CoreException {
        project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        return Status.OK_STATUS;
      }
    };

    buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    buildJob.setUser(true);
    buildJob.schedule();
  }

  /**
   * Schedules a full rebuild on all projects in the workspace that have any of
   * the specified natures.
   */
  public static void scheduleRebuildAll(final String... natureIds) {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    WorkspaceJob buildJob = new WorkspaceJob("Building workspace projects") {
      @Override
      public boolean belongsTo(Object family) {
        return ResourcesPlugin.FAMILY_MANUAL_BUILD.equals(family);
      }

      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor)
          throws CoreException {
        for (IProject project : workspace.getRoot().getProjects()) {
          for (String natureId : natureIds) {
            if (NatureUtils.hasNature(project, natureId)) {
              project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
              break;
            }
          }
        }

        return Status.OK_STATUS;
      }
    };

    buildJob.setRule(workspace.getRuleFactory().buildRule());
    buildJob.setUser(true);
    buildJob.schedule();
  }

  private static IEditorPart getOpenEditor(ICompilationUnit cu) {
    // Need to get the workbench window from the UI thread
    final IWorkbenchWindow[][] windows = new IWorkbenchWindow[1][];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        windows[0] = PlatformUI.getWorkbench().getWorkbenchWindows();
      }
    });

    for (IWorkbenchWindow window : windows[0]) {
      for (IWorkbenchPage page : window.getPages()) {
        for (IEditorReference editorRef : page.getEditorReferences()) {
          try {
            IEditorInput editorInput = editorRef.getEditorInput();

            // See if this editor has the compilation unit resource open
            if (editorInput instanceof FileEditorInput) {
              IFile file = ((FileEditorInput) editorInput).getFile();
              if (file.equals(cu.getResource())) {
                return editorRef.getEditor(false);
              }
            }
          } catch (PartInitException e) {
            CorePluginLog.logError(e);
          }
        }
      }
    }
    return null;
  }

}
