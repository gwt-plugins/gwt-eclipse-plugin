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

import com.google.gwt.eclipse.core.search.IIndexedJavaRef;
import com.google.gwt.eclipse.core.search.JavaRefIndex;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.Set;

/**
 * Tests the {@link GWTDeleteCompilationUnitParticipant} class.
 */
public class GWTDeleteCompilationUnitParticipantTest extends AbstractRefactoringTestImpl {

  public void testCheckConditions() {
    GWTDeleteCompilationUnitParticipant participant = new GWTDeleteCompilationUnitParticipant();
    RefactoringStatus status = participant.checkConditions(null, null);
    assertTrue(status.isOK());
  }

  public void testCreateChange() throws OperationCanceledException,
      CoreException {
    // We're going to delete compilation unit R.java
    ICompilationUnit cu = rClass.getCompilationUnit();
    IType r = cu.getType("R");

    // Verify that the index currently contains one JSNI reference to R
    Set<IIndexedJavaRef> refs = JavaRefIndex.getInstance().findTypeReferences(
        r.getFullyQualifiedName());
    assertEquals(1, refs.size());

    // Delete R.java
    cu.delete(true, null);
    assertFalse(cu.exists());

    // Now verify that the index entries pointing to R have been purged
    refs = JavaRefIndex.getInstance().findTypeReferences(r.getElementName());
    assertEquals(0, refs.size());
  }

  public void testInitializeWithCompilationUnit() {
    GWTDeleteCompilationUnitParticipant participant = new GWTDeleteCompilationUnitParticipant();
    ICompilationUnit cu = rClass.getCompilationUnit();
    assertTrue(participant.initialize(cu));
  }

  public void testInitializeWithInvalidElement() {
    GWTDeleteCompilationUnitParticipant participant = new GWTDeleteCompilationUnitParticipant();
    assertFalse(participant.initialize(getTestProject()));
  }

}
