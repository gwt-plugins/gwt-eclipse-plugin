/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.validators;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.validators.java.GaeChecker;
import com.google.appengine.eclipse.core.validators.java.GoogleCloudSqlChecker;
import com.google.appengine.eclipse.core.validators.java.JavaCompilationParticipant;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.List;

/**
 * Compilation participant validating GAE WTP project.
 */
public final class GaeCompilationParticipant extends JavaCompilationParticipant {

  @Override
  public boolean isActive(IJavaProject javaProject) {
    try {
      return ProjectUtils.isGaeProject(javaProject);
    } catch (CoreException e) {
      AppEnginePlugin.logMessage(e);
    }
    return false;
  }

  /**
   * Performs validation.
   */
  @Override
  protected List<CategorizedProblem> doValidateCompilationUnit(CompilationUnit root,
      ICompilationUnit cu) {
    IJavaProject javaProject = cu.getJavaProject();
    List<CategorizedProblem> problems = GoogleCloudSqlChecker.check(root, cu.getJavaProject());
    try {
      IPath sdkLocation = ProjectUtils.getGaeSdkLocation(javaProject.getProject());
      if (sdkLocation != null) {
        SdkSet<GaeSdk> sdks = GaePreferences.getSdkManager().getSdks();
        GaeSdk sdk = SdkUtils.findSdkForInstallationPath(sdks, sdkLocation);
        problems.addAll(GaeChecker.check(root, javaProject, sdk));
      }
    } catch (CoreException e) {
      AppEnginePlugin.logMessage(e);
    }
    return problems;
  }
}
