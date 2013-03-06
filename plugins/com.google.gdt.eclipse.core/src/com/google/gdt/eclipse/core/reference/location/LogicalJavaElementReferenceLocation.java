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
package com.google.gdt.eclipse.core.reference.location;

import com.google.gdt.eclipse.core.reference.logicaljavamodel.ILogicalJavaElement;

/**
 * Tracks a reference to/from a logical Java element.
 * <p>
 * This class is thread-safe due to its immutability.
 */
public final class LogicalJavaElementReferenceLocation extends
    AbstractReferenceLocation implements IMatchable {

  private final ILogicalJavaElement logicalJavaElement;

  public LogicalJavaElementReferenceLocation(
      ILogicalJavaElement logicalJavaElement) {
    this.logicalJavaElement = logicalJavaElement;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LogicalJavaElementReferenceLocation)) {
      return false;
    }
    
    LogicalJavaElementReferenceLocation other = (LogicalJavaElementReferenceLocation) obj;
    return logicalJavaElement.equals(other.logicalJavaElement);
  }

  public ILogicalJavaElement getLogicalJavaElement() {
    return logicalJavaElement;
  }

  @Override
  public int hashCode() {
    return logicalJavaElement.hashCode();
  }

  public boolean matches(Object javaElement) {
    return logicalJavaElement.matches(javaElement);
  }

  @Override
  public String toString() {
    return logicalJavaElement.toString();
  }

}
