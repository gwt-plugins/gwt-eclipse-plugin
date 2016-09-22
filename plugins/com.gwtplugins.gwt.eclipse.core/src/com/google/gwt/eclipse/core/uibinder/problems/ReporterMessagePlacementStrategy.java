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
package com.google.gwt.eclipse.core.uibinder.problems;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.validation.internal.operations.LocalizedMessage;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;

/**
 * Strategy to place validation results as messages on the validator (this is
 * one of the WST validation frameworks).
 */
@SuppressWarnings("restriction")
public class ReporterMessagePlacementStrategy implements
    IValidationResultPlacementStrategy<IMessage> {

  private static int severityFromIMarkerSeverity(int imarkerSeverity) {
    switch (imarkerSeverity) {
      case IMarker.SEVERITY_ERROR:
        return IMessage.HIGH_SEVERITY;

      case IMarker.SEVERITY_WARNING:
        return IMessage.NORMAL_SEVERITY;

      case IMarker.SEVERITY_INFO:
        return IMessage.LOW_SEVERITY;

      default:
        return IMessage.LOW_SEVERITY;
    }
  }

  private final IValidator validator;

  private final IReporter reporter;

  private boolean isClearAllowed = true;

  public ReporterMessagePlacementStrategy(IValidator validator,
      IReporter reporter) {
    this.validator = validator;
    this.reporter = reporter;
  }

  public void clearValidationResults(IResource resource) {
    if (isClearAllowed) {
      reporter.removeAllMessages(validator);
    }
  }

  /**
   * @see #setClearAllowed
   */
  public boolean isClearAllowed() {
    return isClearAllowed;
  }

  public IMessage placeValidationResult(IResource resource, IDocument document,
      IRegion position, String message, int severity) {
    IMessage validationMessage = new LocalizedMessage(
        severityFromIMarkerSeverity(severity), message);
    validationMessage.setLength(position.getLength());
    validationMessage.setOffset(position.getOffset());

    reporter.addMessage(validator, validationMessage);
    return validationMessage;
  }

  /**
   * Sets whether to allow the client to clear messages from the reporter.
   */
  public void setClearAllowed(boolean isClearAllowed) {
    this.isClearAllowed = isClearAllowed;
  }

}
