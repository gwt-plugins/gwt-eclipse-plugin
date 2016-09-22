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

import java.io.File;
import java.net.URL;

/**
 * A DownloadJob that just sends the request and doesn't actually save the file.
 * Useful for pings for stat purposes
 */
public class PingJob extends DownloadJob {

  /**
   * @param name the name of the job.
   * @param url the URL to ping.
   */
  public PingJob(String name, URL url) {
    super(name, url, null);
  }
  
  @Override
  protected IRunnableWithProgressAndStatus doGetRunnable(URL url, File target) {
    return new PingRunnable(url);
  }
  
}
