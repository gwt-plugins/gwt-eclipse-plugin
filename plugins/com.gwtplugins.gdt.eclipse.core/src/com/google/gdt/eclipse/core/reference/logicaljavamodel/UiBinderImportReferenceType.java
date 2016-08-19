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


import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;

/**
 * When a ui.xml file has a dependency on a class through a <ui:import> tag,
 * the ElementChangeListener gets passed instances of CompilationUnit instead
 * of IType, as LogicalType expects. This class extends the behavior of the
 * matches() method to include matching to instances of CompilationUnit.
 */
@SuppressWarnings("restriction")
public class UiBinderImportReferenceType extends LogicalType {

  public UiBinderImportReferenceType(IType type) {
    super(type);
  }

  public UiBinderImportReferenceType(String fullyQualifiedTypeName) {
    super(fullyQualifiedTypeName);
  }
  
  @Override
  public boolean matches(Object javaElement) {
    
    if (javaElement instanceof IType) {      
      
      return super.matches(javaElement);
      
    } else if (javaElement instanceof CompilationUnit) {
      // When a java file that is changed is referenced from a ui.xml file,
      // eclipse gives a CompilationUnit instead of an IType, so this
      // logic is needed for this case.

      String enclosingClassName = getFullyQualifiedName();
      
      // if this LogicalType represents an inner class, we want only the outermost
      // enclosing class, because the given CompilationUnit represents a 
      // java file, and hence an outermost class
      int dollarIndex = enclosingClassName.indexOf('$');
      if (dollarIndex != -1) {
        enclosingClassName = enclosingClassName.substring(0, dollarIndex); 
      }
      
      CompilationUnit cu = ((CompilationUnit) javaElement);
      
      // CompilationUnit class doesn't just give us the full qualified name
      // of the top-most class... so we must assemble it ourselves...
      IPackageDeclaration[] pkgs;
      try {
        pkgs = cu.getPackageDeclarations();
      } catch (JavaModelException e) {
        return false;
      }
      
      if (pkgs.length > 0) {
        // cu.getElementName() returns a filename, so lop off the extension to get the class name
        String className = cu.getElementName().substring(0, cu.getElementName().indexOf('.'));
        String cuName = pkgs[0].getElementName() + "." + className;
        if (enclosingClassName.equals(cuName)) {
          return true;
        }
      }
    }

    return false;
  }
  
}
