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
package com.google.gwt.eclipse.core.validators.clientbundle;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.java.ClasspathResourceUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.core.validation.ValidationResult;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResourceDependencyIndex;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleUtilities;
import com.google.gwt.eclipse.core.markers.ClientBundleProblem;
import com.google.gwt.eclipse.platform.clientbundle.ResourceTypeDefaultExtensions;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates custom ClientBundle subtypes.
 */
public class ClientBundleValidator {

  private static class ClientBundleValidationVisitor extends ASTVisitor {

    /**
     * Returns the string to use as the first argument of a
     * {@link com.google.gwt.eclipse.core.markers.ClientBundleProblemType#MISSING_RESOURCE_FILE}
     * problem. For resource types that support multiple default extensions
     * (e.g. ImageResource), we want to indicate them to the user.
     */
    private static String computeExpectedFileNameArgument(
        String nameWithoutExtension, String[] extensions) {
      if (extensions.length == 0) {
        return nameWithoutExtension;
      }

      if (extensions.length == 1) {
        return nameWithoutExtension + extensions[0];
      }

      for (int i = 0; i < extensions.length; i++) {
        String ext = extensions[i];
        // Strip off the leading dot from each extension
        if (ext.length() > 1 && ext.startsWith(".")) {
          extensions[i] = ext.substring(1);
        }
      }

      // Indicate all the possible extensions for this resource
      return nameWithoutExtension + " (supported extensions: "
          + StringUtilities.join(extensions, ", ") + ")";
    }

    /**
     * Determine where a missing resource should actually have been found. This
     * is used only for generating the error message string.
     * 
     * Note that the @Source path can be interpreted as either an absolute
     * classpath reference (com/example/foo.jpg) or as a path relative to the
     * containing package (foo.jpg). This method uses a simple heuristic to
     * figure out which interpretation you wanted: if the path is just a
     * filename you meant it as a relative path; otherwise, it was probably an
     * absolute path.
     * 
     * @param literalPath the literal path specified by @Source
     * @param absResourcePath the resource path computed by appending the
     *          literal path onto the end of the container (package) path
     * @return the expected resource path
     */
    private static IPath computeMissingResourceExpectedPath(IPath literalPath,
        IPath absResourcePath) {
      // If the the user specified just a filename, we would expect to find the
      // resource in the current package.
      if (literalPath.segmentCount() == 1) {
        return absResourcePath;
      }
      // Otherwise, assume the literal is the full path of the resource.
      return literalPath;
    }

    /**
     * Returns the classpath of the package containing the compilation unit
     * being validated.
     */
    private static IPath getPackagePath(ASTNode node) {
      ICompilationUnit cu = JavaASTUtils.getCompilationUnit(node);
      IPackageFragment pckg = (IPackageFragment) cu.getParent();
      return new Path(pckg.getElementName().replace('.', '/'));
    }

    private static ITypeBinding getReturnTypeBinding(
        MethodDeclaration methodDecl) {
      Type returnType = methodDecl.getReturnType2();
      if (returnType != null) {
        return returnType.resolveBinding();
      }
      return null;
    }

    private static boolean isSourceAnnotation(ASTNode node) {
      if (node instanceof Annotation) {
        Annotation annotation = (Annotation) node;
        String typeName = annotation.getTypeName().getFullyQualifiedName();

        // Annotation can be used with its fully-qualified name
        if (typeName.equals(ClientBundleUtilities.CLIENT_BUNDLE_SOURCE_ANNOTATION_NAME)) {
          return true;
        }

        // Simple name is fine, too
        String sourceAnnotationSimpleName = Signature.getSimpleName(ClientBundleUtilities.CLIENT_BUNDLE_SOURCE_ANNOTATION_NAME);
        if (typeName.equals(sourceAnnotationSimpleName)) {
          return true;
        }
      }
      return false;
    }

    private static boolean shouldValidateType(TypeDeclaration type) {
      if (!type.isInterface()) {
        return false;
      }

      ITypeBinding typeBinding = type.resolveBinding();
      if (typeBinding == null) {
        return false;
      }

      if (!ClientBundleUtilities.isClientBundle(typeBinding)) {
        return false;
      }

      // Don't actually validate the built-in ClientBundle and
      // ClientBundleWithLookup types.
      String typeName = typeBinding.getQualifiedName();
      if (typeName.equals(ClientBundleUtilities.CLIENT_BUNDLE_TYPE_NAME)
          || typeName.equals(ClientBundleUtilities.CLIENT_BUNDLE_WITH_LOOKUP_TYPE_NAME)) {
        return false;
      }

      // Passed all tests, so validate
      return true;
    }

    private final IJavaProject javaProject;

    private final ClientBundleValidationResult result = new ClientBundleValidationResult();

    public ClientBundleValidationVisitor(IJavaProject javaProject) {
      this.javaProject = javaProject;
    }

    public ClientBundleValidationResult getResult() {
      return result;
    }

    @Override
    public boolean visit(TypeDeclaration type) {
      if (!shouldValidateType(type)) {
        return true;
      }

      CompilationUnit cu = (CompilationUnit) type.getRoot();
      for (MethodDeclaration methodDecl : type.getMethods()) {
        if (JavaASTUtils.hasErrors(methodDecl, cu.getProblems())) {
          // Skip any methods that already have JDT errors
          continue;
        }
        validateReturnType(methodDecl);
        validateParameterList(methodDecl);
        validateResourceFile(methodDecl);
      }

      return true;
    }

    /**
     * Validates that a ClientBundle method has zero arguments.
     */
    private void validateParameterList(MethodDeclaration methodDecl) {
      if (methodDecl.parameters().size() > 0) {
        result.addProblem(ClientBundleProblem.createNonEmptyParameterList(methodDecl));
      }
    }

    /**
     * Validates the existence of the resource file backing an accessor method.
     * We also index the file by its classpath-relative path so we know to
     * re-validate this compilation unit when it changes. We need to do this
     * whether the file currently exists or not.
     */
    @SuppressWarnings("unchecked")
    private void validateResourceFile(MethodDeclaration methodDecl) {
      // Only need to worry about methods that return a ResourceProtoype (except
      // GwtCreateResource, which does not have a backing resource file).
      ITypeBinding returnTypeBinding = getReturnTypeBinding(methodDecl);
      if (returnTypeBinding == null
          || !ClientBundleUtilities.isResourceType(returnTypeBinding)
          || ClientBundleUtilities.isGwtCreateResource(returnTypeBinding)) {
        return;
      }

      try {
        // Look for an explicit @Source annotation
        List<ASTNode> modifiers = (List<ASTNode>) methodDecl.getStructuralProperty(methodDecl.getModifiersProperty());
        for (ASTNode node : modifiers) {
          if (isSourceAnnotation(node)) {
            validateSourceAnnotationValues((Annotation) node);
            return;
          }
        }

        // If no @Source, try locating resource file via via naming conventions
        // and resource types' default extensions
        String resourceTypeName = returnTypeBinding.getErasure().getQualifiedName();
        IType resourceType = JavaModelSearch.findType(javaProject,
            resourceTypeName);
        if (resourceType == null) {
          // Shouldn't happen since we already resolved the return type binding,
          // but we'll be defensive here anyway
          GWTPluginLog.logError("Could not find type " + resourceTypeName);
          return;
        }
        IPath pckgPath = getPackagePath(methodDecl);
        String methodName = methodDecl.getName().getIdentifier();

        String[] defaultExtensions = ResourceTypeDefaultExtensions.getDefaultExtensions(resourceType);
        if (defaultExtensions.length > 0) {
          // Compute resource path with each default extension, and add to index
          Set<IPath> possibleResourcePaths = new HashSet<IPath>();
          for (String defaultExtension : defaultExtensions) {
            possibleResourcePaths.add(pckgPath.append(methodName
                + defaultExtension));
          }
          result.addAllPossibleResourcePaths(possibleResourcePaths);

          // Now see if there's actually a resource at any of those paths
          for (IPath possibleResourcePath : possibleResourcePaths) {
            if (ClasspathResourceUtilities.isResourceOnClasspath(javaProject,
                possibleResourcePath)) {
              // We found a matching resource with one of the default
              // extensions, so no errors on this method.
              return;
            }
          }
        } else {
          // If the resource type doesn't define any default extensions, there
          // must be an @Source annotation.
          result.addProblem(ClientBundleProblem.createSourceAnnotationRequired(
              methodDecl, resourceType.getFullyQualifiedName()));
          return;
        }

        // Couldn't find any matching files with the default extensions
        String expectedFileName = computeExpectedFileNameArgument(methodName,
            defaultExtensions);
        result.addProblem(ClientBundleProblem.createMissingResourceFile(
            methodDecl.getName(), expectedFileName, pckgPath));
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
      }
    }

    /**
     * Validates that a ClientBundle method returns either a resource type or
     * another ClientBundle.
     */
    private void validateReturnType(MethodDeclaration methodDecl) {
      ITypeBinding returnTypeBinding = getReturnTypeBinding(methodDecl);
      if (returnTypeBinding == null) {
        // We shouldn't be checking the method if it already had errors, but
        // just in case, let's bail out here if we can't resolve the type.
        return;
      }

      // Return type should be a resource type or a nested ClientBundle
      if (ClientBundleUtilities.isResourceType(returnTypeBinding)
          || ClientBundleUtilities.isClientBundle(returnTypeBinding)) {
        return;
      }

      // Anything else results in an error
      result.addProblem(ClientBundleProblem.createInvalidReturnType(methodDecl.getReturnType2()));
    }

    private void validateSourceAnnotationValue(StringLiteral literalNode)
        throws JavaModelException {
      String value = literalNode.getLiteralValue();

      // Interpret the literal path as an absolute path, and then as a path
      // relative to the containing package (indexing both).
      IPath literalPath = new Path(value);
      result.addPossibleResourcePath(literalPath);
      IPath fullResourcePathIfPackageRelative = getPackagePath(literalNode).append(
          literalPath);
      result.addPossibleResourcePath(fullResourcePathIfPackageRelative);

      // If the @Source path was absolute and we found it, great
      if (ClasspathResourceUtilities.isResourceOnClasspath(javaProject,
          literalPath)) {
        return;
      }

      if (!ClasspathResourceUtilities.isResourceOnClasspath(javaProject,
          fullResourcePathIfPackageRelative)) {
        // Didn't work as a relative path either, so now it's an error
        IPath expectedResourcePath = computeMissingResourceExpectedPath(
            literalPath, fullResourcePathIfPackageRelative);
        ClientBundleProblem problem = ClientBundleProblem.createMissingResourceFile(
            literalNode, literalPath.lastSegment(),
            expectedResourcePath.removeLastSegments(1));
        result.addProblem(problem);
      }
    }

    @SuppressWarnings("unchecked")
    private void validateSourceAnnotationValues(Annotation annotation)
        throws JavaModelException {
      Expression exp = JavaASTUtils.getAnnotationValue(annotation);
      if (exp == null) {
        return;
      }

      // There will usually just be one string value
      if (exp instanceof StringLiteral) {
        validateSourceAnnotationValue((StringLiteral) exp);
      }

      // But there could be multiple values; if so, check each one.
      if (exp instanceof ArrayInitializer) {
        ArrayInitializer array = (ArrayInitializer) exp;

        for (Expression item : (List<Expression>) array.expressions()) {
          if (item instanceof StringLiteral) {
            validateSourceAnnotationValue((StringLiteral) item);
          }
        }
      }
    }
  }

  /**
   * Validation entry point.
   */
  public ValidationResult validate(CompilationUnit cu) {
    ICompilationUnit icu = JavaASTUtils.getCompilationUnit(cu);
    ClientBundleValidationVisitor visitor = new ClientBundleValidationVisitor(
        icu.getJavaProject());
    cu.accept(visitor);
    ClientBundleValidationResult result = visitor.getResult();

    ClientBundleResourceDependencyIndex.getInstance().putResourcesForCompilationUnit(
        icu, result.getPossibleResourcePaths());

    return result;
  }

}
