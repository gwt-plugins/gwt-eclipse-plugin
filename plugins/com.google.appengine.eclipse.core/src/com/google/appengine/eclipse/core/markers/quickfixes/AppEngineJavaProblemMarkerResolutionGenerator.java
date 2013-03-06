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
package com.google.appengine.eclipse.core.markers.quickfixes;

import com.google.appengine.eclipse.core.markers.AppEngineProblemType;
import com.google.gdt.eclipse.core.markers.quickfixes.JavaMarkerResolutionGenerator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates quick fixes for App Engine problems.
 */
public class AppEngineJavaProblemMarkerResolutionGenerator extends
    JavaMarkerResolutionGenerator {

  public IJavaCompletionProposal[] getCorrections(IInvocationContext context,
      IProblemLocation[] locations) throws CoreException {
    List<IJavaCompletionProposal> proposals = new ArrayList<IJavaCompletionProposal>();

    for (IProblemLocation problem : locations) {
      AppEngineProblemType problemType = AppEngineProblemType.getProblemType(problem.getProblemId());
      if (problemType == null) {
        continue;
      }

      switch (problemType) {
        case UNSUPPORTED_JRE_TYPE:
          proposals.add(new ExcludeFromValidationProposal(
              context.getCompilationUnit(), 100));
          proposals.add(new ExcludeFromValidationProposal(
              (IPackageFragment) context.getCompilationUnit().getAncestor(
                  IJavaElement.PACKAGE_FRAGMENT), 99));
          break;
        case WRONG_JDBC_URL:
          IJavaProject javaProject = context.getCompilationUnit().getJavaProject();
          proposals.add(new UrlCorrectionProposal(javaProject, problem, 100));
          break;
      }
    }

    return proposals.toArray(new IJavaCompletionProposal[0]);
  }

  public boolean hasCorrections(ICompilationUnit cu, int problemId) {
    AppEngineProblemType problemType = AppEngineProblemType.getProblemType(problemId);
    if (problemType != null) {
      switch (problemType) {
        case UNSUPPORTED_JRE_TYPE:
          return true;
        case WRONG_JDBC_URL:
          return true;
        default:
          return false;
      }
    }

    // Non-App Engine problem
    return false;
  }

}