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

import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;

import org.eclipse.wst.sse.core.internal.document.IDocumentLoader;
import org.eclipse.wst.sse.core.internal.provisional.IModelLoader;
import org.eclipse.wst.xml.core.internal.modelhandler.ModelHandlerForXML;

/**
 * Entry point for generating structured models and documents from UiBinder XML
 * files. This is the default handler for UiBinder XML files, set via the
 * "org.eclipse.wst.sse.core.modelHandler" extension point.
 * <p>
 * This class is required to have the editor use our document partitioner. See
 * {@link com.google.gwt.eclipse.core.uibinder.sse.StructuredTextPartitionerForUiBinderXml} for
 * more information.
 */
@SuppressWarnings("restriction")
public class ModelHandlerForUiBinderXml extends ModelHandlerForXML {

  /*
   * Derived from ModelHandlerForXML's implementation.
   */
  public ModelHandlerForUiBinderXml() {
    setId(UiBinderConstants.UI_BINDER_XML_MODEL_HANDLER_ID);
    setAssociatedContentTypeId(UiBinderConstants.UI_BINDER_XML_CONTENT_TYPE_ID);
  }

  /*
   * Derived from ModelHandlerForXML's implementation.
   */
  @Override
  public IDocumentLoader getDocumentLoader() {
    return new UiBinderXmlDocumentLoader();
  }

  /*
   * Derived from ModelHandlerForXML's implementation.
   */
  @Override
  public IModelLoader getModelLoader() {
    return new UiBinderXmlModelLoader();
  }
}
