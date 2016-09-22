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

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleFile;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a new HTML host page.
 */
public class NewHostPageWizard extends AbstractNewFileWizard {

  private static IPath getPathRelativeToAncestor(IPath path, IPath ancestor) {
    if (ancestor.isPrefixOf(path)) {
      return path.removeFirstSegments(ancestor.segmentCount());
    }
    return null;
  }

  private static String getRelativeUrl(IPath fromFile, IPath refFile) {
    IPath fromContainer = fromFile.removeLastSegments(1);
    IPath refFileContainer = refFile.removeLastSegments(1);

    // Find the paths' common ancestor
    IPath commonAncestor = new Path("");
    int matchingSegments = fromContainer.matchingFirstSegments(refFileContainer);
    for (int i = 0; i < matchingSegments; i++) {
      commonAncestor = commonAncestor.append(fromContainer.segment(i));
    }

    // Walk up until we get to the common ancestor
    StringBuilder sb = new StringBuilder();
    int fromLevelsFromCommon = fromContainer.segmentCount() - matchingSegments;
    for (int i = 0; i < fromLevelsFromCommon; i++) {
      sb.append("../");
    }

    // Now add on the path to the referenced file, relative to the ancestor
    IPath refFilePathRelativeToCommon = getPathRelativeToAncestor(refFile,
        commonAncestor);
    sb.append(refFilePathRelativeToCommon.toString());

    return sb.toString();
  }

  private NewHostPageWizardPage wizardPage;

  @Override
  public void addPages() {
    wizardPage = new NewHostPageWizardPage();
    wizardPage.init(getInitialSelectedResource());
    addPage(wizardPage);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    super.init(workbench, selection);
    setHelpAvailable(false);
    setWindowTitle("New HTML Page");
    setDefaultPageImageDescriptor(GWTPlugin.getDefault().getImageDescriptor(
        GWTImages.NEW_HOST_PAGE_LARGE));
  }

  @Override
  public boolean performFinish() {
    IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(
        wizardPage.getPath());
    if (!folder.exists()) {
      try {
        folder.create(true, true, null);
      } catch (CoreException e) {
        GWTPluginLog.logError(e);
        return false;
      }
    }

    return super.performFinish();
  }

  @Override
  protected String getFileExtension() {
    return "html";
  }

  @Override
  protected IPath getFilePath() {
    IPath containerPath = wizardPage.getPath();
    IPath pagePath = containerPath.append(wizardPage.getFileName());
    return pagePath;
  }

  @Override
  protected InputStream getInitialContents() {
    List<String> startupScriptPaths = new ArrayList<String>();

    for (ModuleFile moduleFile : wizardPage.getModules()) {
      String startupScriptPath = determineStartupScriptPath(getFilePath(),
          moduleFile);
      if (startupScriptPath != null) {
        startupScriptPaths.add(startupScriptPath);
      } else {
        GWTPluginLog.logWarning("Could not calculate startup script reference for module "
            + moduleFile.getQualifiedName() + "; skipping.");
      }
    }

    // TODO: get this template out of GWT runtime

    StringBuilder sb = new StringBuilder();
    sb.append(getDocType() + "\n");
    sb.append("<html>\n");
    sb.append("  <head>\n");
    sb.append("    <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">\n");

    // Inject title
    String hostPageName = new Path(wizardPage.getFileName()).removeFileExtension().toString();
    sb.append("    <title>" + hostPageName + "</title>\n");

    // Inject startup script tags
    for (String startupScriptPath : startupScriptPaths) {
      sb.append("    <script type=\"text/javascript\" language=\"javascript\" src=\""
          + startupScriptPath + "\"></script>\n");
    }
    if (startupScriptPaths.size() == 0) {
      sb.append("    <!-- Could not find GWT module loading script. Is this page in a valid location? -->\n");
      sb.append("    <!-- <script type=\"text/javascript\" language=\"javascript\" src=\"(gwt module name here).nocache.js\"></script> -->\n");
    }

    sb.append("  </head>\n");
    sb.append("\n");
    sb.append("  <body>\n");

    // Inject history frame, if desired
    if (wizardPage.isHistorySupportIncluded()) {
      sb.append("    <iframe src=\"javascript:''\" id=\"__gwt_historyFrame\" tabIndex='-1' style=\"position:absolute;width:0;height:0;border:0\"></iframe>\n");
    }

    sb.append("\n");
    sb.append("  </body>\n");
    sb.append("</html>");

    String html = sb.toString();
    ByteArrayInputStream stream = new ByteArrayInputStream(html.getBytes());
    return stream;
  }

  /**
   * Given the workspace-relative path for the HTML host page and a GWT Module,
   * determine the relative path for loading that GWT Module in the host page.
   * This may return null in the case of an unexpected (ie, likely-to-fail)
   * location for the host page; callers should be able to handle this.
   */
  private String determineStartupScriptPath(IPath workspaceRelativeHtmlPath,
      ModuleFile referencedModule) {

    IPath outRelativeHtmlPath = getOutDirRelativeHtmlPath(workspaceRelativeHtmlPath);
    if (outRelativeHtmlPath == null) {
      // The HTML page doesn't live in a place that we expect; in this case, we
      // can't determine the relative URL of the startup script. Bail out here.
      return null;
    }
    IPath outRelativeStartupScriptPath = getOutDirRelativeStartupScriptPath(referencedModule);

    // Find the path delta (html file -> module)
    return getRelativeUrl(outRelativeHtmlPath, outRelativeStartupScriptPath);
  }

  /**
   * Determines the doctype to use by looking at the project's current GWT SDK
   * and seeing what WebAppCreator uses.
   */
  private String getDocType() {
    try {
      IProject project = wizardPage.getProject();
      GWTRuntime sdk = GWTRuntime.findSdkFor(JavaCore.create(project));
      if (sdk != null) {
        URLClassLoader cl = sdk.createClassLoader();
        String hostPageTemplate = ResourceUtils.getResourceAsString(
            cl,
            "com/google/gwt/user/tools/templates/sample/_warFolder_/_moduleShortName_.htmlsrc");
        int firstLineEnding = hostPageTemplate.indexOf('\n');
        if (firstLineEnding > -1) {
          String firstLine = hostPageTemplate.substring(0, firstLineEnding);
          if (firstLine.toUpperCase().contains("DOCTYPE")) {
            return firstLine.trim();
          }
        }
      }
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
    } catch (MalformedURLException e) {
      GWTPluginLog.logError(e);
    } catch (SdkException e) {
      GWTPluginLog.logError(e);
    }

    // Use standards-mode doctype by default
    return "<!doctype html>";
  }

  private IPath getOutDirRelativeHtmlPath(IPath workspaceRelativeHtmlPath) {
    IProject project = wizardPage.getProject();

    // If the HTML file will end up in the war directory, calculate its path
    // relative to the war directory (which will be the output directory)
    if (WebAppUtilities.isWebApp(project)) {
      IFolder warFolder = WebAppUtilities.getWarSrc(project);
      if (warFolder.exists()) {
        IPath warRelativePath = getPathRelativeToAncestor(
            workspaceRelativeHtmlPath, warFolder.getFullPath());
        if (warRelativePath != null) {
          return warRelativePath;
        }
      }
    }

    // If the HTML file is inside a module's public folder, calculate its path
    // relative to the output directory
    for (IModule module : ModuleUtils.findAllModules(
        JavaCore.create(wizardPage.getProject()), false)) {
      ModuleFile moduleFile = (ModuleFile) module;
      IPath moduleContainerPath = moduleFile.getFile().getParent().getFullPath();

      // Check each of the module's public paths
      for (IPath modulePublicPath : module.getPublicPaths()) {
        IPath workspaceRelativePublicPath = moduleContainerPath.append(modulePublicPath);
        IPath publicRelativeHtmlPath = getPathRelativeToAncestor(
            workspaceRelativeHtmlPath, workspaceRelativePublicPath);
        if (publicRelativeHtmlPath != null) {
          return new Path(moduleFile.getCompiledName()).append(publicRelativeHtmlPath);
        }
      }
    }

    // We can't figure out where in the output directory this HTML file will end
    // up (or if it even *will* end up in the output).
    return null;
  }

  private IPath getOutDirRelativeStartupScriptPath(ModuleFile referencedModule) {
    // Ask the module what it's compiled name will be (this will be the fully
    // qualified name unless a rename-to specifies something else)
    String compiledName = referencedModule.getCompiledName();

    // Path is: moduleName/moduleName.nocache.js
    return new Path(compiledName).append(compiledName + ".nocache.js");
  }
}
