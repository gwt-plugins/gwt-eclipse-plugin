/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.editors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.api.client.util.Sets;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.AttributeDocumentation;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.BeanDocumentation;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationService.ClassDocumentation;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
/**
 * Unit test for {@link WebEditorCompletionProcessor}.
 */
@RunWith(JUnit4.class)
public class WebEditorCompletionProcessorTest {
  
  private static final Collection<AttributeDocumentation> NO_ATTRIBUTES = ImmutableList.of();
  private static final Set<CompletionProposalInfo> NO_PROPOSALS = ImmutableSet.of();
  
  // Currently, if content assist is invoked with the cursor in a place where there are no
  // contextual clues, we return no proposals. Alternatives would be to propose all keywords, or all
  // keywords and class names. If such a change were to be made, updating this constant would bring
  // the relevant tests up to date.
  private static final Set<CompletionProposalInfo> EXPECTED_PROPOSALS_WITH_NO_CONTEXT =
      NO_PROPOSALS;
  
  private static final ClassDocumentation CLASS_WITH_ATTRIBUTES =
      new ClassDocumentation(
          "MyClass",
          "Description of MyClass",
          ImmutableList.of(
              new AttributeDocumentation("a", "aType", "Description of a", "a(p1)"),
              new AttributeDocumentation("ab", "abType", "Description of ab", "ab"),
              new AttributeDocumentation("abc", "abcType", "Description of abc", "abc(p1, p2)"),
              new AttributeDocumentation(
                  "abcd", "abcdType", "Description of abcd", "abcd(p1, p2)"),
              new AttributeDocumentation("e", "eType", "", "e"),
              new AttributeDocumentation(
                  "m0parms", "m0pType", "Description of m0parms", "m0parms()"),
              new AttributeDocumentation(
                  "m1parm", "m1pType", "Description of m1parm", "m1parm(p1)"),
              new AttributeDocumentation(
                  "m2parms", "m2pType", "Description of m2parms", "m2parms(p1, p2)")));
  
  private static final BeanDocumentation BEAN_WITH_TOP_LEVEL_CLASS_WITH_ATTRIBUTES =
      new BeanDocumentation(CLASS_WITH_ATTRIBUTES, ImmutableList.<ClassDocumentation>of());
  
  private static final BeanDocumentation BEAN_WITH_CHILD_TYPE =
      new BeanDocumentation(
          new ClassDocumentation(
              "XyzParentType",
              "Description of XyzParentType",
              ImmutableList.of(
                  new AttributeDocumentation(
                      "create", "XyzChildType", "Description of create", "create()"))),
          ImmutableList.of(
              new ClassDocumentation(
                  "XyzChildType",
                  "Description of XyzChildType",
                  ImmutableList.of(
                      new AttributeDocumentation(
                          "process", "void", "Description of process", "process()")))));
  
  private static final Collection<BeanDocumentation> API_DATA =
      ImmutableList.of(
          beanWithoutChildTypes("A"),
          beanWithoutChildTypes("Ab"),
          beanWithoutChildTypes("Abc"),
          beanWithoutChildTypes("Abcd"),
          beanWithoutChildTypes("Abcef"),
          BEAN_WITH_TOP_LEVEL_CLASS_WITH_ATTRIBUTES,
          BEAN_WITH_CHILD_TYPE);
  
  private static final Map<String, String> METHOD_NAMES_TO_REPLACEMENT_TEMPLATES =
      ImmutableMap.<String, String>builder()
          .put("a", "a(${p1})")
          .put("abc", "abc(${p1}, ${p2})")
          .put("abcd", "abcd(${p1}, ${p2})")
          .put("abcde", "abcde(${p1}, ${p2})")
          .put("m0parms", "m0parms()")
          .put("m1parm", "m1parm(${p1})")
          .put("m2parms", "m2parms(${p1}, ${p2})")
          .put("create", "create()")
          .put("process", "process()")
          .build();
  
  private static final Map<String, CompletionProposalInfo> CLASS_NAMES_TO_EXPECTED_COMPLETIONS =
      mapClassNamesToProposals(API_DATA);

  private static final Map<String, CompletionProposalInfo> ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS =
      mapAttributeNamesToProposals(API_DATA);
 
  private static final Map<String, CompletionProposalInfo>
      MY_CLASS_ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS =
          mapTypeAttributesToProposals(CLASS_WITH_ATTRIBUTES);

  
  @Mock private TextViewer mockTextViewer;

  @Test
  public void testComputeCompletionProposals_emptyDocument() {
    testComputeCompletionProposals("^", EXPECTED_PROPOSALS_WITH_NO_CONTEXT);
  }

  @Test
  public void testComputeCompletionProposals_cursorAtStartOfDocument() {
    testComputeCompletionProposals("^blah blah", EXPECTED_PROPOSALS_WITH_NO_CONTEXT);
  }

  @Test
  public void testComputeCompletionProposals_cursorAtClassNameMatchAfterStartOfDocument() {
    testComputeCompletionProposals(
        "Abc^ blah",
        ImmutableSet.of(
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abc"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcd"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcef")));
  }

  @Test
  public void testComputeCompletionProposals_cursorAfterNoncompletableWord() {
    testComputeCompletionProposals("blah^ blah", NO_PROPOSALS);
  }

  @Test
  public void testComputeCompletionProposals_cursorAfterNonNameCharacter() {
    for (char whitespaceChar : " \t\n\f\r\u000B-+=".toCharArray()) {
      String editorBuffer = "blah" + whitespaceChar + "^blah";
      testComputeCompletionProposals(editorBuffer, EXPECTED_PROPOSALS_WITH_NO_CONTEXT);
    }
  }

  @Test
  public void testComputeCompletionProposals_progressiveNarrowingOfClassNameProposals() {
    testComputeCompletionProposals(
        "blah A^ blah",
        ImmutableSet.of(
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("A"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Ab"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abc"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcd"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcef")));
    testComputeCompletionProposals(
        "blah Ab^ blah",
        ImmutableSet.of(
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Ab"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abc"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcd"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcef")));
    testComputeCompletionProposals(
        "blah Abc^ blah",
        ImmutableSet.of(
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abc"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcd"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcef")));
    testComputeCompletionProposals(
        "blah Abcd^ blah", ImmutableSet.of(CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcd")));
    testComputeCompletionProposals("blah Abcde^ blah",  NO_PROPOSALS);
  }

  @Test
  public void testComputeCompletionProposals_cursorAfterDotAtStartOfDocument() {
    testComputeCompletionProposals(".^ blah", NO_PROPOSALS);
  }  

  @Test
  public void testComputeCompletionProposals_cursorAfterWhitespaceDotAtStartOfDocument() {
    testComputeCompletionProposals("\n.^ blah", NO_PROPOSALS);
  }  

  @Test
  public void testComputeCompletionProposals_cursorAfterWhitespaceDotWhitespaceAtStartOfDocument() {
    testComputeCompletionProposals("\n. ^ blah", NO_PROPOSALS);
  }  

  @Test
  public void testComputeCompletionProposals_cursorAfterRandomDot() {
    testComputeCompletionProposals("blah SomeOtherClass.^ blah", NO_PROPOSALS);
  }  

  @Test
  public void testComputeCompletionProposals_progressiveNarrowingOfAttributeNameProposals() {
    testComputeCompletionProposals(
        "blah MyClass.^ blah",
        allProposalsInMaps(MY_CLASS_ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS));
    testComputeCompletionProposals(
        "blah MyClass.a^ blah",
        ImmutableSet.of(
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("a"),
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("ab"),
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abc"),
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abcd")));
    testComputeCompletionProposals(
        "blah MyClass.ab^ blah",
        ImmutableSet.of(
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("ab"),
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abc"),
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abcd")));
    testComputeCompletionProposals(
        "blah MyClass.abc^ blah",
        ImmutableSet.of(
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abc"),
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abcd")));
    testComputeCompletionProposals(
        "blah MyClass.abcd^ blah",
        ImmutableSet.of(ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abcd")));
    testComputeCompletionProposals("blah MyClass.abcde^ blah", NO_PROPOSALS);
  }

  @Test
  public void testComputeCompletionProposals_whitespaceAroundDot() {
    testComputeCompletionProposals(
        "blah MyClass.\n   abc^ blah",
        ImmutableSet.of(
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abc"),
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abcd")));
    testComputeCompletionProposals(
        "blah MyClass\n   .abc^ blah",
        ImmutableSet.of(
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abc"),
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abcd")));
    testComputeCompletionProposals(
        "blah MyClass .\n   abc^ blah",
        ImmutableSet.of(
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abc"),
            ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("abcd")));
  }

  @Test
  public void testComputeCompletionProposals_cursorAtEndOfDocument() {
    testComputeCompletionProposals(
        "blah Abc^",
        ImmutableSet.of(
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abc"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcd"),
            CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("Abcef")));
  }
  
  @Test
  public void testComputeCompletionProposals_childTypeNotSuggestedOutOfContext() {
    testComputeCompletionProposals(
        "Xyz^", ImmutableSet.of(CLASS_NAMES_TO_EXPECTED_COMPLETIONS.get("XyzParentType")));
  }
  
  @Test
  public void testComputeCompletionProposals_childTypeMethodsSuggestedInContext() {
    testComputeCompletionProposals(
        "XyzParentType.create().^",
        ImmutableSet.of(ATTRIBUTE_NAMES_TO_EXPECTED_COMPLETIONS.get("process")));
  }
  
  @Test
  public void testComputeCompletionProposals_formOfIdentifierProposal() {
    String beforeMatch = "blah blah ";
    String matchedPrefix = "Abce";
    String expectedReplacement = "Abcef";
    String expectedDescription = "Description of " + expectedReplacement;
    String afterMatch = " blah";
    int expectedMatchOffset = beforeMatch.length();
    int expectedCursorPositionAfter = expectedMatchOffset + expectedReplacement.length();
    String buffer = beforeMatch + matchedPrefix + '^' + afterMatch;
    
    ICompletionProposal[] result = setUpMockingAndInvokeCompleter(buffer);
    
    assertEquals(1, result.length);
    ICompletionProposal proposal = result[0];
    assertTrue(proposal instanceof CompletionProposal);
    ReplacementCapturingDocument phonyDocument = new ReplacementCapturingDocument();
    proposal.apply(phonyDocument);
    assertEquals(expectedReplacement, phonyDocument.getReplacementText());
    assertEquals(expectedMatchOffset, phonyDocument.getReplacementOffset());
    assertEquals(matchedPrefix.length(), phonyDocument.getReplacedTextLength());
    assertEquals(expectedCursorPositionAfter, proposal.getSelection(null).x);
    assertEquals(expectedReplacement, proposal.getDisplayString());
    assertEquals(expectedDescription, proposal.getAdditionalProposalInfo());
  }
  
  @Test
  public void testComputeCompletionProposals_formOfMethodProposal() {
    testProposalFormForOneMethod("m0", "m0parms", "", "", "m0pType");
    testProposalFormForOneMethod("m1", "m1parm", "${p1}", "p1", "m1pType");
    testProposalFormForOneMethod("m2", "m2parms", "${p1}, ${p2}", "p1, p2", "m2pType");
  }

  private void testProposalFormForOneMethod(String matchedPrefix,
      String expectedMethodName, String expectedParameterPattern,
      String expectedParameterDisplay, String expectedReturnType) {
    String expectedReplacementPattern = expectedMethodName + "(" + expectedParameterPattern + ")";
    String expectedMethodDisplay =
        expectedMethodName + "(" + expectedParameterDisplay + ") - " + expectedReturnType;
    String expectedDescription = "Description of " + expectedMethodName;
    String buffer = "blah blah MyClass." + matchedPrefix + '^' + " blah";
    
    ICompletionProposal[] result = setUpMockingAndInvokeCompleter(buffer);
    
    assertEquals(1, result.length);
    ICompletionProposal proposal = result[0];
    assertTrue(proposal instanceof AppsScriptTemplateProposal);
    AppsScriptTemplateProposal templateProposal = (AppsScriptTemplateProposal) proposal;
    assertEquals(templateProposal.getDisplayString(), expectedMethodDisplay);
    assertEquals(templateProposal.getAdditionalProposalInfo(), expectedDescription);
    Template template = templateProposal.getTemplateUnprotected();
    assertEquals(expectedReplacementPattern, template.getPattern());
    assertTrue(template.isAutoInsertable());
  }

  private void testComputeCompletionProposals(
      String editorBuffer, Set<CompletionProposalInfo> expectedCompletions) {
    ICompletionProposal[] result = setUpMockingAndInvokeCompleter(editorBuffer);
    if (expectedCompletions.isEmpty()) {
      if (result != null) {
          fail("No expected completions, but result is " + proposalInfosForProposalArray(result));
      }
    } else {
      assertNotNull(
          "Result is null, but expected the following completions: " + expectedCompletions, result);
      assertEquals(expectedCompletions, proposalInfosForProposalArray(result));
    }
  }

  private ICompletionProposal[] setUpMockingAndInvokeCompleter(String editorBuffer) {
    MockitoAnnotations.initMocks(this);
    int offset = editorBuffer.indexOf('^');
    String content = editorBuffer.substring(0, offset) + editorBuffer.substring(offset + 1);
    when(mockTextViewer.getDocument()).thenReturn(new Document(content));
    WebEditorCompletionProcessor completer =
       WebEditorCompletionProcessor.make(
            ApiDocumentationService.translateBeanDocumentations(API_DATA));
    ICompletionProposal[] result = completer.computeCompletionProposals(mockTextViewer, offset);
    return result;
  }
  
  private static class StringHolder {
    public String value;
  }
  
  private static String getReplacementString(CompletionProposal proposal) {
    final StringHolder replacementStringHolder = new StringHolder();
    proposal.apply(
        new AbstractDocument() {
          @Override public void replace(int pos, int length, String text) {
            replacementStringHolder.value = text;
          }
        });
    return replacementStringHolder.value;
  }
  
  private static Map<String, CompletionProposalInfo> mapClassNamesToProposals(
      Collection<BeanDocumentation> beanDocs) {
    Map<String, CompletionProposalInfo> result = Maps.newHashMap();
    for (BeanDocumentation beanDoc : beanDocs) {
      addMappingsForType(result, beanDoc.getTopLevelType());
      for (ClassDocumentation childDoc : beanDoc.getChildTypes()) {
        addMappingsForType(result, childDoc);
      }
    }
    return result;
  }
  
  private static void addMappingsForType(
      Map<String, CompletionProposalInfo> map, ClassDocumentation classDoc) {
    String className = classDoc.getName();
    map.put(
        className,
        new CompletionProposalInfo(
            CompletionProposal.class, className, classDoc.getDescription(), className));
  }

  private static Map<String, CompletionProposalInfo> mapAttributeNamesToProposals(
      Collection<BeanDocumentation> beanDocs) {
    Map<String, CompletionProposalInfo> result = Maps.newHashMap();
    for (BeanDocumentation beanDoc : beanDocs) {
      addMappingsFromTypeAttributesToProposals(beanDoc.getTopLevelType(), result);
      for (ClassDocumentation childTypeDoc : beanDoc.getChildTypes()) {
        addMappingsFromTypeAttributesToProposals(childTypeDoc, result);
      }
    }
    return result;
  }
  
  private static Map<String, CompletionProposalInfo> mapTypeAttributesToProposals(
      ClassDocumentation classDoc) {
    Map<String, CompletionProposalInfo> result = Maps.newHashMap();
    addMappingsFromTypeAttributesToProposals(classDoc, result);
    return result;
  }
  
  private static void addMappingsFromTypeAttributesToProposals(
      ClassDocumentation classDoc, Map<String, CompletionProposalInfo> result) {
    for (AttributeDocumentation attributeDoc : classDoc.getAttributes()) {
      String attributeName = attributeDoc.getName();
      String methodReplacementPattern = METHOD_NAMES_TO_REPLACEMENT_TEMPLATES.get(attributeName);
      Class<? extends ICompletionProposal> proposalClass;
      String replacementPattern;
      if (methodReplacementPattern == null) {
        proposalClass = CompletionProposal.class;
        replacementPattern = attributeName;
      } else {
        proposalClass = TemplateProposal.class;
        replacementPattern = methodReplacementPattern;
      }
      result.put(
          attributeName,
          new CompletionProposalInfo(
              proposalClass,
              attributeDoc.getUseTemplate() + " - " + attributeDoc.getType(),
              attributeDoc.getDescription(),
              replacementPattern));
    }
  }

  @SafeVarargs
  private static Set<CompletionProposalInfo> allProposalsInMaps(
      Map<String, CompletionProposalInfo>... maps) {
    Set<CompletionProposalInfo> result = Sets.newHashSet();
    for (Map<String, CompletionProposalInfo> map : maps) {
      result.addAll(map.values());
    }
    return result;
  }
  
  private static Set<CompletionProposalInfo> proposalInfosForProposalArray(
      ICompletionProposal[] proposals) {
    Set<CompletionProposalInfo> result = Sets.newHashSet();
    for (ICompletionProposal proposal : proposals) {
      result.add(CompletionProposalInfo.getInfo(proposal));
    }
    return result;
  }
  
  private static BeanDocumentation beanWithoutChildTypes(String topLevelClassName) {
    return
        new BeanDocumentation(
            new ClassDocumentation(
                topLevelClassName, "Description of " + topLevelClassName, NO_ATTRIBUTES),
            ImmutableList.<ClassDocumentation>of());
  }
  
  /**
   * A summary of the essential characteristics of completion proposals that are checked in tests.
   * The {@link #equals(Object)} and {@link #hashCode()} methods are overridden to allow a
   * {@code CompletionProposalInfo} to be used as a member of a {@code HashSet}.
   */
  private static class CompletionProposalInfo {
    
    public static CompletionProposalInfo getInfo(ICompletionProposal proposal) {
      if (proposal instanceof CompletionProposal) {
        return new CompletionProposalInfo(
            CompletionProposal.class,
            proposal.getDisplayString(),
            proposal.getAdditionalProposalInfo(),
            getReplacementString((CompletionProposal) proposal));
      } else if (proposal instanceof AppsScriptTemplateProposal) {
        return new CompletionProposalInfo(
            TemplateProposal.class,
            proposal.getDisplayString(),
            proposal.getAdditionalProposalInfo(),
            ((AppsScriptTemplateProposal) proposal).getReplacementString());
      } else {
        throw new Error("Unexpected ICompletionProposal class " + proposal.getClass().getName());
      }
    }
    
    private final Class<? extends ICompletionProposal> proposalClass;
    private final String displayString;
    private final String additionalInfo;
    private final String replacementString;

    public CompletionProposalInfo(
        Class<? extends ICompletionProposal> proposalClass, String displayString,
        String additionalInfo, String replacementString) {
      this.proposalClass = proposalClass;
      this.displayString = displayString == null ? "" : displayString;
      this.additionalInfo = additionalInfo == null ? "" : additionalInfo;
      this.replacementString = replacementString == null ? "" : replacementString;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CompletionProposalInfo) {
        CompletionProposalInfo that = (CompletionProposalInfo) obj;
        return
            this.proposalClass.equals(that.proposalClass)
            && this.displayString.equals(that.displayString)
            && this.additionalInfo.equals(that.additionalInfo)
            && this.replacementString.equals(that.replacementString);
      } else {
        return false;
      }
    }
    
    @Override
    public int hashCode() {
      return Objects.hashCode(proposalClass, displayString, additionalInfo, replacementString);
    }
    
    @Override
    public String toString() {
      return
          Objects.toStringHelper(this)
              .add("proposalClass", proposalClass.getSimpleName())
              .add("displayString", displayString)
              .add("additionalInfo", additionalInfo)
              .add("replacementString", replacementString)
              .toString();
    }
    
  }
  
  // The only way to infer the replacement text in a CompletionProposal is to see the effect of
  // applying the CompletionProposal to an IDocument.
  private static class ReplacementCapturingDocument extends AbstractDocument {    
    private int replacementOffset;
    private int replacementLength;
    private String replacementText;

    @Override
    public void replace(int offset, int length, String text) {
      replacementOffset = offset;
      replacementLength = length;
      replacementText = text;
    }

    public int getReplacementOffset() {
      return replacementOffset;
    }

    public int getReplacedTextLength() {
      return replacementLength;
    }

    public String getReplacementText() {
      return replacementText;
    }    
  }

}
