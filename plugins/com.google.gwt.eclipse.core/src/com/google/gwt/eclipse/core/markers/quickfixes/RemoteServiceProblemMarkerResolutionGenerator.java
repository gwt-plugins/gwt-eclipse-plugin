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
package com.google.gwt.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.markers.quickfixes.JavaMarkerResolutionGenerator;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceProblemType;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates quick fixes for GWT RPC problems.
 */
public class RemoteServiceProblemMarkerResolutionGenerator extends
    JavaMarkerResolutionGenerator {

  public IJavaCompletionProposal[] getCorrections(IInvocationContext context,
      IProblemLocation[] locations) throws CoreException {
    List<IJavaCompletionProposal> proposals = new ArrayList<IJavaCompletionProposal>();

    for (IProblemLocation problem : locations) {
      RemoteServiceProblemType problemType = RemoteServiceProblemType.getProblemType(problem.getProblemId());
      if (problemType == null) {
        continue;
      }

      switch (problemType) {
        case MISSING_ASYNC_TYPE:
          proposals.addAll(CreateAsyncInterfaceProposal.createProposals(
              context, problem));
          break;
        case MISSING_ASYNC_METHOD:
          // Provide different resolutions depending on which type error is on
          if (problem.getProblemArguments()[0].equals("async")) {
            String syncMethodBindingKey = problem.getProblemArguments()[1];
            proposals.addAll(CreateAsyncMethodProposal.createProposalsForProblemOnAsyncType(
                context.getCompilationUnit(), context.getCoveringNode(),
                syncMethodBindingKey));
            proposals.addAll(DeleteMethodProposal.createProposalsForProblemOnAsyncType(
                context.getCoveringNode(), syncMethodBindingKey));
            proposals.addAll(UpdateAsyncSignatureProposal.createProposalsForProblemsOnAsyncType(
                context.getCoveringNode(), syncMethodBindingKey));
          } else { // on sync type
            proposals.addAll(CreateAsyncMethodProposal.createProposalsForProblemOnSyncMethod(context.getCoveringNode()));
            proposals.addAll(DeleteMethodProposal.createProposalsForProblemOnExtraMethod(context.getCoveringNode()));
            proposals.addAll(UpdateAsyncSignatureProposal.createProposalsForProblemsOnSyncMethod(context.getCoveringNode()));
          }
          break;
        case MISSING_SYNC_METHOD:
          if (problem.getProblemArguments()[0].equals("sync")) {
            String asyncMethodBindingKey = problem.getProblemArguments()[1];
            proposals.addAll(CreateSyncMethodProposal.createProposalsForProblemOnSyncType(
                context.getCompilationUnit(), context.getCoveringNode(),
                asyncMethodBindingKey));
            proposals.addAll(DeleteMethodProposal.createProposalsForProblemOnSyncType(
                context.getCoveringNode(), asyncMethodBindingKey));
            proposals.addAll(UpdateSyncSignatureProposal.createProposalsForProblemsOnSyncType(
                context.getCoveringNode(), asyncMethodBindingKey));
          } else { // on async type
            proposals.addAll(CreateSyncMethodProposal.createProposalsForProblemOnAsyncMethod(context.getCoveringNode()));
            proposals.addAll(DeleteMethodProposal.createProposalsForProblemOnExtraMethod(context.getCoveringNode()));
            proposals.addAll(UpdateSyncSignatureProposal.createProposalsForProblemsOnAsyncMethod(context.getCoveringNode()));
          }
          break;
        case INVALID_ASYNC_RETURN_TYPE:
          proposals.addAll(ChangeAsyncMethodReturnTypeProposal.createProposals(context.getCoveringNode()));
          break;
        case ASYNCCALLBACK_TYPE_ARGUMENT_MISMATCH:
          if (problem.getProblemArguments()[0].equals("sync")) {
            proposals.addAll(UpdateAsyncSignatureProposal.createProposalsForProblemsOnSyncMethod(context.getCoveringNode()));
          } else {
            proposals.addAll(UpdateSyncSignatureProposal.createProposalsForProblemsOnAsyncMethod(context.getCoveringNode()));
          }
          break;
      }
    }

    return proposals.toArray(new IJavaCompletionProposal[0]);
  }

  public boolean hasCorrections(ICompilationUnit cu, int problemId) {
    RemoteServiceProblemType problemType = RemoteServiceProblemType.getProblemType(problemId);
    if (problemType != null) {
      switch (problemType) {
        case MISSING_ASYNC_TYPE:
        case MISSING_ASYNC_METHOD:
        case MISSING_SYNC_METHOD:
        case INVALID_ASYNC_RETURN_TYPE:
        case ASYNCCALLBACK_TYPE_ARGUMENT_MISMATCH:
          return true;
        default:
          return false;
      }
    }

    // Non-Remote Service problem
    return false;
  }

}
