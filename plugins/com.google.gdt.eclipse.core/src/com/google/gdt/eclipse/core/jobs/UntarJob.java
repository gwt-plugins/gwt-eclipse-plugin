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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * UNUSED!!!
 * 
 * An ExtractJob for extracting tar files, optionally compressed with bzip2 or
 * gzip. This is a thin wrapper around an external library, so while it's
 * working, it can't be canceled and it doesn't update the progress bar.
 */
public class UntarJob extends ExtractJob {

  private UntarRunnable untar;

  public UntarJob(File archive, File targetDir) throws IllegalArgumentException,
      IOException {
    this(archive, targetDir, null);
  }

  public UntarJob(File archive, File targetDir, Action completedAction)
      throws IllegalArgumentException, IOException {
    super(archive, targetDir, completedAction);
    this.untar = new UntarRunnable(archive,targetDir);
  }
  
  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IStatus jobStatus = Status.OK_STATUS;
    try {
      jobStatus = untar.run(monitor);
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
