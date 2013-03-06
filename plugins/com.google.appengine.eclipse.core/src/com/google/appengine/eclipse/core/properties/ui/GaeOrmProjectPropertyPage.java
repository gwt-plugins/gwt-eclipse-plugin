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
package com.google.appengine.eclipse.core.properties.ui;

import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.ui.AbstractProjectPropertyPage;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.osgi.service.prefs.BackingStoreException;

import java.util.List;

/**
 * Specifies classes that use Java ORM.
 */
public class GaeOrmProjectPropertyPage extends AbstractProjectPropertyPage {

  private FileFilterPatternsBlock patternsList;

  private List<IPath> initialPatterns;

  public GaeOrmProjectPropertyPage() {
    noDefaultAndApplyButton();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    GridLayout panelLayout = new GridLayout();
    panelLayout.marginWidth = 0;
    panelLayout.marginHeight = 0;
    panel.setLayout(panelLayout);

    Label label = new Label(panel, SWT.NONE);
    label.setText("Enhance the following classes for usage with Java ORM (specify file, folder, or pattern):");

    initialPatterns = GaeProjectProperties.getOrmEnhancementInclusionPatterns(getProject());
    patternsList = new FileFilterPatternsBlock(panel, getProject(), false,
        initialPatterns);

    return panel;
  }

  @Override
  protected void saveProjectProperties() throws BackingStoreException {
    List<IPath> inclusions = patternsList.getPatterns();
    if (inclusions.equals(initialPatterns)) {
      return;
    }

    GaeProjectProperties.setOrmEnhancementInclusionPatterns(getProject(),
        inclusions);
    BuilderUtilities.scheduleRebuild(getProject());
  }

}
