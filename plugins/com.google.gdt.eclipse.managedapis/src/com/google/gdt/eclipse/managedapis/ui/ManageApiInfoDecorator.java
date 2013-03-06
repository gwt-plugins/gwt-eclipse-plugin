/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.Resources;
import com.google.gdt.eclipse.managedapis.platform.ManagedApiContainer;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import java.net.MalformedURLException;

/**
 * Display update available decoration on icons where the corresponding
 * ManagedApi has an update available.
 */
@SuppressWarnings("restriction")
public class ManageApiInfoDecorator extends BaseManagedApiDecorator
    implements ILightweightLabelDecorator {

  private Resources resources;

  public ManageApiInfoDecorator() {
    super();
    resources = ManagedApiPlugin.getDefault().getResources();
  }

  public void addListener(ILabelProviderListener listener) {
    // Nothing to do here
  }

  public void decorate(Object element, IDecoration decoration) {
    // Enables use of REPLACE as a valid image decoration
    ((DecorationContext) decoration.getDecorationContext()).putProperty(
        IDecoration.ENABLE_REPLACE, true);

    try {
      if (ManagedApiContainer.isManagedApiContainer(element)) {
        ClassPathContainer container = (ClassPathContainer) element;
        ManagedApiProject managedApiProject = ManagedApiContainer
          .getManagedApiProjectForClassPathContainer(container);
        ManagedApi managedApi = ManagedApiContainer
          .getManagedApiForClassPathContainer(container);
        if (checkAndApplyErrorDecoration(
            decoration, managedApiProject, container.getClasspathEntry())) {
          return;
        }
        if (managedApi.isUpdateAvailable()) {
          applyUpdateAvailableDecoration(decoration);
        }
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

  private void applyUpdateAvailableDecoration(IDecoration decoration)
      throws MalformedURLException {
    decoration.addSuffix(" [Update available]");
    ImageDescriptor overlay = resources
      .getUpdateAvailableOverlay16ImageDescriptor();
    decoration.addOverlay(overlay, IDecoration.BOTTOM_RIGHT);
  }

  /**
   * Checks if classpath entry will cause error. If it does, it will add error
   * icon to Managed Api container and return true. Else will return false.
   */
  private boolean checkAndApplyErrorDecoration(IDecoration decoration,
      ManagedApiProject managedApiProject, IClasspathEntry entry) {
    if (!JavaConventions.validateClasspathEntry(
        managedApiProject.getJavaProject(), entry, false).isOK()) {
      ImageDescriptor overlay = JFaceResources.getImageRegistry()
          .getDescriptor("org.eclipse.jface.fieldassist.IMG_DEC_FIELD_ERROR");
      decoration.addOverlay(overlay, IDecoration.BOTTOM_LEFT);
      return true;
    }
    return false;
  }
}
