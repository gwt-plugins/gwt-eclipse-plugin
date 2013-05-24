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
package com.google.appengine.eclipse.wtp.jpa.libprov;

import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;

import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderOperationConfig;
import org.eclipse.jst.common.project.facet.ui.libprov.LibraryProviderOperationPanel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponentType;

/**
 * {@link LibraryProviderOperationPanel} displaying which GAE Runtime (SDK) is selected and be used
 * as library.
 */
public final class GaeLibraryProviderActionPanel extends LibraryProviderOperationPanel {

  @Override
  public Control createControl(Composite parent) {
    LibraryProviderOperationConfig operationConfig = getOperationConfig();
    IRuntime primaryRuntime = operationConfig.getFacetedProject().getPrimaryRuntime();
    String labelText = "Using " + primaryRuntime.getName();
    for (IRuntimeComponent component : primaryRuntime.getRuntimeComponents()) {
      IRuntimeComponentType type = component.getRuntimeComponentType();
      if (GaeRuntime.GAE_RUNTIME_ID.equals(type.getId())) {
        String location = component.getProperty("location");
        if (location != null && location.trim().length() > 0) {
          labelText += " at " + location;
        }
        break;
      }
    }
    Label label = new Label(parent, SWT.NONE);
    label.setText(labelText);
    return label;
  }
}
