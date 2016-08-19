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

import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaDocumentSetupParticipant;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;

/**
 * Sets up Java source files with Java and GWT partitions.
 */
@SuppressWarnings("restriction")
public class GWTDocumentSetupParticipant extends JavaDocumentSetupParticipant {

  private static final String[] LEGAL_CONTENT_TYPES = {
      IJavaPartitions.JAVA_DOC, IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
      IJavaPartitions.JAVA_SINGLE_LINE_COMMENT, IJavaPartitions.JAVA_STRING,
      IJavaPartitions.JAVA_CHARACTER, GWTPartitions.JSNI_METHOD};

  public static void setupGWTPartitioning(IDocument document) {
    assert document instanceof IDocumentExtension3;
    IPartitionTokenScanner javaScanner = JavaPlugin.getDefault().getJavaTextTools().getPartitionScanner();
    IPartitionTokenScanner jsniScanner = new GWTPartitionScanner();
    IDocumentPartitioner partitioner = new FastPartitioner(
        new CompositePartitionScanner(javaScanner, jsniScanner),
        LEGAL_CONTENT_TYPES);
    IDocumentExtension3 extension3 = (IDocumentExtension3) document;
    extension3.setDocumentPartitioner(GWTPartitions.GWT_PARTITIONING,
        partitioner);
    partitioner.connect(document);
  }

  @Override
  public void setup(IDocument document) {
    try {
      super.setup(document);
      setupGWTPartitioning(document);
    } catch (Exception e) {
      GWTPluginLog.logError(e);
    }
  }

}
