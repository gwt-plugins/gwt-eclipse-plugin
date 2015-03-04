/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.cloudsdk.eclipse.wtp.deploy;

import com.google.cloudsdk.eclipse.wtp.CloudSdkUtils;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * CloudSdkProjectPropertyTester provides a test to determine whether or not to enable the
 * "Cloud SDK Deploy Project" context menu item for an Eclipse project.
 */
public class CloudSdkProjectPropertyTester extends PropertyTester {
  /**
   * Returns true if the selected project has the Cloud SDK facet.
   */
  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if (receiver instanceof IProject) {
      try {
        return CloudSdkUtils.hasCloudSdkFacet((IProject) receiver);
      } catch (CoreException e) {
        return false;
      }
    }
    return false;
  }
}
