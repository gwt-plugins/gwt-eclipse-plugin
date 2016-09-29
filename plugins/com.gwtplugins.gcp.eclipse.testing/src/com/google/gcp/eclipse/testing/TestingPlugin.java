/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.gcp.eclipse.testing;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * Activator for the testing plug-in.
 */
public class TestingPlugin extends Plugin {
  // The plug-in ID
  public static final String PLUGIN_ID = "com.gwtplugins.gcp.eclipse.testing";

  // The shared instance
  private static TestingPlugin instance;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    instance = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    instance = null;
    super.stop(context);
  }

  /**
   * Returns the activator's {@link Plugin} instance.
   */
  public static TestingPlugin getDefault() {
    return instance;
  }

  /**
   * Logs the given status.
   */
  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  /**
   * Logs the given informational message.
   */
  public static void logInfo(String message) {
    getDefault().getLog().log(createStatus(IStatus.INFO, 0, message, null));
  }

  /**
   * Creates a new status object.
   */
  public static IStatus createStatus(int severity, int code, String message, Throwable t) {
    return new Status(severity, PLUGIN_ID, code, message, t);
  }

  /**
   * Creates a new error status with the given message.
   */
  public static IStatus createError(String message) {
    return createStatus(IStatus.ERROR, 0, message, null);
  }

  /**
   * Creates a new error status from the given exception.
   */
  public static IStatus createError(Exception e) {
    return createStatus(IStatus.ERROR, 0, e.getLocalizedMessage(), e);
  }

}
