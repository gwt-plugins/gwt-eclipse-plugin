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
package com.gwtplugins.gwt.eclipse.gss;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import com.gwtplugins.gwt.eclipse.gss.model.GssResourceSourceViewerConfiguration;

/**
 * Text editor for CssResource CSS files.
 * <p>
 * See {@link com.google.gwt.eclipse.core.uibinder.sse.UiBinderXmlTextEditor}
 * for a description of why we require a subclass of
 * {@link StructuredTextEditor}.
 */
public class GssResourceTextEditor extends StructuredTextEditor {

	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);

		setSourceViewerConfiguration(new GssResourceSourceViewerConfiguration());
	}

}
