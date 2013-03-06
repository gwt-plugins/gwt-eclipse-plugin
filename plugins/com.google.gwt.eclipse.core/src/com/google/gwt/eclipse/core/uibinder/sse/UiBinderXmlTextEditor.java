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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

/**
 * Text editor for UiBinder XML template files.
 * <p>
 * SSE allows extending the existing editors (e.g. XML editor) without
 * subclassing. We define a content type, and a structured source viewer
 * configuration for that type. Any time the content type is being edited (even
 * with the default XML editor), our extensions appear in the editor. The
 * problem with this approach is we want to still allow users to use the default
 * XML editor WITHOUT our extensions. We created this editor, and
 * programmatically set the {@link UiBinderXmlSourceViewerConfiguration}.
 * <p>
 * Unfortunately, our model handler will still be used, regardless of the editor
 * (default or this specific editor.) This means the default XML editor still
 * has some enhancements (e.g. the partitioning of <ui:style> blocks, the model
 * will have @medias for @ifs, etc.). The features specific to our editor are
 * those provided by the {@link UiBinderXmlSourceViewerConfiguration}.
 */
public class UiBinderXmlTextEditor extends StructuredTextEditor {

  @Override
  protected void doSetInput(final IEditorInput input) throws CoreException {

    super.doSetInput(input);
    setSourceViewerConfiguration(new UiBinderXmlSourceViewerConfiguration());
  }
}
