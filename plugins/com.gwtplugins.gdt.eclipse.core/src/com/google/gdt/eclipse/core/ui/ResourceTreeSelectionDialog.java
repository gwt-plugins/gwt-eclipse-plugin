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
package com.google.gdt.eclipse.core.ui;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects one or more workspace resources from a tree widget. All displayed
 * resources are descendants of a particular root resource (which does not
 * itself appear in the tree).
 */
public class ResourceTreeSelectionDialog extends ElementTreeSelectionDialog {

  /**
   * Generic filter that can work as either a viewer or selection validator
   * filter.
   */
  public static class ResourceFilter extends ViewerFilter implements
      ISelectionStatusValidator {

    private final int acceptedResourceTypes;

    private final boolean multiSelection;

    public ResourceFilter(int acceptedResourceTypes) {
      this(acceptedResourceTypes, false);
    }

    public ResourceFilter(int acceptedResourceTypes, boolean multiSelection) {
      this.acceptedResourceTypes = acceptedResourceTypes;
      this.multiSelection = multiSelection;
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      return isAcceptableResourceType(element);
    }

    public IStatus validate(Object[] selection) {
      if (isValid(selection)) {
        return StatusUtilities.OK_STATUS;
      }
      return StatusUtilities.newErrorStatus("", CorePlugin.PLUGIN_ID);
    }

    private boolean isAcceptableResourceType(Object element) {
      if (!(element instanceof IResource)) {
        return false;
      }

      IResource resource = (IResource) element;
      if (resource.isDerived()) {
        return false;
      }

      if (resource.getName().startsWith(".")) {
        return false;
      }

      return (resource.getType() & acceptedResourceTypes) > 0;
    }

    private boolean isValid(Object[] selection) {
      if (selection.length == 0) {
        return false;
      }

      if (!multiSelection && selection.length != 1) {
        return false;
      }

      for (Object element : selection) {
        if (!isAcceptableResourceType(element)) {
          return false;
        }
      }
      return true;
    }
  }

  private IContainer rootResource;

  /**
   * Constructs an instance of <code>ResourceTreeSelectionDialog</code>.
   * 
   * @param parent the parent shell for the dialog
   * @param title dialog title
   * @param message dialog message
   * @param rootResource resource that serves as the root of the tree. This
   *          resource's descendants are visible in the tree, but the root
   *          itself is not.
   * @param initialResource the initially-selected resource
   * @param visibleResourceTypes a set of {@link IResource} types that are
   *          visible in the tree (e.g.
   *          <code>IResource.FILE | IResource.FOLDER</code>)
   * @param acceptedResourceTypes a set of {@link IResource} types that can be
   *          selected
   * @param multiSelection whether or not to allow selection of multiple
   *          resources
   */
  public ResourceTreeSelectionDialog(Shell parent, String title,
      String message, IContainer rootResource, IResource initialResource,
      int visibleResourceTypes, int acceptedResourceTypes,
      boolean multiSelection) {
    super(parent, new WorkbenchLabelProvider(), new WorkbenchContentProvider());

    setTitle(title);
    setMessage(message);
    setHelpAvailable(false);

    setInput(rootResource);
    if (initialResource != null) {
      setInitialSelection(initialResource);
    }
    setComparator(new ResourceComparator(ResourceComparator.NAME));
    setValidator(new ResourceFilter(acceptedResourceTypes, multiSelection));
    addFilter(new ResourceFilter(visibleResourceTypes));
  }

  public List<IPath> chooseResourcePaths() {
    return chooseResourcePaths(false);
  }

  /**
   * Return the selected resource(s) as a list of paths relative to the root
   * resource.
   * 
   * @param addTrailingSeparatorToContainerPaths if <code>true</code>, all paths
   *          to containers will end with a trailing slash. This is useful when
   *          the paths are used as filter patterns, since a trailing slash will
   *          include all sub-directories (e.g. foo/ == foo/**).
   */
  public List<IPath> chooseResourcePaths(
      boolean addTrailingSeparatorToContainerPaths) {
    List<IResource> resources = chooseResources();

    if (resources != null) {
      List<IPath> paths = new ArrayList<IPath>();

      // Convert each selected IResource into a path relative to the ancestor
      for (IResource resource : resources) {
        int ancestorSegments = rootResource.getFullPath().segmentCount();
        IPath path = resource.getFullPath().removeFirstSegments(
            ancestorSegments).makeRelative();
        if (addTrailingSeparatorToContainerPaths
            && resource instanceof IContainer) {
          path = path.addTrailingSeparator();
        }
        paths.add(path);
      }

      return paths;
    }
    return null;
  }

  /**
   * Return the selected resource(s) as a list of IResource objects.
   */
  public List<IResource> chooseResources() {
    if (open() == Window.OK) {
      List<IResource> resources = new ArrayList<IResource>();
      for (Object obj : getResult()) {
        resources.add((IResource) obj);
      }
      return resources;
    }
    return null;
  }

  @Override
  public void setInput(Object input) {
    assert (input instanceof IContainer);
    rootResource = (IContainer) input;
    super.setInput(input);
  }

}
