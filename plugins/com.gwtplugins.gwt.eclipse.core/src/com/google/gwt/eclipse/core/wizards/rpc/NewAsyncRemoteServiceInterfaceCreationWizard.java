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
package com.google.gwt.eclipse.core.wizards.rpc;

import org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard;
import org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

/**
 * Modified {@link NewInterfaceCreationWizard} for creating asynchronous remote
 * services.
 */
@SuppressWarnings("restriction")
public class NewAsyncRemoteServiceInterfaceCreationWizard extends
    NewInterfaceCreationWizard {
  public static final String DEFAULT_WINDOW_TITLE = "New Asynchronous Remote Service Interface";
  public NewAsyncRemoteServiceInterfaceCreationWizard(NewInterfaceWizardPage page,
      boolean openEditorOnFinish) {
    super(page, openEditorOnFinish);
  }
  
  @Override
  public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
    super.init(workbench, currentSelection);
    setWindowTitle(DEFAULT_WINDOW_TITLE);
  }
}
