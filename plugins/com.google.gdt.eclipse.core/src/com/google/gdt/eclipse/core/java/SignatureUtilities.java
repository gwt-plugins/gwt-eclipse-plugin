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
package com.google.gdt.eclipse.core.java;

import org.eclipse.jdt.core.Signature;

import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for Java signatures.
 */
public final class SignatureUtilities {

  private static final List<String> SIG_PRIMITIVE_TYPES = Arrays.asList(new String[] {
      Signature.SIG_BOOLEAN, Signature.SIG_BYTE, Signature.SIG_CHAR,
      Signature.SIG_DOUBLE, Signature.SIG_FLOAT, Signature.SIG_INT,
      Signature.SIG_LONG, Signature.SIG_SHORT});

  /**
   * Tests whether the given type is a primitive type.
   * 
   * @param typeSignature the signature of the type
   * @return true if the type is a primitive
   */
  public static boolean isPrimitiveType(String typeSignature) {
    return SIG_PRIMITIVE_TYPES.contains(typeSignature);
  }
  
  private SignatureUtilities() {
  }
}
