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
package com.google.gdt.eclipse.appengine.rpc.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO: doc me.
 */
@SuppressWarnings("restriction")
public class JavaUtils {

  public static Iterable<IMethod> asMethods(Object[] elements) {
    List<IMethod> methods = new ArrayList<IMethod>();
    for (Object elem : elements) {
      if (elem instanceof IMethod) {
        methods.add((IMethod) elem);
      }
    }
    return methods;
  }

  public static Iterable<IType> asTypes(Object[] elements) {
    List<IType> types = new ArrayList<IType>();
    for (Object elem : elements) {
      if (elem instanceof IType) {
        types.add((IType) elem);
      }
    }
    return types;
  }

  /**
   * Creates a package fragment root from a path. The path should be
   * pre-validated using {@link #validateContainer(String)},
   */
  public static IPackageFragmentRoot createContainer(String containerPath) {
    IPath path = new Path(containerPath);
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IResource res = workspaceRoot.findMember(path);
    if (res != null) {
      int resType = res.getType();
      if (resType == IResource.PROJECT || resType == IResource.FOLDER) {
        IProject proj = res.getProject();
        IJavaProject jproject = JavaCore.create(proj);
        return jproject.getPackageFragmentRoot(res);
      }
    }
    return null;
  }

  public static Image getFieldImage(int flags) {
    // TODO: can we remove this discouraged reference?
    ImageDescriptor desc = JavaElementImageProvider.getFieldImageDescriptor(
        false, Flags.AccPublic);
    int adornmentFlags = Flags.isStatic(flags)
        ? JavaElementImageDescriptor.STATIC : 0;
    desc = new JavaElementImageDescriptor(desc, adornmentFlags,
        JavaElementImageProvider.BIG_SIZE);
    // TODO: can we remove this discouraged reference?
    return JavaPlugin.getImageDescriptorRegistry().get(desc);
  }

  public static IMethod getGetter(IField field) throws JavaModelException {
    return GetterSetterUtil.getGetter(field);
  }

  public static String getGetterName(IField field, String[] excludedNames)
      throws JavaModelException {
    return GetterSetterUtil.getGetterName(field, excludedNames);
  }

  /**
   * Returns the package fragment root of <code>IJavaElement</code>. If the
   * given element is already a package fragment root, the element itself is
   * returned.
   * 
   * @param element the element
   * @return the package fragment root of the element or <code>null</code>
   */
  public static IPackageFragmentRoot getPackageFragmentRoot(IJavaElement element) {
    return (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
  }

  public static String getPackageFragmentRootText(
      IPackageFragmentRoot packageFragmentRoot) {
    return (packageFragmentRoot == null)
        ? "" : packageFragmentRoot.getPath().makeRelative().toString(); //$NON-NLS-1$
  }

  public static String getPackageFragmentText(IPackageFragment pkg) {
    return pkg.getElementName();
  }

  public static Set<String> getParamsAndReturnTypeNames(IMethod method)
      throws JavaModelException {
    Set<String> typeNames = new HashSet<String>();
    typeNames.addAll(getElementSignatures(method.getReturnType()));
    String[] params = method.getParameterTypes();
    for (String string : params) {
      typeNames.addAll(getElementSignatures(string));
    }
    return typeNames;
  }

  public static List<IMethod> getPublicMethods(IType type)
      throws JavaModelException {
    IMethod[] methods = type.getMethods();
    List<IMethod> methodList = new ArrayList<IMethod>();
    for (IMethod method : methods) {
      if (Flags.isPublic(method.getFlags())) {
        methodList.add(method);
      }
    }
    return methodList;
  }

  public static IMethod getSetter(IField field) throws JavaModelException {
    return GetterSetterUtil.getSetter(field);
  }

  public static String getSetterName(IField field, String[] excludedNames)
      throws JavaModelException {
    return GetterSetterUtil.getSetterName(field, excludedNames);
  }

  /**
   * Get the source folder for this project. If there is a source folder called
   * "src" it will be returned. If not, the first source folder found will be
   * returned (or <code>null</code> if none is found).
   */
  public static IPackageFragmentRoot getSourcePackageFragmentRoot(
      IJavaProject javaProject) throws JavaModelException {

    IPackageFragmentRoot firstSourceFolder = null;
    for (IPackageFragmentRoot fragmentRoot : javaProject.getPackageFragmentRoots()) {
      if (fragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
        // prefer "src"
        if (fragmentRoot.getElementName().equals("src")) { //$NON-NLS-1$
          return fragmentRoot;
        }
        // cache first found
        if (firstSourceFolder != null) {
          firstSourceFolder = fragmentRoot;
        }
      }
    }
    // fall back on first
    return firstSourceFolder;
  }

  public static boolean hasParams(IMethod method) throws JavaModelException {
    return method.getParameterNames().length > 0;
  }

  public static boolean isBooleanReturnType(String returnType) {
    return Signature.SIG_BOOLEAN.endsWith(returnType)
        || returnType.equals("QBoolean;"); //$NON-NLS-1$
  }

  public static boolean isPrefixedBy(String methodName, String prefix) {
    return methodName.startsWith(prefix)
        && methodName.length() > prefix.length()
        && Character.isUpperCase(methodName.charAt(prefix.length()));
  }

  public static boolean isPublicInstanceMethod(IMethod method)
      throws JavaModelException {
    return Flags.isPublic(method.getFlags())
        && !Flags.isStatic(method.getFlags());
  }

  public static IStatus validateContainer(String packageRootText) {
    StatusInfo status = new StatusInfo();

    String str = packageRootText;
    if (str.length() == 0) {
      status.setError("Source folder name is empty."); //$NON-NLS-1$
      return status;
    }
    IPath path = new Path(str);
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IResource res = workspaceRoot.findMember(path);
    if (res != null) {
      int resType = res.getType();
      if (resType == IResource.PROJECT || resType == IResource.FOLDER) {
        IProject proj = res.getProject();
        if (!proj.isOpen()) {
          status.setError(Messages.format(
              NewWizardMessages.NewContainerWizardPage_error_ProjectClosed,
              BasicElementLabels.getPathLabel(proj.getFullPath(), false)));
          return status;
        }
        IJavaProject jproject = JavaCore.create(proj);
        IPackageFragmentRoot root = jproject.getPackageFragmentRoot(res);
        if (res.exists()) {
          if (isVirtual(res)) {
            status.setError("Source folder cannot be a virtual folder."); //$NON-NLS-1$
            return status;
          }
          try {
            if (!proj.hasNature(JavaCore.NATURE_ID)) {
              if (resType == IResource.PROJECT) {
                status.setError(NewWizardMessages.NewContainerWizardPage_warning_NotAJavaProject);
              } else {
                status.setWarning(NewWizardMessages.NewContainerWizardPage_warning_NotInAJavaProject);
              }
              return status;
            }
            if (root.isArchive()) {
              status.setError(Messages.format(
                  NewWizardMessages.NewContainerWizardPage_error_ContainerIsBinary,
                  BasicElementLabels.getPathLabel(path, false)));
              return status;
            }
            if (root.getKind() == IPackageFragmentRoot.K_BINARY) {
              status.setWarning(Messages.format(
                  NewWizardMessages.NewContainerWizardPage_warning_inside_classfolder,
                  BasicElementLabels.getPathLabel(path, false)));
            } else if (!jproject.isOnClasspath(root)) {
              status.setWarning(Messages.format(
                  NewWizardMessages.NewContainerWizardPage_warning_NotOnClassPath,
                  BasicElementLabels.getPathLabel(path, false)));
            }
          } catch (JavaModelException e) {
            status.setWarning(NewWizardMessages.NewContainerWizardPage_warning_NotOnClassPath);
          } catch (CoreException e) {
            status.setWarning(NewWizardMessages.NewContainerWizardPage_warning_NotAJavaProject);
          }
        }
        return status;
      } else {
        status.setError(Messages.format(
            NewWizardMessages.NewContainerWizardPage_error_NotAFolder,
            BasicElementLabels.getPathLabel(path, false)));
        return status;
      }
    } else {
      status.setError(Messages.format(
          NewWizardMessages.NewContainerWizardPage_error_ContainerDoesNotExist,
          BasicElementLabels.getPathLabel(path, false)));
      return status;
    }
  }

  public static IStatus validateJavaTypeName(String name) {
    return JavaConventions.validateJavaTypeName(name, JavaCore.VERSION_1_3,
        JavaCore.VERSION_1_3);
  }

  public static IStatus validatePackageName(String text) {
    return JavaConventions.validatePackageName(text, JavaCore.VERSION_1_5,
        JavaCore.VERSION_1_5);
  }

  private static List<String> getElementSignatures(String typeName) {
    List<String> list = new ArrayList<String>();
    if (Signature.getTypeSignatureKind(typeName) == Signature.ARRAY_TYPE_SIGNATURE) {
      list.add(Signature.getElementType(typeName));
    } else {
      String[] names = Signature.getTypeArguments(typeName);
      list.add(Signature.getTypeErasure(typeName));
      if (names.length > 0) {
        list.add(names[0]);
      }
    }
    return list;
  }

  private static boolean isVirtual(IResource res) {
    // Virtual folders were added to eclipse in 3.6 so we need to do this
    // reflectively to play nice with 3.5
    for (Method method : IResource.class.getMethods()) {
      if ("isVirtual".equals(method.getName())) { //$NON-NLS-1$
        try {
          return (Boolean) method.invoke(res, (Object[]) null);
        } catch (IllegalArgumentException e) {
          // ignore
        } catch (IllegalAccessException e) {
          // ignore
        } catch (InvocationTargetException e) {
          // ignore
        }
      }
    }
    return false;
  }

}
