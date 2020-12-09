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
package com.google.gwt.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.platform.jdt.text.correction.proposals.NewCompilationUnitUsingWizardProposal;
import com.google.gdt.eclipse.platform.shared.ui.IPixelConverter;
import com.google.gdt.eclipse.platform.ui.PixelConverterFactory;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceUtilities;
import com.google.gwt.eclipse.core.wizards.rpc.NewAsyncRemoteServiceInterfaceCreationWizard;
import com.google.gwt.eclipse.core.wizards.rpc.NewAsyncRemoteServiceInterfaceCreationWizardPage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.CorextMessages;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.jdt.internal.corext.util.ValidateEditException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.TextEdit;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Creates an RPC async interface for an existing sync interface.
 *
 * TODO: Can I just extend ChangeCorrectionProposal?
 */
@SuppressWarnings("restriction")
public class CreateAsyncInterfaceProposal extends NewCompilationUnitUsingWizardProposal {
  public static List<IJavaCompletionProposal> createProposals(IInvocationContext context, IProblemLocation problem)
      throws JavaModelException {
    String syncTypeName = problem.getProblemArguments()[0];
    IJavaProject javaProject = context.getCompilationUnit().getJavaProject();
    IType syncType = JavaModelSearch.findType(javaProject, syncTypeName);
    if (syncType == null || !syncType.isInterface()) {
      return Collections.emptyList();
    }

    CompilationUnit cu = context.getASTRoot();
    ASTNode coveredNode = problem.getCoveredNode(cu);
    TypeDeclaration syncTypeDecl = (TypeDeclaration) coveredNode.getParent();
    assert (cu.getAST().hasResolvedBindings());

    ITypeBinding syncTypeBinding = syncTypeDecl.resolveBinding();
    assert (syncTypeBinding != null);

    String asyncName = RemoteServiceUtilities.computeAsyncTypeName(problem.getProblemArguments()[0]);
    AST ast = context.getASTRoot().getAST();
    Name name = ast.newName(asyncName);

    /*
     * HACK: NewCUUsingWizardProposal wants a name that has a parent expression so we create an
     * assignment so that the name has a valid parent
     */
    ast.newAssignment().setLeftHandSide(name);

    IJavaElement typeContainer = syncType.getParent();
    if (typeContainer.getElementType() == IJavaElement.COMPILATION_UNIT) {
      typeContainer = syncType.getPackageFragment();
    }

    // Add a create async interface proposal
    CreateAsyncInterfaceProposal createAsyncInterfaceProposal = new CreateAsyncInterfaceProposal(
        context.getCompilationUnit(), name, K_INTERFACE, typeContainer, 2, syncTypeBinding);

    // Add the stock create interface proposal
    NewCompilationUnitUsingWizardProposal fallbackProposal = new NewCompilationUnitUsingWizardProposal(
        context.getCompilationUnit(), name, K_INTERFACE, context.getCompilationUnit().getParent(), 1);

    return Arrays.<IJavaCompletionProposal>asList(createAsyncInterfaceProposal, fallbackProposal);
  }

  private final ICompilationUnit compilationUnit;

  private IType createdType;

  private Name node;

  private ITypeBinding syncTypeBinding;

  private final IJavaElement typeContainer;

  private final String typeNameWithParameters;

  public CreateAsyncInterfaceProposal(ICompilationUnit cu, Name node, int typeKind, IJavaElement typeContainer,
      int severity, ITypeBinding syncTypeBinding) {
    super(cu, node, typeKind, typeContainer, severity);
    this.compilationUnit = cu;
    this.node = node;
    this.syncTypeBinding = syncTypeBinding;
    this.typeContainer = typeContainer;

    typeNameWithParameters = computeTypeNameWithParameters();

    String displayName = computeDisplayName();
    setDisplayName(displayName);
    setImage(GWTPlugin.getDefault().getImageRegistry().get(GWTImages.NEW_ASYNC_INTERFACE_SMALL));
  }

  @Override
  public void apply(IDocument document) {
    StructuredSelection selection = new StructuredSelection(compilationUnit);
    NewElementWizard wizard = createWizard(selection);
    wizard.init(JavaPlugin.getDefault().getWorkbench(), selection);

    IType localCreatedType = null;

    if (isShowDialog()) {
      Shell shell = JavaPlugin.getActiveWorkbenchShell();
      WizardDialog dialog = new WizardDialog(shell, wizard);

      IPixelConverter converter = PixelConverterFactory.createPixelConverter(JFaceResources.getDialogFont());
      dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
      dialog.create();
      dialog.getShell().setText(NewAsyncRemoteServiceInterfaceCreationWizard.DEFAULT_WINDOW_TITLE);

      if (dialog.open() == Window.OK) {
        localCreatedType = (IType) wizard.getCreatedElement();
      }
    } else {
      wizard.addPages();
      try {
        NewTypeWizardPage page = getPage(wizard);
        page.createType(null);
        localCreatedType = page.getCreatedType();
      } catch (CoreException e) {
        JavaPlugin.log(e);
      } catch (InterruptedException e) {}
    }

    if (localCreatedType != null) {
      IJavaElement container = localCreatedType.getParent();
      if (container instanceof ICompilationUnit) {
        container = container.getParent();
      }

      if (!container.equals(typeContainer)) {
        // add import
        try {
          ImportRewrite rewrite = StubUtility.createImportRewrite(compilationUnit, true);
          rewrite.addImport(localCreatedType.getFullyQualifiedName('.'));
          applyEdit(compilationUnit, rewrite.rewriteImports(null), false, null);
        } catch (CoreException e) {}
      }
      this.createdType = localCreatedType;
    }
  }

  // Copied from JavaModelUtil
  private static void applyEdit(ICompilationUnit cu, TextEdit edit, boolean save, IProgressMonitor monitor)
      throws CoreException, ValidateEditException {
    IFile file = (IFile) cu.getResource();
    if (!save || !file.exists()) {
      cu.applyTextEdit(edit, monitor);
    } else {
      if (monitor == null) {
        monitor = new NullProgressMonitor();
      }
      monitor.beginTask(CorextMessages.JavaModelUtil_applyedit_operation, 2);
      try {
        IStatus status = Resources.makeCommittable(file, null);
        if (!status.isOK()) {
          throw new ValidateEditException(status);
        }

        cu.applyTextEdit(edit, SubMonitor.convert(monitor, 1));

        cu.save(SubMonitor.convert(monitor, 1), true);
      } finally {
        monitor.done();
      }
    }
  }

  @Override
  public String getAdditionalProposalInfo(IProgressMonitor monitor) {
    StringBuilder buf = new StringBuilder();
    buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_createinterface_info);
    buf.append("<br>"); //$NON-NLS-1$
    buf.append("<br>"); //$NON-NLS-1$

    if (typeContainer instanceof IType) {
      buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_tooltip_enclosingtype);
    } else {
      buf.append(CorrectionMessages.NewCUCompletionUsingWizardProposal_tooltip_package);
    }

    buf.append(" <b>"); //$NON-NLS-1$
    buf.append(JavaElementLabels.getElementLabel(typeContainer, JavaElementLabels.T_FULLY_QUALIFIED));
    buf.append("</b><br>"); //$NON-NLS-1$
    buf.append("public "); //$NON-NLS-1$
    buf.append("interface <b>"); //$NON-NLS-1$
    nameToHTML(typeNameWithParameters, buf);

    ITypeBinding superclass = getPossibleSuperTypeBinding(node);
    if (superclass != null) {
      buf.append("</b> extends <b>"); //$NON-NLS-1$
    }

    buf.append("</b> {"); //$NON-NLS-1$

    ASTParser parser = ASTParser.newParser(AST.JLS4);
    parser.setProject(compilationUnit.getJavaProject());
    parser.setResolveBindings(true);
    StringBuilder sb = new StringBuilder();
    IPackageFragment packageFragment = (IPackageFragment) compilationUnit.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
    if (packageFragment != null) {
      sb.append("package ");
      sb.append(packageFragment.getElementName());
      sb.append(";\n");
    }

    sb.append("public interface ");
    sb.append(typeNameWithParameters);
    sb.append("{}\n");

    IPath fullPath = compilationUnit.getResource().getFullPath();
    String extension = fullPath.getFileExtension();
    fullPath = fullPath.removeFileExtension();
    parser.setUnitName(fullPath.toString() + "Async." + extension);
    parser.setSource(sb.toString().toCharArray());
    CompilationUnit astNode = (CompilationUnit) parser.createAST(null);
    TypeDeclaration asyncTypeDecl = (TypeDeclaration) astNode.types().get(0);

    List<IMethodBinding> methodsToConvert = NewAsyncRemoteServiceInterfaceCreationWizardPage
        .computeSyncMethodsThatNeedAsyncVersions(syncTypeBinding, asyncTypeDecl.resolveBinding());

    // Cheat, its just a preview anyway
    NewAsyncRemoteServiceInterfaceCreationWizardPage.ImportManagerAdapter importAdapter =
        new NewAsyncRemoteServiceInterfaceCreationWizardPage.ImportManagerAdapter() {
          @Override
          public String addImport(ITypeBinding typeBinding) {
            return typeBinding.getName();
          }

          @Override
          public String addImport(String qualifiedTypeName) {
            return Signature.getSimpleName(qualifiedTypeName);
          }
        };

    for (IMethodBinding methodToConvert : methodsToConvert) {
      try {
        buf.append("<br><pre>  </pre>");
        String methodContents = NewAsyncRemoteServiceInterfaceCreationWizardPage.createMethodContents(null,
            importAdapter, methodToConvert, false);
        nameToHTML(methodContents, buf);
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
      } catch (CoreException e) {
        GWTPluginLog.logError(e);
      }
    }

    buf.append("<br>}<br>"); //$NON-NLS-1$
    return buf.toString();
  }

  @Override
  public IType getCreatedType() {
    return createdType;
  }

  private String computeDisplayName() {
    String displayName;
    String typeName = typeNameWithParameters;
    String containerName = ASTNodes.getQualifier(node);

    String containerLabel = asLabel(containerName);
    String typeLabel = asLabel(typeName);

    boolean isInnerType = typeContainer instanceof IType;
    if (isInnerType) {
      if (containerName.length() == 0) {
        displayName = MessageFormat.format("Create member asynchronous RemoteService interface ''{0}''", typeLabel);
      } else {
        displayName = MessageFormat.format("Create member asynchronous RemoteService interface ''{0}'' in type ''{1}''",
            typeLabel, containerLabel);
      }
    } else {
      if (containerName.length() == 0) {
        displayName =
            MessageFormat.format("Create asynchronous RemoteService interface ''{0}'' in type ''{1}''", typeLabel);
      } else {
        displayName = MessageFormat.format("Create asynchronous RemoteService interface ''{0}'' in package ''{1}''",
            typeLabel, containerLabel);
      }
    }

    return displayName;
  }

  private String computeTypeNameWithParameters() {
    StringBuilder sb = new StringBuilder();

    if (syncTypeBinding.getTypeParameters().length > 0) {
      sb.append("<");
      ITypeBinding[] typeParameters = syncTypeBinding.getTypeParameters();
      for (int i = 0; i < typeParameters.length; ++i) {
        if (i != 0) {
          sb.append(", ");
        }
        sb.append(typeParameters[i].getName());
        sb.append(" extends ");
        ITypeBinding[] typeBounds = typeParameters[i].getTypeBounds();
        for (int j = 0; j < typeBounds.length; ++j) {
          if (j != 0) {
            sb.append(" & ");
          }

          sb.append(typeBounds[i].getQualifiedName());
        }
      }
      sb.append(">");
    }

    // TODO: Factor out access to internal class, does it even matter now?
    return ASTNodes.getSimpleNameIdentifier(node) + sb.toString();
  }

  /**
   * Fill-in the "Package" and "Name" fields.
   *
   * @param page the wizard page.
   */
  private void configureWizardPage(NewTypeWizardPage page) {
    /*
     * Don't allow this to be edited. If do allow edits then we should only allow either the type
     * parameters from the sync type or no type parameters at all.
     */
    page.setTypeName(typeNameWithParameters, false);

    boolean isInEnclosingType = typeContainer instanceof IType;
    if (isInEnclosingType) {
      page.setEnclosingType((IType) typeContainer, false);
    } else {
      page.setPackageFragment((IPackageFragment) typeContainer, false);
    }
    page.setEnclosingTypeSelection(isInEnclosingType, false);
  }

  private NewElementWizard createWizard(StructuredSelection selection) {
    NewAsyncRemoteServiceInterfaceCreationWizardPage page =
        new NewAsyncRemoteServiceInterfaceCreationWizardPage(syncTypeBinding);
    page.init(selection);
    configureWizardPage(page);
    return new NewAsyncRemoteServiceInterfaceCreationWizard(page, true);
  }

  private NewTypeWizardPage getPage(NewElementWizard wizard) {
    IWizardPage[] pages = wizard.getPages();
    Assert.isTrue(pages.length > 0 && pages[0] instanceof NewTypeWizardPage);
    return (NewTypeWizardPage) pages[0];
  }

  private ITypeBinding getPossibleSuperTypeBinding(ASTNode node) {
    ITypeBinding binding = ASTResolving.guessBindingForTypeReference(node);
    if (binding != null && !binding.isRecovered()) {
      return binding;
    }
    return null;
  }

  private void nameToHTML(String name, StringBuilder sb) {
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (ch == '>') {
        sb.append("&gt;"); //$NON-NLS-1$
      } else if (ch == '<') {
        sb.append("&lt;"); //$NON-NLS-1$
      } else {
        sb.append(ch);
      }
    }
  }
}
