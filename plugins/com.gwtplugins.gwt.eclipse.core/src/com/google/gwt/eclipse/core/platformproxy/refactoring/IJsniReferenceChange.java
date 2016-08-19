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

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * Interface to the version-specific <code>JsniReferenceChange</code> class.
 */
public interface IJsniReferenceChange {

  /**
   * Returns the compilation unit that this change works on.
   */
  ICompilationUnit getCompilationUnit();

  /**
   * Returns the refactoring support instance that should be used when
   * performing a refactor on a <code>JsniReferenceChange</code>.
   */
  GWTRefactoringSupport getRefactoringSupport();
}
