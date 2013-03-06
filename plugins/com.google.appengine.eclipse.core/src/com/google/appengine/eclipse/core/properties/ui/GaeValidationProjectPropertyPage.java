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
 * Excludes user-specified classes from GAE validation.
 */
public class GaeValidationProjectPropertyPage extends
    AbstractProjectPropertyPage {

  private FileFilterPatternsBlock patternsList;

  private List<IPath> initialPatterns;

  public GaeValidationProjectPropertyPage() {
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
    label.setText("Exclude the following classes from validation (specify file, folder, or pattern):");

    initialPatterns = GaeProjectProperties.getValidationExclusionPatterns(getProject());
    patternsList = new FileFilterPatternsBlock(panel, getProject(), true,
        initialPatterns);

    return panel;
  }

  @Override
  protected void saveProjectProperties() throws BackingStoreException {
    List<IPath> exclusions = patternsList.getPatterns();
    if (exclusions.equals(initialPatterns)) {
      return;
    }

    GaeProjectProperties.setValidationExclusionPatterns(getProject(),
        exclusions);
    BuilderUtilities.scheduleRebuild(getProject());
  }
}
