/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.jpa;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jpt.common.ui.jface.ItemTreeStateProviderFactoryProvider;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.ui.JpaPlatformUi;
import org.eclipse.jpt.jpa.ui.JpaPlatformUiProvider;
import org.eclipse.jpt.jpa.ui.internal.platform.base.AbstractJpaPlatformUi;
import org.eclipse.jpt.jpa.ui.internal.wizards.conversion.java.GenericJavaGeneratorConversionWizardPage;
import org.eclipse.jpt.jpa.ui.internal.wizards.conversion.java.GenericJavaQueryConversionWizardPage;

/**
 * Currently this is convenience wrapper class for {@link JpaPlatformUi}.
 */
@SuppressWarnings("restriction")
public class GaeJpaPlatformUi extends AbstractJpaPlatformUi {

  public GaeJpaPlatformUi(ItemTreeStateProviderFactoryProvider navigatorFactoryProvider,
      JpaPlatformUiProvider platformUiProvider) {
    super(navigatorFactoryProvider, platformUiProvider);
  }

  @Override
  public void convertJavaGeneratorMetadataToGlobal(JpaProject jpaProject) {
    this.openInDialog(new GenericJavaGeneratorConversionWizardPage(jpaProject));
  }

  @Override
  public void convertJavaQueryMetadataToGlobal(JpaProject jpaProject) {
    this.openInDialog(new GenericJavaQueryConversionWizardPage(jpaProject));
  }

  @Override
  public void generateDDL(JpaProject project, IStructuredSelection selection) {
    this.displayMessage("Generate Tables from Entities",
        "Generate Tables from Entities currently is not supported by the App Engine Platform");
  }
}
