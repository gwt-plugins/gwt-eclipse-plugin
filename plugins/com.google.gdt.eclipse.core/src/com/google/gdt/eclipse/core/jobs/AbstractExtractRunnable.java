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

import java.io.File;

/**
 * Parent of extract runnables. This type provides common functionality between
 * unzip and untar actions.
 */
public abstract class AbstractExtractRunnable implements IRunnableWithProgressAndStatus {

  protected File targetDir;
  protected File archive;
  protected IStatus jobStatus;

  /**
   * Checks and saves constructor parameters.
   * 
   * @param archive the archive to extract
   * @param targetDir the destination directory of the extraction process.
   * @throws IllegalArgumentException if either the archive or target does not exist or can not be read.
   */
  protected AbstractExtractRunnable(File archive, File targetDir) throws IllegalArgumentException {
    if (targetDir.exists() && targetDir.isDirectory()) {
      this.targetDir = targetDir;
    } else {
      throw new IllegalArgumentException("Specified target " + targetDir.getAbsolutePath()
          + " is not a directory.");
    }

    if (archive.exists() && archive.isFile() && archive.canRead()) {
      this.archive = archive;
    } else {
      throw new IllegalArgumentException("Specified archive file " + archive.getAbsolutePath()
          + " does not exist or can not be read.");
    }
    this.archive = archive;
  }
}
