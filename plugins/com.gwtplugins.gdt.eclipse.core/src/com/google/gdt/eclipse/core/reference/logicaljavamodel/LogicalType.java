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

import com.google.gdt.eclipse.core.JavaUtilities;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Represents a type in the logical Java model.
 * <p>
 * Inner classes should be separated with '$'.
 * <p>
 * This class is thread-safe due to its immutability.
 */
public class LogicalType implements ILogicalJavaElement {

  protected final LogicalPackage logicalPackage;

  protected final String typeName;

  public LogicalType(IType type) {
    logicalPackage = new LogicalPackage(
        type.getPackageFragment().getElementName());
    typeName = type.getTypeQualifiedName('$');
  }

  public LogicalType(String fullyQualifiedTypeName) {
    this(JavaUtilities.getPackageName(fullyQualifiedTypeName),
        JavaUtilities.getTypeName(fullyQualifiedTypeName));
  }

  private LogicalType(String packageName, String typeName) {
    logicalPackage = new LogicalPackage(packageName);
    this.typeName = typeName;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LogicalType)) {
      return false;
    }
    
    LogicalType other = (LogicalType) obj;
    return logicalPackage.equals(other.logicalPackage)
        && typeName.equals(other.typeName);
  }

  public String getFullyQualifiedName() {
    return JavaUtilities.getQualifiedTypeName(typeName,
        logicalPackage.getPackageName());
  }

  public LogicalPackage getLogicalPackage() {
    return logicalPackage;
  }

  public IType getType(IJavaProject javaProject) throws JavaModelException {
    return javaProject.findType(getFullyQualifiedName().replace('$', '.'));
  }

  public String getTypeName() {
    return typeName;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = result * 37 + logicalPackage.hashCode();
    result = result * 37 + typeName.hashCode();

    return result;
  }

  public boolean matches(Object javaElement) {
    if (!(javaElement instanceof IType)) {
      return false;
    }

    IType type = (IType) javaElement;
    return getFullyQualifiedName().equals(type.getFullyQualifiedName('$'));
  }

  @Override
  public String toString() {
    return getFullyQualifiedName();
  }

}
