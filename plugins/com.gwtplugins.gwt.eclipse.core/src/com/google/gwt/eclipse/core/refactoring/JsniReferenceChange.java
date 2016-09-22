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

import com.google.gwt.eclipse.core.platformproxy.refactoring.IJsniReferenceChange;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;

/**
 * Executes a change to Java references in JSNI blocks. We need to override the
 * perform() method to re-parse the compilation unit since the JDT refactorings
 * that occurred before us may have invalidated our TextEdit offsets.
 * 
 * Note that this is the Eclipse 3.5 implementation of the common
 * {@link IJsniReferenceChange} interface.
 */
public class JsniReferenceChange extends CompilationUnitChange implements
    IJsniReferenceChange {

  protected final JsniReferenceChangeHelper helper;
  protected GWTRefactoringSupport refactoringSupport;

  public JsniReferenceChange(GWTRefactoringSupport refactoringSupport,
      ICompilationUnit cu) {
    super("", cu);
    this.refactoringSupport = refactoringSupport;
    helper = new JsniReferenceChangeHelper(this);
  }

  public GWTRefactoringSupport getRefactoringSupport() {
    return refactoringSupport;
  }

  public TextFileChange getTextFileChange() {
    return this;
  }

  @Override
  public Change perform(IProgressMonitor pm) throws CoreException {
    helper.perform(pm, getCompilationUnit(), getEdit());
    return super.perform(pm);
  }
}
