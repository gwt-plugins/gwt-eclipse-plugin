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

import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;

/**
 * Launch shortcut for running a GWT JUnit test.
 */
public class GWTJUnitWebLaunchShortcut extends GWTJUnitLaunchShortcut {
  @Override
  protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(
      IJavaElement element) throws CoreException {
    ILaunchConfigurationWorkingCopy wc = super.createLaunchConfiguration(element);
    wc.setAttribute(GWTLaunchConstants.ATTR_WEB_MODE, Boolean.toString(true));
    if (Util.isPlatformMac()) {
      wc.setAttribute(GWTLaunchConstants.ATTR_NOT_HEADLESS, true);
    }
    return wc;
  }

  protected String getLaunchConfigNameSuffix() {
    return " Production";
  }
}
