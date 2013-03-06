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

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A super class for wizard pages. It provides page enter and exit notifications.
 */
public abstract class AbstractWizardPage extends WizardPage implements
    IFinishablePage {

  protected AbstractWizardPage(String pageName) {
    super(pageName);
  }

  public final void createControl(Composite parent) {
    Control contents = createPageContents(parent);

    setControl(contents);

    IWizardContainer container = getWizard().getContainer();

    if (container instanceof WizardDialog) {
      WizardDialog dialog = (WizardDialog) container;

      dialog.addPageChangingListener(new IPageChangingListener() {
        public void handlePageChanging(PageChangingEvent event) {
          if (event.getCurrentPage() == AbstractWizardPage.this
              && event.getTargetPage() != getPreviousPage()) {
            if (!exitingPage()) {
              event.doit = false;
            }
          }
        }
      });
    }
  }

  public boolean performFinish() {
    return true;
  }

  @Override
  public final void setVisible(boolean visible) {
    super.setVisible(visible);

    if (visible) {
      enteringPage();
    }
  }

  protected abstract Control createPageContents(Composite parent);

  protected void enteringPage() {
  }

  protected boolean exitingPage() {
    return true;
  }

}
