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
package com.google.gdt.eclipse.core;

import com.google.gdt.eclipse.core.markers.GdtProblemSeverities;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtProblemType;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.text.MessageFormat;

/**
 * Helper methods for creating problem markers on resources.
 */
public class MarkerUtilities {

  /**
   * String attribute which stores additional information about the problem
   * given by the <code>PROBLEM_TYPE_ID</code>. This attribute is read by quick
   * fix resolution generators.
   */
  public static final String PROBLEM_TYPE_DATA = "problemTypeData";

  /**
   * String attribute which stores an <code>id</code> for the type of problem.
   * This attribute is read by quick fix resolution generators.
   */
  public static final String PROBLEM_TYPE_ID = "problemTypeId";

  /**
   * Clears all markers with the given markerID from the project and all of its
   * child resources.
   */
  public static void clearMarkers(String markerID, IProject project)
      throws CoreException {
    project.deleteMarkers(markerID, false, IResource.DEPTH_INFINITE);
  }

  /**
   * Create a marker with the given markerID and problem type on a resource.
   * 
   * @return the generated marker, or <code>null</code> if the severity for the
   *         problem type is {@link GdtProblemSeverity#IGNORE}.
   */
  public static IMarker createMarker(String markerID,
      IGdtProblemType problemType, IResource resource, String... messageArgs)
      throws CoreException {
    // Look up the problem severity in the workspace settings
    GdtProblemSeverity severity = GdtProblemSeverities.getInstance().getSeverity(
        problemType);

    if (severity != GdtProblemSeverity.IGNORE) {
      String message = MessageFormat.format(problemType.getMessage(),
          (Object[]) messageArgs);
      return createMarker(markerID, resource, message,
          severity.getMarkerSeverity());
    }
    return null;
  }

  /**
   * Create a marker with the given markerID on a resource.
   */
  public static IMarker createMarker(String markerID, IResource resource,
      String message, int severity) throws CoreException {
    IMarker marker = resource.createMarker(markerID);
    marker.setAttribute(IMarker.SEVERITY, severity);
    marker.setAttribute(IMarker.MESSAGE, message);
    return marker;
  }

  /**
   * Create a marker with the given markerID on a resource. The marker will be
   * determined by the severity. The <code>problemType.getProblemId()</code> and
   * <code>problemTypeData</code> are stored as attributes on the marker.
   * 
   * @return the generated marker, or <code>null</code> if the severity for the
   *         problem type is {@link GdtProblemSeverity#IGNORE}.
   */
  public static IMarker createQuickFixMarker(String markerID,
      IGdtProblemType problemType, String problemTypeData, IResource resource,
      String... messageArgs) throws CoreException {
    IMarker marker = createMarker(markerID, problemType, resource, messageArgs);
    if (marker != null) {
      marker.setAttribute(PROBLEM_TYPE_ID, problemType.getProblemId());
      marker.setAttribute(PROBLEM_TYPE_DATA, problemTypeData);
      return marker;
    }
    return null;
  }

  /**
   * Finds the first marker matching the given position and message.
   * 
   * @param start the start, or -1 if the marker does not contain this attribute
   * @param end the end, or -1 if the marker does not contain this attribute
   * @param message the message, or null if the marker does not contain this
   *          attribute
   */
  public static IMarker findMarker(String markerId, int start, int end,
      IResource resource, String message, boolean includeSubtypes)
      throws CoreException {
    IMarker[] markers = resource.findMarkers(markerId, includeSubtypes,
        IResource.DEPTH_ZERO);
    for (IMarker marker : markers) {
      int curStart = marker.getAttribute(IMarker.CHAR_START, -1);
      if (curStart != start) {
        continue;
      }

      int curEnd = marker.getAttribute(IMarker.CHAR_END, -1);
      if (curEnd != end) {
        continue;
      }

      String curMsg = marker.getAttribute(IMarker.MESSAGE, null);
      if (curMsg == null || !message.equals(curMsg)) {
        continue;
      }

      return marker;
    }

    return null;
  }

}
