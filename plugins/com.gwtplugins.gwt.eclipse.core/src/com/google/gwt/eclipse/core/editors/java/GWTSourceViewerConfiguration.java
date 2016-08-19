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
package com.google.gwt.eclipse.core.editors.java;

import com.google.gdt.eclipse.core.formatter.IDocumentCloner;
import com.google.gdt.eclipse.core.formatter.IndependentMultiPassContentFormatter;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.editors.java.contentassist.JsniCompletionProcessor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.text.JavaCompositeReconcilingStrategy;
import org.eclipse.jdt.internal.ui.text.JavaReconciler;
import org.eclipse.jdt.internal.ui.text.java.JavaFormattingStrategy;
import org.eclipse.jdt.internal.ui.text.spelling.JavaSpellingReconcileStrategy;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.Map;

/**
 * Provides custom editor behavior for the GWT-specific Java editor.
 */
@SuppressWarnings("restriction")
public class GWTSourceViewerConfiguration extends JavaSourceViewerConfiguration {

  /**
   * Clones a document representing a .java source file. The clone will have the
   * same text and partitioning (GWTPartitions.GWT_PARTITIONING and
   * IJavaPartitions.JAVA_PARTITIONING) as the original.
   */
  private static final IDocumentCloner JAVA_DOCUMENT_CLONER = new IDocumentCloner() {

    public IDocument clone(IDocument original) {
      IDocument clone = new Document(original.get());
      new GWTDocumentSetupParticipant().setup(clone);
      return clone;
    }

    public void release(IDocument clone) {
      // Nothing to release
    }
  };

  private final JsniScanner jsniScanner;

  public GWTSourceViewerConfiguration(IColorManager colorManager,
      IPreferenceStore preferenceStore, ITextEditor editor) {
    /*
     * We're passing null for the partitioning here, and will instead provide it
     * on demand in our getConfiguredDocumentPartitioning override. This allows
     * us to emulate the Java editor by using Java partitioning for .java files
     * in non-GWT projects. If we always used GWT partitioning, non-GWT .java
     * files would end up with no syntax coloring when opened by the GWT editor
     * (since GWT partitioning is only applied within GWT projects).
     */
    super(colorManager, preferenceStore, editor, null);

    jsniScanner = new JsniScanner(colorManager);
    jsniScanner.setDefaultReturnToken(new Token(new TextAttribute(
        colorManager.getColor(JsniColorConstants.JSNI_DEFAULT))));
  }

  @Override
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer,
      String contentType) {
    if (GWTPartitions.JSNI_METHOD.equals(contentType)) {
      // Get project's formatting settings
      IJavaProject javaProject = ((GWTJavaEditor) getEditor()).getInputJavaProject();
      Map<?, ?> prefs = (javaProject != null ? javaProject.getOptions(true)
          : JavaCore.getOptions());
      return new IAutoEditStrategy[] {new JsniAutoEditStrategy(prefs)};
    }
    return super.getAutoEditStrategies(sourceViewer, contentType);
  }

  @Override
  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    // Include all native Java content types as well as custom types like JSNI
    String[] javaContentTypes = super.getConfiguredContentTypes(sourceViewer);
    String[] jsniContentTypes = {GWTPartitions.JSNI_METHOD};
    String[] allContentTypes = new String[javaContentTypes.length
        + jsniContentTypes.length];
    System.arraycopy(javaContentTypes, 0, allContentTypes, 0,
        javaContentTypes.length);
    System.arraycopy(jsniContentTypes, 0, allContentTypes,
        javaContentTypes.length, jsniContentTypes.length);
    return allContentTypes;
  }

  @Override
  public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
    try {
      ITextEditor editor = getEditor();
      return ((GWTJavaEditor) editor).getInputPartitioning();
    } catch (Exception e) {
      GWTPluginLog.logError(e);
      return IJavaPartitions.JAVA_PARTITIONING;
    }
  }

  @Override
  public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
    ContentAssistant assistant = (ContentAssistant) super.getContentAssistant(sourceViewer);

    ICompilationUnit cu = ((GWTJavaEditor) getEditor()).getCompilationUnit();
    IContentAssistProcessor processor = new JsniCompletionProcessor(cu);
    assistant.setContentAssistProcessor(processor, GWTPartitions.JSNI_METHOD);

    return assistant;
  }

  @Override
  public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
    IndependentMultiPassContentFormatter formatter = new IndependentMultiPassContentFormatter(
        getConfiguredDocumentPartitioning(sourceViewer),
        IDocument.DEFAULT_CONTENT_TYPE, JAVA_DOCUMENT_CLONER);
    formatter.setMasterStrategy(new JavaFormattingStrategy());
    formatter.setSlaveStrategy2(new JsniFormattingStrategy(),
        GWTPartitions.JSNI_METHOD);
    return formatter;
  }

  @Override
  public IPresentationReconciler getPresentationReconciler(
      ISourceViewer sourceViewer) {
    PresentationReconciler reconciler = (PresentationReconciler) super.getPresentationReconciler(sourceViewer);

    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(jsniScanner);
    reconciler.setDamager(dr, GWTPartitions.JSNI_METHOD);
    reconciler.setRepairer(dr, GWTPartitions.JSNI_METHOD);

    return reconciler;
  }

  @Override
  public IReconciler getReconciler(ISourceViewer sourceViewer) {
    JavaReconciler reconciler = (JavaReconciler) super.getReconciler(sourceViewer);
    if (reconciler != null) {
      try {
        JavaCompositeReconcilingStrategy strategy = (JavaCompositeReconcilingStrategy) reconciler.getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
        IReconcilingStrategy[] strategies = strategy.getReconcilingStrategies();
        IReconcilingStrategy[] newStrategies = new IReconcilingStrategy[strategies.length];
        for (int i = 0; i < strategies.length; i++) {
          if (strategies[i] instanceof JavaSpellingReconcileStrategy) {
            // Replace the default Java reconcile strategy with our own, which
            // will suppress spell checking within JSNI blocks
            newStrategies[i] = new GWTJavaSpellingReconcileStrategy(
                sourceViewer, getEditor());
          } else {
            newStrategies[i] = strategies[i];
          }
        }
        strategy.setReconcilingStrategies(newStrategies);
      } catch (Exception e) {
        // We're being defensive to ensure that we always return a reconciler
        GWTPluginLog.logError(e);
      }
      return reconciler;
    }

    return null;
  }
}
