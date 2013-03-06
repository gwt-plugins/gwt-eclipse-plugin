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
package com.google.gdt.eclipse.gph.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * A wizard for displaying errors to the user (suitable for nesting).
 */
public class ErrorWizard extends Wizard implements IWorkbenchWizard {

  private final IStatus error;

  /**
   * Create an instance.
   * 
   * @param error the error
   */
  public ErrorWizard(IStatus error) {
    this.error = error;

    setWindowTitle(ImportHostedProjectsWizard.getDefaultPageTitle());
    setDefaultPageImageDescriptor(ImportHostedProjectsWizard.getDefaultPageImageDescriptor());
  }

  @Override
  public void addPages() {
    addPage(new ShowErrorPage(this, error));
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // ignored
  }

  @Override
  public boolean performFinish() {
    return true;
  }

}
