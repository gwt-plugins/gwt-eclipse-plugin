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
package com.google.gwt.eclipse.oophm.launch;

import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

/**
 * Returns the port that the remote UI server is listening on.
 */
public class GwtRemoteUiServerPortVariableResolver implements
    IDynamicVariableResolver {

  public String resolveValue(IDynamicVariable variable, String argument)
      throws CoreException {
    try {
      return String.valueOf(RemoteUIServer.getInstance().getPort());
    } catch (Throwable e) {
      CorePluginLog.logError(e, "Could not get remote UI server's port");
      // We can pass an invalid value which will cause GWT's Swing UI to be used
      return "-1";
    }
  }
}
