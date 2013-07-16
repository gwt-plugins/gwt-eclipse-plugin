/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.webtools.update;

import com.google.appengine.eclipse.webtools.facet.JpaFacetHelper;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.update.internal.core.UpdateQueryArgContributor;

import org.eclipse.core.resources.IProject;
import org.eclipse.jpt.jpa.core.JpaProject;

// Due to code-reorganization in the WTP plugins used for Eclipse 3.7 vs
// Eclipse 3.6 vs Eclipse 3.5, the App Engine WTP plugin needed to be split
// into 3.7, 3.6, and 3.5 versions.
// Whenever you modify this class, please make corresponding changes to the
// 3.5 and 3.6 classes.
public class WebtoolsUpdateQueryArgContributor
    implements UpdateQueryArgContributor {

  public String getContribution(IProject project) {
    if (project == null || !JpaFacetHelper.areJpaDepsAvailable()) {
      return "";
    }
    // Log JPA Platform ID.
    JpaProject jpaProject = (JpaProject) project.getAdapter(JpaProject.class);
    if (jpaProject != null) {
      return "&jpaPlatform="
          + StringUtilities.urlEncode(jpaProject.getJpaPlatform().getId());
    }
    return "";
  }
}
