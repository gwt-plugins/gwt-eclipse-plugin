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

import com.google.gwt.eclipse.core.launch.GWTJUnitLaunchDelegate;
import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.launch.util.GWTJUnitLaunchUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Launch shortcut for running a GWT JUnit test case.
 */
public class GWTJUnitLaunchShortcut extends JUnitLaunchShortcut {

  private static final String[] NO_STRINGS = new String[0];

  @Override
  protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(
      IJavaElement element) throws CoreException {
    ILaunchConfigurationWorkingCopy wc = super.createLaunchConfiguration(element);

    wc.rename(getDecoratedName(wc.getName()));

    GWTJUnitLaunchUtils.setDefaults(wc);
    return wc;
  }

  @Override
  protected String[] getAttributeNamesToCompare() {
    ArrayList<String> names = new ArrayList<String>(
        Arrays.asList(super.getAttributeNamesToCompare()));
    names.add(GWTLaunchConstants.ATTR_WEB_MODE);

    return names.toArray(NO_STRINGS);
  }

  protected String getLaunchConfigNameSuffix() {
    return " Development";
  }

  @Override
  protected String getLaunchConfigurationTypeId() {
    return GWTJUnitLaunchDelegate.LAUNCH_TYPE_ID;
  }

  private String getDecoratedName(String oldname) {
    return DebugPlugin.getDefault().getLaunchManager().generateUniqueLaunchConfigurationNameFrom(
        oldname + getLaunchConfigNameSuffix());
  }
}
