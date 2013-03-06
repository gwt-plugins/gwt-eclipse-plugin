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
package com.google.gdt.eclipse.managedapis.platform;

import com.google.gdt.eclipse.managedapis.ManagedApi;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;

/**
 * UpdateAvailablePropertyTester provides a test to determine whether or not to
 * display context menu for ManagedApis on a project in the PackageExplorer. The
 * tester applies to a single selection and provides the property
 * "updateAvailable".
 */
@SuppressWarnings("restriction")
public class UpdateAvailablePropertyTester extends PropertyTester {

  /**
   * The test method.
   */
  public boolean test(Object receiver, String property, Object[] args,
      Object expectedValue) {
    if (receiver instanceof ClassPathContainer) {
      ClassPathContainer container = (ClassPathContainer) receiver;
      if ("updateAvailable".equals(property)) {
        if (ManagedApiContainer.isManagedApiContainer(container)) {
          ManagedApi managedApi = ManagedApiContainer.getManagedApiForClassPathContainer(container);
          // the managedApi can be null if the API is being removed, so check it
          if (managedApi != null && managedApi.isUpdateAvailable()) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
