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

import org.eclipse.jdt.core.IType;

/**
 * Abstract class for GWT-specific participants of type renaming.
 */
public abstract class GWTRenameTypeParticipant extends GWTRenameParticipant {

  @Override
  public GWTTypeRefactoringSupport createRefactoringSupport() {
    return new GWTTypeRefactoringSupport();
  }

  protected GWTTypeRefactoringSupport getTypeRefactoringSupport() {
    return (GWTTypeRefactoringSupport) getRefactoringSupport();
  }
  
  @Override
  protected boolean initialize(Object element) {
    if (!super.initialize(element)) {
      return false;
    }
  
    return (element instanceof IType);
  }
}