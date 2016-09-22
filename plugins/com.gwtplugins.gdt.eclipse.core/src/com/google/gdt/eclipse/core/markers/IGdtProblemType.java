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
package com.google.gdt.eclipse.core.markers;

/**
 * Describes a particular GDT problem type.
 */
public interface IGdtProblemType {
  /*
   * Problem IDs should be globally-unique (else we run into problems when
   * trying to compute quick-fixes), so define all starting offsets here.
   */

  int GWT_OFFSET = 100;

  int APP_ENGINE_OFFSET = 200;

  int REMOTE_SERVICE_OFFSET = 300;

  int UIBINDER_TEMPLATE_OFFSET = 400;

  int UIBINDER_JAVA_OFFSET = 500;

  int CLIENTBUNDLE_OFFSET = 600;

  int PROJECT_STRUCTURE_OR_SDK_OFFSET = 700;

  GdtProblemCategory getCategory();

  GdtProblemSeverity getDefaultSeverity();

  String getDescription();

  String getMessage();

  int getProblemId();

  // TODO: add boolean hasQuickFixes();

  // TODO: add IJavaCompletionProposal[] getQuickFixes(IInvocationContext
  // context, IProblemLocation problem);

}