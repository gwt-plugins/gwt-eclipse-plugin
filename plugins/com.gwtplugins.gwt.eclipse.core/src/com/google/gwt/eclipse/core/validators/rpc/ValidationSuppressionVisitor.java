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
package com.google.gwt.eclipse.core.validators.rpc;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

/**
 * Visitor which processes annotations to determine if RPC validation should be
 * suppressed.
 */
final class ValidationSuppressionVisitor extends ASTVisitor {
  private boolean suppressValidation;

  public boolean shouldSuppressValidation() {
    return suppressValidation;
  }

  @Override
  public boolean visit(NormalAnnotation node) {
    IAnnotationBinding resolvedAnnotationBinding = node.resolveAnnotationBinding();
    processAnnotationBinding(resolvedAnnotationBinding);
    
    // Don't visit this node's children; they don't impact the result
    return false;
  }

  @Override
  public boolean visit(SingleMemberAnnotation node) {
    IAnnotationBinding resolvedAnnotationBinding = node.resolveAnnotationBinding();
    processAnnotationBinding(resolvedAnnotationBinding);
    
    // Don't visit this node's children; they don't impact the result    
    return false;
  }

  private boolean computeSuppressWarning(
      IMemberValuePairBinding iMemberValuePairBinding) {
    if (!"value".equals(iMemberValuePairBinding.getName())) {
      return false;
    }

    Object value = iMemberValuePairBinding.getValue();
    return findString(value, "rpc-validation");
  }

  private boolean findString(Object o, String string) {
    if (o instanceof Object[]) {
      for (Object v : (Object[]) o) {
        if (findString(v, string)) {
          return true;
        }
      }
    } else if (o instanceof String) {
      return o.equals(string);
    }
    return false;
  }

  private void processAnnotationBinding(
      IAnnotationBinding resolvedAnnotationBinding) {
    if (resolvedAnnotationBinding != null) {
      ITypeBinding annotationType = resolvedAnnotationBinding.getAnnotationType();
      if (annotationType != null) {
        if (annotationType.getQualifiedName().equals(
            SuppressWarnings.class.getName())) {
          IMemberValuePairBinding[] allMemberValuePairs = resolvedAnnotationBinding.getAllMemberValuePairs();
          for (IMemberValuePairBinding iMemberValuePairBinding : allMemberValuePairs) {
            if (computeSuppressWarning(iMemberValuePairBinding)) {
              suppressValidation = true;
              break;
            }
          }
        }
      }
    }
  }
}