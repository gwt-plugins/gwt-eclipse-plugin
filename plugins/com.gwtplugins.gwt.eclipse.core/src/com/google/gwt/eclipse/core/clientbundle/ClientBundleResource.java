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
package com.google.gwt.eclipse.core.clientbundle;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities;
import com.google.gwt.eclipse.platform.clientbundle.ResourceTypeDefaultExtensions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;

import java.text.MessageFormat;
import java.util.Arrays;

/**
 * Resource to be added to a ClientBundle interface.
 */
@SuppressWarnings("restriction")
public class ClientBundleResource {

  public static ClientBundleResource create(IFile file, String methodName, String resourceTypeName) {
    // CssResource gets special treatment: we automatically synthesize custom
    // subtypes for CSS files.
    if (resourceTypeName.equals(ClientBundleUtilities.CSS_RESOURCE_TYPE_NAME)) {
      return new GeneratedCssResource(file, methodName);
    }
    return new ClientBundleResource(file, methodName, resourceTypeName);
  }

  /**
   * Creates a new ClientBundle resource from the given file, inferring its accessor method name and
   * resource type from the filename.
   *
   * NOTE: If the file is probably not intended to be bundled (e.g. a .java source file), this
   * method returns null.
   */
  public static ClientBundleResource createFromFile(IJavaProject javaProject, IFile file) {
    if (!isProbableClientBundleResource(file)) {
      return null;
    }

    String methodName = ClientBundleUtilities.suggestMethodName(file);
    String resourceTypeName = ClientBundleUtilities.suggestResourceTypeName(javaProject, file);

    return ClientBundleResource.create(file, methodName, resourceTypeName);
  }

  public static IStatus errorStatus(String message, Object... args) {
    return StatusUtilities.newErrorStatus(message, GWTPlugin.PLUGIN_ID, args);
  }

  public static boolean isProbableClientBundleResource(IFile file) {
    // The user probably doesn't want to bundle .java source files or JARs
    if (JavaCore.create(file) != null) {
      return false;
    }

    // Probably don't want to bundle module files or UI templates either
    if (ModuleUtils.isModuleXml(file) || UiBinderUtilities.isUiXml(file)) {
      return false;
    }

    return true;
  }

  private final IFile file;

  private final String methodName;

  private final String resourceTypeName;

  protected ClientBundleResource(IFile file, String methodName, String resourceTypeName) {
    this.file = file;
    this.methodName = methodName;
    this.resourceTypeName = resourceTypeName;
  }

  public void addToClientBundle(IType clientBundle, ImportsManager imports, boolean addComments,
      IProgressMonitor monitor) throws CoreException {
    String methodSource =
        MessageFormat.format("{0} {1}();", Signature.getSimpleName(getReturnTypeName()), getMethodName());

    String sourceAnnotation = getSourceAnnotationValue(clientBundle);
    if (sourceAnnotation != null) {
      // Insert the annotation above the method declaration
      methodSource = MessageFormat.format("@Source(\"{0}\")", sourceAnnotation)
          + StubUtility.getLineDelimiterUsed(clientBundle.getJavaProject()) + methodSource;
    }

    clientBundle.createMethod(methodSource, null, false, monitor);
    imports.addImport(getReturnTypeName());
  }

  public MethodDeclaration createMethodDeclaration(IType clientBundle, ASTRewrite astRewrite,
      ImportRewrite importRewrite, boolean addComments) throws CoreException {
    AST ast = astRewrite.getAST();
    MethodDeclaration methodDecl = ast.newMethodDeclaration();

    // Method is named after the resource it accesses
    methodDecl.setName(ast.newSimpleName(getMethodName()));

    // Method return type is a ResourcePrototype subtype
    ITypeBinding resourceTypeBinding = JavaASTUtils.resolveType(clientBundle.getJavaProject(), getReturnTypeName());
    Type resourceType = importRewrite.addImport(resourceTypeBinding, ast);
    methodDecl.setReturnType2(resourceType);

    // Add @Source annotation if necessary
    String sourceAnnotationValue = getSourceAnnotationValue(clientBundle);
    if (sourceAnnotationValue != null) {
      // Build the annotation
      SingleMemberAnnotation sourceAnnotation = ast.newSingleMemberAnnotation();
      sourceAnnotation.setTypeName(ast.newName("Source"));
      StringLiteral annotationValue = ast.newStringLiteral();
      annotationValue.setLiteralValue(sourceAnnotationValue);
      sourceAnnotation.setValue(annotationValue);

      // Add the annotation to the method
      ChildListPropertyDescriptor modifiers = methodDecl.getModifiersProperty();
      ListRewrite modifiersRewriter = astRewrite.getListRewrite(methodDecl, modifiers);
      modifiersRewriter.insertFirst(sourceAnnotation, null);
    }

    return methodDecl;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ClientBundleResource)) {
      return false;
    }

    ClientBundleResource other = (ClientBundleResource) obj;
    if (!JavaUtilities.equalsWithNullCheck(methodName, other.methodName)) {
      return false;
    }

    if (!JavaUtilities.equalsWithNullCheck(file, other.file)) {
      return false;
    }

    if (!JavaUtilities.equalsWithNullCheck(resourceTypeName, other.resourceTypeName)) {
      return false;
    }

    return true;
  }

  public IFile getFile() {
    return file;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getResourceTypeName() {
    return resourceTypeName;
  }

  @Override
  public int hashCode() {
    int result = 17;

    if (methodName != null) {
      result = 37 * result + methodName.hashCode();
    }
    if (file != null) {
      result = 37 * result + file.hashCode();
    }
    if (resourceTypeName != null) {
      result = 37 * result + resourceTypeName.hashCode();
    }
    return result;
  }

  public IStatus validate(IJavaProject javaProject, String[] extendedInterfaces, String[] methodNames) {
    IStatus fileStatus = validateFile(javaProject);
    IStatus methodNameStatus = validateMethodName(javaProject, extendedInterfaces, methodNames);
    IStatus resourceTypeStatus = validateResourceTypeName(javaProject);

    return StatusUtil.getMostSevere(new IStatus[] {fileStatus, methodNameStatus, resourceTypeStatus});
  }

  protected String getReturnTypeName() {
    return resourceTypeName;
  }

  private String getSourceAnnotationValue(IType clientBundle) throws JavaModelException {
    IJavaProject javaProject = clientBundle.getJavaProject();
    assert (javaProject.isOnClasspath(file));

    IPackageFragment resourcePckg = javaProject.findPackageFragment(file.getParent().getFullPath());

    // If the resource is not in the same package as our ClientBundle, we need
    // an @Source with the full classpath-relative path to the resource.
    if (!clientBundle.getPackageFragment().equals(resourcePckg)) {
      return ResourceUtils.getClasspathRelativePath(resourcePckg, file.getName()).toString();
    }

    // If the resource has a different name than the method, we need an @Source,
    // although in this case we don't need the full path.
    String fileNameWithoutExt = ResourceUtils.filenameWithoutExtension(file);
    if (!ResourceUtils.areFilenamesEqual(fileNameWithoutExt, methodName)) {
      return file.getName();
    }

    // If resource doesn't have one of the default extensions, we need @Source.
    IType resourceType = JavaModelSearch.findType(javaProject, resourceTypeName);
    if (!hasDefaultExtension(file, resourceType)) {
      return file.getName();
    }

    // If the resource is in ClientBundle package and its name (without file
    // extension) matches the method name, no need for @Source
    return null;
  }

  private boolean hasDefaultExtension(IFile file, IType resourceType) throws JavaModelException {
    String fileExtension = "." + file.getFileExtension();

    for (String defaultExtension : ResourceTypeDefaultExtensions.getDefaultExtensions(resourceType)) {
      if (ResourceUtils.areFilenamesEqual(fileExtension, defaultExtension)) {
        return true;
      }
    }
    return false;
  }

  private boolean isMethodAlreadyDefined(IJavaProject javaProject, String[] extendedInterfaces)
      throws JavaModelException {
    for (String extendedInterface : extendedInterfaces) {
      IType type = JavaModelSearch.findType(javaProject, extendedInterface);
      if (type == null) {
        // Ignore unresolved types
        continue;
      }

      boolean isInterface = false;
      try {
        isInterface = type.isInterface();
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
      }

      if (!isInterface) {
        // Ignore non-interface types
        continue;
      }

      // Find a matching method in this interface or an extended interface
      ITypeHierarchy superHierarchy = type.newSupertypeHierarchy(null);
      IMethod method = JavaModelSearch.findMethodInHierarchy(superHierarchy, type, methodName, new String[0]);
      if (method != null) {
        // We found a matching accessor method
        return true;
      }
    }
    return false;
  }

  private IStatus validateFile(IJavaProject javaProject) {
    if (file == null || !file.exists()) {
      return errorStatus("The file ''{0}'' does not exist", file.getName());
    }

    if (!javaProject.isOnClasspath(file)) {
      return errorStatus("The file ''{0}'' is not on the project''s classpath", file.getName());
    }

    return StatusUtilities.OK_STATUS;
  }

  private IStatus validateMethodName(IJavaProject javaProject, String[] extendedInterfaces, String[] methodNames) {
    assert (methodName != null);

    if (methodName.length() == 0) {
      return errorStatus("Enter the method name");
    }

    // Look for a conflict with another method we're adding
    if (Arrays.asList(methodNames).contains(methodName)) {
      return errorStatus("Another method is already named ''{0}''", methodName);
    }

    try {
      // Look for a conflict with one of our super interfaces
      if (isMethodAlreadyDefined(javaProject, extendedInterfaces)) {
        return errorStatus("Another method is already named ''{0}''", methodName);
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
      return errorStatus("Error while looking for conflicting methods.  See Eclipse log for details.");
    }

    // Validate method name according to Java conventions
    IStatus nameStatus = JavaUtilities.validateMethodName(methodName);
    if (!nameStatus.isOK()) {
      return nameStatus;
    }

    return StatusUtilities.OK_STATUS;
  }

  private IStatus validateResourceTypeName(IJavaProject javaProject) {
    assert (resourceTypeName != null);

    if (resourceTypeName.length() == 0) {
      return errorStatus("Enter the resource type name");
    }

    IType resourceType = JavaModelSearch.findType(javaProject, resourceTypeName);
    if (resourceType == null) {
      return errorStatus("Resource type ''{0}'' does not exist", resourceTypeName);
    }

    if (ClientBundleUtilities.findResourcePrototypeType(javaProject) == null) {
      return errorStatus("{0} is not on the project''s classpath", ClientBundleUtilities.RESOURCE_PROTOTYPE_TYPE_NAME);
    }

    try {
      if (!ClientBundleUtilities.isResourceType(javaProject, resourceType)) {
        return errorStatus("{0} is not a ClientBundle resource type", resourceType.getFullyQualifiedName());
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
      return errorStatus("Error while calculating resource type's super types.  See Eclipse log for details.");
    }

    return StatusUtilities.OK_STATUS;
  }
}
