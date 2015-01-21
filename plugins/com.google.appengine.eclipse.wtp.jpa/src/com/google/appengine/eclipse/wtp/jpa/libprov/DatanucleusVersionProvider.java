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
package com.google.appengine.eclipse.wtp.jpa.libprov;

import com.google.appengine.eclipse.core.orm.enhancement.EnhancerJob.IEnhancerJobDatanucleusVersionProvider;
import com.google.appengine.eclipse.wtp.jpa.AppEngineJpaPlugin;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Extension point providing Datanucleus version for GPE WTP projects.
 */
public final class DatanucleusVersionProvider implements IEnhancerJobDatanucleusVersionProvider {

  @Override
  public String getVersion(IJavaProject javaProject) {
    try {
      if (ProjectUtils.isGaeProject(javaProject)) {
        // TODO(amitin): currently, only one version is supported (v2), when we have v3 we will
        // provide additional JPA platform type (like "Google App Engine (Datanucleus v2)" and
        // "Google App Engine (Datanucleus v3)") and provide version depending on what platform
        // type is currently selected.
        return "v2";
      }
    } catch (CoreException e) {
      AppEngineJpaPlugin.logMessage(e);
    }
    return null;
  }
}
