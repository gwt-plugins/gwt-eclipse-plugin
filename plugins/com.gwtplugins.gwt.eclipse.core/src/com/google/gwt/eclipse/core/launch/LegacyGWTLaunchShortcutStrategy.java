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
package com.google.gwt.eclipse.core.launch;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.launch.ILaunchShortcutStrategy;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.ui.LegacyGWTHostPageSelectionDialog;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleFile;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Infers the proper startup URL for legacy (v < 1.5) GWT projects. These
 * require a URL of the form: fully.qualified.ModuleName/path/to/HostPage.html
 */
public class LegacyGWTLaunchShortcutStrategy implements ILaunchShortcutStrategy {

  public static IPath getPublicRelativeHostPagePath(IFile module, IFile hostPage) {
    List<IFolder> publicFolders = getModulePublicFolders(module);

    for (IFolder publicFolder : publicFolders) {
      if (publicFolder.getFullPath().isPrefixOf(hostPage.getFullPath())) {
        return hostPage.getFullPath().removeFirstSegments(
            publicFolder.getFullPath().segmentCount());
      }
    }

    return null;
  }

  private static Set<IFile> findHostPages(IFile moduleFile) {
    List<IFolder> publicFolders = getModulePublicFolders(moduleFile);
    if (publicFolders.isEmpty()) {
      return Collections.emptySet();
    }

    final Set<IFile> hostPages = new HashSet<IFile>();
    try {
      for (IFolder publicFolder : publicFolders) {
        publicFolder.accept(new IResourceVisitor() {
          public boolean visit(IResource resource) throws CoreException {
            // Look for any HTML files
            if (resource.getType() == IResource.FILE
                && "html".equalsIgnoreCase(resource.getFileExtension())) {
              hostPages.add((IFile) resource);
            }
            return true;
          }
        });
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }

    return hostPages;
  }

  private static String generateUrl(IFile module, IFile hostPage) {
    String moduleName = ModuleUtils.create(module).getQualifiedName();
    return new Path(moduleName).append(
        getPublicRelativeHostPagePath(module, hostPage)).toString();
  }

  private static List<IFolder> getModulePublicFolders(IFile moduleFile) {
    IModule module = ModuleUtils.create(moduleFile);
    List<IFolder> publicFolders = new ArrayList<IFolder>();
    for (IPath publicPath : module.getPublicPaths()) {
      publicFolders.add(moduleFile.getParent().getFolder(publicPath));
    }

    return publicFolders;
  }

  private static Shell getShell() {
    return GWTPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
  }

  private final Map<IFile, Set<IFile>> allHostPagesByModule = new HashMap<IFile, Set<IFile>>();

  private IFile selectedHostPage;

  private IFile selectedModule;

  public String generateUrl(IResource selection, boolean isExternal) {
    IProject project = selection.getProject();
    assert (GWTNature.isGWTProject(project));

    findAllHostPages(project);

    // TODO: differs from behavior of Web Archive projects in this case, which
    // return a URL to the root of the war directory

    // We couldn't find any host pages, so nothing to launch
    if (allHostPagesByModule.size() == 0) {
      MessageDialog.openError(getShell(), "Launch failed",
          "Could not find any host pages in project " + project.getName() + ".");
      return null;
    }

    inferModuleAndHostPage(selection);

    if (selectedModule != null && selectedHostPage != null && !isExternal) {
      // If we have a module and host page, build the URL
      return generateUrl(selectedModule, selectedHostPage);
    }

    // If we couldn't infer the module and host page, ask the user
    return getUrlFromUser(project, isExternal);
  }

  public String getUrlFromUser(IResource resource, boolean isExternal) {
    findAllHostPages(resource.getProject());

    return getUrlFromUser(resource.getProject(), isExternal);
  }

  private Map<String, Set<String>> convertFromFileMapToStringMap(
      Map<IFile, Set<IFile>> hostPagesByModule) {
    Map<String, Set<String>> moduleToPublicRelativeHostPagesStr = new HashMap<String, Set<String>>();

    for (Entry<IFile, Set<IFile>> entry : hostPagesByModule.entrySet()) {
      Set<String> publicRelativeHostPagesForModule = new HashSet<String>();
      for (IFile hostPage : entry.getValue()) {
        IPath publicRelativeHostPagePath = getPublicRelativeHostPagePath(
            entry.getKey(), hostPage);
        if (publicRelativeHostPagePath != null) {
          publicRelativeHostPagesForModule.add(publicRelativeHostPagePath.toString());
        }
      }
      IModule module = ModuleUtils.create(entry.getKey());
      moduleToPublicRelativeHostPagesStr.put(module.getQualifiedName(),
          publicRelativeHostPagesForModule);
    }

    return moduleToPublicRelativeHostPagesStr;
  }

  private void findAllHostPages(IProject project) {
    IModule[] projectModules = ModuleUtils.findAllModules(
        JavaCore.create(project), false);

    for (IModule projectModule : projectModules) {
      IFile projectModuleFile = ((ModuleFile) projectModule).getFile();
      Set<IFile> moduleHostPages = findHostPages(projectModuleFile);
      if (moduleHostPages.size() > 0) {
        allHostPagesByModule.put(projectModuleFile, moduleHostPages);
      }
    }
  }

  private String getUrlFromUser(IProject project, boolean isExternal) {
    Map<IFile, Set<IFile>> hostPagesByModule = allHostPagesByModule;

    if (selectedModule != null) {
      // If we already know which module we're launching, filter the dialog to
      // only show that module's host pages
      hostPagesByModule = new HashMap<IFile, Set<IFile>>();
      hostPagesByModule.put(selectedModule,
          allHostPagesByModule.get(selectedModule));
    }

    if (selectedModule != null) {
      // If we already know which module we're launching, filter the dialog to
      // only show that module's host pages
      hostPagesByModule = new HashMap<IFile, Set<IFile>>();
      hostPagesByModule.put(selectedModule,
          allHostPagesByModule.get(selectedModule));
    }

    return LegacyGWTHostPageSelectionDialog.getStartupUrl(getShell(), project,
        convertFromFileMapToStringMap(hostPagesByModule), isExternal);
  }

  private void inferFromHostPage(IResource selection) {
    this.selectedHostPage = (IFile) selection;

    IFile hostPageModule = null;

    for (IFile module : allHostPagesByModule.keySet()) {
      if (allHostPagesByModule.get(module).contains(this.selectedHostPage)) {
        if (hostPageModule == null) {
          hostPageModule = module;
        } else {
          // Host page belongs to more than one module, so we don't know which
          // one to use
          return;
        }
      }
    }

    // Host page is contained by just one module, so use it
    this.selectedModule = hostPageModule;
  }

  private void inferFromModule(IResource selection) {
    this.selectedModule = (IFile) selection;

    Set<IFile> hostPages = allHostPagesByModule.get(this.selectedModule);
    if (hostPages.size() == 1) {
      // If this module has exactly one host page, use it
      this.selectedHostPage = (IFile) hostPages.toArray()[0];
    }
  }

  private void inferFromProject(IProject project) {
    if (allHostPagesByModule.size() == 1) {
      // If there's exactly one module in the project, select it
      this.selectedModule = (IFile) allHostPagesByModule.keySet().toArray()[0];
      Set<IFile> selectedModulesHostPages = allHostPagesByModule.get(this.selectedModule);
      if (selectedModulesHostPages.size() == 1) {
        // If that module has exactly one host page in the project, select it
        this.selectedHostPage = (IFile) selectedModulesHostPages.toArray()[0];
      }
    }
  }

  private void inferModuleAndHostPage(IResource selection) {
    if (selection.getType() == IResource.FILE) {
      if ("html".equalsIgnoreCase(selection.getFileExtension())) {
        for (IFile module : allHostPagesByModule.keySet()) {
          if (allHostPagesByModule.get(module).contains(selection)) {
            // Try to infer the module from the host page
            inferFromHostPage(selection);
          }
        }
      } else if (ModuleUtils.isModuleXml(selection)) {
        if (allHostPagesByModule.containsKey(selection)) {
          // Try to infer the host page from the selected module
          inferFromModule(selection);
        }
      }
    } else if (selection.getType() == IResource.PROJECT) {
      // Try to infer the module and host page from the project
      inferFromProject(selection.getProject());
    }
  }
}