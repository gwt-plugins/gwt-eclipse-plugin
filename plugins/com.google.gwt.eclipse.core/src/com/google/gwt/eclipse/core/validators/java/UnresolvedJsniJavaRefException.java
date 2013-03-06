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
package com.google.gwt.eclipse.core.validators.java;

import com.google.gwt.eclipse.core.markers.GWTProblemType;

/**
 * Thrown when a Java reference in JSNI cannot be resolved.
 */
@SuppressWarnings("serial")
public class UnresolvedJsniJavaRefException extends Exception {

  private final JsniJavaRef javaRef;

  private final GWTProblemType problemType;

  public UnresolvedJsniJavaRefException(GWTProblemType problemType,
      JsniJavaRef javaRef) {
    this.problemType = problemType;
    this.javaRef = javaRef;
  }

  public JsniJavaRef getJavaRef() {
    return javaRef;
  }

  /**
   * 
   * @return the problem type for this unresolved reference, or null if we
   *         should ignore this reference. For example, @null::nullMethod().
   */
  public GWTProblemType getProblemType() {
    return problemType;
  }
}