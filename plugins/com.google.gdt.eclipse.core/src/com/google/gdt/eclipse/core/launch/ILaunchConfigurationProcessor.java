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
package com.google.gdt.eclipse.core.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * Processes a launch configuration to typically update the configuration's
 * arguments. This will only be called for web apps launch configuration types.
 * <p>
 * There is no guarantee that
 * {@link #validate(ILaunchConfiguration, IJavaProject, List, List)} will have
 * been called before
 * {@link #update(ILaunchConfigurationWorkingCopy, IJavaProject, List, List)}.
 * These are targeting different use cases.
 * <p>
 * When there is a UI widget linked with a particular argument, ensure the state
 * is being synchronized properly between the persistent storage, the arguments
 * tab, and the UI widget. The typical pattern is:
 * 
 * <ul>
 * <li>have the tab register as an argument container listener so it can persist
 * when the user changes arguments
 * <li>have the tab's createControl() create the widget and set a change
 * listener to call updateLaunchConfigurationDialog()
 * <li>have the tab's initializeFrom() initialize the widget from the persistent
 * launch configuration working copy
 * <li>have the tab's performApply() persist the widget's state to the launch
 * configuration working copy
 * <li>have the tab's persistFromArguments() read the value from the given
 * arguments and persist it to the launch configuration working copy
 * <li>create member variable blockUpdateLaunchConfigurationDialog which will be
 * true while the method initializeFrom() is being called. Set it to true at the
 * top of initializeFrom(), and add a try-finally to reset it to false at the
 * end of initializeFrom(). Finally, override the
 * updateLaunchConfigurationDialog() to only call through to super if
 * blockUpdateLaunchConfigurationDialog is false. This is required since the
 * initializeFrom() will set contents on the UI widgets (since it updates the UI
 * from the persisted values), which triggers the widgets' listeners. These
 * listeners call updateLaunchConfigurationDialog(), which call performApply,
 * which could potentially persist with old data. For example, if a tab has
 * three text fields, when the first text field's set method is called from
 * initializeFrom(), it will end up calling performApply(). performApply() will
 * persist the current value of all three text fields. Problem is, the persisted
 * value could be newer than the value in the UI field (if the user changed the
 * value in the arguments tab and is now switching back to the tab with the
 * three text fields.)
 * <li>in performApply(), exit right away (noop) if the current tab is not
 * active, based on the getLaunchConfigurationDialog().getActiveTab(). An
 * example to illustrate why we need this: the user changes the port in the
 * Server tab, which causes the new value to be persisted to the port attribute
 * and program args attribute in the launch configuration working copy. Now, if
 * the user hits "Run", performApply() will be called on ALL tabs. The ordering
 * is based on the UI ordering, so the arguments tab's performApply() will be
 * called last. The problem is that tab's performApply() will persist the
 * program args text box's value (which was never updated with the new port
 * value) to the launch configuration working copy, overwriting the user's
 * changes. We do not update the UI in the args tab with each user change
 * because this causes excess callbacks and is noticably slower for the user.
 * Note that when the user is switching tabs, calling getActiveTab() from the
 * old tab will actually return the new tab. Because of this, the performApply()
 * that happens when the user leaves the tab (called by decativated()) will just
 * noop. This is ok, since our UI widget calls updateLaunchConfigurationDialog()
 * which calls performApply() as soon as the user changes the UI state.
 * 
 * </ul>
 */
public interface ILaunchConfigurationProcessor {

  /**
   * Update the launch configuration, if necessary.
   * <p>
   * The client is allowed to place data in the launch configuration, just
   * ensure the key is unique.
   */
  void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException;

  /**
   * Validate the launch configuration.
   * 
   * @return an error message, or null
   */
  String validate(ILaunchConfiguration launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException;
}
