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
package com.google.gwt.eclipse.core.actions;

import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.console.CustomMessageConsole;
import com.google.gdt.eclipse.core.console.MessageConsoleUtilities;
import com.google.gdt.eclipse.core.console.TerminateProcessAction;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.compile.GWTCompileRunner;
import com.google.gwt.eclipse.core.compile.GWTCompileSettings;
import com.google.gwt.eclipse.core.compile.ui.GWTCompileDialog;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Toolbar action which invokes a GWT compile of the selected project.
 */
public class GWTCompileToolbarHandler extends AbstractHandler {

  public Object execute(ExecutionEvent event) {
    // Check for dirty editors and prompt to save
    IWorkbench workbench = PlatformUI.getWorkbench();
    if (!workbench.saveAllEditors(true)) {
      return null;
    }

    // Get initial project selection
    IProject initialProject = null;
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window != null)
    {
        IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
        Object firstElement = selection.getFirstElement();
        if (firstElement instanceof IAdaptable)
        {
            initialProject = AdapterUtilities.getAdapter(firstElement, IProject.class);
        }
    }
    else {
      return null;
    }

    // Gather compile parameters
    GWTCompileDialog dialog = new GWTCompileDialog(Display.getDefault().getActiveShell(), initialProject);
    if (dialog.open() != Window.OK) {
      return null;
    }

    // The project may have changed
    final IProject project = dialog.getProject();

    final String taskName = GWTCompileRunner.computeTaskName(project);
    final GWTCompileSettings compileSettings = dialog.getCompileSettings();

    try {
      // Remember the compilation settings for next time
      GWTProjectProperties.setGwtCompileSettings(project, compileSettings);
    } catch (BackingStoreException e) {
      // Failed to save project properties
      GWTPluginLog.logError(e);
    }

    // Perform the GWT compilation
    new WorkspaceJob(taskName) {

      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor)
          throws CoreException {
        // Get a message console for GWT compiler output

        CustomMessageConsole messageConsole = MessageConsoleUtilities.getMessageConsole(
            taskName, null);

        TerminateProcessAction terminateAction = new TerminateProcessAction();
        messageConsole.setTerminateAction(terminateAction);

        messageConsole.activate();
        OutputStream consoleOutputStream = messageConsole.newMessageStream();

        try {
          IPath warLocation = null;

          if (WebAppUtilities.isWebApp(project)) {
            /*
             * First, check the additional compiler arguments to see if the user
             * specified the -war option manually. If not, use the project's
             * managed WAR output directory (if set) or failing that, prompt for
             * a file-system path.
             */
            if (!compileSettings.getExtraArgs().contains("-war")) {
              warLocation = WebAppUtilities.getWarOutLocationOrPrompt(project);
              if (warLocation == null) {
                // User canceled the dialog
                return Status.OK_STATUS;
              }
            }
          }

          GWTCompileRunner.compileWithCancellationSupport(
              JavaCore.create(project), warLocation, compileSettings,
              consoleOutputStream, terminateAction, monitor, terminateAction);
          return Status.OK_STATUS;

        } catch (IOException e) {
          GWTPluginLog.logError(e);
          throw new CoreException(new Status(IStatus.ERROR,
              GWTPlugin.PLUGIN_ID, e.getLocalizedMessage(), e));
        } catch (InterruptedException e) {
          GWTPluginLog.logError(e);
          throw new CoreException(new Status(IStatus.ERROR,
              GWTPlugin.PLUGIN_ID, e.getLocalizedMessage(), e));
        } catch (OperationCanceledException e) {
          // Ignore since the user canceled
          return Status.OK_STATUS;
        } finally {
          terminateAction.setEnabled(false);

          try {
            assert (consoleOutputStream != null);
            consoleOutputStream.close();
          } catch (IOException e) {
            // Ignore IOExceptions during stream close
          }
        }
      }
    }.schedule();
    return null;
  }
}