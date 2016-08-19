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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Tests the {@link GWTRenameParticipant} class.
 */
public class GWTRenameParticipantTest extends AbstractRefactoringTestImpl {

  private static class DummyGWTRefactoringSupport extends GWTRefactoringSupport {

    @Override
    protected TextEdit createEdit(IIndexedJavaRef ref) {
      return new ReplaceEdit(0, 0, "");
    }

    @Override
    protected String getEditDescription() {
      return "";
    }
  }

  private static class DummyRenameParticipant extends GWTRenameParticipant {

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException,
        OperationCanceledException {
      return null;
    }

    @Override
    public String getName() {
      return "";
    }

    @Override
    protected GWTRefactoringSupport createRefactoringSupport() {
      return new DummyGWTRefactoringSupport();
    }
  }

  public void testCheckConditions() {
    GWTRenameParticipant participant = new DummyRenameParticipant();
    boolean status = participant.checkConditions(null, null).isOK();
    assertTrue(status);
  }

}
