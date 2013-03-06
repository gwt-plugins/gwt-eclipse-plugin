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
package com.google.gwt.eclipse.core.launch.ui;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * Basically the same as the JUnitLaunchConfigurationTab, but adds the
 * constraint that the selected project must have the GWT nature.
 */
public class JUnitLaunchConfigurationTabDecorator extends
    JUnitLaunchConfigurationTab {

  private ILaunchConfiguration launchConfiguration;

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    super.initializeFrom(config);
    launchConfiguration = config;
  }

  @Override
  public boolean isValid(ILaunchConfiguration config) {
    try {
      IJavaProject javaProject = JavaRuntime.getJavaProject(getCurrentLaunchConfiguration());

      if (javaProject != null
          && !GWTNature.isGWTProject(javaProject.getProject())) {
        setErrorMessage("Selected project does not have GWT enabled.");
        return false;
      }
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
    }

    return super.isValid(config);
  }

  protected ILaunchConfiguration getCurrentLaunchConfiguration() {
    return launchConfiguration;
  }
}
