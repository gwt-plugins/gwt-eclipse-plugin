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

import com.google.gdt.eclipse.core.XmlUtilities;
import com.google.gdt.eclipse.core.contentassist.XmlContentAssistUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.contentassist.ReplacementCompletionProposal;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

import java.beans.Introspector;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A proposal computer that generates completion proposals for attributes based
 * on existing type setter methods.
 */
public class SetterAttributeProposalComputer extends
    AbstractJavaProposalComputer {

  private static final String DESCRIPTION_FORMAT = "{0}.{1}";
  private static final String SETTER_PREFIX = "set";
  private static final int SETTER_PREFIX_LENGTH = SETTER_PREFIX.length();

  /*
   * List of parsable setter/attribute parameter types. Should be kept in sync
   * with com.google.gwt.uibinder.attributeparsers.AttributeParsers.
   * 
   * TODO: look into reusing the GWT classes to avoid this duplication.
   */
  private static final Set<String> PARSABLE_ARGS = new HashSet<String>(
      Arrays.asList(new String[] {
          // supported primitive types
          "boolean",
          "int",
          "double",
          "int,int",

          // supported wrapper types
          Boolean.class.getCanonicalName(),
          Integer.class.getCanonicalName(),
          Double.class.getCanonicalName(),

          // supported simple types
          String.class.getCanonicalName(),

          // supported GWT types
          "com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant",
          "com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant",
          "com.google.gwt.user.client.ui.TextBoxBase.TextAlignConstant",
          "double,com.google.gwt.dom.client.Style.Unit"}));

  private final IDOMNode widgetNode;
  private final String widgetTypeName;

  public SetterAttributeProposalComputer(IDOMNode node, String typeName,
      IJavaProject javaProject, String enteredText, int replaceOffset,
      int replaceLength) {
    super(javaProject, enteredText, replaceOffset, replaceLength);

    this.widgetNode = node;
    this.widgetTypeName = typeName;
  }

  public void computeProposals(List<ICompletionProposal> completions) {
    Map<String, ICompletionProposal> proposals = new HashMap<String, ICompletionProposal>();

    IType type;
    try {
      // walk the inheritance chain
      for (String name = widgetTypeName; name != null; name = type.getSuperclassName()) {
        type = getJavaProject().findType(name);
        if (type == null) {
          break;
        }

        addProposalsFromType(proposals, type);
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e,
          "Could not generate setter-based attribute proposal.");
    }

    completions.addAll(proposals.values());
  }

  private void addProposal(Map<String, ICompletionProposal> proposals,
      IType type, IMethod method) {
    String methodName = method.getElementName();
    StringBuffer buf = new StringBuffer();

    // "setCamelCaseAttr" -> "camelCaseAttr"
    buf.append(Introspector.decapitalize(methodName.substring(SETTER_PREFIX_LENGTH)));
    String attributeName = buf.toString();

    buf.append("=\"\"");
    String replacement = buf.toString();

    // filter out
    // - proposals not matching the entered text
    // - proposals for already existing attributes
    // - duplicate proposals
    if (attributeName.startsWith(getEnteredText())
        && XmlUtilities.getAttribute(widgetNode, attributeName, true, null) == null
        && !proposals.containsKey(attributeName)) {

      String description = getDescription(type, method);
      ICompletionProposal completion = new ReplacementCompletionProposal(
          replacement, getReplaceOffset(), getReplaceLength(),
          getReplaceOffset() + replacement.length() - 1, description,
          attributeName, XmlContentAssistUtilities.getImageForAttribute());

      proposals.put(attributeName, completion);
    }
  }

  private void addProposalsFromType(Map<String, ICompletionProposal> proposals,
      IType type) throws JavaModelException {

    for (IMethod method : type.getMethods()) {
      if (isSetterMethod(method) && isParsableSetter(method)) {
        addProposal(proposals, type, method);
      }
    }
  }

  private String getDescription(IType type, IMethod method) {
    try {
      return MessageFormat.format(DESCRIPTION_FORMAT, type.getElementName(),
          Signature.toString(method.getSignature(), method.getElementName(),
              method.getParameterNames(), false, false));
    } catch (JavaModelException e) {
      // if the above throws, we fall-back on a safer/simpler version
      return MessageFormat.format(DESCRIPTION_FORMAT, type.getElementName(),
          method.getElementName());
    }
  }

  /**
   * Determines whether a setter's parameter signature can be parsed from an
   * attribute based on the GWT AttributeParsers implementation.
   */
  private boolean isParsableSetter(IMethod method) {
    StringBuffer signature = new StringBuffer();

    for (String paramType : method.getParameterTypes()) {
      if (signature.length() > 0) {
        signature.append(',');
      }

      String qualifier = Signature.getSignatureQualifier(paramType);
      if (qualifier.length() > 0) {
        signature.append(qualifier);
        signature.append('.');
      }

      signature.append(Signature.getSignatureSimpleName(paramType));
    }

    return PARSABLE_ARGS.contains(signature.toString());
  }

  /**
   * Determines whether the argument is a setter method based on heuristics
   * similar to GWT's OwnerFieldClass.
   */
  private boolean isSetterMethod(IMethod method) throws JavaModelException {
    // All setter methods should be public void setSomething(...)
    // (com.google.gwt.uibinder.rebind.model.OwnerFieldClass.isSetterMethod)
    String methodName = method.getElementName();

    return methodName.startsWith(SETTER_PREFIX)
        && methodName.length() > SETTER_PREFIX_LENGTH
        && Character.isUpperCase(methodName.charAt(SETTER_PREFIX_LENGTH))
        && Flags.isPublic(method.getFlags())
        && !Flags.isStatic(method.getFlags())
        && Signature.SIG_VOID.equals(method.getReturnType());
  }
}
