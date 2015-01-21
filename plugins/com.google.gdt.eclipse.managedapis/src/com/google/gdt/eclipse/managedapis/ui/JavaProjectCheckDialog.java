/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.managedapis.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Utility for error dialog popup in case the project is not a Java project.
 */
public class JavaProjectCheckDialog {
  public static boolean canAddGoogleApi(IProject project) {
    boolean hasJavaNature;
    try {
      hasJavaNature = project.hasNature(JavaCore.NATURE_ID);
    } catch (CoreException e) {
      MessageDialog.openInformation(Display.getDefault().getActiveShell(),
          "Google Plugin for Eclipse", "Project " + project.getName()
              + " is not open. Please open the project and try to add a Google API again.");
      return false;
    }

    if (hasJavaNature) {
      return true;
    } else {
      MessageDialog.openInformation(Display.getDefault().getActiveShell(),
          "Google Plugin for Eclipse", "Add Google APIs is supported only for Java Projects");
      return false;
    }
  }
}
