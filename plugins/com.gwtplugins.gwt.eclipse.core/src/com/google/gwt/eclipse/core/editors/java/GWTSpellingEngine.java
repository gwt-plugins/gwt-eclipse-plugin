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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.text.spelling.DefaultSpellingEngine;
import org.eclipse.jdt.internal.ui.text.spelling.JavaSpellingEngine;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Spelling engine that ignores JSNI blocks.
 */
@SuppressWarnings("restriction")
public class GWTSpellingEngine extends DefaultSpellingEngine {

  private class GWTJavaSpellingEngine extends JavaSpellingEngine {

    @Override
    protected void check(IDocument document, IRegion[] regions,
        ISpellChecker checker, ISpellingProblemCollector collector,
        IProgressMonitor monitor) {
      try {
        List<IRegion> regionList = new ArrayList<IRegion>();
        for (int i = 0; i < regions.length; i++) {
          IRegion region = regions[i];
          // Compute the GWT partitioning so we can identify JSNI blocks
          ITypedRegion[] partitions = TextUtilities.computePartitioning(
              document, GWTPartitions.GWT_PARTITIONING, region.getOffset(),
              region.getLength(), false);
          // Spelling engine should ignore all JSNI block regions
          for (int j = 0; j < partitions.length; j++) {
            ITypedRegion partition = partitions[j];
            if (!GWTPartitions.JSNI_METHOD.equals(partition.getType())) {
              regionList.add(partition);
            }
          }
        }
        super.check(document,
            regionList.toArray(new IRegion[regionList.size()]), checker,
            collector, monitor);
      } catch (BadLocationException e) {
        // Ignore: the document has been changed in another thread and will be
        // checked again (our super class JavaSpellingEngine does the same).
      }
    }
  }

  private GWTJavaSpellingEngine gwtEngine = new GWTJavaSpellingEngine();

  @Override
  public void check(IDocument document, IRegion[] regions,
      SpellingContext context, ISpellingProblemCollector collector,
      IProgressMonitor monitor) {
    if (JavaCore.JAVA_SOURCE_CONTENT_TYPE.equals(context.getContentType().getId())) {
      gwtEngine.check(document, regions, context, collector, monitor);
    } else {
      super.check(document, regions, context, collector, monitor);
    }
  }
}
