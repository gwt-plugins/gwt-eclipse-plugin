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
package com.google.gdt.eclipse.suite.actions;

import com.google.gdt.eclipse.core.actions.AbstractOpenWizardAction;
import com.google.gdt.eclipse.suite.wizards.NewWebAppProjectWizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;

/**
 * 
 */
@SuppressWarnings("restriction")
public class OpenNewWebApplicationWizardAction extends AbstractOpenWizardAction {
  public OpenNewWebApplicationWizardAction() {
    setText("New Web Application Project");
    setDescription("New Web Application Project");
    setToolTipText("New Web Application Project");
    // setImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
    // PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
    // IJavaHelpContextIds.OPEN_PROJECT_WIZARD_ACTION);
    setShell(JavaPlugin.getActiveWorkbenchShell());
  }

  @Override
  protected INewWizard createWizard() throws CoreException {
    return new NewWebAppProjectWizard();
  }

  @Override
  protected boolean doCreateProjectFirstOnEmptyWorkspace(Shell shell) {
    // Okay to create a new project in an empty workspace
    return true;
  }
}
