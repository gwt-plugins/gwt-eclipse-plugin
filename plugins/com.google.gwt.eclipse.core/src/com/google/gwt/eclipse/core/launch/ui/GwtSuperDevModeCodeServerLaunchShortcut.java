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
package com.google.gwt.eclipse.core.launch.ui;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gwt.eclipse.core.launch.util.GwtSuperDevModeCodeServerLaunchUtil;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

/**
 * Launch shortcut for SDM Code server.
 */
public class GwtSuperDevModeCodeServerLaunchShortcut implements ILaunchShortcut {

  @Override
  public void launch(IEditorPart editor, String mode) {
    IResource resource = ResourceUtils.getEditorInput(editor);
    if (resource != null) {
      GwtSuperDevModeCodeServerLaunchUtil.launch(resource.getProject(), mode);
    }
  }

  @Override
  public void launch(ISelection selection, String mode) {
    IResource resource = ResourceUtils.getSelectionResource(selection);
    if (resource != null) {
      GwtSuperDevModeCodeServerLaunchUtil.launch(resource.getProject(), mode);
    }
  }

}
