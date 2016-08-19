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
package com.google.gwt.eclipse.core.uibinder.contentassist.computers;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.contentassist.XmlContentAssistUtilities;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities;
import com.google.gwt.eclipse.core.uibinder.contentassist.ReplacementCompletionProposal;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import java.util.List;
import java.util.Set;

/**
 * A proposal computer that generates completion proposals for ui:field
 * attribute values.
 */
public class UiFieldProposalComputer extends AbstractJavaProposalComputer {

  private final Set<IType> uiBinderTypes;
  private final String widgetTypeName;

  UiFieldProposalComputer(Set<IType> ownerTypes, String widgetTypeName,
      IJavaProject javaProject, String enteredText, int replaceOffset,
      int replaceLength) {
    super(javaProject, enteredText, replaceOffset, replaceLength);

    this.uiBinderTypes = ownerTypes;
    this.widgetTypeName = widgetTypeName;
  }

  public void computeProposals(List<ICompletionProposal> proposals)
      throws UiBinderException {

    // Scan all the associated UiBinder interfaces
    for (IType uiBinderType : uiBinderTypes) {
      IType ownerType = UiBinderUtilities.getOwnerType(uiBinderType);

      TypeDeclaration ownerDecl = JavaASTUtils.findTypeDeclaration(
          getJavaProject(), ownerType.getFullyQualifiedName('.'));
      if (ownerDecl != null) {
        addOwnerProposals(proposals, ownerDecl);
      }
    }
  }

  private void addFieldProposal(List<ICompletionProposal> proposals,
      FieldDeclaration fieldDecl) {

    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> fragments = fieldDecl.fragments();

    for (VariableDeclarationFragment fragment : fragments) {
      String fieldName = fragment.getName().getIdentifier();

      if (fieldName.startsWith(getEnteredText())) {
        proposals.add(new ReplacementCompletionProposal(fieldName,
            getReplaceOffset(), getReplaceLength(),
            ReplacementCompletionProposal.DEFAULT_CURSOR_POSITION, null,
            fieldName, XmlContentAssistUtilities.getImageForLocalVariable()));
      }
    }
  }

  private void addOwnerProposals(List<ICompletionProposal> proposals,
      TypeDeclaration ownerDecl) {

    // Generate proposals for "ui:field" annotated field declarations
    for (FieldDeclaration fieldDecl : ownerDecl.getFields()) {
      if (UiBinderUtilities.isUiField(fieldDecl)) {

        // If the widget qualified type name is known and the binding can be
        // resolved, validate them.
        ITypeBinding binding = fieldDecl.getType().resolveBinding();
        if (widgetTypeName != null && binding != null
            && !widgetTypeName.equals(binding.getQualifiedName())) {

          continue;
        }

        addFieldProposal(proposals, fieldDecl);
      }
    }
  }
}
