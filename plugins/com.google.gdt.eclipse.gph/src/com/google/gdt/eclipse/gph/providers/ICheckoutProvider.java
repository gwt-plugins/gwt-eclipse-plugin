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
package com.google.gdt.eclipse.gph.providers;

import com.google.gdt.eclipse.gph.install.P2InstallationUnit;
import com.google.gdt.eclipse.gph.model.GPHProject;

import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * Implementers provide wizards for performing source checkouts.
 */
public interface ICheckoutProvider {

  /**
   * Returns the wizard to drive the checkout process.
   * 
   * @param shellProvider the shell provider
   * @param project the GPH project
   * @return the wizard object
   */
  IWorkbenchWizard createWizard(IShellProvider shellProvider, GPHProject project);

  /**
   * @return the P2InstallationUnit that will provision this SCM provider
   */
  P2InstallationUnit getP2InstallationUnit();

  /**
   * @return whether all the needed plugins / features are installed
   */
  boolean isFullyInstalled();

}
