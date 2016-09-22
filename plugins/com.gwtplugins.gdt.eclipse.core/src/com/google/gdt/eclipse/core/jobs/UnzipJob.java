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

import com.google.gdt.eclipse.core.CorePlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.progress.IProgressConstants;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * An ExtractJob that works for zip files.
 * 
 * Throws an IllegalArgumentException in the case that the specified file is not
 * a zip archive, or the specified target is not a directory.
 */
public class UnzipJob extends ExtractJob {

  private UnzipRunnable unzip;

  /**
   * Exact equivalent to new UnzipJob(zip, targetDir, null).
   */
  public UnzipJob(File zip, File targetDir) throws IllegalArgumentException {
    this(zip, targetDir, null);
  }

  /**
   * Extracts the archive zip file into the target directory and calls any
   * provided action on completion.
   */
  public UnzipJob(File zip, File targetDir, Action completedAction) throws IllegalArgumentException {
    super(zip, targetDir, completedAction);
    unzip = new UnzipRunnable(zip, targetDir);
  }

  /**
   * Run the unzip job using the provided monitor.
   */
  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IStatus jobStatus = Status.OK_STATUS;
    try {
      jobStatus = unzip.run(monitor);
    } catch (InvocationTargetException e) {
      jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID, "Invocation Target Exception", e);
    }
    setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
    setProperty(IProgressConstants.ACTION_PROPERTY, getViewStatusAction(jobStatus));

    maybeFireCompletedAction(jobStatus);

    monitor.done();
    return jobStatus;
  }
}
