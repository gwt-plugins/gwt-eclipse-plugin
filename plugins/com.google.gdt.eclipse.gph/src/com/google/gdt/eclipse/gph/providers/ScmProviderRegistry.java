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

import static com.google.gdt.eclipse.gph.ProjectHostingUIPlugin.PLUGIN_ID;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads {@link ScmProvider}s contributed via the Platform extension registry.
 */
public class ScmProviderRegistry {

  /**
   * Extension Point identifiers.
   */
  static class ExtensionId {
    /**
     * The identifier for checkoutProvider extension point class attribute
     * (value <code>"class"</code>).
     */
    public static final String CHECKOUT_PROVIDER_TYPE = "checkoutProviderClass"; //$NON-NLS-1$

    /**
     * The identifier for checkoutProvider extension point scm attribute (value
     * <code>"scmType"</code>).
     */
    public static final String SCM_PRIORITY = "priority"; //$NON-NLS-1$

    /**
     * The identifier for checkoutProvider extension point contributions (value
     * <code>"checkoutProvider"</code>).
     */
    public static final String SCM_PROVIDER_ID = "scmProvider"; //$NON-NLS-1$

    /**
     * The identifier for checkoutProvider extension point scm attribute (value
     * <code>"providerName"</code>).
     */
    public static final String SCM_PROVIDER_NAME = "providerName"; //$NON-NLS-1$

    /**
     * The identifier for checkoutProvider extension point scm attribute (value
     * <code>"providerIcon"</code>).
     */
    public static final String SCM_PROVIDER_IMAGE = "providerIcon"; //$NON-NLS-1$
    
    /**
     * The identifier for checkoutProvider extension point scm attribute (value
     * <code>"providerDescription"</code>).
     */
    public static final String SCM_PROVIDER_DESCRIPTION = "providerDescription"; //$NON-NLS-1$
    
    /**
     * The identifier for checkoutProviders extension point contributions (value
     * <code>"com.google.gdt.eclipse.gph.checkoutProviders"</code>).
     */
    public static final String SCM_PROVIDERS_ID = PLUGIN_ID
        + ".checkoutProviders"; //$NON-NLS-1$

    /**
     * The identifier for checkoutProvider extension point scm attribute (value
     * <code>"scmType"</code>).
     */
    public static final String SCM_TYPE = "scmType"; //$NON-NLS-1$

    /**
     * The identifier for checkoutProvider extension point scm attribute (value
     * <code>"scmType"</code>).
     */
    public static final String SCM_TYPE_LABEL = "scmTypeLabel"; //$NON-NLS-1$
  }

  private static final ScmProviderRegistry registry = new ScmProviderRegistry();

  public static ScmProviderRegistry getRegistry() {
    return registry;
  }

  private ScmProviderRegistry() {
  }

  /**
   * Get all {@link ScmProvider}s registered for this type.
   * 
   * @param type the SCM type (legal values include <code>"hg"</code> and
   *          <code>"svn"</code>).
   * 
   * @return a list of SCM providers
   */
  public List<ScmProvider> getScmProviders(String type) {
    if (type == null) {
      throw new IllegalArgumentException("type should not be null"); //$NON-NLS-1$
    }

    List<ScmProvider> providers = new ArrayList<ScmProvider>();

    for (ScmProvider provider : getScmProviders()) {
      if (provider.getScmType().equalsIgnoreCase(type)) {
        providers.add(provider);
      }
    }

    return providers;
  }

  /**
   * Get all {@link ScmProvider}s registered.
   * 
   * @return a list of SCM providers
   */
  private List<ScmProvider> getScmProviders() {
    // note: to accommodate dynamically loaded providers, this lookup is not
    // cached
    IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
        ExtensionId.SCM_PROVIDERS_ID);

    List<ScmProvider> providers = new ArrayList<ScmProvider>();

    for (IConfigurationElement element : elements) {
      if (element.getName().equals(ExtensionId.SCM_PROVIDER_ID)) {
        providers.add(new ScmProvider(element));
      }
    }

    Collections.sort(providers);

    return providers;
  }

}
