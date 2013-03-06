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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Abstract wizard page for {@link AbstractNewTypeWizard}.
 */
public abstract class AbstractNewTypeWizardPage extends NewTypeWizardPage {

  public AbstractNewTypeWizardPage(String title, String description,
      boolean isClass, String pageName) {
    super(isClass, pageName);
    setTitle(title);
    setDescription(description);
  }

  public void init(IStructuredSelection selection) {
    IJavaElement jelem = getInitialJavaElement(selection);
    initContainerPage(jelem);
    initTypePage(jelem);

    doStatusUpdate();
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      setFocus();
    }
  }

  protected abstract void doStatusUpdate();

  @Override
  protected void handleFieldChanged(String fieldName) {
    super.handleFieldChanged(fieldName);
    doStatusUpdate();
  }

}
