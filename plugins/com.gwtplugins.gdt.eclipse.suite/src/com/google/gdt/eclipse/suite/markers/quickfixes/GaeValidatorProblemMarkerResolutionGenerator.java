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
package com.google.gdt.eclipse.suite.markers.quickfixes;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.core.markers.ProjectStructureOrSdkProblemType;
import com.google.gdt.eclipse.core.markers.quickfixes.StopManagingWarOutputDirectoryResolution;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates marker resolutions for markers of type {@link GaeProjectValidator#PROBLEM_MARKER_ID}.
 *
 * To differentiate between the different types of problems, each marker of the given type has a problem id stored in
 * the {@link MarkerUtilities#PROBLEM_TYPE_ID} attribute of marker. The problem id is stored as the string which is
 * returned by the getId() method on the {@link IMarker} values.
 *
 * Additional data specific to the problem is stored in the {@link MarkerUtilities#PROBLEM_TYPE_DATA} attribute of the
 * marker. Not every problem requires additional data.
 */
public class GaeValidatorProblemMarkerResolutionGenerator implements IMarkerResolutionGenerator {

  @Override
  public IMarkerResolution[] getResolutions(IMarker marker) {
    List<IMarkerResolution> markerResolutions = new ArrayList<IMarkerResolution>();

    int problemId = marker.getAttribute(MarkerUtilities.PROBLEM_TYPE_ID, -1);
    // if (problemId == -1) {
    // AppEngineCorePluginLog.logError("Could not retrieve PROBLEM_TYPE_ID attribute from marker on resource " +
    // marker.getResource());
    // return new IMarkerResolution[0];
    // }

    ProjectStructureOrSdkProblemType sdkProblemType = ProjectStructureOrSdkProblemType.getProblemType(problemId);
    if (sdkProblemType != null) {
      switch (sdkProblemType) {
      case NO_SDK:
      case INVALID_SDK: {
        // TODO ?
        break;
      }

      case SDK_FILE_SIZE_MISMATCH:
      case SDK_LIBRARY_MISSING_FROM_WEBINF_LIB: {
        /*
         * TODO: This block of code is similar to that in GWTValidatorMarkerResolutionGenerator. See if there is an
         * elegant refactoring that can be done.
         */
        IProject project = marker.getResource().getProject();
        IJavaProject javaProject = JavaCore.create(project);

        if (JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject)) {
          // TODO ?
        }
        markerResolutions.add(new StopManagingWarOutputDirectoryResolution(project));
        break;
      }
      }
    } else {
      // TODO ?
    }

    return markerResolutions.toArray(new IMarkerResolution[0]);
  }
}
