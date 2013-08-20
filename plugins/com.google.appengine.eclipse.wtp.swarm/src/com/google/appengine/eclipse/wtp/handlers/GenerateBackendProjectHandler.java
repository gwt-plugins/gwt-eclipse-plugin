/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.appengine.eclipse.wtp.handlers;

import com.google.appengine.eclipse.wtp.swarm.BackendGeneratorWizard;
import com.google.gdt.eclipse.appengine.swarm_backend.impl.BackendGenerator;
import com.google.gdt.eclipse.platform.shared.ui.IPixelConverter;
import com.google.gdt.eclipse.platform.ui.PixelConverterFactory;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Generates the backend project for a given Android project.
 */
public final class GenerateBackendProjectHandler extends AbstractHandler {

  private static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getActiveWorkbenchWindow(
        event).getActivePage().getSelection();

    IProject androidProject = (IProject) selection.getFirstElement();
    if (!androidProject.getFile(ANDROID_MANIFEST_FILENAME).exists()) {
      MessageDialog.openInformation(Display.getDefault().getActiveShell(),
          "Error in Generating App Engine Backend",
          "The selected project \"" + androidProject.getName() + "\" is not an Android Project.");
      return null;
    }
    String backendProjectName = BackendGenerator.getAndroidBackendProjectName(androidProject.getName());
    if (ResourcesPlugin.getWorkspace().getRoot().getProject(backendProjectName).exists()) {
      MessageDialog.openInformation(Display.getDefault().getActiveShell(),
          "Error in Generating App Engine Backend", "Connected App Engine Backend project \""
              + backendProjectName + "\" already exists.");
      return null;
    }

    BackendGeneratorWizard wizard = new BackendGeneratorWizard();
    wizard.init(androidProject, backendProjectName);
    WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
    IPixelConverter converter = PixelConverterFactory.createPixelConverter(JFaceResources.getDialogFont());
    dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70),
        converter.convertHeightInCharsToPixels(33));
    dialog.create();
    dialog.open();
    return null;
  }
}
