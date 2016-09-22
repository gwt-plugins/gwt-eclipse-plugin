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
package com.google.gdt.eclipse.core;

import com.google.gdt.eclipse.core.java.JavaModelSearch;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.TypeBindingVisitor;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility methods for using the JDT Java AST.
 */
@SuppressWarnings("restriction")
public final class JavaASTUtils {

  private static final Map<String, String> primitiveTypeNameToWrapperTypeName = new HashMap<String, String>();

  private static final Map<String, String> wrapperTypeNameToPrimitiveTypeName = new HashMap<String, String>();

  static {
    primitiveTypeNameToWrapperTypeName.put("boolean", "java.lang.Boolean");
    primitiveTypeNameToWrapperTypeName.put("byte", "java.lang.Byte");
    primitiveTypeNameToWrapperTypeName.put("char", "java.lang.Character");
    primitiveTypeNameToWrapperTypeName.put("double", "java.lang.Double");
    primitiveTypeNameToWrapperTypeName.put("float", "java.lang.Float");
    primitiveTypeNameToWrapperTypeName.put("int", "java.lang.Integer");
    primitiveTypeNameToWrapperTypeName.put("long", "java.lang.Long");
    primitiveTypeNameToWrapperTypeName.put("short", "java.lang.Short");
    primitiveTypeNameToWrapperTypeName.put("void", "java.lang.Void");

    // Create a reverse map
    for (Entry<String, String> primitiveToWrapper : primitiveTypeNameToWrapperTypeName.entrySet()) {
      wrapperTypeNameToPrimitiveTypeName.put(primitiveToWrapper.getValue(),
          primitiveToWrapper.getKey());
    }
  }

  /**
   * Clones the given parameter declaration.
   */
  public static SingleVariableDeclaration cloneMethodParameter(AST ast,
      SingleVariableDeclaration param, ImportRewrite imports) {
    SingleVariableDeclaration newParam = ast.newSingleVariableDeclaration();

    // New parameter has a normalized type equivalent to the original
    Type type = normalizeTypeAndAddImport(ast, param.getType(), imports);
    newParam.setType(type);

    // Give new parameter same name as the original
    newParam.setName(ast.newSimpleName(param.getName().getIdentifier()));

    return newParam;
  }

  /**
   * Clones a method's parameter list.
   */
  public static List<SingleVariableDeclaration> cloneParameters(AST ast,
      List<SingleVariableDeclaration> params, ImportRewrite imports) {
    List<SingleVariableDeclaration> newParams = new ArrayList<SingleVariableDeclaration>();

    for (SingleVariableDeclaration param : params) {
      SingleVariableDeclaration newParam = cloneMethodParameter(ast, param,
          imports);
      newParams.add(newParam);
    }
    return newParams;
  }

  public static boolean containsTypeVariable(Type type) {
    final boolean[] containsTypeVariable = {false};

    ASTResolving.visitAllBindings(type, new TypeBindingVisitor() {
      public boolean visit(ITypeBinding visitedBinding) {
        if (visitedBinding.isTypeVariable()) {
          containsTypeVariable[0] = true;
          return false;
        }
        return true;
      }
    });
    return containsTypeVariable[0];
  }

  /**
   * Calculates the edit distance between two argument lists.
   */
  public static int editDistance(List<SingleVariableDeclaration> params,
      List<SingleVariableDeclaration> peerParams) {
    ASTMatcher matcher = new ASTMatcher();

    // Classic DP implementation
    int[][] dp = new int[params.size() + 1][peerParams.size() + 1];

    for (int i = 0; i <= params.size(); i++) {
      dp[i][0] = i;
    }

    for (int j = 0; j <= peerParams.size(); j++) {
      dp[0][j] = j;
    }

    for (int i = 1; i <= params.size(); i++) {
      for (int j = 1; j <= peerParams.size(); j++) {
        int distance = dp[i - 1][j - 1];

        if (!params.get(i - 1).subtreeMatch(matcher, peerParams.get(j - 1))) {
          distance += 1;
        }

        if (dp[i - 1][j] + 1 < distance) {
          distance = dp[i - 1][j] + 1;
        }

        if (dp[i][j - 1] + 1 < distance) {
          distance = dp[i][j - 1] + 1;
        }

        dp[i][j] = distance;
      }
    }

    return dp[params.size()][peerParams.size()];
  }

  @SuppressWarnings("unchecked")
  public static <T> T findAncestor(ASTNode node, Class<?> ancestorType) {
    while (node != null && (!ancestorType.isAssignableFrom(node.getClass()))) {
      node = node.getParent();
    }
    return (T) node;
  }

  /**
   * Finds an annotation of the given type (as a fully-qualified name) on a
   * declaration (type, method, field, etc.). If no such annotation exists,
   * returns <code>null</code>.
   */
  @SuppressWarnings("unchecked")
  public static Annotation findAnnotation(BodyDeclaration decl,
      String annotationTypeName) {
    if (annotationTypeName == null) {
      throw new IllegalArgumentException("annotationTypeName cannot be null");
    }

    List<ASTNode> modifiers = (List<ASTNode>) decl.getStructuralProperty(decl.getModifiersProperty());
    for (ASTNode modifier : modifiers) {
      if (modifier instanceof Annotation) {
        Annotation annotation = (Annotation) modifier;
        String typeName = getAnnotationTypeName(annotation);
        if (annotationTypeName.equals(typeName)) {
          return annotation;
        }
      }
    }
    return null;
  }

  public static MethodDeclaration findMethodDeclaration(
      CompilationUnit astRoot, String methodBindingKey) {
    ASTNode bindingDecl = astRoot.findDeclaringNode(methodBindingKey);
    if (bindingDecl == null
        || bindingDecl.getNodeType() != ASTNode.METHOD_DECLARATION) {
      return null;
    }
    return (MethodDeclaration) bindingDecl;
  }

  public static MethodDeclaration findMethodDeclaration(ICompilationUnit cu,
      String methodBindingKey) {
    CompilationUnit astRoot = ASTResolving.createQuickFixAST(cu, null);
    return findMethodDeclaration(astRoot, methodBindingKey);
  }

  @SuppressWarnings("unchecked")
  public static Modifier findPrivateModifier(BodyDeclaration decl) {
    List<IExtendedModifier> modifiers = decl.modifiers();
    for (IExtendedModifier m : modifiers) {
      if (m.isModifier()) {
        Modifier modifier = (Modifier) m;
        if (modifier.isPrivate()) {
          return modifier;
        }
      }
    }
    return null;
  }

  public static TypeDeclaration findTypeDeclaration(CompilationUnit unit,
      String qualifiedTypeName) {
    final List<TypeDeclaration> typeDeclarations = new ArrayList<TypeDeclaration>();
    unit.accept(new ASTVisitor() {
      @Override
      public boolean visit(TypeDeclaration node) {
        typeDeclarations.add(node);
        return true;
      }
    });

    return findTypeDeclaration(typeDeclarations, qualifiedTypeName);
  }

  public static TypeDeclaration findTypeDeclaration(IJavaProject javaProject,
      String qualifiedTypeName) {
    IType type = JavaModelSearch.findType(javaProject, qualifiedTypeName);
    if (type != null) {
      CompilationUnit astUnit = parseCompilationUnit(type.getCompilationUnit());
      return findTypeDeclaration(astUnit, qualifiedTypeName);
    }

    return null;
  }

  /**
   * Identical to
   * {@link org.eclipse.jdt.internal.corext.dom.Bindings#findTypeInHierarchy(ITypeBinding, String)}
   * , except this one ignores type parameters when matching type names.
   */
  public static ITypeBinding findTypeInHierarchy(ITypeBinding hierarchyType,
      String fullyQualifiedTypeName) {
    if (hierarchyType.isArray() || hierarchyType.isPrimitive()) {
      return null;
    }
    // Ignore type arguments
    if (fullyQualifiedTypeName.equals(hierarchyType.getErasure().getQualifiedName())) {
      return hierarchyType;
    }
    ITypeBinding superClass = hierarchyType.getSuperclass();
    if (superClass != null) {
      ITypeBinding res = findTypeInHierarchy(superClass, fullyQualifiedTypeName);
      if (res != null) {
        return res;
      }
    }
    ITypeBinding[] superInterfaces = hierarchyType.getInterfaces();
    for (int i = 0; i < superInterfaces.length; i++) {
      ITypeBinding res = findTypeInHierarchy(superInterfaces[i],
          fullyQualifiedTypeName);
      if (res != null) {
        return res;
      }
    }
    return null;
  }

  /**
   * Returns the fully-qualified name of an annotation, or <code>null</code> if
   * the annotation's type could not be resolved.
   */
  public static String getAnnotationTypeName(Annotation annotation) {
    IAnnotationBinding binding = annotation.resolveAnnotationBinding();
    if (binding != null) {
      ITypeBinding annotationTypeBinding = binding.getAnnotationType();
      if (annotationTypeBinding != null) {
        return annotationTypeBinding.getQualifiedName();
      }
    }
    return null;
  }

  /**
   * Returns an annotation's value. If the annotation not a single-member
   * annotation, this is the value corresponding to the key named "value".
   */
  @SuppressWarnings("unchecked")
  public static Expression getAnnotationValue(Annotation annotation) {
    if (annotation instanceof SingleMemberAnnotation) {
      return ((SingleMemberAnnotation) annotation).getValue();
    } else if (annotation instanceof NormalAnnotation) {
      NormalAnnotation normalAnnotation = (NormalAnnotation) annotation;
      for (MemberValuePair pair : (List<MemberValuePair>) normalAnnotation.values()) {
        if (pair.getName().getIdentifier().equals("value")) {
          return pair.getValue();
        }
      }
    }
    return null;
  }

  /**
   * Gets the compilation unit containing a particular Java AST node.
   */
  public static ICompilationUnit getCompilationUnit(ASTNode node) {
    CompilationUnit root = (CompilationUnit) node.getRoot();
    ICompilationUnit cu = (ICompilationUnit) root.getJavaElement();
    return cu;
  }

  public static TypeDeclaration getEnclosingType(TypeDeclaration typeDecl) {
    ASTNode parent = typeDecl.getParent();
    if (parent instanceof TypeDeclaration) {
      return (TypeDeclaration) parent;
    }
    return null;
  }

  public static String getPrimitiveTypeName(String wrapperTypeName) {
    return wrapperTypeNameToPrimitiveTypeName.get(wrapperTypeName);
  }

  /**
   * Gets the AST node's original source code.
   */
  public static String getSource(ASTNode node) {
    ICompilationUnit cu = getCompilationUnit(node);

    try {
      String source = cu.getSource();
      int endPos = node.getStartPosition() + node.getLength();
      return source.substring(node.getStartPosition(), endPos);

    } catch (JavaModelException e) {
      CorePluginLog.logError(e);
      return "";
    }
  }

  public static String getWrapperTypeName(String primitiveTypeName) {
    return primitiveTypeNameToWrapperTypeName.get(primitiveTypeName);
  }

  /**
   * Returns <code>true</code> if any of the problems fall within the
   * {@link ASTNode}'s source range.
   */
  public static boolean hasErrors(ASTNode node, IProblem[] problems) {
    int startPosition = node.getStartPosition();
    int endPosition = startPosition + node.getLength() - 1;

    for (IProblem problem : problems) {
      if (!problem.isError()) {
        // Skip any problem that is not an error
        continue;
      }
      if (problem.getSourceStart() >= startPosition
          && problem.getSourceEnd() <= endPosition) {
        return true;
      }
    }

    return false;
  }

  @SuppressWarnings("unchecked")
  public static boolean hasSuppressWarnings(BodyDeclaration decl,
      String warningType) {
    List<IExtendedModifier> modifiers = decl.modifiers();
    for (IExtendedModifier modifier : modifiers) {
      if (modifier.isAnnotation()) {
        assert (modifier instanceof Annotation);
        Annotation annotation = (Annotation) modifier;

        // Get the (simple) type name of the annotation (we can't resolve the
        // annotation's type if binding resolution is disabled on our AST).
        Name annotationType = annotation.getTypeName();
        if (annotationType.isQualifiedName()) {
          annotationType = ((QualifiedName) annotationType).getName();
        }
        assert (annotationType.isSimpleName());
        String annotationTypeName = ((SimpleName) annotationType).getIdentifier();

        // Look for @SuppressWarnings annotations
        if (annotationTypeName.equals(SuppressWarnings.class.getSimpleName())) {
          // Now extract the parameter representing the set of warnings that
          // should be suppressed
          if (annotation instanceof SingleMemberAnnotation) {
            SingleMemberAnnotation suppressWarnings = (SingleMemberAnnotation) annotation;
            Expression annotationValue = suppressWarnings.getValue();
            return containsAnnotationValue(annotationValue, warningType);
          } else if (annotation instanceof NormalAnnotation) {
            NormalAnnotation suppressWarnings = (NormalAnnotation) annotation;
            List<MemberValuePair> annotationValues = suppressWarnings.values();
            for (MemberValuePair annotationValue : annotationValues) {
              SimpleName annotationValueName = annotationValue.getName();
              if (annotationValueName.getIdentifier().equals("value")) {
                return containsAnnotationValue(annotationValue.getValue(),
                    warningType);
              }
            }
          }

          return false;
        }
      }
    }

    return false;
  }

  /**
   * Generates the normalized form and adds the required imports for a given
   * {@link Type}.
   */
  public static Type normalizeTypeAndAddImport(AST ast, Type type,
      ImportRewrite imports) {
    ITypeBinding binding = type.resolveBinding();

    // Eliminate type variables in the generated type
    // TODO(): maybe leave the type variables, if we can verify that the type
    // parameters on the target type are exactly the same as those on the source
    // type (all names and type bounds are identical)
    if (JavaASTUtils.containsTypeVariable(type)) {
      binding = binding.getErasure();
    }

    // Report the type binding to the import rewriter, which will record the
    // import and give us either a SimpleType or a QualifiedType to use.
    return imports.addImport(binding, ast);
  }

  public static CompilationUnit parseCompilationUnit(
      ICompilationUnit compilationUnit) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setResolveBindings(true);
    parser.setSource(compilationUnit);
    return (CompilationUnit) parser.createAST(null);
  }

  public static ITypeBinding resolveType(IJavaProject javaProject,
      String qualifiedTypeName) throws JavaModelException {
    IType type = javaProject.findType(qualifiedTypeName);
    if (type == null || !type.exists()) {
      return null;
    }

    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setProject(javaProject);
    IBinding[] bindings = parser.createBindings(new IJavaElement[] {type}, null);
    if (bindings == null) {
      return null;
    }

    return (ITypeBinding) bindings[0];
  }

  @SuppressWarnings("unchecked")
  private static boolean containsAnnotationValue(Expression annotationValue,
      String value) {
    if (annotationValue.getNodeType() == ASTNode.STRING_LITERAL) {
      String valueString = ((StringLiteral) annotationValue).getLiteralValue();
      return value.equals(valueString);
    } else if (annotationValue.getNodeType() == ASTNode.ARRAY_INITIALIZER) {
      // If the annotation value is actually an array, check each element
      List<Expression> warningTypes = ((ArrayInitializer) annotationValue).expressions();
      for (Expression warningType : warningTypes) {
        if (containsAnnotationValue(warningType, value)) {
          return true;
        }
      }
    }

    return false;
  }

  private static TypeDeclaration findTypeDeclaration(
      List<TypeDeclaration> typeDeclarations, String qualifiedTypeName) {
    qualifiedTypeName = qualifiedTypeName.replace('$', '.');
    for (TypeDeclaration typeDeclaration : typeDeclarations) {
      if (typeDeclaration.resolveBinding().getQualifiedName().equals(
          qualifiedTypeName)) {
        return typeDeclaration;
      }
    }

    return null;
  }

  private JavaASTUtils() {
    // Not instantiable
  }
}
