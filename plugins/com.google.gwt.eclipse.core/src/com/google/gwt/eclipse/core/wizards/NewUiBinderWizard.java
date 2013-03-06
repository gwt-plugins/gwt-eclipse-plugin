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

/**
 * Wizard for creating UiBinder ui.xml template + owner class pairs.
 */
@SuppressWarnings("restriction")
public class NewUiBinderWizard extends AbstractNewTypeWizard {

  public NewUiBinderWizard() {
    super("New UiBinder", GWTPlugin.getDefault().getImageDescriptor(
        GWTImages.NEW_UI_BINDER_LARGE), new NewUiBinderWizardPage());
    setHelpAvailable(false);
  }

  @Override
  public boolean performFinish() {
    boolean ret = super.performFinish();

    // Open the generated ui.xml file in an editor alongside the .java file,
    // which is opened automatically by our superclass.
    IFile uiXmlFile = ((NewUiBinderWizardPage) page).getUiXmlFile();
    selectAndReveal(uiXmlFile);
    openResource(uiXmlFile);

    return ret;
  }
}
