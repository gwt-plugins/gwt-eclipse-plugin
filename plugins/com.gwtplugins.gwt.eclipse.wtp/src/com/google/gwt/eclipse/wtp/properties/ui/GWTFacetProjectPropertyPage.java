/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.properties.ui;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gdt.eclipse.core.ui.AbstractProjectPropertyPage;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.wtp.GwtWtpPlugin;

/**
 * Property page for setting GWT project properties (SDK selection and the
 * default set of entry point modules).
 */
@SuppressWarnings("restriction")
public class GWTFacetProjectPropertyPage extends AbstractProjectPropertyPage {

  public static final String ID = GwtWtpPlugin.PLUGIN_ID + ".properties.ui.gwtFacetProjectPropertyPage";

  private GwtFacetComposite gwtFacetPanel;

  public GWTFacetProjectPropertyPage() {
    noDefaultAndApplyButton();
  }

  @Override
  protected Control createContents(Composite parent) {
    gwtFacetPanel = new GwtFacetComposite(parent, SWT.NONE);

    loadSettings();
    
    return gwtFacetPanel;
  }

  private void loadSettings() {
    Boolean syncServer = GWTProjectProperties.getFacetSyncCodeServer(getProject());
    if (syncServer == null) {
      syncServer = true;
    }
    gwtFacetPanel.setSyncServer(syncServer);
  }

  @Override
  protected void saveProjectProperties() throws BackingStoreException, CoreException, FileNotFoundException {
    GWTProjectProperties.setFacetSyncCodeServer(getProject(), gwtFacetPanel.getSyncServer());
  }

  protected IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }

}
