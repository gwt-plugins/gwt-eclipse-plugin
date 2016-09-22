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

import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Generates problem quick fixes for both the Java editor and the Problems view.
 */
@SuppressWarnings("restriction")
public abstract class JavaMarkerResolutionGenerator implements
    IMarkerResolutionGenerator2, IQuickFixProcessor {

  private static final IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

  // From org.eclipse.jdt.internal.ui.text.correction.
  // CorrectionMarkerResolutionGenerator
  private static ICompilationUnit getCompilationUnit(IMarker marker) {
    IResource res = marker.getResource();
    if (res instanceof IFile && res.isAccessible()) {
      IJavaElement element = JavaCore.create((IFile) res);
      if (element instanceof ICompilationUnit) {
        return (ICompilationUnit) element;
      }
    }
    return null;
  }

  public IMarkerResolution[] getResolutions(IMarker marker) {
    if (!hasResolutions(marker)) {
      return NO_RESOLUTIONS;
    }

    ICompilationUnit cu = getCompilationUnit(marker);
    if (cu != null) {
      IEditorInput input = new FileEditorInput(
          (IFile) cu.getPrimary().getResource());
      if (input != null) {
        int offset = marker.getAttribute(IMarker.CHAR_START, -1);
        int length = marker.getAttribute(IMarker.CHAR_END, -1) - offset;
        int problemId = marker.getAttribute(IJavaModelMarker.ID, -1);
        boolean isError = (marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR);
        String[] arguments = CorrectionEngine.getProblemArguments(marker);

        IProblemLocation location = new ProblemLocation(offset, length,
            problemId, arguments, isError, null);
        IInvocationContext context = new AssistContext(cu, offset, length);

        IJavaCompletionProposal[] proposals = new IJavaCompletionProposal[0];

        try {
          proposals = getCorrections(context, new IProblemLocation[] {location});
        } catch (CoreException e) {
          CorePluginLog.logError(e);
        }

        int nProposals = proposals.length;
        IMarkerResolution[] resolutions = new IMarkerResolution[nProposals];
        for (int i = 0; i < nProposals; i++) {
          resolutions[i] = new QuickFixCompletionProposalWrapper(cu, offset,
              length, proposals[i]);
        }
        return resolutions;
      }
    }

    return NO_RESOLUTIONS;
  }

  public boolean hasResolutions(IMarker marker) {
    int problemId = marker.getAttribute(IJavaModelMarker.ID, -1);
    return hasCorrections(getCompilationUnit(marker), problemId);
  }

}