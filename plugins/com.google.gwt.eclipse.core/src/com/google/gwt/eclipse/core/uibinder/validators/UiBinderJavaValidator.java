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
package com.google.gwt.eclipse.core.uibinder.validators;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.SseUtilities;
import com.google.gdt.eclipse.core.java.ClasspathResourceUtilities;
import com.google.gdt.eclipse.core.reference.Reference;
import com.google.gdt.eclipse.core.reference.ReferenceManager;
import com.google.gdt.eclipse.core.reference.location.ClasspathRelativeFileReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.LogicalJavaElementReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.ReferenceLocationType;
import com.google.gdt.eclipse.core.reference.logicaljavamodel.LogicalType;
import com.google.gdt.eclipse.core.validation.ValidationResult;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities;
import com.google.gwt.eclipse.core.uibinder.model.UiBinderSubtypeToOwnerIndex;
import com.google.gwt.eclipse.core.uibinder.model.UiBinderSubtypeToUiXmlIndex;
import com.google.gwt.eclipse.core.uibinder.model.UiXmlReferencedFieldIndex;
import com.google.gwt.eclipse.core.uibinder.model.reference.UiBinderXmlParser;
import com.google.gwt.eclipse.core.uibinder.model.reference.UiBinderXmlParser.ParseResults;
import com.google.gwt.eclipse.core.uibinder.problems.MarkerPlacementStrategy;
import com.google.gwt.eclipse.core.uibinder.problems.UiBinderProblemMarkerManager;
import com.google.gwt.eclipse.core.uibinder.problems.java.UiBinderJavaProblem;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates UiBinder subtypes and their respective owner classes. Also
 * populates the indices which link UiBinder subtypes to ui.xml template files
 * and owner classes.
 */
@SuppressWarnings("restriction")
public class UiBinderJavaValidator {

  private class UiBinderOwnerVisitor extends ASTVisitor {

    private static final String SUPPRESS_WARNINGS_UIBINDER = "uibinder";

    @Override
    public boolean visit(TypeDeclaration typeDecl) {
      if (!shouldValidateType(typeDecl)) {
        return true;
      }

      for (IType uiBinderType : uiBinderToOwner.getUiBinderTypes(typeDecl.resolveBinding().getQualifiedName())) {
        result.addTypeDependency(uiBinderType.getFullyQualifiedName());
      }

      for (FieldDeclaration field : typeDecl.getFields()) {
        if (UiBinderUtilities.isUiField(field)) {
          validateUiField(field);
        }
      }

      for (MethodDeclaration method : typeDecl.getMethods()) {
        if (UiBinderUtilities.isUiHandler(method)) {
          validateUiHandler(method);
        }
      }

      return true;
    }

    private Set<IPath> getUiXmlWorkspaceRelativePaths(
        TypeDeclaration ownerTypeDecl) {
      Set<IPath> paths = new HashSet<IPath>();

      String ownerTypeName = ownerTypeDecl.resolveBinding().getQualifiedName();
      for (IType uiBinderSubtype : uiBinderToOwner.getUiBinderTypes(ownerTypeName)) {
        IPath uiXmlClasspathRelativePath = ownerToUiXml.getUiXmlPath(uiBinderSubtype);
        try {
          IStorage uiXmlFile = ClasspathResourceUtilities.resolveFile(
              uiXmlClasspathRelativePath, javaProject);

          // Verify that the ui.xml exists and is not in a JAR
          if (uiXmlFile instanceof IFile) {
            paths.add(uiXmlFile.getFullPath());
          }
        } catch (JavaModelException e) {
          GWTPluginLog.logError(e);
        }
      }
      return paths;
    }

    /**
     * If true is returned, the type declaration has a resolvable binding.
     */
    private boolean shouldValidateType(TypeDeclaration typeDecl) {
      if (typeDecl.isInterface()) {
        return false;
      }

      IType ownerType = getType(typeDecl);
      if (ownerType == null) {
        return false;
      }

      if (!uiBinderToOwner.isOwnerType(ownerType.getFullyQualifiedName('.'))) {
        return false;
      }

      if (typeDecl.resolveBinding() == null) {
        GWTPluginLog.logWarning("Could not resolve binding for "
            + typeDecl.getName().getFullyQualifiedName());
        return false;
      }

      return true;
    }

    private void validateFieldExistenceInUiXml(TypeDeclaration ownerTypeDecl,
        ASTNode problemNode, String fieldName) {
      for (IPath uiXmlPath : getUiXmlWorkspaceRelativePaths(ownerTypeDecl)) {
        if (!uiXmlFieldRefs.isFieldReferencedByUiXml(uiXmlPath, fieldName)) {
          result.addProblem(UiBinderJavaProblem.createMissingUiFieldInXml(
              problemNode, fieldName, uiXmlPath));
        }
      }
    }

    private void validateUiField(FieldDeclaration uiFieldDecl) {
      if (JavaASTUtils.hasSuppressWarnings(uiFieldDecl,
          SUPPRESS_WARNINGS_UIBINDER)) {
        return;
      }

      validateUiFieldVisibility(uiFieldDecl);
      validateUiFieldExistenceInUiXml(uiFieldDecl);
    }

    @SuppressWarnings("unchecked")
    private void validateUiFieldExistenceInUiXml(FieldDeclaration uiFieldDecl) {
      List<VariableDeclarationFragment> varDecls = uiFieldDecl.fragments();
      for (VariableDeclarationFragment varDecl : varDecls) {
        SimpleName fieldNameDecl = varDecl.getName();
        validateFieldExistenceInUiXml(
            (TypeDeclaration) uiFieldDecl.getParent(), fieldNameDecl,
            fieldNameDecl.getIdentifier());
      }
    }

    private void validateUiFieldVisibility(FieldDeclaration uiFieldDecl) {
      Modifier privateModifier = JavaASTUtils.findPrivateModifier(uiFieldDecl);
      if (privateModifier != null) {
        result.addAllProblems(UiBinderJavaProblem.createPrivateUiField(
            uiFieldDecl, privateModifier));
      }
    }

    private void validateUiHandler(MethodDeclaration uiHandlerDecl) {
      if (JavaASTUtils.hasSuppressWarnings(uiHandlerDecl,
          SUPPRESS_WARNINGS_UIBINDER)) {
        return;
      }

      validateUiHandlerVisibility(uiHandlerDecl);
      validateUiHandlerFieldExistenceInUiXml(uiHandlerDecl);
    }

    @SuppressWarnings("unchecked")
    private void validateUiHandlerFieldExistenceInUiXml(
        MethodDeclaration uiHandlerDecl) {
      Annotation annotation = JavaASTUtils.findAnnotation(uiHandlerDecl,
          UiBinderConstants.UI_HANDLER_TYPE_NAME);

      if (annotation instanceof SingleMemberAnnotation) {
        SingleMemberAnnotation uiHandlerAnnotation = (SingleMemberAnnotation) annotation;
        Expression exp = uiHandlerAnnotation.getValue();
        if (exp instanceof StringLiteral) {
          validateFieldExistenceInUiXml(
              (TypeDeclaration) uiHandlerDecl.getParent(), exp,
              ((StringLiteral) exp).getLiteralValue());
        } else if (exp instanceof ArrayInitializer) {
          for (Expression element : (List<Expression>) ((ArrayInitializer) exp).expressions()) {
            if (element instanceof StringLiteral) {
              validateFieldExistenceInUiXml(
                  (TypeDeclaration) uiHandlerDecl.getParent(), element,
                  ((StringLiteral) element).getLiteralValue());
            }
          }
        }
      }
    }

    private void validateUiHandlerVisibility(MethodDeclaration uiHandlerDecl) {
      Modifier privateModifier = JavaASTUtils.findPrivateModifier(uiHandlerDecl);
      if (privateModifier != null) {
        result.addProblem(UiBinderJavaProblem.createPrivateUiHandler(
            uiHandlerDecl, privateModifier));
      }
    }
  }

  private class UiBinderSubtypeVisitor extends ASTVisitor {

    @Override
    public boolean visit(TypeDeclaration typeDecl) {
      if (!shouldValidateType(typeDecl)) {
        return true;
      }

      validateUiXmlExistence(typeDecl);
      validateTypeVisibility(typeDecl);

      if (referenceManager != null) {
        parseAndValidateUiXml(getType(typeDecl), referenceManager);
      }

      // Record our owner class in the subtype -> owner index
      IType uiBinderType = getType(typeDecl);
      ITypeBinding ownerTypeBinding = getOwnerTypeBinding(typeDecl);
      if (uiBinderType != null && ownerTypeBinding != null) {
        uiBinderToOwner.setOwnerType(uiBinderType,
            ownerTypeBinding.getQualifiedName());
      }

      return true;
    }

    /**
     * Parses the ui.xml file for the subtype. If parsing fails with an
     * exception, a warning is logged.
     * 
     * @param referenceManager receives references to resources and non-type
     *          Java elements from the ui.xml file
     */
    private void parseAndValidateUiXml(IType uiBinderSubtype,
        ReferenceManager referenceManager) {
      try {
        IPath uiXmlPath = UiBinderJavaValidator.this.ownerToUiXml.getUiXmlPath(uiBinderSubtype);
        Object uiXmlFileAsObj = ClasspathResourceUtilities.resolveFile(
            uiXmlPath, javaProject);
        if (!(uiXmlFileAsObj instanceof IFile)) {
          return;
        }

        IDOMModel uiXmlModel = null;
        try {
          IFile uiXmlFile = (IFile) uiXmlFileAsObj;
          uiXmlModel = SseUtilities.getModelForRead(uiXmlFile);
          if (uiXmlModel == null) {
            GWTPluginLog.logWarning("Could not find a corresponding model for "
                + uiXmlPath);
            return;
          }

          ParseResults parseResults = UiBinderXmlParser.newInstance(uiXmlModel,
              referenceManager,
              new MarkerPlacementStrategy(UiBinderProblemMarkerManager.MARKER_ID)).parse();
          if (parseResults == null) {
            return;
          }

          result.addAllTypeDependencies(parseResults.getJavaTypeReferences());
          UiBinderJavaValidator.this.uiXmlFieldRefs.putFieldReferencesForUiXml(
              uiXmlFile.getFullPath(), parseResults.getFieldNames());

        } finally {
          if (uiXmlModel != null) {
            uiXmlModel.releaseFromRead();
          }
        }
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
      } catch (FileNotFoundException e) {
        // Ignore since we'll put up an marker
      } catch (IOException e) {
        GWTPluginLog.logError(e);
      } catch (CoreException e) {
        GWTPluginLog.logError(e);
      } catch (UiBinderException e) {
        GWTPluginLog.logError(e);
      }
    }

    /**
     * If true is returned, the type declaration has a resolveable binding.
     */
    private boolean shouldValidateType(TypeDeclaration typeDecl) {
      if (!typeDecl.isInterface()) {
        return false;
      }

      ITypeBinding typeBinding = typeDecl.resolveBinding();
      if (typeBinding == null) {
        return false;
      }

      if (!isUiBinder(typeBinding)) {
        return false;
      }

      // Passed all tests, so validate
      return true;
    }

    private void validateTypeVisibility(TypeDeclaration uiBinderSubtypeDecl) {
      Modifier privateModifier = JavaASTUtils.findPrivateModifier(uiBinderSubtypeDecl);
      if (privateModifier != null) {
        result.addProblem(UiBinderJavaProblem.createPrivateUiBinderSubtype(
            uiBinderSubtypeDecl, privateModifier));
      }
    }

    private void validateUiXmlExistence(IPath uiXmlPath, ASTNode node) {
      try {
        TypeDeclaration uiBinderSubtypeDecl = JavaASTUtils.findAncestor(node,
            TypeDeclaration.class);
        IType uiBinderSubtype = getType(uiBinderSubtypeDecl);
        if (uiBinderSubtype != null) {
          ownerToUiXml.setUiXmlPath(uiBinderSubtype, uiXmlPath);

          if (referenceManager != null) {
            // Delete the old reference for this UiBinder subtype
            referenceManager.removeReferences(referenceManager.getReferencesWithMatchingJavaElement(
                uiBinderSubtype, EnumSet.of(ReferenceLocationType.SOURCE)));

            // Add the new reference
            LogicalType uiBinderSubtypeLogicalType = new LogicalType(
                uiBinderSubtype);
            LogicalJavaElementReferenceLocation uiBinderSubtypeRefLocation = new LogicalJavaElementReferenceLocation(
                uiBinderSubtypeLogicalType);
            ClasspathRelativeFileReferenceLocation uiXmlRefLocation = new ClasspathRelativeFileReferenceLocation(
                uiXmlPath);
            referenceManager.addReference(new Reference(
                uiBinderSubtypeRefLocation, uiXmlRefLocation,
                uiBinderSubtype.getJavaProject().getProject()));
          }
        }

        if (!ClasspathResourceUtilities.isResourceOnClasspath(javaProject,
            uiXmlPath)) {
          result.addProblem(UiBinderJavaProblem.createMissingUiXmlFile(node,
              uiXmlPath.lastSegment(), uiXmlPath.removeLastSegments(1)));
        }
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e,
            "Error while searching for {0} in project {1}",
            uiXmlPath.toString(), javaProject.getElementName());
      }
    }

    private void validateUiXmlExistence(TypeDeclaration uiBinderSubtypeDecl) {
      ITypeBinding typeBinding = uiBinderSubtypeDecl.resolveBinding();

      Annotation annotation = JavaASTUtils.findAnnotation(uiBinderSubtypeDecl,
          UiBinderConstants.UI_TEMPLATE_ANNOTATION_NAME);
      if (annotation instanceof SingleMemberAnnotation || 
          annotation instanceof NormalAnnotation) {
        Expression exp = null;
        if (annotation instanceof SingleMemberAnnotation) {
          SingleMemberAnnotation uiTemplateAnnotation = (SingleMemberAnnotation) annotation;
          exp = uiTemplateAnnotation.getValue();
        } else if (annotation instanceof NormalAnnotation) {
          NormalAnnotation uiTemplateAnnotation = (NormalAnnotation) annotation;
          List<?> values = uiTemplateAnnotation.values();
          if (values.size() == 1) {
            exp = ((MemberValuePair) values.get(0)).getValue();
          }
        }
        if (exp instanceof StringLiteral) {
          String uiTemplateValue = ((StringLiteral) exp).getLiteralValue();
          IPath explicitUiXmlPath = new Path(
              typeBinding.getPackage().getName().replace('.', '/')).append(uiTemplateValue);
          validateUiXmlExistence(explicitUiXmlPath, exp);
        }
      } else {
        TypeDeclaration enclosingType = JavaASTUtils.getEnclosingType(uiBinderSubtypeDecl);
        if (enclosingType != null) {
          typeBinding = enclosingType.resolveBinding();
        }
        String uiXmlFileName = typeBinding.getName()
            + UiBinderConstants.UI_BINDER_XML_EXTENSION;
        IPath implicitUiXmlPath = new Path(
            typeBinding.getErasure().getQualifiedName().replace('.', '/'));
        implicitUiXmlPath = implicitUiXmlPath.removeLastSegments(1).append(
            uiXmlFileName);
        validateUiXmlExistence(implicitUiXmlPath, uiBinderSubtypeDecl.getName());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static ITypeBinding getOwnerTypeBinding(
      TypeDeclaration uiBinderSubtype) {
    List<Type> superInterfaces = uiBinderSubtype.superInterfaceTypes();
    for (Type superInterface : superInterfaces) {
      ITypeBinding binding = superInterface.resolveBinding();
      if (binding != null) {
        if (binding.getErasure().getQualifiedName().equals(
            UiBinderConstants.UI_BINDER_TYPE_NAME)) {
          if (superInterface instanceof ParameterizedType) {
            ParameterizedType uiBinderType = (ParameterizedType) superInterface;
            List<Type> typeArgs = uiBinderType.typeArguments();
            if (typeArgs.size() == 2) {
              Type ownerType = typeArgs.get(1);
              return ownerType.resolveBinding();
            }
          }
        }
      }
    }
    return null;
  }

  private static IType getType(TypeDeclaration typeDecl) {
    if (typeDecl == null) {
      return null;
    }

    ITypeBinding typeBinding = typeDecl.resolveBinding();
    if (typeBinding == null) {
      return null;
    }

    IJavaElement javaElement = typeBinding.getJavaElement();
    return (javaElement instanceof IType ? (IType) javaElement : null);
  }

  private static boolean isUiBinder(ITypeBinding typeBinding) {
    for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
      if (superInterface.getErasure().getQualifiedName().equals(
          UiBinderConstants.UI_BINDER_TYPE_NAME)) {
        return true;
      }
    }
    return false;
  }

  private final CompilationUnit cu;

  private final IJavaProject javaProject;

  private final UiBinderSubtypeToUiXmlIndex ownerToUiXml;

  private final ReferenceManager referenceManager;

  private ValidationResult result;

  private final UiBinderSubtypeToOwnerIndex uiBinderToOwner;

  private final UiXmlReferencedFieldIndex uiXmlFieldRefs;

  public UiBinderJavaValidator(CompilationUnit cu,
      UiBinderSubtypeToOwnerIndex uiBinderToOwner,
      UiBinderSubtypeToUiXmlIndex ownerToUiXml,
      UiXmlReferencedFieldIndex uiXmlFieldRefs,
      ReferenceManager referenceManager) {
    this.cu = cu;
    this.javaProject = JavaASTUtils.getCompilationUnit(cu).getJavaProject();
    this.uiBinderToOwner = uiBinderToOwner;
    this.ownerToUiXml = ownerToUiXml;
    this.uiXmlFieldRefs = uiXmlFieldRefs;
    this.referenceManager = referenceManager;
  }

  /**
   * Validation entry point.
   */
  public ValidationResult validate() {
    result = new ValidationResult();

    /*
     * The subtype visitor must go first, so it can populate the index we use to
     * identify owner classes (which can only be identified via a type argument
     * in the UiBinder subtype declaration).
     */
    cu.accept(new UiBinderSubtypeVisitor());
    cu.accept(new UiBinderOwnerVisitor());

    return result;
  }

}
