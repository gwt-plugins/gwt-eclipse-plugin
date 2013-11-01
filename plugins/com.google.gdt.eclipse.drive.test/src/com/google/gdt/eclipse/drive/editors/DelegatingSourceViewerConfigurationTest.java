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

import com.google.api.client.util.Sets;
import com.google.common.collect.ImmutableSet;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;

/**
 * Unit test for {@link DelegatingSourceViewerConfiguration}.
 */
@SuppressWarnings("deprecation") //IAutoIndentStrategy
@RunWith(JUnit4.class)
public class DelegatingSourceViewerConfigurationTest {
  
  private static class BaseClass extends SourceViewerConfiguration {  
    private Set<String> calledMethods;
    
    public BaseClass() {
      calledMethods = Sets.newHashSet();
    }
  
    protected void recordCall(String qualifiedMethodName) {
      calledMethods.add(qualifiedMethodName);
    }
    
    public Set<String> getCalledMethods() {
      return calledMethods;
    }
  
    @Override public int getTabWidth(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getTableWidth");
      return 0;
    }
  
    @Override public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getUndoManager");
      return null;
    }
  
    @Override public IReconciler getReconciler(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getReconciler");
      return null;
    }
  
    @Override public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getPresentationReconciler");
      return null;
    }
  
    @Override public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getContentFormatter");
      return null;
    }
  
    @Override public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getContentAssistant");
      return null;
    }
  
    @Override public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getQuickAssistAssistant");
      return null;
    }
  
    @Override public IAutoIndentStrategy getAutoIndentStrategy(
        ISourceViewer sourceViewer, String contentType) {
      recordCall("BaseClass.getAutoIndentStrategy");
      return null;
    }
  
    @Override public IAutoEditStrategy[] getAutoEditStrategies(
        ISourceViewer sourceViewer, String contentType) {
      recordCall("BaseClass.getAutoEditStrategies");
      return null;
    }
  
    @Override public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
      recordCall("BaseClass.getDefaultPrefixes");
      return null;
    }
  
    @Override public ITextDoubleClickStrategy getDoubleClickStrategy(
        ISourceViewer sourceViewer, String contentType) {
      recordCall("BaseClass.getDoubleClickStrategy");
      return null;
    }
  
    @Override public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
      recordCall("BaseClass.getIndentPrefixes");
      return null;
    }
  
    @Override public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getAnnotationHover");
      return null;
    }
  
    @Override public IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getOverviewRulerAnnotationHover");
      return null;
    }
  
    @Override
    public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
      recordCall("BaseClass.getConfiguredTextHoverStateMasks");
      return null;
    }
  
    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
      recordCall("BaseClass.getTextHover3");
      return null;
    }
  
    @Override public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
      recordCall("BaseClass.getTextHover2");
      return null;
    }
  
    @Override
    public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getInformationControlCreator");
      return null;
    }
  
    @Override public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getInformationPresenter");
      return null;
    }
  
    @Override public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getConfiguredContentTypes");
      return null;
    }
  
    @Override public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getConfiguredDocumentPartitioning");
      return null;
    }
  
    @Override public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getHyperlinkDetectors");
      return null;
    }
  
    @Override public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getHyperlinkPresenter");
      return null;
    }
  
    @Override public int getHyperlinkStateMask(ISourceViewer sourceViewer) {
      recordCall("BaseClass.getHyperlinkStateMask");
      return 0;
    }
  
    @Override public int hashCode() {
      recordCall("BaseClass.hashCode");
      return 0;
    }
  
    @Override public boolean equals(Object obj) {
      recordCall("BaseClass.equals");
      return false;
    }
  
    @Override public String toString() {
      recordCall("BaseClass.toString");
      return null;
    }
  }
    
  private static final Set<String> ALL_BASE_CLASS_METHODS =
      ImmutableSet.of(
          "BaseClass.getTableWidth", "BaseClass.getUndoManager", "BaseClass.getReconciler",
          "BaseClass.getPresentationReconciler", "BaseClass.getContentFormatter",
          "BaseClass.getContentAssistant", "BaseClass.getQuickAssistAssistant",
          "BaseClass.getAutoIndentStrategy", "BaseClass.getAutoEditStrategies",
          "BaseClass.getDefaultPrefixes", "BaseClass.getDoubleClickStrategy",
          "BaseClass.getIndentPrefixes", "BaseClass.getAnnotationHover",
          "BaseClass.getOverviewRulerAnnotationHover", "BaseClass.getConfiguredTextHoverStateMasks",
          "BaseClass.getTextHover3", "BaseClass.getTextHover2",
          "BaseClass.getInformationControlCreator", "BaseClass.getInformationPresenter",
          "BaseClass.getConfiguredContentTypes", "BaseClass.getConfiguredDocumentPartitioning",
          "BaseClass.getHyperlinkDetectors", "BaseClass.getHyperlinkPresenter",
          "BaseClass.getHyperlinkStateMask", "BaseClass.hashCode", "BaseClass.equals",
          "BaseClass.toString");
  
  @Test
  public void testAllExtensions() {
    testExtension(new BaseClass(){}, ALL_BASE_CLASS_METHODS);
    testExtension(
        new BaseClass(){
          @Override public int getTabWidth(ISourceViewer sourceViewer) {
            recordCall("Extension.getTableWidth");
            return 0;
          }
        },
        replace(ALL_BASE_CLASS_METHODS, "BaseClass.getTableWidth", "Extension.getTableWidth"));
    testExtension(
        new BaseClass(){
          @Override public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
            recordCall("Extension.getUndoManager");
            return null;
          }
        },
        replace(ALL_BASE_CLASS_METHODS, "BaseClass.getUndoManager", "Extension.getUndoManager"));
    testExtension(
        new BaseClass(){
          @Override public IReconciler getReconciler(ISourceViewer sourceViewer) {
            recordCall("Extension.getReconciler");
            return null;
          }
        },
        replace(ALL_BASE_CLASS_METHODS, "BaseClass.getReconciler", "Extension.getReconciler"));
    testExtension(
        new BaseClass(){
          @Override public IPresentationReconciler getPresentationReconciler(
              ISourceViewer sourceViewer) {
            recordCall("Extension.getPresentationReconciler");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getPresentationReconciler",
            "Extension.getPresentationReconciler"));
    testExtension(
        new BaseClass(){
          @Override public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
            recordCall("Extension.getContentFormatter");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getContentFormatter",
            "Extension.getContentFormatter"));
    testExtension(
        new BaseClass(){
          @Override public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
            recordCall("Extension.getContentAssistant");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getContentAssistant",
            "Extension.getContentAssistant"));
    testExtension(
        new BaseClass(){
          @Override public IQuickAssistAssistant getQuickAssistAssistant(
              ISourceViewer sourceViewer) {
            recordCall("Extension.getQuickAssistAssistant");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getQuickAssistAssistant",
            "Extension.getQuickAssistAssistant"));
    testExtension(
        new BaseClass(){
          @Override public IAutoIndentStrategy getAutoIndentStrategy(
              ISourceViewer sourceViewer, String contentType) {
            recordCall("Extension.getAutoIndentStrategy");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getAutoIndentStrategy",
            "Extension.getAutoIndentStrategy"));
    testExtension(
        new BaseClass(){
          @Override public IAutoEditStrategy[] getAutoEditStrategies(
              ISourceViewer sourceViewer, String contentType) {
            recordCall("Extension.getAutoEditStrategies");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getAutoEditStrategies",
            "Extension.getAutoEditStrategies"));
    testExtension(
        new BaseClass(){
          @Override public String[] getDefaultPrefixes(
              ISourceViewer sourceViewer, String contentType) {
            recordCall("Extension.getDefaultPrefixes");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getDefaultPrefixes",
            "Extension.getDefaultPrefixes"));
    testExtension(
        new BaseClass(){
          @Override public ITextDoubleClickStrategy getDoubleClickStrategy(
              ISourceViewer sourceViewer, String contentType) {
            recordCall("Extension.getDoubleClickStrategy");
            return null;
          }
        },
        replace(ALL_BASE_CLASS_METHODS,
            "BaseClass.getDoubleClickStrategy",
            "Extension.getDoubleClickStrategy"));
    testExtension(
        new BaseClass(){
          @Override public String[] getIndentPrefixes(
              ISourceViewer sourceViewer, String contentType) {
            recordCall("Extension.getIndentPrefixes");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getIndentPrefixes",
            "Extension.getIndentPrefixes"));
    testExtension(
        new BaseClass(){
          @Override public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
            recordCall("Extension.getAnnotationHover");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getAnnotationHover",
            "Extension.getAnnotationHover"));
    testExtension(
        new BaseClass(){
          @Override public IAnnotationHover getOverviewRulerAnnotationHover(
              ISourceViewer sourceViewer) {
            recordCall("Extension.getOverviewRulerAnnotationHover");
            return null;
          }
        },
        replace(ALL_BASE_CLASS_METHODS,
            "BaseClass.getOverviewRulerAnnotationHover",
            "Extension.getOverviewRulerAnnotationHover"));
    testExtension(
        new BaseClass(){
          @Override
          public int[] getConfiguredTextHoverStateMasks(
              ISourceViewer sourceViewer, String contentType) {
            recordCall("Extension.getConfiguredTextHoverStateMasks");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getConfiguredTextHoverStateMasks",
            "Extension.getConfiguredTextHoverStateMasks"));
    testExtension(
        new BaseClass(){
          @Override
          public ITextHover getTextHover(
              ISourceViewer sourceViewer, String contentType, int stateMask) {
            recordCall("Extension.getTextHover3");
            return null;
          }
        },
        replace(ALL_BASE_CLASS_METHODS, "BaseClass.getTextHover3", "Extension.getTextHover3"));
    testExtension(
        new BaseClass(){
          @Override public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
            recordCall("Extension.getTextHover2");
            return null;
          }
        },
        replace(ALL_BASE_CLASS_METHODS, "BaseClass.getTextHover2", "Extension.getTextHover2"));
    testExtension(
        new BaseClass(){
          @Override
          public IInformationControlCreator getInformationControlCreator(
              ISourceViewer sourceViewer) {
            recordCall("Extension.getInformationControlCreator");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getInformationControlCreator",
            "Extension.getInformationControlCreator"));
    testExtension(
        new BaseClass(){
          @Override public IInformationPresenter getInformationPresenter(
              ISourceViewer sourceViewer) {
            recordCall("Extension.getInformationPresenter");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getInformationPresenter",
            "Extension.getInformationPresenter"));
    testExtension(
        new BaseClass(){
          @Override public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
            recordCall("Extension.getConfiguredContentTypes");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getConfiguredContentTypes",
            "Extension.getConfiguredContentTypes"));
    testExtension(
        new BaseClass(){
          @Override public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
            recordCall("Extension.getConfiguredDocumentPartitioning");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getConfiguredDocumentPartitioning",
            "Extension.getConfiguredDocumentPartitioning"));
    testExtension(
        new BaseClass(){
          @Override public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
            recordCall("Extension.getHyperlinkDetectors");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getHyperlinkDetectors",
            "Extension.getHyperlinkDetectors"));
    testExtension(
        new BaseClass(){
          @Override public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
            recordCall("Extension.getHyperlinkPresenter");
            return null;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getHyperlinkPresenter",
            "Extension.getHyperlinkPresenter"));
    testExtension(
        new BaseClass(){
          @Override public int getHyperlinkStateMask(ISourceViewer sourceViewer) {
            recordCall("Extension.getHyperlinkStateMask");
            return 0;
          }
        },
        replace(
            ALL_BASE_CLASS_METHODS,
            "BaseClass.getHyperlinkStateMask",
            "Extension.getHyperlinkStateMask"));
    testExtension(
        new BaseClass(){
          @Override public int hashCode() {
            recordCall("Extension.hashCode");
            return 0;
          }
        },
        replace(ALL_BASE_CLASS_METHODS, "BaseClass.hashCode", "Extension.hashCode"));
    testExtension(
        new BaseClass(){
          @Override public boolean equals(Object obj) {
            recordCall("Extension.equals");
            return false;
          }
        },
        replace(ALL_BASE_CLASS_METHODS, "BaseClass.equals", "Extension.equals"));
    testExtension(
        new BaseClass(){
          @Override public String toString() {
            recordCall("Extension.toString");
            return null;
          }
        },
        replace(ALL_BASE_CLASS_METHODS, "BaseClass.toString", "Extension.toString"));
    testExtension(
        new BaseClass(){
          @Override public int hashCode() {
            recordCall("Extension.hashCode");
            return 0;
          }

          @Override public boolean equals(Object obj) {
            recordCall("Extension.equals");
            return false;
          }

          @Override public String toString() {
            recordCall("Extension.toString");
            return null;
          }
        },
        replace(
            replace(
                replace(ALL_BASE_CLASS_METHODS, "BaseClass.hashCode", "Extension.hashCode"),
                "BaseClass.equals",
                "Extension.equals"),
            "BaseClass.toString",
            "Extension.toString"));
  }
  
  private static Set<String> replace(
      Set<String> original, String toBeReplaced, String replacement) {
    Set<String> result = Sets.newHashSet();
    result.addAll(original);
    result.remove(toBeReplaced);
    result.add(replacement);
    return result;
  }
  
  private static void testExtension(BaseClass delegate, Set<String> expectedCalls) {
    DelegatingSourceViewerConfiguration delegater =
        new DelegatingSourceViewerConfiguration(delegate){ };
    callAllPublicMethods(delegater);
    assertEquals(expectedCalls, delegate.getCalledMethods());
  }

  private static void callAllPublicMethods(DelegatingSourceViewerConfiguration receiver) {
    receiver.getTabWidth(null);
    receiver.getUndoManager(null);
    receiver.getReconciler(null);
    receiver.getPresentationReconciler(null);
    receiver.getContentFormatter(null);
    receiver.getContentAssistant(null);
    receiver.getQuickAssistAssistant(null);
    receiver.getAutoIndentStrategy(null, null);
    receiver.getAutoEditStrategies(null, null);
    receiver.getDefaultPrefixes(null, null);
    receiver.getDoubleClickStrategy(null, null);
    receiver.getIndentPrefixes(null, null);
    receiver.getAnnotationHover(null);
    receiver.getOverviewRulerAnnotationHover(null);
    receiver.getConfiguredTextHoverStateMasks(null, null);
    receiver.getTextHover(null, null, 0);
    receiver.getTextHover(null, null);
    receiver.getInformationControlCreator(null);
    receiver.getInformationPresenter(null);
    receiver.getConfiguredContentTypes(null);
    receiver.getConfiguredDocumentPartitioning(null);
    receiver.getHyperlinkDetectors(null);
    receiver.getHyperlinkPresenter(null);
    receiver.getHyperlinkStateMask(null);
    receiver.hashCode();
    receiver.equals(null);
    receiver.toString();
  }
}
