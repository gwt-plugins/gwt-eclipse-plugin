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
package com.google.gdt.eclipse.appengine.rpc.markers.quickfixes;

import com.google.gdt.eclipse.core.markers.quickfixes.JavaMarkerResolutionGenerator;
import com.google.gwt.eclipse.core.markers.GWTProblemType;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates quick fixes for request factory problems
 */
@SuppressWarnings("restriction")
public class ReqFactoryProblemMarkerResolutionGenerator extends
    JavaMarkerResolutionGenerator {

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jdt.ui.text.java.IQuickFixProcessor#getCorrections(org.eclipse
   * .jdt.ui.text.java.IInvocationContext,
   * org.eclipse.jdt.ui.text.java.IProblemLocation[])
   */
  public IJavaCompletionProposal[] getCorrections(IInvocationContext context,
      IProblemLocation[] locations) throws CoreException {
    List<IJavaCompletionProposal> proposals = new ArrayList<IJavaCompletionProposal>();

    for (IProblemLocation problem : locations) {
      GWTProblemType problemType = GWTProblemType.getProblemType(problem.getProblemId());
      if (problemType == null) {
        continue;
      }
      switch (problemType) {
        case REQ_FACTORY_SERVICE_METHOD_ERROR:
          proposals.addAll(CreateRequestFactoryMethodProposal.createProposals(
              context.getCompilationUnit(), context.getCoveringNode()));
          proposals.addAll(DeleteServiceMethodAnnotationProposal.createProposals(
              context.getCompilationUnit(), context.getCoveringNode()));
          break;
      }
    }
    return proposals.toArray(new IJavaCompletionProposal[0]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jdt.ui.text.java.IQuickFixProcessor#hasCorrections(org.eclipse
   * .jdt.core.ICompilationUnit, int)
   */
  public boolean hasCorrections(ICompilationUnit unit, int problemId) {
    GWTProblemType problemType = GWTProblemType.getProblemType(problemId);
    if (problemType != null) {
      switch (problemType) {
        case REQ_FACTORY_SERVICE_METHOD_ERROR:
          return true;
        default:
          return false;
      }
    }
    // Non Request Factory problem
    return false;
  }

}
