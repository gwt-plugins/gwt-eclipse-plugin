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
package com.google.gdt.eclipse.core.launch;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Gets the startup URL for a launch configuration invoked by a launch shortcut.
 */
public interface ILaunchShortcutStrategy {

  /**
   * Based on the selection, tries to determine the appropriate startup URL.
   * This may prompt the user if it's ambiguous. The project associated with the
   * selection must be non-null, and a Java project.
   * 
   * If launching against an external server (GWT's -noserver mode), then the
   * user will always be prompted, so that they have the option to enter the
   * external server's root.
   * 
   * @param selection
   * @param isExternalLaunch
   * @return the URL, or empty string if a URL is not applicable
   * @throws CoreException
   */
  String generateUrl(IResource selection, boolean isExternalLaunch)
      throws CoreException;

  /**
   * Prompts the user to select a host page from the project associated with
   * this resource. The project must be non-null and a Java project.
   * 
   * If launching against an external server (GWT's -noserver mode), then the
   * user will have the option enter the external server's root.
   * 
   * @param resource
   * @param isExternalLaunch
   * @return the URL of the host page
   */
  String getUrlFromUser(IResource resource, boolean isExternalLaunch);
}
