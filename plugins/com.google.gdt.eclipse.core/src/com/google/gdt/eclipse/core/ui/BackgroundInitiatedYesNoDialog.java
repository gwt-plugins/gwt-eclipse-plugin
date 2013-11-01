/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.core.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A yes/no dialog that can be opened from a non-UI thread by calling the {@link #userAnsweredYes()}
 * method. The method blocks until the user has responded, then returns a value indicating the
 * user's response. The dialog may be of any of the types described by the integer codes in
 * {@link MessageDialog}.
 */
public class BackgroundInitiatedYesNoDialog {
  
  private enum YesOrNo { YES, NO }
  
  private static final String[] LABELS = {"Yes", "No"};

  /**
   * Displays a yes/no dialog of a specified type with a specified title,a specified message, and a
   * specified default value, then blocks until the user responds or interrupts the dialog.
   * 
   * @param type the dialog type, specified by one of the int constants in {@link MessageDialog}
   * @param title the specified title
   * @param message the specified message
   * @param defaultIsYes
   *     {@link true} if the specified default value is <i>yes</i>, false if the specified default
   *      value is <i>no</i>
   * @return
   *     {@code true} if the user responded <i>yes</i>, {@code false} if the user responded
   *     <i>no</i>, or the value of {@code defaultValueIsYes} if the user interrupts the dialog
   */
  public boolean userAnsweredYes(
      final int type, final String title, final String message, boolean defaultIsYes) {
    final Semaphore barrier = new Semaphore(0);
    final AtomicBoolean responseContainer = new AtomicBoolean();
    final int defaultPosition = defaultIsYes ? YesOrNo.YES.ordinal() : YesOrNo.NO.ordinal();
    UIJob dialogJob =
        new UIJob("background-initiated question dialog"){
          @Override public IStatus runInUIThread(IProgressMonitor monitor) {
            Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            MessageDialog dialog =
                new MessageDialog(activeShell, title, null, message, type, LABELS, defaultPosition);
            int selection = dialog.open();
            responseContainer.set(selection == YesOrNo.YES.ordinal());
            barrier.release();
            return Status.OK_STATUS;
          }
        };
    dialogJob.schedule();
    try {
      barrier.acquire();
    } catch (InterruptedException e) {
      return defaultIsYes;
    }
    return responseContainer.get();
  }

}
