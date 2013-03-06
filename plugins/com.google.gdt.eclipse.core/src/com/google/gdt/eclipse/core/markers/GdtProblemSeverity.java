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
package com.google.gdt.eclipse.core.markers;

import org.eclipse.core.resources.IMarker;

/**
 * Defines the severity levels a problem can have.
 */
public enum GdtProblemSeverity {
  ERROR(2, "Error", IMarker.SEVERITY_ERROR), IGNORE(0, "Ignore", -1), WARNING(
      1, "Warning", IMarker.SEVERITY_WARNING);

  public static GdtProblemSeverity getSeverity(int severityId) {
    for (GdtProblemSeverity severity : GdtProblemSeverity.values()) {
      if (severity.severityId == severityId) {
        return severity;
      }
    }

    return null;
  }

  private final String displayName;

  private final int markerSeverity;

  private final int severityId;

  private GdtProblemSeverity(int severityId, String displayName,
      int markerSeverity) {
    this.severityId = severityId;
    this.displayName = displayName;
    this.markerSeverity = markerSeverity;
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the marker severity constant (defined by {@link IMarker}). Invalid
   * for IGNORE.
   */
  public int getMarkerSeverity() {
    return markerSeverity;
  }

  public int getSeverityId() {
    return severityId;
  }

  @Override
  public String toString() {
    return displayName;
  }
}