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

import java.io.IOException;
import java.net.URL;

/**
 * Pings the given URL.
 */
@SuppressWarnings("restriction")
public class PingRunnable implements IRunnableWithProgressAndStatus {

  private final URL url;

  public PingRunnable(URL url) {
    this.url = url;
  }

  public IStatus run(IProgressMonitor monitor) {

    IStatus jobStatus = Status.OK_STATUS;

    try {
      url.openConnection();
      url.openStream();
    } catch (IOException e) {
      jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID,
          "IO error while downloading " + url.toString(), e);
    }

    return jobStatus;
  }
}
