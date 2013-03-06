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
package com.google.gdt.eclipse.gph.providers;

import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;
import com.google.gdt.eclipse.gph.install.P2InstallationUnit;
import com.google.gdt.eclipse.gph.providers.ScmProviderRegistry.ExtensionId;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * A SCM provider provides SCM related information and functionality to GPH.
 */
public final class ScmProvider implements Comparable<ScmProvider> {

  private IConfigurationElement element;
  private int priority;
  private String providerName;
  private String providerDescription;
  private String scmType;
  private ImageDescriptor providerImageDescriptor;
  
  private String scmTypeLabel;
  
  private ICheckoutProvider checkoutProvider;
  
  ScmProvider(IConfigurationElement element) {
    this.element = element;
    
    scmType = element.getAttribute(ScmProviderRegistry.ExtensionId.SCM_TYPE);
    scmTypeLabel = element.getAttribute(ScmProviderRegistry.ExtensionId.SCM_TYPE_LABEL);
    providerName = element.getAttribute(ScmProviderRegistry.ExtensionId.SCM_PROVIDER_NAME);
    providerDescription = element.getAttribute(ScmProviderRegistry.ExtensionId.SCM_PROVIDER_DESCRIPTION);
    
    String imagePath = element.getAttribute(ScmProviderRegistry.ExtensionId.SCM_PROVIDER_IMAGE);
    
    if (imagePath != null) {
      providerImageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
          element.getContributor().getName(), imagePath);
    }
    
    try {
      priority = Integer.parseInt(element.getAttribute(ScmProviderRegistry.ExtensionId.SCM_PRIORITY));
    } catch (NumberFormatException nfe) {
      priority = Short.MAX_VALUE;
    }
  }
  
  public int compareTo(ScmProvider provider) {
    int strCompare = getScmType().compareToIgnoreCase(provider.getScmType());
    
    if (strCompare != 0) {
      return strCompare;
    }
    
    boolean installed = isFullyInstalled();
    boolean otherInstalled = provider.isFullyInstalled();
    
    if (installed && !otherInstalled) {
      return -1;
    } else if (!installed && otherInstalled) {
      return 1;
    }
    
    return getPriority() - provider.getPriority();
  }
  
  /**
   * Returns the checkout provider for this SCMProvider. This is used in the 
   * project import wizard.
   * 
   * @return the checkout provider for this SCMProvider
   * @throws CoreException
   */
  public ICheckoutProvider getCheckoutProvider() {
    if (checkoutProvider == null) {
      try {
        checkoutProvider = (ICheckoutProvider)
          element.createExecutableExtension(ExtensionId.CHECKOUT_PROVIDER_TYPE);
      } catch (Throwable t) {
        // indicates that the provider is not installed
        
        ProjectHostingUIPlugin.logError(t);
      }
    }

    return checkoutProvider;    
  }
  
  public P2InstallationUnit getInstallInfo() {
    if (getCheckoutProvider() != null) {
      return getCheckoutProvider().getP2InstallationUnit();
    } else {
      return null;
    }
  }
  
  /**
   * Returns the SCM provider priority. This is used to decide between two providers
   * for the same SCM type. Lower priority wins.
   * 
   * @return the SCM provider priority
   */
  public int getPriority() {
    return priority;
  }
  
  /**
   * @return the SCM provider description
   */
  public String getProviderDescription() {
    return providerDescription;
  }
  
  /**
   * @return the SCM provider description
   */
  public ImageDescriptor getProviderImageDescriptor() {
    return providerImageDescriptor;
  }

  /**
   * @return the SCM provider. for example "Subclipse"
   */
  public String getProviderName() {
    return providerName;
  }

  /**
   * @return the SCM type, for example "svn"
   */
  public String getScmType() {
    return scmType;
  }

  /**
   * @return the SCM label, for example "Subversion"
   */
  public String getScmTypeLabel() {
    return scmTypeLabel;
  }

  /**
   * @return whether the provider referred to by this SCMProvider is installed
   */
  public boolean isFullyInstalled() {
    if (getCheckoutProvider() != null) {
      return getCheckoutProvider().isFullyInstalled();
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return getScmType() + ";" + getProviderName();
  }
  
}
