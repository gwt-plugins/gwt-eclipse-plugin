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
package com.google.gdt.eclipse.gph.actions;

import com.google.gdt.eclipse.gph.wizards.ImportHostedProjectsWizard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * 
 */
public class ImportHostedProjectsHandler extends AbstractHandler {

  public Object execute(ExecutionEvent event) {

    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

    ImportHostedProjectsWizard wizard = new ImportHostedProjectsWizard();
    wizard.init(window.getWorkbench(), new StructuredSelection());

    WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
    dialog.create();
    dialog.open();

    return null;
  }

}
