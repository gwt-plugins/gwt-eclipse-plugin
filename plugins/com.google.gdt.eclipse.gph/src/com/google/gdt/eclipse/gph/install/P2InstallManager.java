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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import java.util.List;

/**
 * The P2InstallManager interface provides a facade over P2 and breaks the hard
 * dependencies on the P2 plugins and classes.
 */
public interface P2InstallManager {

  /**
   * Determine whether these features are installed locally and return that
   * info.
   * 
   * @return copies of all the given features with their installation status
   *         filled in
   */
  List<P2InstallationFeature> resolveInstalledStatus();

  /**
   * Parse the P2 update site info for the given installation units, resolve
   * dependencies, and return back all features that will be installed and
   * license agreements that the user needs to agree to. Invoke this method
   * after {@link #setInstallationUnits(List)} and before
   * {@link #runP2Install()}.
   * 
   * @param monitor the (non-null) progress monitor
   * @return features and license agreements for the install
   * @throws CoreException
   */
  IStatus resolveP2Information(IProgressMonitor monitor);

  /**
   * The last call of the {@link P2InstallManager} interface. This does the
   * actual work of installing the features. This should only be called if
   * {@link #resolveP2Information(IProgressMonitor)} returns without errors.
   */
  void runP2Install();

  /**
   * Set the installation units that will be installed. This call should be made
   * once right after the creation of the P2InstallManager instance.
   * {@link P2InstallManagerFactory#createInstallManager(List)} calls this
   * method automatically.
   * 
   * @param installationUnits the installation units to install
   */
  void setInstallationUnits(List<P2InstallationUnit> installationUnits);

}
