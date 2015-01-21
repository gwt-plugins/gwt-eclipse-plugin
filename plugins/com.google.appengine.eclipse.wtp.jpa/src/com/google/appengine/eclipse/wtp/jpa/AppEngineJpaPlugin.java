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
package com.google.appengine.eclipse.wtp.jpa;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Google App Engine WTP plug-in life-cycle.
 */
public final class AppEngineJpaPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "com.google.appengine.eclipse.wtp.jpa";
  private static AppEngineJpaPlugin INSTANCE;

  public static IStatus createErrorStatus(String mess, Exception e) {
    return new Status(IStatus.ERROR, PLUGIN_ID, -1, mess, e);
  }

  public static AppEngineJpaPlugin getInstance() {
    return INSTANCE;
  }

  public static void logMessage(String mess) {
    logMessage(mess, null);
  }

  public static void logMessage(String msg, Throwable e) {
    msg = msg == null ? "Google App Engine Error" : "Google App Engine: " + msg;
    Status status = new Status(IStatus.ERROR, PLUGIN_ID, 1, msg, e);
    getInstance().getLog().log(status);
  }

  public static void logMessage(Throwable e) {
    logMessage(null, e);
  }

  public AppEngineJpaPlugin() {
    INSTANCE = this;
  }

}
