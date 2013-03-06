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
package com.google.gwt.eclipse.core.uibinder.formatter;

import com.google.gdt.eclipse.core.XmlUtilities;
import com.google.gdt.eclipse.core.formatter.IndependentMultiPassContentFormatter;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.sse.StructuredTextPartitionerForUiBinderXml;
import com.google.gwt.eclipse.core.uibinder.sse.css.InlinedCssFormattingStrategy;
import com.google.gwt.eclipse.core.uibinder.text.IDocumentPartitionerFactory;
import com.google.gwt.eclipse.core.uibinder.text.StructuredDocumentCloner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.wst.css.core.text.ICSSPartitions;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredPartitioning;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.text.IXMLPartitions;
import org.eclipse.wst.xml.ui.internal.XMLFormattingStrategy;

import java.io.IOException;

/**
 * Utility methods for formatting UiBinder template (ui.xml) files.
 */
@SuppressWarnings("restriction")
public final class UiBinderFormatter {

  private static final IDocumentPartitionerFactory UIBINDER_XML_PARTITIONER_FACTORY = new IDocumentPartitionerFactory() {
    public IDocumentPartitioner createDocumentPartitioner() {
      return new StructuredTextPartitionerForUiBinderXml();
    }
  };

  public static IContentFormatter createFormatter(String partitioning) {
    /*
     * Ideally, we would have no master strategy and two slave strategies, one
     * for XML and one for CSS. The problem is, XMLFormattingStrategy won't work
     * properly since some paired opening and closing tags are spread across
     * different partitions (for example, the <ui:style> tag since the CSS
     * partition will be between the opening and closing tag.)
     */
    IndependentMultiPassContentFormatter formatter = new IndependentMultiPassContentFormatter(
        partitioning, IXMLPartitions.XML_DEFAULT, new StructuredDocumentCloner(
            partitioning, UIBINDER_XML_PARTITIONER_FACTORY));
    formatter.setMasterStrategy(new XMLFormattingStrategy());
    formatter.setSlaveStrategy2(new InlinedCssFormattingStrategy(),
        ICSSPartitions.STYLE);

    /*
     * If the <ui:style> contains a '%' (for example, in something like
     * "width: 50%;"), the XML following the <ui:style> tag will not be
     * formatted. For the '%', the region type is UNDEFINED, and there is no
     * corresponding DOM node. Before formatting, the
     * DefaultXMLPartitionFormatter ensures the current region matches the
     * current DOM node's first region, and since there is no DOM node for the
     * UNDEFINED region, the formatting abruptly stops. To workaround this
     * issue, we replace the CSS with whitespace when the master formatter runs.
     */
    formatter.setReplaceSlavePartitionsDuringMasterFormat(true);

    formatter.setCheckForNonwhitespaceChanges(true);

    return formatter;
  }

  /**
   * Formats a UiBinder template file.
   * 
   * @param uiXmlFile the ui.xml file to format
   * @param forceSave if <code>true</code>, the file will be saved even if it
   *          already has pending edits. If there are no pending edits, the file
   *          will always be saved after formatting.
   */
  public static void format(IFile uiXmlFile, boolean forceSave)
      throws UiBinderException {
    final IContentFormatter formatter = createFormatter(IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING);
    try {
      new XmlUtilities.EditOperation(uiXmlFile) {
        @Override
        protected void edit(IDOMDocument document) {
          IStructuredDocument doc = document.getModel().getStructuredDocument();
          formatter.format(doc, new Region(0, doc.getLength()));
        }
      }.run(forceSave);
    } catch (IOException e) {
      throw new UiBinderException(e);
    } catch (CoreException e) {
      throw new UiBinderException(e);
    }
  }

  private UiBinderFormatter() {
  }

}
