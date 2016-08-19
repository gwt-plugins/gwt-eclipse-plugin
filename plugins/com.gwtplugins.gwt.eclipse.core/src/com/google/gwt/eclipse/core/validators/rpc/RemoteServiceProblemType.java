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
package com.google.gwt.eclipse.core.validators.rpc;

import com.google.gdt.eclipse.core.markers.GdtProblemCategory;
import com.google.gdt.eclipse.core.markers.GdtProblemSeverity;
import com.google.gdt.eclipse.core.markers.IGdtProblemType;

/**
 * Problems associated with the synchronous and asynchronous versions of
 * RemoteService interfaces.
 */
public enum RemoteServiceProblemType implements IGdtProblemType {
  /**
   * No asynchronous version of the synchronous interface.
   */
  MISSING_ASYNC_TYPE(REMOTE_SERVICE_OFFSET + 1,
      "Missing asynchronous interface", "Missing asynchronous interface {0}",
      GdtProblemSeverity.ERROR),

  /**
   * Missing the asynchronous version of a synchronous method.
   */
  MISSING_ASYNC_METHOD(REMOTE_SERVICE_OFFSET + 2,
      "Missing method on asynchronous interface", "{0} is missing method {1}",
      GdtProblemSeverity.ERROR),

  /**
   * Missing the synchronous version of an asynchronous method.
   */
  MISSING_SYNC_METHOD(REMOTE_SERVICE_OFFSET + 3,
      "Missing method on synchronous interface", "{0} is missing method {1}",
      GdtProblemSeverity.ERROR),

  /**
   * The method on the asynchronous interface does not have a parameter of
   * AsyncCallback type.
   */
  MISSING_ASYNCCALLBACK_PARAMETER(REMOTE_SERVICE_OFFSET + 4,
      "Missing AsyncCallback parameter",
      "Last parameter must be of type AsyncCallback.", GdtProblemSeverity.ERROR),

  /**
   * The callback parameter's parameterization is not compatible with the
   * synchronous method's return type.
   */
  ASYNCCALLBACK_TYPE_ARGUMENT_MISMATCH(
      REMOTE_SERVICE_OFFSET + 5,
      "Incompatible AsyncCallback parameterization",
      "AsyncCallback parameterization {0} is not compatible with the synchronous return type {1}",
      GdtProblemSeverity.ERROR),

  /**
   * Return type of the asynchronous method is invalid.
   */
  INVALID_ASYNC_RETURN_TYPE(REMOTE_SERVICE_OFFSET + 6,
      "Invalid asynchronous method return type",
      "Asynchronous methods can only return {0}", GdtProblemSeverity.ERROR);

  public static RemoteServiceProblemType getProblemType(int problemId) {
    for (RemoteServiceProblemType type : RemoteServiceProblemType.values()) {
      if (type.getProblemId() == problemId) {
        return type;
      }
    }
    return null;
  }

  private final GdtProblemSeverity defaultSeverity;

  private final String description;

  private final String message;

  private final int problemId;

  private RemoteServiceProblemType(int problemId, String description,
      String message, GdtProblemSeverity defaultSeverity) {
    this.problemId = problemId;
    this.description = description;
    this.message = message;
    this.defaultSeverity = defaultSeverity;
  }

  public GdtProblemCategory getCategory() {
    return GdtProblemCategory.GWT_RPC;
  }

  public GdtProblemSeverity getDefaultSeverity() {
    return defaultSeverity;
  }

  public String getDescription() {
    return description;
  }

  public String getMessage() {
    return message;
  }

  public int getProblemId() {
    return problemId;
  }

}
