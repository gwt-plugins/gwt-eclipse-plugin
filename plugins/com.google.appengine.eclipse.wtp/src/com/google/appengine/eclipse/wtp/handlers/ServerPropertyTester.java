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
package com.google.appengine.eclipse.wtp.handlers;

import com.google.gdt.eclipse.core.AdapterUtilities;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Property tester for server-related properties.
 * <ul>
 * <li>Property "isRunning". Returns <code>true</code> if the receiver is {@link IServer} and it has
 * {@link IServer#STATE_STARTED} state.</li>
 * <li>Property "hasModule". Returns <code>true</code> if the receiver is {@link IServer} and it has
 * at least one module associated with it.</li>
 * </ul>
 */
public final class ServerPropertyTester extends PropertyTester {
  private static final String PROPERTY_IS_RUNNING = "isRunning";
  private static final String PROPERTY_HAS_MODULE = "hasModule";

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IServer server = AdapterUtilities.getAdapter(receiver, IServer.class);
    if (server == null) {
      return false;
    }
    if (PROPERTY_IS_RUNNING.equals(property)) {
      return server.getServerState() == IServer.STATE_STARTED;
    } else if (PROPERTY_HAS_MODULE.equals(property)) {
      IModule[] modules = server.getModules();
      return modules != null && modules.length > 0;
    }
    return false;
  }
}
