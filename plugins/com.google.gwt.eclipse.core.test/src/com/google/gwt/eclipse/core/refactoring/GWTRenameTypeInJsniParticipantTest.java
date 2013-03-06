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
package com.google.gwt.eclipse.core.refactoring;

import org.eclipse.jdt.core.IType;

/**
 * Tests the {@link GWTRenameTypeInJsniParticipant} class.
 */
public class GWTRenameTypeInJsniParticipantTest extends AbstractRefactoringTestImpl {

  public void testInitializeWithInvalidElement() {
    GWTRenameTypeInJsniParticipant participant = new GWTRenameTypeInJsniParticipant();
    assertFalse(participant.initialize(getTestProject()));
  }

  // TODO: this halts the tests because Eclipse displays a warning dialog about
  // renaming classes with native methods. Once we can suppress that warning,
  // we can add this test back in
  public void testRenameContainingType() throws Exception {
    // IType oldType = refactorTestClass.getCompilationUnit().getType(
    // "RefactorTest");
    // assertTypeRename(oldType, "com.hello.client.RefactorTest2");
  }

  // TODO: this halts the tests because Eclipse displays a warning dialog about
  // renaming classes with native methods. Once we can suppress that warning,
  // we can add this test back in
  public void testRenameInnerType() throws Exception {
    // IType oldType = rClass.getCompilationUnit().getType("R").getType("B");
    // assertTypeRename(oldType, "BBB");
  }

  // TODO: this halts the tests because Eclipse displays a warning dialog about
  // renaming classes with native methods. Once we can suppress that warning,
  // we can add this test back in
  public void testRenameType() throws Exception {
    // IType oldType = rClass.getCompilationUnit().getType("R");
    // assertTypeRename(oldType, "RRR");
  }

}
