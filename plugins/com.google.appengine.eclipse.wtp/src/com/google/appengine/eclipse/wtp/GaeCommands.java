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
package com.google.appengine.eclipse.wtp;

import com.google.appengine.eclipse.wtp.server.GaeServer;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Simple Eclipse do/undo framework
 */
public class GaeCommands extends AbstractOperation {

  protected String value;
  protected String oldValue;
  protected String commandName;
  protected GaeServer gaeServer;

  public GaeCommands(IServerWorkingCopy server, String newValue, String commandName) {
    super(commandName);
    gaeServer = GaeServer.getGaeServer(server);
    value = newValue;
    this.commandName = commandName;
  }

  @Override
  public IStatus execute(IProgressMonitor monitor, IAdaptable adapt) {
    oldValue = gaeServer.getServerInstanceProperties().get(commandName);
    gaeServer.getServerInstanceProperties().put(commandName, value);
    return null;
  }

  @Override
  public IStatus redo(IProgressMonitor monitor, IAdaptable adapt) {
    return execute(monitor, adapt);
  }

  @Override
  public IStatus undo(IProgressMonitor monitor, IAdaptable adapt) {
    if (gaeServer != null) {
      gaeServer.getServerInstanceProperties().put(commandName, oldValue);
    }
    return null;
  }
}
