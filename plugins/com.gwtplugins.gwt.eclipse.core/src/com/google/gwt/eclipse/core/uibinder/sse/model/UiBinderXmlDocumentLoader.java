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
package com.google.gwt.eclipse.core.uibinder.sse.model;

import com.google.gwt.eclipse.core.uibinder.sse.StructuredTextPartitionerForUiBinderXml;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.wst.sse.core.internal.document.IDocumentLoader;
import org.eclipse.wst.xml.core.internal.encoding.XMLDocumentLoader;

/**
 * Constructs a structured document from a UiBinder XML file.
 * <p>
 * This class is required to have the editor use our document partitioner. See
 * {@link com.google.gwt.eclipse.core.uibinder.sse.StructuredTextPartitionerForUiBinderXml} for
 * more information.
 */
@SuppressWarnings("restriction")
public class UiBinderXmlDocumentLoader extends XMLDocumentLoader {

  /*
   * Derived from XMLDocumentLoader's implementation.
   */
  @Override
  public IDocumentPartitioner getDefaultDocumentPartitioner() {
    return new StructuredTextPartitionerForUiBinderXml();
  }

  /*
   * Derived from XMLDocumentLoader's implementation.
   */
  @Override
  public IDocumentLoader newInstance() {
    return new UiBinderXmlDocumentLoader();
  }
}
