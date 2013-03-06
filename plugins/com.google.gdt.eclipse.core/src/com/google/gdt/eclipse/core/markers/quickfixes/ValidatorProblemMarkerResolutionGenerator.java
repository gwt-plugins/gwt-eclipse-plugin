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
package com.google.gdt.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.MarkerUtilities;
import com.google.gdt.eclipse.core.markers.ProjectStructureOrSdkProblemType;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates marker resolutions for markers of type
 * {@link com.google.gdt.eclipse.core.validators.WebAppProjectValidator#PROBLEM_MARKER_ID}
 * 
 * To differentiate between the different types of problems, each marker of the
 * given type has a problem id stored in the
 * {@link MarkerUtilities#PROBLEM_TYPE_ID} attribute of marker.
 * 
 * Additional data specific to the problem is stored in the
 * {@link MarkerUtilities#PROBLEM_TYPE_DATA} attribute of the marker. Not every
 * problem requires additional data.
 */
public class ValidatorProblemMarkerResolutionGenerator implements
    IMarkerResolutionGenerator {

  public IMarkerResolution[] getResolutions(IMarker marker) {

    List<IMarkerResolution> markerResolutions = new ArrayList<IMarkerResolution>();

    int problemId = marker.getAttribute(MarkerUtilities.PROBLEM_TYPE_ID, -1);
    if (problemId == -1) {
      return new IMarkerResolution[0];
    }

    ProjectStructureOrSdkProblemType problemType = ProjectStructureOrSdkProblemType.getProblemType(problemId);

    String problemTypeData = marker.getAttribute(
        MarkerUtilities.PROBLEM_TYPE_DATA, "");

    switch (problemType) {
      case MISSING_WEB_XML: {
        markerResolutions.add(new CreateWebXMLFileMarkerResolution());
        break;
      }

      case BUILD_OUTPUT_DIR_NOT_WEBINF_CLASSES: {
        markerResolutions.add(new WrongOutputDirectoryMarkerResolution());
        markerResolutions.add(new StopManagingWarOutputDirectoryResolution(
            marker.getResource().getProject()));
        break;
      }

      case JAR_OUTSIDE_WEBINF_LIB: {
        IPath buildClasspathFilePath = Path.fromPortableString(problemTypeData);
        markerResolutions.add(new CopyToServerClasspathMarkerResolution(
            buildClasspathFilePath));
        markerResolutions.add(new AddToWarningExclusionListMarkerResolution(
            buildClasspathFilePath));
        markerResolutions.add(new StopManagingWarOutputDirectoryResolution(
            marker.getResource().getProject()));
        break;
      }
    }

    return markerResolutions.toArray(new IMarkerResolution[0]);
  }
}
