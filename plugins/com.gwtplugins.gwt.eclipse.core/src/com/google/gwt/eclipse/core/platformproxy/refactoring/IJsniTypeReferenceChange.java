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

import com.google.gwt.eclipse.core.refactoring.GWTTypeRefactoringSupport;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * Interface to the version-specific <code>JsniTypeReferenceChange</code>
 * class.
 */
public interface IJsniTypeReferenceChange extends IJsniReferenceChange {

  /**
   * Returns the refactoring support instance that should be used when
   * performing a refactor on a <code>JsniTypeReferenceChange</code>.
   */
  GWTTypeRefactoringSupport getRefactoringSupport();

  /**
   * Set the compilation unit that this change will work on.
   */
  void setCompilationUnit(ICompilationUnit newCu);
}
