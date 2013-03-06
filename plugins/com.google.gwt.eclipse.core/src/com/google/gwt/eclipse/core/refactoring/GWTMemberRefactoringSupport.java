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

import com.google.gwt.eclipse.core.search.IIndexedJavaRef;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Common code used by our method and field rename participants.
 */
class GWTMemberRefactoringSupport extends GWTRefactoringSupport {

  @Override
  protected TextEdit createEdit(IIndexedJavaRef ref) {
    return new ReplaceEdit(ref.getMemberOffset(), ref.memberName().length(),
        getNewElement().getElementName());
  }

  @Override
  protected String getEditDescription() {
    IJavaElement oldElement = getOldElement();
    if (oldElement instanceof IMethod) {
      return "Update method reference";
    } else if (oldElement instanceof IField) {
      return "Update field reference";
    }

    assert (false);
    return "Update member reference";
  }

}
