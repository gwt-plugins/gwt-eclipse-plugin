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
package com.google.appengine.eclipse.wtp.handlers;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.deploy.DeployJob;
import com.google.appengine.eclipse.wtp.deploy.RollbackJob;
import com.google.appengine.eclipse.wtp.server.GaeServer;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.StringUtilities;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IServer;

import java.io.OutputStream;
import java.lang.reflect.Constructor;

/**
 * Handler for remote operations which are able to dynamically create jobs depending on parameters
 * in plugin.xml
 *
 * For example, see {@link DeployJob}, {@link RollbackJob}.
 */
public final class ParameterizedRemoteOperationHandler extends RemoteOperationHandler {
  private static final String DEPLOY_PACKAGE = "com.google.appengine.eclipse.wtp.deploy.";
  private static final String PARAM_JOB_CLASS = "com.google.appengine.eclipse.wtp.params.jobClass";

  protected String jobClassName;

  @Override
  protected Job createJob() throws CoreException {
    try {
      Class<?> jobClass = Class.forName(jobClassName);
      Constructor<?> jobConstructor = jobClass.getConstructor(String.class, String.class,
          String.class, GaeServer.class, OutputStream.class);
      return (Job) jobConstructor.newInstance(clientId, clientSecret, refreshToken, gaeServer,
          newMessageStream);
    } catch (Exception e) {
      throw new CoreException(StatusUtilities.newErrorStatus(e, AppEnginePlugin.PLUGIN_ID));
    }
  }

  @Override
  protected Object execute(ExecutionEvent event, IServer server) throws ExecutionException {
    String jobClassName = event.getParameter(PARAM_JOB_CLASS);
    if (jobClassName == null) {
      throw new ExecutionException("Incorrect handler configuration.");
    }
    this.jobClassName = DEPLOY_PACKAGE + StringUtilities.capitalize(jobClassName) + "Job";
    doExecute(server);
    return null;
  }
}
