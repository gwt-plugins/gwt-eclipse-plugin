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
package com.google.gwt.eclipse.core.wizards;

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.resources.GWTImages;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;

/**
 * Wizard for creating ClientBundle interfaces.
 * 
 * NOTE: Although this wizard is very similar to the
 * {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard}, we do not subclass it 
 * because it expects the wizard page to be an instance of 
 * {@link org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage}, which our page cannot be.
 */
@SuppressWarnings("restriction")
public class NewClientBundleWizard extends NewElementWizard {

  private final NewClientBundleWizardPage page;

  public NewClientBundleWizard() {
    super();
    this.page = new NewClientBundleWizardPage();

    setWindowTitle("New ClientBundle Interface");
    setDefaultPageImageDescriptor(GWTPlugin.getDefault().getImageDescriptor(
        GWTImages.NEW_CLIENT_BUNDLE_LARGE));
  }

  @Override
  public void addPages() {
    page.init(getSelection());
    addPage(page);
  }

  /**
   * Copied from {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard}.
   */
  @Override
  public IJavaElement getCreatedElement() {
    return page.getCreatedType();
  }

  /**
   * Copied from {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard}.
   */
  @Override
  public boolean performFinish() {
    warnAboutTypeCommentDeprecation();
    boolean res = super.performFinish();
    if (res) {
      IResource resource = page.getModifiedResource();
      if (resource != null) {
        selectAndReveal(resource);
        openResource((IFile) resource);
      }
    }
    return res;
  }

  /**
   * Copied from {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard}.
   */
  @Override
  protected boolean canRunForked() {
    return !page.isEnclosingTypeSelected();
  }

  /**
   * Copied from {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard}.
   */
  @Override
  protected void finishPage(IProgressMonitor monitor)
      throws InterruptedException, CoreException {
    page.createType(monitor);
  }

}
