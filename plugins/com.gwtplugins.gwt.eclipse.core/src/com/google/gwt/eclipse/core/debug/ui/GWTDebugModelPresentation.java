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
package com.google.gwt.eclipse.core.debug.ui;

import com.google.gdt.eclipse.core.AdapterUtilities;

import org.eclipse.jdt.internal.debug.ui.JDIModelPresentation;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ide.IDE;

/**
 * Custom debug model presentation to ensure that the GWT Java Editor is used in
 * debug mode for GWT projects. The default JDT model presentation class only
 * uses the editor input filename to determine the editor, so the GWT Java
 * Editor (which is only the default for GWT projects) never gets selected.
 */
@SuppressWarnings("restriction")
public class GWTDebugModelPresentation extends JDIModelPresentation {

  @Override
  public String getEditorId(IEditorInput input, Object inputObject) {
    IFileEditorInput fileEditorInput = AdapterUtilities.getAdapter(input,
        IFileEditorInput.class);
    if (fileEditorInput != null) {
      IEditorDescriptor editor = IDE.getDefaultEditor(fileEditorInput.getFile());
      if (editor != null) {
        return editor.getId();
      }
    }

    // If editor input isn't pointing to a file, we can't properly determine
    // the content type so delegate to the default implementation.
    return super.getEditorId(input, inputObject);
  }

}
