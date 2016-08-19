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
package com.google.gdt.eclipse.suite.launch.ui;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.platform.debug.ui.WorkingDirectoryBlock;
import com.google.gdt.eclipse.suite.launch.processors.WarArgumentProcessor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * A WorkingDirectoryBlock to customize the displayed value for the default
 * working directory text box.
 * 
 * The value set on the default working directory text box is a fixed string
 * instead of being up-to-date with the current WAR directory. The reason is
 * when the user changes the WAR directory in the program arguments, there is
 * not a clean (non-reflective) way of updating the text of the default working
 * dir text box without the superclass automatically reseting the launch
 * configuration to use the default working directory. See the superclass's
 * implementation of setDefaultWorkingDirectoryText.
 */
public class WebAppWorkingDirectoryBlock extends WorkingDirectoryBlock {

  private static final String DEFAULT_WAR_DIRECTORY_STRING = "WAR directory";

  @Override
  public boolean isValid(ILaunchConfiguration config) {
    // super's implementation will try to resolve the actual string to a path,
    // so if the user is using the default, assume it is valid
    if (DEFAULT_WAR_DIRECTORY_STRING.equals(getWorkingDirectoryText())) {
      setErrorMessage(null);
      setMessage(null);
      return true;
    } else {
      return super.isValid(config);
    }
  }

  @Override
  protected void setDefaultWorkingDir() {
    ILaunchConfiguration config = getLaunchConfiguration();
    try {
      if (WarArgumentProcessor.doesMainTypeTakeWarArgument(config)) {
        setDefaultWorkingDirectoryText(DEFAULT_WAR_DIRECTORY_STRING);
        return;
      }
    } catch (CoreException e) {
      CorePluginLog.logWarning(e, "Could not set default working directory");
    }

    super.setDefaultWorkingDir();
  }
}
