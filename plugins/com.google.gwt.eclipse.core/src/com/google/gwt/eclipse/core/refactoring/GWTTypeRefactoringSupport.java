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

import org.eclipse.jdt.core.IType;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Common code used by our type rename and move participants.
 */
public class GWTTypeRefactoringSupport extends GWTRefactoringSupport {

  public IType getNewType() {
    return (IType) getNewElement();
  }

  public IType getOldType() {
    return (IType) getOldElement();
  }

  @Override
  protected TextEdit createEdit(IIndexedJavaRef ref) {
    /*
     * Use the length of the fully-qualified name of the original type here
     * instead of ref.className().length(), so we can update just the part of
     * the ref instead of the whole thing (this is important for renaming types
     * with inner types that are referenced by the IIndexedJavaRef).
     */
    int length = getOldType().getFullyQualifiedName().length();

    // Use the original type separator for the replacement string
    char typeSeparator = (ref.rawClassName().indexOf('$') > 1 ? '$' : '.');

    String replacement = getNewType().getFullyQualifiedName(typeSeparator);

    // Use original package segment separator for replacement string
    if (ref.rawClassName().indexOf('/') > -1) {
      replacement = replacement.replace('.', '/');
    }

    return new ReplaceEdit(ref.getClassOffset(), length, replacement);
  }

  @Override
  protected String getEditDescription() {
    return "Update type reference";
  }

}
