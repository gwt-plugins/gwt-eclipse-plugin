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
package com.google.gwt.eclipse.core.uibinder.sse;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.contentassist.UiBinderXmlCompletionProcessor;
import com.google.gwt.eclipse.core.uibinder.formatter.UiBinderFormatter;
import com.google.gwt.eclipse.core.uibinder.sse.css.InlinedCssContentAssistProcessor;
import com.google.gwt.eclipse.core.uibinder.sse.css.LineStyleProviderForInlinedCss;
import com.google.gwt.eclipse.core.uibinder.sse.css.StructuredAutoEditStrategyInlinedCss;
import com.google.gwt.eclipse.platform.uibinder.UiBinderStructuredRegionProcessor;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.wst.css.core.text.ICSSPartitions;
import org.eclipse.wst.css.ui.StructuredTextViewerConfigurationCSS;
import org.eclipse.wst.css.ui.internal.autoedit.StructuredAutoEditStrategyCSS;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;
import org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A source viewer configuration for UiBinder XML template files.
 */
@SuppressWarnings("restriction")
public class UiBinderXmlSourceViewerConfiguration extends
    StructuredTextViewerConfigurationXML {

  private static final String[] ADDITIONAL_CONTENT_TYPES = {ICSSPartitions.STYLE};

  /**
   * A CSS source viewer configuration that we delegate to for simple CSS tasks.
   */
  private final StructuredTextViewerConfigurationCSS cssSourceViewerConfiguration;

  public UiBinderXmlSourceViewerConfiguration() {
    cssSourceViewerConfiguration = new StructuredTextViewerConfigurationCSS();
    try {
      UiBinderStructuredRegionProcessor.setAsReconciler(this);
    } catch (Exception e) {
      GWTPluginLog.logError(e, "Could not set custom UiBinder reconciler.");
    }
  }

  @Override
  public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer,
      String contentType) {

    if (contentType == ICSSPartitions.STYLE) {
      IAutoEditStrategy[] autoEditStrategies = cssSourceViewerConfiguration.getAutoEditStrategies(
          sourceViewer, contentType);

      for (int i = 0; i < autoEditStrategies.length; i++) {
        // Replace any StructuredAutoEditStrategyCSS with the one that works
        // with inlined CSS
        if (autoEditStrategies[i] instanceof StructuredAutoEditStrategyCSS) {
          autoEditStrategies[i] = new StructuredAutoEditStrategyInlinedCss();
        }
      }

      return autoEditStrategies;
    }

    return super.getAutoEditStrategies(sourceViewer, contentType);
  }

  @Override
  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    ArrayList<String> contentTypes = new ArrayList<String>(
        Arrays.asList(super.getConfiguredContentTypes(sourceViewer)));
    contentTypes.addAll(Arrays.asList(ADDITIONAL_CONTENT_TYPES));

    return contentTypes.toArray(new String[contentTypes.size()]);
  }

  @Override
  public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
    return UiBinderFormatter.createFormatter(getConfiguredDocumentPartitioning(sourceViewer));
  }

  @Override
  public String[] getIndentPrefixes(ISourceViewer sourceViewer,
      String contentType) {

    if (contentType == ICSSPartitions.STYLE) {
      return cssSourceViewerConfiguration.getIndentPrefixes(sourceViewer,
          contentType);
    }

    return super.getIndentPrefixes(sourceViewer, contentType);
  }

  @Override
  public LineStyleProvider[] getLineStyleProviders(ISourceViewer sourceViewer,
      String partitionType) {
    if (partitionType == ICSSPartitions.STYLE) {
      return new LineStyleProvider[] {new LineStyleProviderForInlinedCss()};
    }

    return super.getLineStyleProviders(sourceViewer, partitionType);
  }

  @Override
  protected IContentAssistProcessor[] getContentAssistProcessors(
      ISourceViewer sourceViewer, String partitionType) {

    if (partitionType == ICSSPartitions.STYLE) {
      return new IContentAssistProcessor[] {new InlinedCssContentAssistProcessor()};
    }

    return new IContentAssistProcessor[] {new UiBinderXmlCompletionProcessor()};
  }

}
