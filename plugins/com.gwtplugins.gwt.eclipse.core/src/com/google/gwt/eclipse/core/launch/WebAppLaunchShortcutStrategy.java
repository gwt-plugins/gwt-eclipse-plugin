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

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchShortcutStrategy;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.ui.WebAppHostPageSelectionDialog;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Infers the proper startup URL for WAR-based GWT and GAE projects. These
 * require a URL relative to the root of the WAR directory.
 */
public class WebAppLaunchShortcutStrategy implements ILaunchShortcutStrategy {

  public String generateUrl(IResource selection, boolean isExternal)
      throws CoreException {
    IProject project = selection.getProject();
    WebAppUtilities.verifyIsWebApp(project);

    IFolder warFolder = WebAppUtilities.getWarSrc(project);
    if (warFolder == null) {
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          "Couldn't find WAR folder."));
    }

    if (isExternal) {
      return getUrlFromUser(selection, isExternal);
    }

    if (ResourceUtils.hasJspOrHtmlExtension(selection)) {
      int warSegments = warFolder.getFullPath().segmentCount();
      IPath pathRelativeToWar = selection.getFullPath().removeFirstSegments(
          warSegments);

      return pathRelativeToWar.toString();
    }

    List<IResource> candidates = new ArrayList<IResource>();

    for (IResource member : warFolder.members()) {
      if (ResourceUtils.hasJspOrHtmlExtension(member)) {
        candidates.add(member);
      }
    }

    if (candidates.isEmpty()) {
      // NOTE: This can happen for a gae-only project that only has a servlet.
      return "";
    } else if (candidates.size() == 1) {
      return candidates.get(0).getName();
    } else {
      return getUrlFromUser(selection, isExternal);
    }
  }

  public String getUrlFromUser(IResource resource, boolean isExternal) {
    IJavaProject javaProject = JavaCore.create(resource.getProject());
    return WebAppHostPageSelectionDialog.show(javaProject, isExternal);
  }
}
