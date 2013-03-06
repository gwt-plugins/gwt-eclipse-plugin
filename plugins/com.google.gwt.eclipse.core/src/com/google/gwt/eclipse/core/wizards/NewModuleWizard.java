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
package com.google.gwt.eclipse.core.wizards;

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard for creating a new GWT Module XML file, and its associated public path
 * directory and client package directory.
 */
@SuppressWarnings("restriction")
public class NewModuleWizard extends AbstractNewFileWizard {

  private static final String NO_VERSION_FOUND_DTD = "<!-- Could not determine the version of your GWT SDK; using the module DTD from GWT 1.6.4. You may want to change this. -->\n"
      + "<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit//EN\" \"http://google-web-toolkit.googlecode.com/svn/trunk/distro-source/core/src/gwt-module.dtd\">";

  private NewModuleWizardPage newModuleWizardPage;

  @Override
  public void addPages() {
    newModuleWizardPage = new NewModuleWizardPage();
    newModuleWizardPage.initModulePage(getSelection());
    addPage(newModuleWizardPage);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    super.init(workbench, selection);
    setHelpAvailable(false);
    setWindowTitle("New GWT Module");

    setDefaultPageImageDescriptor(GWTPlugin.getDefault().getImageDescriptor(
        GWTImages.NEW_MODULE_LARGE));
  }

  @Override
  public boolean performFinish() {

    IPackageFragmentRoot root = newModuleWizardPage.getPackageFragmentRoot();
    String packName = newModuleWizardPage.getModulePackageName();

    try {

      IPackageFragment createdPackageFragment = root.createPackageFragment(
          packName, false, new NullProgressMonitor());

      if (newModuleWizardPage.shouldCreateClientPackage()) {

        String clientPackName = null;

        if (packName == null || packName.length() == 0) {
          clientPackName = "client";
        } else {
          clientPackName = packName + ".client";
        }

        root.createPackageFragment(clientPackName, false,
            new NullProgressMonitor());
      }

      if (newModuleWizardPage.shouldCreatePublicPath()) {
        IFolder folder = (IFolder) createdPackageFragment.getCorrespondingResource();
        IFolder publicFolder = folder.getFolder("public");
        if (!publicFolder.exists()) {
          publicFolder.create(false, true, new NullProgressMonitor());
        }
      }

    } catch (Exception e) {
      MessageDialog.openError(
          getContainer().getShell(),
          "An error occurred while attempting to create a new GWT Module",
          NLS.bind(
              IDEWorkbenchMessages.WizardNewFileCreationPage_internalErrorMessage,
              e));

      GWTPluginLog.logError(
          e,
          "Unable to create new GWT Module with source folder: {0}, name: {1}, package: {2}, createClientPackage: {4}, createPublicPath: {5}",
          newModuleWizardPage.getPackageFragmentRootText(),
          newModuleWizardPage.getModuleName(),
          newModuleWizardPage.shouldCreateClientPackage(),
          newModuleWizardPage.shouldCreatePublicPath());

      return false;
    }

    // TODO: The actions of creating the client package and the public folder
    // are not undoable, even though the file creation is (since it delegates
    // to super.performFinish(), which uses an undoable file operation to create
    // the file). We should make the creation of the client package and public
    // path part of the same undoable operation.
    return super.performFinish();
  }

  @Override
  protected String getFileExtension() {
    return "gwt.xml";
  }

  @Override
  protected IPath getFilePath() {
    return newModuleWizardPage.getModulePath();
  }

  @Override
  protected InputStream getInitialContents() {
    List<String> contents = new ArrayList<String>();

    IPackageFragmentRoot root = newModuleWizardPage.getPackageFragmentRoot();
    IJavaProject javaProject = root.getJavaProject();

    contents.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    String gwtModuleDtd = NO_VERSION_FOUND_DTD;

    try {
      GWTRuntime runtime = GWTRuntime.findSdkFor(javaProject);
      if (runtime != null) {
        String versionNum = runtime.getVersion();

        if (!versionNum.endsWith(".999") && !versionNum.startsWith("0.0")) {
          gwtModuleDtd = "<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit "
              + versionNum
              + "//EN\" \"http://google-web-toolkit.googlecode.com/svn/tags/"
              + versionNum + "/distro-source/core/src/gwt-module.dtd\">";
        }
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
    }

    contents.add(gwtModuleDtd);

    contents.add("<module>");

    List<IModule> moduleInherits = newModuleWizardPage.getModuleInherits();

    // TODO: Instead of hardcoding a tab character here, we should try and use
    // the WST editor preferences for indentation. We should respect the user's
    // preferences for spaces vs. tabs, and the number of spaces that make up an
    // indentation.
    for (IModule moduleInherit : moduleInherits) {
      contents.add("\t<inherits name=\"" + moduleInherit.getQualifiedName()
          + "\" />");
    }

    // Explicitly add the source path element
    contents.add("\t<source path=\"client\"/>");

    contents.add("</module>");

    String xml = Util.join(contents, System.getProperty("line.separator"));
    ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
    return stream;
  }
}
