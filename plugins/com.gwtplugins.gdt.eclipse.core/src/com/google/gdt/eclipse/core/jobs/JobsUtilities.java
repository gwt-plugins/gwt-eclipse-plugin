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

import com.google.gdt.eclipse.core.SWTUtilities;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * Utility methods that relate to jobs. 
 */
public class JobsUtilities {

  public static boolean isModal(Job job) {
    Boolean isModal = (Boolean) job.getProperty(IProgressConstants.PROPERTY_IN_DIALOG);
    return (isModal == null) ? false : isModal.booleanValue();
  }
  
  /**
   * Wait until all background tasks are complete.
   */
  public static void waitForIdle() {
    while (!Job.getJobManager().isIdle()) {
      SWTUtilities.delay(1000);
    }
  }

  private JobsUtilities() {
    // not instantiable.
  }
}
