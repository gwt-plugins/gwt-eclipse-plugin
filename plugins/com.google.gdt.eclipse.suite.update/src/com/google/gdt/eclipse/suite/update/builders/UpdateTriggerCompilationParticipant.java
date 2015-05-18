/*******************************************************************************
 * Copyright 2009 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.update.builders;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

/**
 * A compilation participant that is used to trigger an update check of the GDT
 * Plugin's feature whenever a Java build is triggered on a project, and the
 * project has either the GWT or GAE natures (or both).
 */
public class UpdateTriggerCompilationParticipant extends CompilationParticipant {

  @Override
  public boolean isActive(IJavaProject project) {
    try {
      if (project.exists()) {
        if (GWTNature.isGWTProject(project.getProject())
            || project.getProject().hasNature(GaeNature.NATURE_ID)) {
          GdtExtPlugin.getFeatureUpdateManager().checkForUpdates();
          return true;
        }
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }
    return false;
  }
}
