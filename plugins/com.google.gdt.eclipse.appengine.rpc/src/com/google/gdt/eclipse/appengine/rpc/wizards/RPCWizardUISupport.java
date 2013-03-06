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
package com.google.gdt.eclipse.appengine.rpc.wizards;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;
import com.google.gdt.eclipse.appengine.rpc.util.JavaUtils;
import com.google.gdt.eclipse.appengine.rpc.util.StatusUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import java.util.Collection;

/**
 * TODO: doc me.
 */
@SuppressWarnings("restriction")
public class RPCWizardUISupport {

  /**
   * Implementation of a <code>ISelectionValidator</code> to validate the type
   * of an element. Empty selections are not accepted.
   */
  private static class TypedElementSelectionValidator implements
      ISelectionStatusValidator {

    private Class<?>[] acceptedTypes;
    private boolean allowMultipleSelection;

    private IStatus fgErrorStatus = StatusUtils.createStatus(IStatus.ERROR, ""); //$NON-NLS-1$
    private IStatus fgOKStatus = StatusUtils.createOKStatus();
    private Collection<?> rejectedElements;

    /**
     * @param acceptedTypes The types accepted by the validator
     * @param allowMultipleSelection If set to <code>true</code>, the validator
     *          allows multiple selection.
     */
    public TypedElementSelectionValidator(Class<?>[] acceptedTypes,
        boolean allowMultipleSelection) {
      this(acceptedTypes, allowMultipleSelection, null);
    }

    /**
     * @param acceptedTypes The types accepted by the validator
     * @param allowMultipleSelection If set to <code>true</code>, the validator
     *          allows multiple selection.
     * @param rejectedElements A list of elements that are not accepted
     */
    public TypedElementSelectionValidator(Class<?>[] acceptedTypes,
        boolean allowMultipleSelection, Collection<?> rejectedElements) {
      Assert.isNotNull(acceptedTypes);
      this.acceptedTypes = acceptedTypes;
      this.allowMultipleSelection = allowMultipleSelection;
      this.rejectedElements = rejectedElements;
    }

    /*
     * @see org.eclipse.ui.dialogs.ISelectionValidator#isValid(java.lang.Object)
     */
    public IStatus validate(Object[] elements) {
      if (isValid(elements)) {
        return fgOKStatus;
      }
      return fgErrorStatus;
    }

    /**
     * @param elem the element to test
     * @return returns if the selected element is valid
     */
    protected boolean isSelectedValid(Object elem) {
      return true;
    }

    private boolean isOfAcceptedType(Object o) {
      for (int i = 0; i < acceptedTypes.length; i++) {
        if (acceptedTypes[i].isInstance(o)) {
          return true;
        }
      }
      return false;
    }

    private boolean isRejectedElement(Object elem) {
      return (rejectedElements != null) && rejectedElements.contains(elem);
    }

    private boolean isValid(Object[] selection) {
      if (selection.length == 0) {
        return false;
      }

      if (!allowMultipleSelection && selection.length != 1) {
        return false;
      }

      for (int i = 0; i < selection.length; i++) {
        Object o = selection[i];
        if (!isOfAcceptedType(o) || isRejectedElement(o) || !isSelectedValid(o)) {
          return false;
        }
      }
      return true;
    }
  }

  private static class TypedViewerFilter extends ViewerFilter {

    private Class<?>[] acceptedTypes;
    private Object[] rejectedElements;

    /**
     * Creates a filter that only allows elements of gives types.
     * 
     * @param acceptedTypes The types of accepted elements
     */
    public TypedViewerFilter(Class<?>[] acceptedTypes) {
      this(acceptedTypes, null);
    }

    /**
     * Creates a filter that only allows elements of gives types, but not from a
     * list of rejected elements.
     * 
     * @param acceptedTypes Accepted elements must be of this types
     * @param rejectedElements Element equals to the rejected elements are
     *          filtered out
     */
    public TypedViewerFilter(Class<?>[] acceptedTypes, Object[] rejectedElements) {
      Assert.isNotNull(acceptedTypes);
      this.acceptedTypes = acceptedTypes;
      this.rejectedElements = rejectedElements;
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if (rejectedElements != null) {
        for (Object rejectedElement : rejectedElements) {
          if (element.equals(rejectedElement)) {
            return false;
          }
        }
      }
      for (Class<?> acceptedType : acceptedTypes) {
        if (acceptedType.isInstance(element)) {
          return true;
        }
      }
      return false;
    }
  }

  private static boolean isAppEngineProject(IProject project) {
    try {
      return project.hasNature(GaeNature.NATURE_ID);
    } catch (CoreException e) {
      return false;
    }
  }

  private final IShellProvider shellProvider;

  public RPCWizardUISupport(IShellProvider shellProvider) {
    this.shellProvider = shellProvider;
  }

  public IPackageFragmentRoot chooseContainer(IType type) {

    Class<?>[] acceptedClasses = new Class[] {
        IPackageFragmentRoot.class, IJavaProject.class};
    TypedElementSelectionValidator validator = new TypedElementSelectionValidator(
        acceptedClasses, false) {
      @Override
      public boolean isSelectedValid(Object element) {
        try {
          if (element instanceof IJavaProject) {
            IJavaProject jproject = (IJavaProject) element;
            IPath path = jproject.getProject().getFullPath();
            return (jproject.findPackageFragmentRoot(path) != null);
          } else if (element instanceof IPackageFragmentRoot) {
            return (((IPackageFragmentRoot) element).getKind() == IPackageFragmentRoot.K_SOURCE);
          }
          return true;
        } catch (JavaModelException e) {
          AppEngineRPCPlugin.log(e);
        }
        return false;
      }
    };

    acceptedClasses = new Class[] {
        IJavaModel.class, IPackageFragmentRoot.class, IJavaProject.class};
    ViewerFilter filter = new TypedViewerFilter(acceptedClasses) {
      @Override
      public boolean select(Viewer viewer, Object parent, Object element) {

        // only show appengine projects
        if (element instanceof IJavaProject) {
          IJavaProject jp = (IJavaProject) element;
          return isAppEngineProject(jp.getProject());
        }

        // only show source folders
        if (element instanceof IPackageFragmentRoot) {
          try {
            return (((IPackageFragmentRoot) element).getKind() == IPackageFragmentRoot.K_SOURCE);
          } catch (JavaModelException e) {
            AppEngineRPCPlugin.log(e);
            return false;
          }
        }
        return super.select(viewer, parent, element);
      }
    };

    StandardJavaElementContentProvider provider = new StandardJavaElementContentProvider();
    ILabelProvider labelProvider = new JavaElementLabelProvider(
        JavaElementLabelProvider.SHOW_DEFAULT);
    ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
        getShell(), labelProvider, provider);
    dialog.setValidator(validator);
    dialog.setComparator(new JavaElementComparator());
    dialog.setTitle("Source Folder Selection"); //$NON-NLS-1$
    dialog.setMessage("&Choose a source folder:"); //$NON-NLS-1$
    dialog.addFilter(filter);
    dialog.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
    if (type != null) {
      dialog.setInitialSelection(JavaUtils.getPackageFragmentRoot(type));
    }
    dialog.setHelpAvailable(false);

    if (dialog.open() == Window.OK) {
      Object element = dialog.getFirstResult();
      if (element instanceof IJavaProject) {
        IJavaProject jproject = (IJavaProject) element;
        return jproject.getPackageFragmentRoot(jproject.getProject());
      } else if (element instanceof IPackageFragmentRoot) {
        return (IPackageFragmentRoot) element;
      }
      return null;
    }
    return null;
  }

  protected IPackageFragment choosePackage(IPackageFragmentRoot containerRoot) {
    IJavaElement[] packages = null;
    try {
      if (containerRoot != null && containerRoot.exists()) {
        packages = containerRoot.getChildren();
      }
    } catch (JavaModelException e) {
      AppEngineRPCPlugin.log(e);
    }
    if (packages == null) {
      packages = new IJavaElement[0];
    }

    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(), new JavaElementLabelProvider(
            JavaElementLabelProvider.SHOW_DEFAULT));
    dialog.setIgnoreCase(false);
    dialog.setTitle(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_title);
    dialog.setMessage(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_description);
    dialog.setEmptyListMessage(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_empty);
    dialog.setElements(packages);
    dialog.setHelpAvailable(false);

    if (dialog.open() == Window.OK) {
      return (IPackageFragment) dialog.getFirstResult();
    }
    return null;
  }

  private Shell getShell() {
    return shellProvider.getShell();
  }

}
