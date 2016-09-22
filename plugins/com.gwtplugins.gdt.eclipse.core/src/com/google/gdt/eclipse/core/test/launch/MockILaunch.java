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
package com.google.gdt.eclipse.core.test.launch;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;

/**
 * Mock implementation of {@link ILaunch} which returns nulls and falses except
 * for:
 * <ul>
 * <li>getLaunchConfiguration(): returns a new {@link MockILaunchConfiguration}
 * </ul>
 */
@SuppressWarnings("unchecked")
public class MockILaunch implements ILaunch {

  public void addDebugTarget(IDebugTarget target) {
  }

  public void addProcess(IProcess process) {
  }

  public boolean canTerminate() {
    return false;
  }

  public Object getAdapter(Class adapter) {
    return null;
  }

  public String getAttribute(String key) {
    return null;
  }

  public Object[] getChildren() {
    return null;
  }

  public IDebugTarget getDebugTarget() {
    return null;
  }

  public IDebugTarget[] getDebugTargets() {
    return null;
  }

  public ILaunchConfiguration getLaunchConfiguration() {
    return new MockILaunchConfiguration();
  }

  public String getLaunchMode() {
    return null;
  }

  public IProcess[] getProcesses() {
    return null;
  }

  public ISourceLocator getSourceLocator() {
    return null;
  }

  public boolean hasChildren() {
    return false;
  }

  public boolean isTerminated() {
    return false;
  }

  public void removeDebugTarget(IDebugTarget target) {
  }

  public void removeProcess(IProcess process) {
  }

  public void setAttribute(String key, String value) {
  }

  public void setSourceLocator(ISourceLocator sourceLocator) {
  }

  public void terminate() throws DebugException {
  }

}
