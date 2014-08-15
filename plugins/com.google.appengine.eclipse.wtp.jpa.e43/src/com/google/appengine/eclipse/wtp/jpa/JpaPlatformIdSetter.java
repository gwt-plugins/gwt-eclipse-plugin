/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.jpa;

import org.eclipse.core.resources.IProject;
import org.eclipse.jpt.jpa.core.JpaPreferences;

/**
 * Provides a method, with a platform-independent interface and a platform-dependent implementation,
 * for setting the JPA platform ID for a given project. This implementation is for the Eclipse 4.3
 * (Kepler) platform and its successors.
 */
public class JpaPlatformIdSetter {
  /**
   * Sets the JPA platform ID for a specified project to a specified string.
   * 
   * @param project the specified project
   * @param platformId the specified string
   */
  public static void setJpaPlatformId(IProject project, String platformId) {
    JpaPreferences.setJpaPlatformID(project, platformId);
  }
}
