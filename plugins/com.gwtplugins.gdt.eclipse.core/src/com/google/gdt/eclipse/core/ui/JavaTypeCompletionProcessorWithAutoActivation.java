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
package com.google.gdt.eclipse.core.ui;

import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;

/**
 * Java type completion processor that automatically activates when the period
 * key is pressed. This is useful for dialog fields, where auto-activation is
 * disabled by default.
 */
@SuppressWarnings("restriction")
public class JavaTypeCompletionProcessorWithAutoActivation extends
    JavaTypeCompletionProcessor {

  private static final char[] ACTIVATION_CHARS = {'.'};

  public JavaTypeCompletionProcessorWithAutoActivation() {
    super(false, false, true);
  }

  @Override
  public char[] getCompletionProposalAutoActivationCharacters() {
    return ACTIVATION_CHARS;
  }
}