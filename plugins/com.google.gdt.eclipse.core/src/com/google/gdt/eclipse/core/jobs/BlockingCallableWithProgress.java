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
package com.google.gdt.eclipse.core.jobs;

import org.eclipse.core.runtime.IProgressMonitor;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Enables the execution, in conjunction with a progress monitor, of an operation (such as a remote
 * service call) that may block for a long time. The operation is specified by a {@link Callable}
 * object. The operation is run in a separate thread while the calling thread polls the progress
 * monitor at fixed intervals to check whether cancellation has been requested. If so, the separate
 * thread running the blocking operation is cancelled.
 * 
 * <p>(If the {@link Callable#call()} method of the {@code Callable} does not call
 * {@link IProgressMonitor#done()}, then the {@link #call(IProgressMonitor)} method of this class is
 * guaranteed not to call {@code IProgressMonitor.done()}. This knowledge may make it unnecessary to
 * spawn a submonitor to pass into the {@link #call(IProgressMonitor)} method of this class.)
 * 
 * @param <T> the type of the result returned by the {@link Callable}
 */
public class BlockingCallableWithProgress<T> {
  
  private Callable<T> delegate;
  private int pollingIntervalInMillis;

  public BlockingCallableWithProgress(Callable<T> delegate, int pollingIntervalInMillis) {
    this.delegate = delegate;
    this.pollingIntervalInMillis = pollingIntervalInMillis;
  }

  /**
   * Invoke the operation specified by the {@link Callable<T>} passed to the constructor while
   * periodically checking a specified {@link IProgressMonitor} for cancellation.
   * 
   * @param monitor the specified {@code IProgressMonitor}
   * @return
   *     the result of the {@code call} method of the {@link Callable<T>} passed to the constructor
   * @throws InvocationTargetException
   *     if the  {@code call} method of the {@link Callable<T>} throws an exception
   * @throws InterruptedException
   *     if the operation is cancelled through the {@link IProgressMonitor} or the calling thread is
   *     interrupted
   */
  public T call(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
    long nextPollTime = System.currentTimeMillis();
    Future<T> result = Executors.newSingleThreadExecutor().submit(delegate);
    while (!result.isDone()) {
      long sleepInterval = nextPollTime - System.currentTimeMillis();
      if (sleepInterval > 0) {
        try {
          Thread.sleep(sleepInterval);
        } catch (InterruptedException e) {
          result.cancel(true);
          throw new InterruptedException();
        }
      }
      if (monitor.isCanceled()) {
        result.cancel(true);
        throw new InterruptedException();
      }
      nextPollTime += pollingIntervalInMillis;
    }
    try {
      return result.get();
    } catch (ExecutionException e) {
      throw new InvocationTargetException(e.getCause());
    }
  }
}
