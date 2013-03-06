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

import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.providers.ICheckoutProvider;

import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * Basic {@link IWizardNode} implementation for driving checkout wizards.
 */
public class CheckoutWizardNode implements IWizardNode {

  private final ICheckoutProvider checkoutProvider;
  private final GPHProject project;
  private final IShellProvider shellProvider;

  private IWizard wizard;

  /**
   * Create an instance.
   * 
   * @param checkoutProvider the checkout provider
   * @param project the GPH project
   * @param shellProvider the shell provider
   */
  public CheckoutWizardNode(ICheckoutProvider checkoutProvider,
      GPHProject project, IShellProvider shellProvider) {
    this.checkoutProvider = checkoutProvider;
    this.project = project;
    this.shellProvider = shellProvider;
  }

  /**
   * Returns the wizard represented by this wizard node.
   * 
   * @return the wizard object
   */
  public IWorkbenchWizard createWizard() {
    return checkoutProvider.createWizard(shellProvider, project);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.wizard.IWizardNode#dispose()
   */
  public void dispose() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.wizard.IWizardNode#getExtent()
   */
  public Point getExtent() {
    return new Point(-1, -1); // unknown
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.wizard.IWizardNode#getWizard()
   */
  public IWizard getWizard() {
    if (wizard != null) {
      return wizard;
    }

    // TODO: consider busy cursor and error handling (see WorkbenchWizardNode)
    wizard = createWizard();
    return wizard;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.wizard.IWizardNode#isContentCreated()
   */
  public boolean isContentCreated() {
    return wizard != null;
  }

}
