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

import com.google.gwt.eclipse.core.platformproxy.refactoring.JsniTypeReferenceChangeFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;

/**
 * Moves Java types referenced from JSNI blocks.
 */
public class GWTMoveTypeInJsniParticipant extends GWTMoveTypeParticipant {

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    GWTTypeRefactoringSupport support = getRefactoringSupport();
    IRefactoringChangeFactory changeFactory = new JsniTypeReferenceChangeFactory(
        support);
    return support.createChange(this, changeFactory);
  }

  @Override
  public GWTTypeRefactoringSupport createRefactoringSupport() {
    return new GWTTypeRefactoringSupport();
  }

  @Override
  public String getName() {
    return "Update references in JSNI methods";
  }

}
