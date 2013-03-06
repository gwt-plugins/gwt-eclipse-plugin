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
package com.google.gwt.eclipse.core.util;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.ui.IFileEditorMapping;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.registry.EditorRegistry;
import org.eclipse.ui.internal.registry.FileEditorMapping;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Utility functions for the GWT plug-in.
 * 
 * TODO(): Unify with the existing utilities classes in the gdt core plugin.
 */
@SuppressWarnings("restriction")
public final class Util {

  public static final String PLATFORM_LINUX = "linux";

  public static final String PLATFORM_MAC = "mac";

  public static final String PLATFORM_WINDOWS = "windows";

  public static final List<String> VALID_ASYNC_RPC_RETURN_TYPES = Arrays.asList(
      "void", "com.google.gwt.http.client.Request",
      "com.google.gwt.http.client.RequestBuilder");

  /**
   * Sync method has same return type as parameterization of last async
   * parameter (AsyncCallback). If the async callback parameter type is raw,
   * just assume sync return type of void.
   * 
   * @param ast {@link AST} associated with the destination compilation unit
   * @param asyncMethod the GWT RPC async method declaration
   * @param imports {@link ImportRewrite} associated with the destination
   *          compilation unit
   * @return the computed return {@link Type}
   */
  public static Type computeSyncReturnType(AST ast,
      MethodDeclaration asyncMethod, ImportRewrite imports) {
    Type returnType = ast.newPrimitiveType(PrimitiveType.VOID);
    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> asyncParameters = asyncMethod.parameters();

    // Check for no parameters on async method... just in case
    if (asyncParameters.isEmpty()) {
      return returnType;
    }

    // Grab the last parameter type, which should be the callback
    Type callbackType = asyncParameters.get(asyncParameters.size() - 1).getType();

    // Make sure we have a parameterized callback type; otherwise, we can't
    // infer the return type of the sync method.
    if (callbackType.isParameterizedType()) {
      ParameterizedType callbackParamType = (ParameterizedType) callbackType;

      ITypeBinding callbackBinding = callbackParamType.getType().resolveBinding();
      if (callbackBinding == null) {
        return returnType;
      }

      // Make sure the callback is of type AsyncCallback
      String callbackBaseTypeName = callbackBinding.getErasure().getQualifiedName();
      if (callbackBaseTypeName.equals(RemoteServiceUtilities.ASYNCCALLBACK_QUALIFIED_NAME)) {
        @SuppressWarnings("unchecked")
        List<Type> callbackTypeArgs = callbackParamType.typeArguments();

        // Make sure we only have one type argument
        if (callbackTypeArgs.size() == 1) {
          Type callbackTypeParameter = callbackTypeArgs.get(0);

          // Check for primitive wrapper type; if we have one use the actual
          // primitive for the sync return type.
          // TODO(): Maybe used linked mode to let the user choose whether to
          // return the primitive or its wrapper type.
          String qualifiedName = callbackTypeParameter.resolveBinding().getQualifiedName();
          String primitiveTypeName = JavaASTUtils.getPrimitiveTypeName(qualifiedName);
          if (primitiveTypeName != null) {
            return ast.newPrimitiveType(PrimitiveType.toCode(primitiveTypeName));
          }

          returnType = JavaASTUtils.normalizeTypeAndAddImport(ast,
              callbackTypeParameter, imports);
        }
      }
    }

    return returnType;
  }

  /**
   * Creates a GWT RPC async callback parameter declaration based on the sync
   * method return type.
   * 
   * @param ast {@link AST} associated with the destination compilation unit
   * @param syncReturnType the sync method return type
   * @param callbackParameterName name of the callback parameter
   * @param imports {@link ImportsRewrite} for the destination compilation unit
   * @return callback paramter declaration
   */
  @SuppressWarnings("unchecked")
  public static SingleVariableDeclaration createAsyncCallbackParameter(AST ast,
      Type syncReturnType, String callbackParameterName, ImportRewrite imports) {
    ITypeBinding syncReturnTypeBinding = syncReturnType.resolveBinding();

    SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();

    String gwtCallbackTypeSig = Signature.createTypeSignature(
        RemoteServiceUtilities.ASYNCCALLBACK_QUALIFIED_NAME, true);
    Type gwtCallbackType = imports.addImportFromSignature(gwtCallbackTypeSig,
        ast);

    if (syncReturnTypeBinding.isPrimitive()) {
      String wrapperName = JavaASTUtils.getWrapperTypeName(syncReturnTypeBinding.getName());
      String wrapperTypeSig = Signature.createTypeSignature(wrapperName, true);
      syncReturnType = imports.addImportFromSignature(wrapperTypeSig, ast);
    } else {
      syncReturnType = JavaASTUtils.normalizeTypeAndAddImport(ast,
          syncReturnType, imports);
    }

    ParameterizedType type = ast.newParameterizedType(gwtCallbackType);
    List typeArgs = type.typeArguments();
    typeArgs.add(syncReturnType);

    parameter.setType(type);
    parameter.setName(ast.newSimpleName(callbackParameterName));

    return parameter;
  }

  /**
   * For a given jar, return the path to a peer jar that contains sources. For
   * example, if a path of <code>/tmp/foo.jar</code> is passed in, then this
   * method will return <code>/tmp/foo-sources.jar</code> if it exists, or
   * <code>null</code> otherwise.
   * 
   * The convention is that a sources jar will have the same name as the binary
   * jar, except with a suffix of <code>-sources</code> before the extension.
   */
  public static IPath findSourcesJarForClassesJar(IPath jarPath) {
    String jarName = jarPath.lastSegment();
    int indexOfJarExtension = jarName.lastIndexOf(".jar");

    if (indexOfJarExtension > -1) {
      String sourceJarName = jarName.substring(0, indexOfJarExtension)
          + "-sources.jar";
      IPath sourceJarPath = jarPath.removeLastSegments(1).append(sourceJarName);
      if (sourceJarPath.toFile().exists()) {
        return sourceJarPath;
      }
    }

    return null;
  }

  public static IPath getAbsoluteFileSystemPath(IPath workspaceRelativePath) {
    assert (workspaceRelativePath.segmentCount() > 0);
    String projectName = workspaceRelativePath.segment(0);

    IProject project = Util.getWorkspaceRoot().getProject(projectName);
    assert (project.exists());

    IPath projectFileSystemPath = project.getLocation();
    IPath projectRelativePath = workspaceRelativePath.removeFirstSegments(1);
    return projectFileSystemPath.append(projectRelativePath);
  }

  /**
   * Returns the callback parameter declaration from an async method.
   * 
   * @param method async method declaration
   * @return callback parameter declaration
   */
  public static SingleVariableDeclaration getCallbackParameter(
      MethodDeclaration method) {

    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> asyncParameters = method.parameters();
    if (asyncParameters.isEmpty()) {
      return null;
    }

    // Grab the last parameter type, which should be the callback
    SingleVariableDeclaration callback = asyncParameters.get(asyncParameters.size() - 1);

    ITypeBinding callbackBinding = callback.getType().resolveBinding();
    if (callbackBinding == null) {
      return null;
    }

    // Make sure the callback is of type AsyncCallback
    String callbackBaseTypeName = callbackBinding.getErasure().getQualifiedName();

    if (RemoteServiceUtilities.ASYNCCALLBACK_QUALIFIED_NAME.equals(callbackBaseTypeName)) {
      return callback;
    }

    return null;
  }

  public static String getDevJarName(IPath sdkLocation) {
    IPath devJarPath = sdkLocation.append(GWTRuntime.GWT_DEV_NO_PLATFORM_JAR);

    if (devJarPath.toFile().exists()) {
      return devJarPath.lastSegment();
    }

    return "gwt-dev-" + getPlatformName() + ".jar";
  }

  /**
   * Gets the platform name: windows, mac, or linux.
   */
  public static String getPlatformName() {
    String os = Platform.getOS();
    if (Platform.OS_WIN32.equals(os)) {
      return PLATFORM_WINDOWS;
    }
    if (Platform.OS_MACOSX.equals(os)) {
      return PLATFORM_MAC;
    }
    if (Platform.OS_LINUX.equals(os)) {
      return PLATFORM_LINUX;
    }

    // If it's not one of the official GWT-supported platforms, just return the
    // the platform name as reported to us by Eclipse
    return os;
  }

  public static IProject getProject(IPath workspaceRelativePath) {
    IFile file = getWorkspaceRoot().getFile(workspaceRelativePath);
    if (!file.exists()) {
      return null;
    }

    return file.getProject();
  }

  /**
   * Returns the names of the validation jars that are co-located with the GWT
   * SDK's core jars. If the validation jars are present, there should be two of
   * them: <code>validation-api-<version>.jar</code> and
   * <code>validation-api-<version>-sources.jar</code>.
   * 
   * If the validation jars are not present (i.e. this is a pre-GWT 2.3 SDK),
   * then an empty array is returned.
   */
  public static String[] getValidationJarNames(IPath sdkLocation) {
    File sdkDir = sdkLocation.toFile();
    if (!sdkDir.exists()) {
      return null;
    }

    String[] validationJarNames = sdkDir.list(new FilenameFilter() {
      public boolean accept(File file, String fileName) {
        if (fileName.startsWith(GWTRuntime.VALIDATION_API_JAR_PREFIX)
            && fileName.endsWith(".jar")
            && file.exists()) {
          return true;
        }
        return false;
      }
    });

    return validationJarNames;
  }

  public static IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  public static boolean isPlatformMac() {
    return PLATFORM_MAC.equals(getPlatformName());
  }

  public static boolean isValidMethodName(String methodName) {
    String complianceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.compliance");
    String sourceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.source");

    return JavaConventions.validateMethodName(methodName, sourceLevel,
        complianceLevel).isOK();
  }

  public static boolean isValidPackageName(String packageName) {
    // As long as the returned status is not an ERROR status, this is a valid
    // package name (i.e. WARNING statuses are okay).
    return !(validatePackageName(packageName).matches(IStatus.ERROR));
  }

  public static boolean isValidTypeName(String typeName) {
    String complianceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.compliance");
    String sourceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.source");

    return JavaConventions.validateJavaTypeName(typeName, sourceLevel,
        complianceLevel).isOK();
  }

  // TODO: remove this and defer to the one in StringUtilities
  public static String join(Iterable<?> items, String delimiter) {
    StringBuffer buffer = new StringBuffer();
    Iterator<?> iter = items.iterator();
    if (iter.hasNext()) {
      buffer.append(iter.next().toString());
      while (iter.hasNext()) {
        buffer.append(delimiter);
        buffer.append(iter.next().toString());
      }
    }
    return buffer.toString();
  }

  // TODO: remove this and defer to the one in StringUtilities
  public static String join(Object[] items, String delimiter) {
    return join(Arrays.asList(items), delimiter);
  }

  public static IStatus newErrorStatus(String message) {
    return new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID, message);
  }

  public static IStatus newErrorStatus(String message, Object... args) {
    return newErrorStatus(MessageFormat.format(message, args));
  }

  public static IStatus newWarningStatus(String message) {
    return new Status(IStatus.WARNING, GWTPlugin.PLUGIN_ID, message);
  }

  public static String removeFileExtension(String filename) {
    return removeFileExtension(new Path(filename)).toString();
  }

  public static void resetDefaultEditor(String extension) {
    EditorRegistry editorRegistry = (EditorRegistry) PlatformUI.getWorkbench().getEditorRegistry();
    IFileEditorMapping[] editorMappings = editorRegistry.getFileEditorMappings();

    // Search the file=>editor mappings for the specified extension
    for (IFileEditorMapping editorMapping : editorMappings) {
      if (extension.equals(editorMapping.getExtension())) {
        FileEditorMapping internalMapping = (FileEditorMapping) editorMapping;
        // Only need to do anything if there's an explicit default set
        if (internalMapping.getDeclaredDefaultEditors().length > 0) {
          // Clear any default editor associations for this extension
          internalMapping.setDefaultEditors(new ArrayList<Object>());

          // Save the updated editor registry to disk
          editorRegistry.saveAssociations();

          // TODO: remove
          GWTPluginLog.logInfo("Reset default editor for extension: "
              + extension);
        }
        break;
      }
    }
  }

  public static IStatus validatePackageName(String packageName) {
    if (packageName.length() > 0) {

      String sourceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.compliance");
      String compliance = JavaCore.getOption("org.eclipse.jdt.core.compiler.source");

      return JavaConventions.validatePackageName(packageName, sourceLevel,
          compliance);
    }

    return newWarningStatus(NewWizardMessages.NewTypeWizardPage_warning_DefaultPackageDiscouraged);
  }

  private static IPath removeFileExtension(IPath path) {
    IPath ret = new Path(path.toString());

    // Remove compound file extensions
    while (ret.getFileExtension() != null) {
      ret = ret.removeFileExtension();
    }

    return ret;
  }

  private Util() {
    // Not instantiable
  }
}
