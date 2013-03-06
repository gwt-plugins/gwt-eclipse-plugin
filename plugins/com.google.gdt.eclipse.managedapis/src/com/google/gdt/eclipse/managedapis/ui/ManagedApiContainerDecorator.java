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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.Resources;
import com.google.gdt.eclipse.managedapis.platform.ManagedApiContainer;

import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import java.net.MalformedURLException;

/**
 * Customizes icons for ManagedApi classpath containers.
 */
@SuppressWarnings("restriction")
public class ManagedApiContainerDecorator extends BaseManagedApiDecorator
    implements ILightweightLabelDecorator {

  private Resources resources;

  public ManagedApiContainerDecorator() {
    super();
    resources = ManagedApiPlugin.getDefault().getResources();
  }

  public void addListener(ILabelProviderListener listener) {
    // Nothing to do here
  }

  /**
   * Conditionally decorate the element by replacing the element's icon
   */
  public void decorate(Object element, IDecoration decoration) {
    // Enables use of REPLACE as a valid image decoration
    ((DecorationContext) decoration.getDecorationContext()).putProperty(
        IDecoration.ENABLE_REPLACE, true);

    try {
      if (ManagedApiContainer.isManagedApiContainer(element)) {
        applyApiSpecificContainerIcon(element, decoration);
      }
    } catch (MalformedURLException e) {
      ManagedApiLogger.warn(e, "Unexpected MalformedURLException");
    }
  }

  public void dispose() {
    // Nothing to do here
  }

  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  public void removeListener(ILabelProviderListener listener) {
    // Nothing to do here
  }

  private void applyApiSpecificContainerIcon(Object element,
      IDecoration decoration) throws MalformedURLException {
    ImageDescriptor replacementIcon = null;

    ManagedApi managedApi = ManagedApiContainer.getManagedApiForClassPathContainer((ClassPathContainer) element);
    if (managedApi != null && managedApi.hasClasspathContainerIcon()) {
      replacementIcon = managedApi.getClasspathContainerIconImageDescriptor();
    }
    if (replacementIcon == null) {
      replacementIcon = resources.getGoogleAPIContainerIcon16ImageDescriptor();
    }
    decoration.addOverlay(replacementIcon, IDecoration.REPLACE);
  }
}
