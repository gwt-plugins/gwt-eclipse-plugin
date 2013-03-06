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

import junit.framework.TestCase;

/**
 * Tests the {@link UnresolvedJsniJavaRefException} class.
 */
public class UnresolvedJsniJavaRefExceptionTest extends TestCase {

  private UnresolvedJsniJavaRefException e;

  private JsniJavaRef javaRef;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    javaRef = JsniJavaRef.parse("@some.package.SomeClass$Inner::someField");
    e = new UnresolvedJsniJavaRefException(GWTProblemType.JSNI_PARSE_ERROR,
        javaRef);
  }

  public void testGetJavaRef() {
    assertEquals(javaRef, e.getJavaRef());
  }

  public void testGetProblemType() {
    assertEquals(GWTProblemType.JSNI_PARSE_ERROR, e.getProblemType());
  }

}
