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
package com.google.gwt.eclipse.core.validators.rpc;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.List;

/**
 * Validates an interface against its paired version.
 */
interface PairedInterfaceValidator {
  /**
   * Returns a list of {@link CategorizedProblem}s that reflect discrepancies
   * between the changed interface and the {@link ITypeBinding} that represents
   * its paired interface. Note that the {@link CategorizedProblem}s can only be
   * associated with the changed interface.
   */
  List<CategorizedProblem> validate(TypeDeclaration changedInterface,
      ITypeBinding dependentTypeBinding);
}
