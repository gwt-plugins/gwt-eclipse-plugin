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
import com.google.gdt.eclipse.core.CorePluginLog;

import org.apache.tools.ant.taskdefs.Untar.UntarCompressionMethod;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Runnable untars an archive to the specified directory.
 */
public class UntarRunnable extends AbstractExtractRunnable {

  /**
   * Internal convenience method determines the archive type from the filename
   * extension.
   */
  private static UntarCompressionMethod compressionMethod(File file) {
    String fn = file.toString();
    UntarCompressionMethod out = new UntarCompressionMethod();

    if (fn.endsWith("gz")) {
      out.setValue("gzip");
    } else if (fn.endsWith("bz2")) {
      out.setValue("bzip2");
    } else if (fn.endsWith("tar")) {
      out.setValue("none");
    } else {
      throw new IllegalArgumentException("UntarJob doesn't know what to do with that file. "
          + "tar, gz, and bz2 files accepted.");
    }
    return out;
  }

  /**
   * Checks and saves constructor parameters.
   * 
   * @param archive
   *          the archive to extract
   * @param targetDir
   *          the destination directory of the extraction process.
   * @throws IllegalArgumentException
   *           if either the archive or target does not exist or can not be
   *           read.
   */
  public UntarRunnable(File archive, File targetDir) throws IllegalArgumentException {
    super(archive, targetDir);
  }

  /**
   * Executes the untarRunnable using the provided monitor to update the user
   * and provide cancel capabilities.
   * 
   * @param monitor
   *          used to monitor progress of the DownloadRunnable process.
   * 
   * @return The completion status of the runnable. If everything goes well,
   *         then this status is OK_STATUS. If the monitor cancels the job,
   *         Status.CANCEL_STATUS. Other errors result in a status with a
   *         severity of ERROR.
   */
  public IStatus run(IProgressMonitor monitor) throws InvocationTargetException {
    jobStatus = Status.OK_STATUS;

    monitor.beginTask("Uncompressing " + archive.getName(), 1);
    org.apache.tools.ant.taskdefs.Untar untar = new org.apache.tools.ant.taskdefs.Untar();
    untar.setDest(targetDir);
    untar.setSrc(archive);

    untar.setCompression(compressionMethod(archive));

    try {
      untar.execute();
      monitor.worked(1);
    } catch (Exception be) {
      CorePluginLog.logError(be);
      jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID, "Error handling archive file "
          + archive.getAbsolutePath() + ". It may be corrupted?");
    }
    return jobStatus;
  }
}
