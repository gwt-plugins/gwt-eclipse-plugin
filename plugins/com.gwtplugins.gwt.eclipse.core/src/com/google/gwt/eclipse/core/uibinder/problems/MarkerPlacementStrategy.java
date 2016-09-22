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

import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Strategy to place validation results as markers on a resource.
 */
public class MarkerPlacementStrategy implements
    IValidationResultPlacementStrategy<IMarker> {

  private final String markerId;

  public MarkerPlacementStrategy(String markerId) {
    this.markerId = markerId;
  }

  public void clearValidationResults(IResource resource) {
    try {
      resource.deleteMarkers(markerId, true, IResource.DEPTH_INFINITE);
    } catch (CoreException e) {
      GWTPluginLog.logError(e, "Could not clear problem markers.");
    }
  }

  public IMarker placeValidationResult(IResource resource, IDocument document,
      IRegion position, String message, int severity) {
    try {
      return createMarker(resource, document, position, message, severity);
    } catch (CoreException e) {
      GWTPluginLog.logError(e, "Could not create marker: {0} (on {1})",
          message, resource.getLocation());
    }

    return null;
  }

  private IMarker createMarker(IResource resource, IDocument document, IRegion position,
      String msg, int severity) throws CoreException {

    if (isDuplicate(resource, position, msg)) {
      return null;
    }

    IMarker marker = MarkerUtilities.createMarker(markerId, resource, msg,
        severity);
    marker.setAttribute(IMarker.CHAR_START, position.getOffset());
    marker.setAttribute(IMarker.CHAR_END, position.getOffset()
        + position.getLength());
    try {
      // +1 since the attribute is 1-relative but document.getLineOfOffset is
      // 0-relative
      marker.setAttribute(IMarker.LINE_NUMBER,
          document.getLineOfOffset(position.getOffset()) + 1);
    } catch (BadLocationException e) {
      GWTPluginLog.logWarning(e,
          "Unexpected bad location when getting line number for marker.");
    }
    return marker;
  }

  private boolean isDuplicate(IResource resource, IRegion position, String msg)
      throws CoreException {
    int start = position.getOffset();
    int end = position.getOffset() + position.getLength();

    return MarkerUtilities.findMarker(markerId, start, end, resource, msg, true) != null;
  }

}
