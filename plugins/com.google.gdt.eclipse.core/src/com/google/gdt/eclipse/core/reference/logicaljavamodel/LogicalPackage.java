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
package com.google.gdt.eclipse.core.reference.logicaljavamodel;

import org.eclipse.jdt.core.IPackageFragment;

/**
 * Represents a package in the logical Java model.
 * <p>
 * This class is thread-safe due to its immutability.
 */
public final class LogicalPackage implements ILogicalJavaElement {

  private final String packageName;

  public LogicalPackage(String packageName) {
    this.packageName = packageName;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LogicalPackage)) {
      return false;
    }
    
    LogicalPackage other = (LogicalPackage) obj;    
    return packageName.equals(other.packageName);
  }

  public String getPackageName() {
    return packageName;
  }

  @Override
  public int hashCode() {
    return packageName.hashCode();
  }

  public boolean matches(Object javaElement) {
    if (!(javaElement instanceof IPackageFragment)) {
      return false;
    }

    IPackageFragment packageFragment = (IPackageFragment) javaElement;
    return packageFragment.getElementName().equals(packageName);
  }

  @Override
  public String toString() {
    return packageName;
  }

}
