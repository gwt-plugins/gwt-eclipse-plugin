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
package com.google.gdt.eclipse.core.reference;

import com.google.gdt.eclipse.core.reference.location.IReferenceLocation;

import org.eclipse.core.resources.IProject;

import java.text.MessageFormat;

/**
 * A simple {@link IReference} implementation.
 * <p>
 * This class is thread-safe due to its immutability.
 */
public final class Reference implements IReference {

  private final IReferenceLocation sourceLocation;

  private final IReferenceLocation targetLocation;

  private final IProject sourceProject;

  public Reference(IReferenceLocation sourceLocation,
      IReferenceLocation targetLocation, IProject sourceProject) {
    this.sourceLocation = sourceLocation;
    this.targetLocation = targetLocation;
    this.sourceProject = sourceProject;

    sourceLocation.setReference(this);
    targetLocation.setReference(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Reference)) {
      return false;
    }

    Reference other = (Reference) obj;
    return sourceLocation.equals(other.sourceLocation)
        && targetLocation.equals(other.targetLocation)
        && sourceProject.equals(other.sourceProject);
  }

  public IReferenceLocation getSourceLocation() {
    return sourceLocation;
  }

  public IProject getSourceProject() {
    return sourceProject;
  }

  public IReferenceLocation getTargetLocation() {
    return targetLocation;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = result * 37 + sourceLocation.hashCode();
    result = result * 37 + targetLocation.hashCode();
    result = result * 37 + sourceProject.hashCode();

    return result;
  }

  @Override
  public String toString() {
    return MessageFormat.format(
        "Project({2}): Reference({0}) -> Reference({1})",
        sourceLocation.toString(), targetLocation.toString(),
        sourceProject.getName());
  }

}
