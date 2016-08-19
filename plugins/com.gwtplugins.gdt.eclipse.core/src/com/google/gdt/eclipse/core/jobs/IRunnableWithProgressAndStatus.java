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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import java.lang.reflect.InvocationTargetException;

/**
 * The <code>IRunnableWithProgressAndStatus</code> interface should be
 * implemented by classes intended to be reused in the context of Job,
 * IRunnableWithProgress or composed into larger Jobs or Runnables.
 * 
 * @see IRunnableContext
 */
public interface IRunnableWithProgressAndStatus {
  /**
   * Runs the operation. Progress should be reported to the given progress
   * monitor. The monitor should be treated as if it were in-progress (it is
   * good practice to create SubMonitor when composing a Job out of multiple
   * runnables). A request to cancel the operation should be honored and
   * acknowledged by returning a CANCEL_STATUS.
   * 
   * NOTE: DO NOT CALL DONE ON THE MONITOR.
   * 
   * @param monitor
   *          the progress monitor to use to display progress and receive
   *          requests for cancelation
   * @exception InvocationTargetException
   *              if the run method must propagate a checked exception, it
   *              should wrap it inside an
   *              <code>InvocationTargetException</code>; runtime exceptions are
   *              automatically wrapped in an
   *              <code>InvocationTargetException</code> by the calling context
   * 
   * @see IRunnableContext#run
   */
  public IStatus run(IProgressMonitor monitor) throws InvocationTargetException;

}
