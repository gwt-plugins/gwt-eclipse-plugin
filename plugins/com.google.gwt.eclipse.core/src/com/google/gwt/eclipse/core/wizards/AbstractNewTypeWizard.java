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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Super type for wizards that create new Java types.
 * 
 * Though this class is very similar to
 * {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard} and
 * {@link org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard}, we cannot
 * subclass those wizards directly because they expect the wizard page to be an
 * instance of {@link org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage} or
 * {@link org.eclipse.jdt.ui.wizards.NewClassWizardPage}, respectively.
 */
@SuppressWarnings("restriction")
public abstract class AbstractNewTypeWizard extends NewElementWizard {

  protected final AbstractNewTypeWizardPage page;

  protected AbstractNewTypeWizard(String title,
      ImageDescriptor imageDescriptor, AbstractNewTypeWizardPage page) {
    this.page = page;
    setWindowTitle(title);
    setDefaultPageImageDescriptor(imageDescriptor);
  }

  @Override
  public void addPages() {
    page.init(getSelection());
    addPage(page);
  }

  /*
   * Copied from
   * {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard}.
   */
  @Override
  public IJavaElement getCreatedElement() {
    return page.getCreatedType();
  }

  /*
   * Copied from
   * {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard}.
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

  /*
   * Copied from
   * {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard}.
   */
  @Override
  protected boolean canRunForked() {
    return !page.isEnclosingTypeSelected();
  }

  /*
   * Copied from
   * {@link org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard}.
   */
  @Override
  protected void finishPage(IProgressMonitor monitor)
      throws InterruptedException, CoreException {
    page.createType(monitor);
  }

  protected AbstractNewTypeWizardPage getPage() {
    return page;
  }

}
