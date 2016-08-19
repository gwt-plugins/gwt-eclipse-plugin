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
import com.google.gwt.eclipse.core.modules.ModuleFile;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;

/**
 * Creates a new entry point class.
 */
@SuppressWarnings("restriction")
public class NewEntryPointWizard extends NewElementWizard {

  private NewEntryPointWizardPage wizardPage;

  public NewEntryPointWizard() {
    setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
    setDefaultPageImageDescriptor(GWTPlugin.getDefault().getImageDescriptor(
        GWTImages.NEW_ENTRY_POINT_LARGE));
    setWindowTitle("New Entry Point Class");
  }

  @Override
  public void addPages() {
    if (wizardPage == null) {
      wizardPage = new NewEntryPointWizardPage();
      wizardPage.init(getSelection());
    }
    addPage(wizardPage);
  }

  @Override
  public IJavaElement getCreatedElement() {
    return wizardPage.getCreatedType();
  }

  @Override
  public boolean performFinish() {
    boolean res = super.performFinish();
    if (res) {
      IResource resource = wizardPage.getModifiedResource();
      if (resource != null) {
        selectAndReveal(resource);
        openResource((IFile) resource);
      }
    }
    return res;
  }

  @Override
  protected boolean canRunForked() {
    /**
     * Run the wizard in the UI thread. We need to do this because some of the
     * operations performed by the wizard (such as modifying the module file
     * associated with the entry point) require the UI thread.
     */
    return false;
  }

  @Override
  protected void finishPage(IProgressMonitor monitor)
      throws InterruptedException, CoreException {
    wizardPage.createType(monitor);

    String typeName = wizardPage.getCreatedType().getFullyQualifiedName();
    ModuleFile module = wizardPage.getModule();

    try {
      // Bail out if we already have an <entry-point> declaration for this type
      for (String existingEntryPoint : module.getEntryPoints()) {
        if (typeName.equals(existingEntryPoint)) {
          return;
        }
      }
      module.addEntryPoint(typeName);
    } catch (Exception e) {
      throw new CoreException(Util.newErrorStatus(e.getMessage()));
    }
  }

}
