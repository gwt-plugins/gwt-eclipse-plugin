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
package com.google.gdt.eclipse.core.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import java.io.File;

/**
 * An ExtractJob takes an archive file and a target directory and uncompresses
 * the archive into the target directory such that files at the top level of the
 * archive are placed in the top level of the target directory. Subclasses of
 * ExtractJob work for specific types of archives.
 * 
 * Throws an IllegalArgumentException in the case that the specified file is not
 * a valid archive, or the specified target is not a directory.
 */

public abstract class ExtractJob extends Job {

  protected Action completedAction;
  protected File targetDir;
  protected File archive;

  // TODO: Consider making our own callback interface instead of using
  // Action.
  protected ExtractJob(File archive, File targetDir, Action completedAction)
      throws IllegalArgumentException {
    super("Uncompressing file " + archive.toString());

    this.completedAction = completedAction;
    this.targetDir = targetDir;
    this.archive = archive;
  }

  protected Action getViewStatusAction(IStatus jobStatus) {
    final String statusTitle = "Extracted " + archive.getName();

    final String statusMessage = "Extracted: "
        + archive.getAbsolutePath()
        + "\n"
        + "into directory: "
        + targetDir.getAbsolutePath()
        + (jobStatus == Status.OK_STATUS ? "" : "\n" + "Status: "
            + jobStatus.getMessage());

    return new Action("view extract status") {
      @Override
      public void run() {
        MessageDialog.openInformation(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            statusTitle, statusMessage);
      }
    };
  }

  /**
   * If we've finished the job and it didn't fail, fire the specified completed
   * action.
   */
  protected void maybeFireCompletedAction(IStatus jobStatus) {
    if (completedAction != null && jobStatus == Status.OK_STATUS) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          completedAction.run();
        }
      });
    }
  }
}
