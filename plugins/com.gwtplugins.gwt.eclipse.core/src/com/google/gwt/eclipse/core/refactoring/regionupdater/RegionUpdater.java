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
package com.google.gwt.eclipse.core.refactoring.regionupdater;

import com.google.gwt.eclipse.core.refactoring.RefactoringException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * Abstract class for updating text regions (start position and length) on text
 * edits. These regions may need to be updated due to text changes in a file
 * that occurred earlier than the region.
 */
public abstract class RegionUpdater {

  private final ReplaceEdit originalEdit;
  private final ReferenceUpdater referenceUpdater;

  public RegionUpdater(ReplaceEdit originalEdit,
      ReferenceUpdater referenceUpdater) {
    this.originalEdit = originalEdit;
    this.referenceUpdater = referenceUpdater;
  }

  /**
   * Returns a new edit (based on the original edit) with text edit regions
   * updated.
   * 
   * @param newCu the current compilation unit where the original edit would be
   *          applied
   */
  public abstract ReplaceEdit createUpdatedEdit(CompilationUnit newCu)
      throws RefactoringException;

  protected ReplaceEdit getOriginalEdit() {
    return originalEdit;
  }

  protected ReferenceUpdater getReferenceUpdater() {
    return referenceUpdater;
  }
}
