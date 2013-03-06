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
package com.google.gwt.eclipse.core.platformproxy.refactoring;

import com.google.gwt.eclipse.core.refactoring.GWTRefactoringSupport;
import com.google.gwt.eclipse.core.refactoring.IRefactoringChangeFactory;
import com.google.gwt.eclipse.core.refactoring.JsniReferenceChange;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.TextFileChange;

/**
 * Creates changes for compilation units containing refs in JSNI blocks.
 */
public class JsniReferenceChangeFactory implements IRefactoringChangeFactory {

  private final GWTRefactoringSupport refactoringSupport;

  public JsniReferenceChangeFactory(GWTRefactoringSupport refactoringSupport) {
    this.refactoringSupport = refactoringSupport;
  }

  /**
   * Returns an instance of the version-specific
   * <code>JsniReferenceChange</code> class.
   * 
   * @param cu The compilation unit that this change will work on.
   */
  public IJsniReferenceChange createChange(ICompilationUnit cu) {
    return new JsniReferenceChange(refactoringSupport, cu);
  }

  public TextFileChange createChange(IFile file) {
    ICompilationUnit cu = (ICompilationUnit) JavaCore.create(file);
    return (TextFileChange) createChange(cu);
  }
}
