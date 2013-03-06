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
package com.google.gdt.eclipse.appengine.rpc.validators;

import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;
import com.google.gdt.eclipse.appengine.rpc.util.RequestFactoryUtils;
import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.markers.GWTProblemType;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that the service methods against the request factory methods
 */
@SuppressWarnings("restriction")
public class RequestFactoryValidator {

  private static class ReqFactoryValidationVisitor extends ASTVisitor {

    private final List<CategorizedProblem> problems = new ArrayList<CategorizedProblem>();

    private IType requestContext;
    private List<IType> projectEntities;
    private List<IType> proxies;

    public ReqFactoryValidationVisitor(IType request, List<IType> entities,
        List<IType> proxy) {
      requestContext = request;
      projectEntities = entities;
      proxies = proxy;
    }

    @SuppressWarnings("unchecked")
    public boolean visit(MethodDeclaration method) {
      boolean hasAnnotation = false;
      List<IExtendedModifier> modifiers = method.modifiers();
      for (IExtendedModifier modifier : modifiers) {
        if (modifier.isAnnotation()
            && ((Annotation) modifier).getTypeName().getFullyQualifiedName().equals(
                "ServiceMethod")) {
          hasAnnotation = true;
          break;
        }
      }

      if (hasAnnotation) {
        try {
          if (methodMissingInRequestFactory(method)) {
            String messageArgs = method.getName().toString();
            String problemArgs = requestContext.getElementName();
            CategorizedProblem problem = GWTJavaProblem.create(method,
                method.getName().getStartPosition(),
                method.getName().getLength(),
                GWTProblemType.REQ_FACTORY_SERVICE_METHOD_ERROR, messageArgs,
                problemArgs);
            problems.add(problem);
          }
        } catch (JavaModelException e) {
          AppEngineRPCPlugin.log(e);
        }
      }

      return true;
    }

    @Override
    public boolean visit(TypeDeclaration changedType) {
      if (changedType.isInterface()) {
        return true;
      }

      ITypeBinding typeBinding = changedType.resolveBinding();
      if (typeBinding == null) {
        return true;
      }
      return true;
    }

    private boolean methodMissingInRequestFactory(
        MethodDeclaration methodDeclaration) throws JavaModelException {

      if (requestContext != null) {
        List<SingleVariableDeclaration> params = methodDeclaration.parameters();
        String[] paramTypes = new String[params.size()];
        for (SingleVariableDeclaration param : params) {
          int i = params.indexOf(param);
          String name = param.getType().resolveBinding().getName();
          paramTypes[i] = Signature.createTypeSignature(name, false);
          for (IType type : projectEntities) {
            if (name.equals(type.getElementName())) {
              IType proxy = RequestFactoryUtils.getProxyForEntity(
                  type.getFullyQualifiedName(), proxies);
              if (proxy != null) {
                paramTypes[i] = Signature.createTypeSignature(
                    proxy.getElementName(), false);
              } else {
                paramTypes[i] = Signature.createTypeSignature(
                    name + "Proxy", false); //$NON-NLS-N$
              }
              break;
            }
          }
        }
        String methodName = methodDeclaration.getName().toString();
        IMethod requestMethod = requestContext.getMethod(methodName, paramTypes);
        IMethod[] existingMethods = requestContext.findMethods(requestMethod);
        if (existingMethods == null) {
          return true;
        }
      }
      return false;
    }
  }

  private IType requestContext;
  private List<IType> projectEntities;
  private List<IType> proxies;

  public RequestFactoryValidator(IType request, List<IType> entities,
      List<IType> proxy) {
    requestContext = request;
    projectEntities = entities;
    proxies = proxy;
  }

  public List<CategorizedProblem> validate(ASTNode ast) {

    assert (ast.getNodeType() == ASTNode.COMPILATION_UNIT);
    ReqFactoryValidationVisitor reqFactoryValidationVisitor = new ReqFactoryValidationVisitor(
        requestContext, projectEntities, proxies);
    ast.accept(reqFactoryValidationVisitor);
    return reqFactoryValidationVisitor.problems;
  }

}
