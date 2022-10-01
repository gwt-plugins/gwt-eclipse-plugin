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
package com.google.gwt.eclipse.core.wizards.rpc;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Modifications to the {@link NewInterfaceWizardPage} to enable us to add the
 * necessary type members to the asynchronous remote service interface.
 *
 * TODO: We might be able to use an AST based approach here. If so, we could
 * unify this with the individual method quick fixes.
 *
 * NOTE: This class assumes that the package, enclosing type and sync type
 * fields cannot be edited. Additional work will be required to change this
 * invariant.
 */
@SuppressWarnings("restriction")
public class NewAsyncRemoteServiceInterfaceCreationWizardPage extends
    NewInterfaceWizardPage {
  /**
   * Adapter that allows us to reuse the method generation code for previews
   * when there isn't a real import manager.
   */
  public interface ImportManagerAdapter {
    String addImport(ITypeBinding typeBinding);

    String addImport(String qualifiedTypeName);
  }

  private static final String[] NO_STRINGS = new String[0];

  public static List<IMethodBinding> computeSyncMethodsThatNeedAsyncVersions(
      ITypeBinding syncTypeBinding, ITypeBinding asyncTypeBinding) {
    // Compute all overridable methods on the sync interface
    List<IMethodBinding> overridableSyncMethods = computeOverridableMethodsForInterface(syncTypeBinding);

    // Remove sync methods that would override existing methods in the
    // async hierarchy
    List<IMethodBinding> remainingMethods = new ArrayList<IMethodBinding>();
    for (IMethodBinding overridableSyncMethod : overridableSyncMethods) {
      IMethod syncMethod = (IMethod) overridableSyncMethod.getJavaElement();
      String[] asyncParameterTypes = RemoteServiceUtilities.computeAsyncParameterTypes(overridableSyncMethod);
      if (Bindings.findMethodInHierarchy(asyncTypeBinding,
          syncMethod.getElementName(), asyncParameterTypes) != null) {
        // Don't add method that appear in the type hierarchy
        continue;
      }

      remainingMethods.add(overridableSyncMethod);
    }

    return remainingMethods;
  }

  public static String createMethodContents(IType newType,
      ImportManagerAdapter imports, IMethodBinding overridableSyncMethod,
      boolean addComments) throws CoreException, JavaModelException {
    StringBuilder sb = new StringBuilder();

    IMethod syncMethod = (IMethod) overridableSyncMethod.getJavaElement();

    if (addComments) {
      String lineDelimiter = "\n"; // OK, since content is formatted afterwards

      // Don't go through CodeGeneration type since it can't deal with delegates
      String comment = StubUtility.getMethodComment(
          newType.getCompilationUnit(), newType.getFullyQualifiedName(),
          syncMethod.getElementName(), NO_STRINGS, NO_STRINGS,
          Signature.SIG_VOID, NO_STRINGS, syncMethod, true, lineDelimiter);
      if (comment != null) {
        sb.append(comment);
        sb.append(lineDelimiter);
      }
    }

    // Expand the type parameters
    ITypeParameter[] typeParameters = syncMethod.getTypeParameters();
    ITypeBinding[] typeParameterBindings = overridableSyncMethod.getTypeParameters();
    if (typeParameters.length > 0) {
      sb.append("<");
      for (int i = 0; i < typeParameters.length; ++i) {
        sb.append(typeParameters[i].getElementName());
        ITypeBinding typeParameterBinding = typeParameterBindings[i];
        ITypeBinding[] typeBounds = typeParameterBinding.getTypeBounds();
        if (typeBounds.length > 0) {
          sb.append(" extends ");
          for (int j = 0; j < typeBounds.length; ++j) {
            if (j != 0) {
              sb.append(" & ");
            }
            expandTypeBinding(typeBounds[j], sb, imports);
          }
        }
      }
      sb.append(">");
    }

    // Default to a void return type for the async method
    sb.append("void ");

    // Expand the method name
    sb.append(overridableSyncMethod.getName());

    // Expand the arguments
    sb.append("(");
    String[] parameterNames = syncMethod.getParameterNames();
    ITypeBinding[] parameterTypes = overridableSyncMethod.getParameterTypes();
    for (int i = 0; i < parameterNames.length; ++i) {
      if (i != 0) {
        sb.append(", ");
      }

      expandTypeBinding(parameterTypes[i], sb, imports);
      sb.append(" ");
      sb.append(parameterNames[i]);
    }

    if (parameterNames.length > 0) {
      sb.append(", ");
    }

    sb.append(imports.addImport(RemoteServiceUtilities.ASYNCCALLBACK_QUALIFIED_NAME));
    sb.append("<");
    ITypeBinding syncReturnType = overridableSyncMethod.getReturnType();
    if (syncReturnType.isPrimitive()) {
      String wrapperTypeName = JavaASTUtils.getWrapperTypeName(syncReturnType.getName());
      sb.append(imports.addImport(wrapperTypeName));
    } else {
      expandTypeBinding(syncReturnType, sb, imports);
    }
    sb.append("> ");
    sb.append(StringUtilities.computeUniqueName(parameterNames, "callback"));
    sb.append(");");

    return sb.toString();
  }

  static void expandTypeBinding(ITypeBinding typeBinding, StringBuilder sb,
      ImportManagerAdapter importsManager) {

    if (typeBinding.isParameterizedType()) {
      expandTypeBinding(typeBinding.getErasure(), sb, importsManager);

      sb.append("<");
      ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
      for (int i = 0; i < typeArguments.length; ++i) {
        if (i != 0) {
          sb.append(", ");
        }
        expandTypeBinding(typeArguments[i], sb, importsManager);
      }

      sb.append(">");
    } else if (typeBinding.isWildcardType()) {
      ITypeBinding bound = typeBinding.getBound();
      if (bound == null) {
        sb.append("?");
      } else {
        if (typeBinding.isUpperbound()) {
          sb.append("? extends ");
        } else {
          sb.append("? super ");
        }
        expandTypeBinding(bound, sb, importsManager);
      }
    } else if (typeBinding.isTypeVariable()) {
      sb.append(typeBinding.getName());
    } else if (typeBinding.isArray()) {
      expandTypeBinding(typeBinding.getComponentType(), sb, importsManager);
      sb.append("[]");
    } else if (typeBinding.isPrimitive()) {
      sb.append(typeBinding.getName());
    } else {
      sb.append(importsManager.addImport(typeBinding));
    }
  }

  private static List<IMethodBinding> computeOverridableMethodsForInterface(
      ITypeBinding interfaceBinding) {
    assert (interfaceBinding.isInterface());

    List<ITypeBinding> superInterfaces = new ArrayList<ITypeBinding>();
    RemoteServiceUtilities.expandSuperInterfaces(interfaceBinding,
        superInterfaces);

    List<IMethodBinding> overridableMethods = new ArrayList<IMethodBinding>();
    for (ITypeBinding superInterface : superInterfaces) {
      for (IMethodBinding declaredMethod : superInterface.getDeclaredMethods()) {
        if (findOverridingMethod(declaredMethod, overridableMethods) == null) {
          overridableMethods.add(declaredMethod);
        }
      }
    }

    return overridableMethods;
  }

  /**
   * NOTE: This method comes from StubUtility2.
   */
  @SuppressWarnings("deprecation")
  private static IMethodBinding findOverridingMethod(IMethodBinding method,
      List<IMethodBinding> allMethods) {
    for (IMethodBinding cur : allMethods) {
      if (Bindings.areOverriddenMethods(cur, method)
          || Bindings.isSubsignature(cur, method)) {
        return cur;
      }
    }
    return null;
  }

  private final ITypeBinding syncTypeBinding;
  private final StringButtonDialogField syncTypeDialogField;

  public NewAsyncRemoteServiceInterfaceCreationWizardPage(
      ITypeBinding syncTypeBinding) {
    super();
    this.syncTypeBinding = syncTypeBinding;
    syncTypeDialogField = new StringButtonDialogField(
        new IStringButtonAdapter() {
          @Override
          public void changeControlPressed(DialogField field) {
            // Purposely ignored
          }
        });
    syncTypeDialogField.setButtonLabel("Browse...");
    syncTypeDialogField.setLabelText("Synchronous type:");
    syncTypeDialogField.setEnabled(false);
    syncTypeDialogField.setText(syncTypeBinding.getQualifiedName());
    ImageDescriptor imageDescriptor = GWTPlugin.getDefault().getImageRegistry().getDescriptor(
        GWTImages.NEW_ASYNC_INTERFACE_LARGE);
    setImageDescriptor(imageDescriptor);
    setDescription("Create a new asynchronous remote service interface");
  }

  @Override
  public void createControl(Composite parent) {
    initializeDialogUnits(parent);

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setFont(parent.getFont());

    int nColumns = 4;

    GridLayout layout = new GridLayout();
    layout.numColumns = nColumns;
    composite.setLayout(layout);

    createContainerControls(composite, nColumns);
    createPackageControls(composite, nColumns);
    createEnclosingTypeControls(composite, nColumns);
    createSyncTypeControls(composite, nColumns);

    createSeparator(composite, nColumns);

    createTypeNameControls(composite, nColumns);
    createModifierControls(composite, nColumns);

    createSuperInterfacesControls(composite, nColumns);

    createCommentControls(composite, nColumns);
    enableCommentControl(true);

    setControl(composite);

    Dialog.applyDialogFont(composite);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
        IJavaHelpContextIds.NEW_INTERFACE_WIZARD_PAGE);
  }

  @Override
  protected void createTypeMembers(IType newType, final ImportsManager imports,
      IProgressMonitor monitor) throws CoreException {
    TypeDeclaration asyncTypeDeclaration = JavaASTUtils.findTypeDeclaration(
        newType.getJavaProject(), newType.getFullyQualifiedName('.'));
    ITypeBinding asyncTypeBinding = asyncTypeDeclaration.resolveBinding();

    ImportManagerAdapter importAdapter = new ImportManagerAdapter() {

      @Override
      public String addImport(ITypeBinding typeBinding) {
        return imports.addImport(typeBinding);
      }

      @Override
      public String addImport(String qualifiedTypeName) {
        return imports.addImport(qualifiedTypeName);
      }
    };

    List<IMethodBinding> syncMethodsToConvert = computeSyncMethodsThatNeedAsyncVersions(
        syncTypeBinding, asyncTypeBinding);
    for (IMethodBinding overridableSyncMethod : syncMethodsToConvert) {
      String methodContents = createMethodContents(newType, importAdapter,
          overridableSyncMethod, isAddComments());

      // Create the new method
      newType.createMethod(methodContents, null, false, monitor);
    }
  }

  /**
   * Creates the controls for the sync type name field. Expects a
   * <code>GridLayout</code> with at least 4 columns.
   *
   * @param composite the parent composite
   * @param columns number of columns to span
   */
  private void createSyncTypeControls(Composite composite, int columns) {
    syncTypeDialogField.doFillIntoGrid(composite, columns);
    Text text = syncTypeDialogField.getTextControl(null);
    LayoutUtil.setWidthHint(text, getMaxFieldWidth());
  }
}
