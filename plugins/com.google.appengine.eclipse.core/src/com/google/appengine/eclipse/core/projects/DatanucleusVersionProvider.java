/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.core.projects;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.orm.enhancement.EnhancerJob.IEnhancerJobDatanucleusVersionProvider;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Extension point providing Datanucleus version for old-stype GPE projects.
 */
public final class DatanucleusVersionProvider implements IEnhancerJobDatanucleusVersionProvider {
  public String getVersion(IJavaProject javaProject) {
    IProject project = javaProject.getProject();
    try {
      if (ProjectUtilities.isGpeProject(project)) {
        return GaeProjectProperties.getGaeDatanucleusVersion(project);
      }
    } catch (CoreException e) {
      AppEngineCorePluginLog.logError(e);
    }
    return null;
  }
}
