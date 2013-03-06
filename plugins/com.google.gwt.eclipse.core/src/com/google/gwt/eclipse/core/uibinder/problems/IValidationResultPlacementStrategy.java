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
package com.google.gwt.eclipse.core.uibinder.problems;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Interface for strategies to place validation results which notify the user of
 * issues.
 * 
 * @param <M> the type of the concrete validation result created by the strategy
 */
public interface IValidationResultPlacementStrategy<M> {

  /**
   * Clears the implementor's validation results on a resource.
   * 
   * @param resource the resource which should be cleared of validation results
   */
  void clearValidationResults(IResource resource);

  /**
   * Places the given validation result based on the implementor's strategy.
   * 
   * @param resource the resource to which the validation result applies
   * @param document the document corresponding to the resource
   * @param position the position within the resource
   * @param message the message for the validation result
   * @param severity the severity level, one of the
   *          {@link org.eclipse.core.resources.IMarker} constants
   */
  M placeValidationResult(IResource resource, IDocument document,
      IRegion position, String message, int severity);
}
