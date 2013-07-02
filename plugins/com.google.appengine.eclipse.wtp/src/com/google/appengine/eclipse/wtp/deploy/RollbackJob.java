/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.deploy;

import com.google.appengine.eclipse.wtp.server.GaeServer;

import org.eclipse.core.runtime.CoreException;

import java.io.OutputStream;
import java.util.List;

/**
 * Performs rollback of operation.
 *
 * AppCfg equivalent: AppCfg rollback &lt;app_dir&gt;
 */
public final class RollbackJob extends PublishingJob {
  private static final String COMMAND_ROLLBACK = "rollback";

  public RollbackJob(String oauth2ClientId, String oauth2ClientSecret, String oauth2RefreshToken,
      GaeServer gaeServer, OutputStream outputStream) throws CoreException {
    super(oauth2ClientId, oauth2ClientSecret, oauth2RefreshToken, gaeServer, outputStream);
  }

  @Override
  protected List<String> getProcessArguments() throws CoreException {
    List<String> args = super.getProcessArguments();
    args.add(COMMAND_ROLLBACK);
    args.add(appDirectory.toOSString());
    return args;
  }
}