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
package com.google.gwt.eclipse.core.refactoring;

import com.google.gwt.eclipse.core.platformproxy.refactoring.IJsniTypeReferenceChange;
import com.google.gwt.eclipse.core.search.JavaRefIndex;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;

/**
 * Helper class that is used by the version-specific implementations of the
 * <code>JsniTypeReferenceChange</code> class.
 */
public class JsniTypeReferenceChangeHelper {

  private final IJsniTypeReferenceChange jsniTypeReferenceChange;

  public JsniTypeReferenceChangeHelper(
      IJsniTypeReferenceChange jsniTypeReferenceChange) {
    this.jsniTypeReferenceChange = jsniTypeReferenceChange;
  }

  public ICompilationUnit getNewCompilationUnit() {
    return jsniTypeReferenceChange.getRefactoringSupport().getNewType().getCompilationUnit();
  }

  public boolean hasCompilationUnitPathChanged(ICompilationUnit cu) {
    IType oldType = jsniTypeReferenceChange.getRefactoringSupport().getOldType();
    return (oldType.getDeclaringType() == null && oldType.getCompilationUnit().getPath().equals(
        cu.getPath()));
  }

  public void perform(IProgressMonitor pm, ICompilationUnit cu)
      throws CoreException {
    // Clear the index entries for the old compilation unit
    IPath oldCuPath = jsniTypeReferenceChange.getRefactoringSupport().getOldType().getPath();
    JavaRefIndex.getInstance().clear(oldCuPath);

    /*
     * If the compilation unit containing the references is the same one we're
     * renaming we need to update the private fields of this change so the edits
     * will be applied to the *renamed* compilation unit and not the old one.
     */
    if (hasCompilationUnitPathChanged(cu)) {
      ICompilationUnit newCu = getNewCompilationUnit();
      jsniTypeReferenceChange.setCompilationUnit(newCu);
    }
  }
}
