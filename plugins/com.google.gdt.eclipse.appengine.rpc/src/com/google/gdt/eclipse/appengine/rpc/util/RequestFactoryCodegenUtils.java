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

import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.CodeGeneration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods used in codegen on RequestFactory classes
 */
public class RequestFactoryCodegenUtils {

  private static String lineDelimiter = System.getProperty(
      "line.separator", "\n"); //$NON-NLS-N$

  /**
   * Construct the signature for method added to service class, to be added to
   * the requestcontext
   */
  public static String constructMethodSignature(IMethod serviceMethod,
      List<IType> projectEntities) {

    StringBuffer buf = new StringBuffer();
    try {

      buf.append("Request<");
      String methodName = serviceMethod.getElementName();
      String signature = Signature.toString(serviceMethod.getSignature(),
          methodName, serviceMethod.getParameterNames(), false, true);
      signature = signature.replace(methodName, "");
      for (IType entity : projectEntities) {
        if (signature.contains(entity.getElementName())) {
          signature = signature.replace(entity.getElementName(),
              entity.getElementName() + "Proxy");
        }
      }
      signature = signature.replace("void", "Void");
      int index = signature.indexOf(" ");
      signature = signature.substring(0, index) + "> " + methodName
          + signature.substring(index) + ";";
      buf.append(signature);

    } catch (JavaModelException e) {
      AppEngineRPCPlugin.log(e);
    }
    return buf.toString();
  }

  public static String constructRequestForEntity(String name) {
    StringBuffer buf = new StringBuffer();
    String proxyName = name + "Proxy"; //$NON-NLS-N$
    buf.append("Request");//$NON-NLS-N$
    buf.append("<").append(proxyName).append(">"); //$NON-NLS-N$
    buf.append(" create").append(name).append("();"); //$NON-NLS-N$
    buf.append(lineDelimiter);
    buf.append("Request");//$NON-NLS-N$
    buf.append("<").append(proxyName).append(">"); //$NON-NLS-N$
    buf.append(" read").append(name).append("(Long id);"); //$NON-NLS-N$
    buf.append(lineDelimiter);
    buf.append("Request");//$NON-NLS-N$
    buf.append("<").append(proxyName).append(">"); //$NON-NLS-N$
    buf.append(" update").append(name).append("("); //$NON-NLS-N$
    buf.append(proxyName).append(" ").append(name.toLowerCase()).append(");"); //$NON-NLS-N$
    buf.append(lineDelimiter);
    buf.append("Request");//$NON-NLS-N$
    buf.append("<Void>"); //$NON-NLS-N$
    buf.append(" delete").append(name).append("("); //$NON-NLS-N$
    buf.append(proxyName).append(" ").append(name.toLowerCase()).append(");"); //$NON-NLS-N$
    buf.append(lineDelimiter);
    buf.append("Request");//$NON-NLS-N$
    buf.append("<"); //$NON-NLS-N$
    buf.append("List");//$NON-NLS-N$
    buf.append("<").append(proxyName).append(">"); //$NON-NLS-N$
    buf.append(">"); //$NON-NLS-N$
    buf.append(" query").append(name).append("s();"); //$NON-NLS-N$
    buf.append(lineDelimiter);
    return buf.toString();
  }

  /**
   * Constructs the service methods for the given entity
   */
  public static String constructServiceMethods(IType entity, IMethod method)
      throws CoreException {

    StringBuffer buf = new StringBuffer();
    String name = entity.getElementName();
    ICompilationUnit cu = method.getCompilationUnit();
    String qualifiedName = method.getDeclaringType().getTypeQualifiedName('.');
    // create
    buf.append("public static "); //$NON-NLS-1$
    buf.append(name);
    buf.append(" create").append(name); //$NON-NLS-N$
    buf.append("() {").append(lineDelimiter); //$NON-NLS-N$
    String content = CodeGeneration.getMethodBodyContent(cu, qualifiedName,
        "create", false, "", lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
    if (content != null && content.length() != 0)
      buf.append(content);
    buf.append("return null;"); //$NON-NLS-1$
    buf.append(lineDelimiter);
    buf.append("}"); //$NON-NLS-1$
    // read
    buf.append("public static "); //$NON-NLS-1$
    buf.append(name);
    buf.append(" read").append(name); //$NON-NLS-N$
    buf.append("(Long id) {").append(lineDelimiter); //$NON-NLS-N$
    content = CodeGeneration.getMethodBodyContent(cu, qualifiedName, "read",
        false, "", lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
    if (content != null && content.length() != 0) {
      buf.append(content);
    }
    buf.append("return null;"); //$NON-NLS-1$
    buf.append(lineDelimiter);
    buf.append("}"); //$NON-NLS-1$
    // update
    buf.append("public static "); //$NON-NLS-1$
    buf.append(name);
    buf.append(" update").append(name); //$NON-NLS-N$
    buf.append("(");
    buf.append(name).append(" ").append(name.toLowerCase()).append(") {");
    buf.append(lineDelimiter); //$NON-NLS-N$
    content = CodeGeneration.getMethodBodyContent(cu, qualifiedName, "read",
        false, "", lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
    if (content != null && content.length() != 0) {
      buf.append(content);
    }
    buf.append("return null;"); //$NON-NLS-1$
    buf.append(lineDelimiter);
    buf.append("}"); //$NON-NLS-1$
    // delete
    buf.append("public static void"); //$NON-NLS-1$
    buf.append(" delete").append(name); //$NON-NLS-N$
    buf.append("(");
    buf.append(name).append(" ").append(name.toLowerCase()).append(") {");
    buf.append(lineDelimiter); //$NON-NLS-N$
    content = CodeGeneration.getMethodBodyContent(cu, qualifiedName, "read",
        false, "", lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
    if (content != null && content.length() != 0) {
      buf.append(content);
    }
    buf.append(lineDelimiter);
    buf.append("}"); //$NON-NLS-1$
    // query
    buf.append("public static "); //$NON-NLS-1$
    buf.append("List<").append(name).append(">"); //$NON-NLS-N$
    buf.append(" query").append(name).append("s"); //$NON-NLS-1$ //$NON-NLS-2$
    buf.append("() {");
    buf.append(lineDelimiter); //$NON-NLS-N$
    content = CodeGeneration.getMethodBodyContent(cu, qualifiedName, "read",
        false, "", lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
    if (content != null && content.length() != 0) {
      buf.append(content);
    }
    buf.append("return null;"); //$NON-NLS-1$
    buf.append(lineDelimiter);
    buf.append("}"); //$NON-NLS-1$
    buf.append(lineDelimiter);
    return buf.toString();
  }

  /**
   * Check method to see if there are any entities that need to be proxied
   */
  public static List<IType> getEntitiesInMethodSignature(IMethod serviceMethod,
      List<IType> projectEntities) throws JavaModelException {
    List<IType> typeList = new ArrayList<IType>();
    Set<String> typeNames = new HashSet<String>(
        signatureToString(JavaUtils.getParamsAndReturnTypeNames(serviceMethod)));
    for (IType type : projectEntities) {
      if (typeNames.contains(type.getElementName())) {
        typeList.add(type);
      }
    }
    return typeList;
  }

  public static List<String> signatureToString(Iterable<String> signatures) {
    List<String> list = new ArrayList<String>();
    for (String signature : signatures) {
      list.add(Signature.toString(signature));
    }
    return list;
  }

}
