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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IServer;

/**
 * A base handler for single-selection {@link IServer} actions.
 */
public abstract class AbstractSingleServerHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getActiveWorkbenchWindow(
        event).getActivePage().getSelection();
    if (selection != null && !selection.isEmpty()) {
      IServer server = (IServer) selection.getFirstElement();
      execute(event, server);
    }
    return null;
  }

  /**
   * Performs execution on selected {@link IServer}.
   */
  protected abstract Object execute(ExecutionEvent event, IServer server) throws ExecutionException;
}