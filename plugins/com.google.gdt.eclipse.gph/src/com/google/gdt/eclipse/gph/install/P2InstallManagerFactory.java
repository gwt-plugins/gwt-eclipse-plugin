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
package com.google.gdt.eclipse.gph.install;

import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;

import static com.google.gdt.eclipse.gph.ProjectHostingUIPlugin.PLUGIN_ID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import java.util.List;

/**
 * A factory to return new instances of {@link P2InstallManager}. If a
 * sufficient version of P2 is not available, the
 * {@link #createInstallManager()} method will return null. Callers will have to
 * check for this result and respond accordingly.
 */
public class P2InstallManagerFactory {

  private static final String INSTALL_MANAGER_EXT_ID = PLUGIN_ID
      + ".installManagerProvider"; //$NON-NLS-1$

  private static final String INSTALL_MANAGER_ID = "installManager"; //$NON-NLS-1$

  /**
   * Create a new instance of a P2InstallManager. This can return null if a
   * recent enough version of P2 is not available.
   * 
   * @return a new instance of a P2InstallManager implementation, or null if no
   *         such is available
   */
  public static P2InstallManager createInstallManager() {
    IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
        INSTALL_MANAGER_EXT_ID);

    for (IConfigurationElement element : elements) {
      if (element.getName().equals(INSTALL_MANAGER_ID)) {
        try {
          P2InstallManager installManager = (P2InstallManager) element.createExecutableExtension("class");

          return installManager;
        } catch (CoreException ce) {
          ProjectHostingUIPlugin.logError(ce);
        }
      }
    }

    return null;
  }

  /**
   * Create a new instance of a P2InstallManager. This can return null if a
   * recent enough version of P2 is not available.
   * 
   * @param installationUnits the installation units we're going to install
   * @return a new instance of a P2InstallManager implementation, or null if no
   *         such is available
   */
  public static P2InstallManager createInstallManager(
      List<P2InstallationUnit> installationUnits) {
    P2InstallManager installManager = createInstallManager();

    if (installManager != null) {
      installManager.setInstallationUnits(installationUnits);
    }

    return installManager;
  }

  /**
   * Returns whether the install manager is available. This is true for Eclipse
   * 3.6 and above.
   * 
   * @return whether the install manager is available
   */
  public static boolean isInstallManagerAvailable() {
    return createInstallManager() != null;
  }

  private P2InstallManagerFactory() {
  }

}
