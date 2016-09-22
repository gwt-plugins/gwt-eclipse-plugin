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
package com.google.gwt.eclipse.core.clientbundle.ui;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Selects a resource on a project's classpath to add to a ClientBundle.
 */
@SuppressWarnings("restriction")
public class ClientBundleResourceSelectionDialog extends
    ElementTreeSelectionDialog {

  private class ViewerFilter extends TypedViewerFilter {

    private final IJavaProject javaProject;

    public ViewerFilter(IJavaProject javaProject) {
      super(new Class[] {
          IJavaProject.class, IPackageFragmentRoot.class,
          IPackageFragment.class, IFile.class});
      this.javaProject = javaProject;
    }

    @Override
    public boolean select(Viewer viewer, Object parent, Object element) {
      // Let the super kick out elements of the wrong type
      boolean isValid = super.select(viewer, parent, element);
      if (!isValid) {
        return false;
      }

      try {
        if (element instanceof IJavaProject) {
          return showProject((IJavaProject) element);
        }

        if (element instanceof IPackageFragmentRoot) {
          return showPackageFragmentRoot((IPackageFragmentRoot) element);
        }

        if (element instanceof IPackageFragment) {
          return showPackage((IPackageFragment) element);
        }

        if (element instanceof IFile) {
          return showFile((IFile) element);
        }

        // Anything else is visible by default
        return true;

      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
        return false;
      }
    }

    private boolean showFile(IFile file) {
      // Only show files on the project's classpath
      if (!javaProject.isOnClasspath(file)) {
        return false;
      }

      // Ignore files that are not likely to be bundled resources
      if (!ClientBundleResource.isProbableClientBundleResource(file)) {
        return false;
      }

      // Ignore files directly under the project (.project, .classpath)
      if (file.getParent() instanceof IProject) {
        return false;
      }

      return true;
    }

    private boolean showPackage(IPackageFragment pckg)
        throws JavaModelException {
      // Ignore packages with no non-Java resources
      Object[] nonJavaResources = pckg.getNonJavaResources();
      if (nonJavaResources.length == 0) {
        return false;
      }

      // Only show packages with visible resources underneath
      for (Object nonJavaResource : nonJavaResources) {
        if (nonJavaResource instanceof IFile) {
          if (showFile((IFile) nonJavaResource)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean showPackageFragmentRoot(IPackageFragmentRoot srcRoot)
        throws JavaModelException {
      // Only show roots defined in source
      if (!(srcRoot.getKind() == IPackageFragmentRoot.K_SOURCE)) {
        return false;
      }

      // Only show roots with visible packages underneath
      for (IJavaElement child : srcRoot.getChildren()) {
        if (child instanceof IPackageFragment) {
          if (showPackage((IPackageFragment) child)) {
            // At least one package underneath this root should be visible
            return true;
          }
        }
      }
      return false;
    }

    private boolean showProject(IJavaProject project) throws JavaModelException {
      // Always show this project
      if (javaProject.equals(project)) {
        return true;
      }

      // Only show dependent projects
      if (!javaProject.isOnClasspath(project)) {
        return false;
      }

      // Only show projects with visible source roots underneath
      for (IPackageFragmentRoot srcRoot : project.getAllPackageFragmentRoots()) {
        if (showPackageFragmentRoot(srcRoot)) {
          return true;
        }
      }
      return false;
    }
  }

  public static List<IFile> getFiles(Shell shell, boolean multiSelection,
      IJavaProject javaProject, IFile initialSelection) {
    ClientBundleResourceSelectionDialog dialog = new ClientBundleResourceSelectionDialog(
        shell, javaProject, multiSelection);
    dialog.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
    dialog.setInitialSelection(initialSelection);

    if (dialog.open() == Window.OK) {
      List<IFile> files = new ArrayList<IFile>();
      for (Object object : dialog.getResult()) {
        files.add((IFile) object);
      }
      return files;
    }
    return null;
  }

  private ClientBundleResourceSelectionDialog(Shell shell,
      IJavaProject javaProject, boolean multiSelection) {
    super(shell, new JavaElementLabelProvider(
        JavaElementLabelProvider.SHOW_DEFAULT),
        new StandardJavaElementContentProvider());
    setValidator(new TypedElementSelectionValidator(new Class[] {IFile.class},
        multiSelection));
    setComparator(new JavaElementComparator());
    setTitle("Resource Selection");
    String message = MessageFormat.format("Choose {0} to bundle:",
        (multiSelection ? "one or more resources" : "a resource"));
    setMessage(message);
    addFilter(new ViewerFilter(javaProject));
  }

}