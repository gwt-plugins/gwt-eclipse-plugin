/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.editors;

import com.google.common.annotations.VisibleForTesting;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;

/**
 * A {@link TemplateProposal} with customizations. Specifically, {@link #getDisplayString()} is
 * overridden to return a string specified in the constructor,
 * {@link #getAdditionalProposalInfo()} is overridden to return the description string, and a
 * {@link #getReplacementString() method is added for use in unit tests.
 */
@VisibleForTesting class AppsScriptTemplateProposal extends TemplateProposal {
  private final String description;
  private final String displayString;

  public AppsScriptTemplateProposal(
      Template template, TemplateContext context, IRegion region, String displayString,
      String description) {
    super(template, context, region, null);
    this.description = description;
    this.displayString = displayString;
  }
  
  @Override public String getDisplayString() {
    return displayString;
  }
  
  @Override public String getAdditionalProposalInfo() {
    return description;
  }
  
  @VisibleForTesting String getReplacementString() {
    return getTemplate().getPattern();
  }
  
  // A way for tests to invoke the final protected method getTemplate().
  @VisibleForTesting Template getTemplateUnprotected() {
    return getTemplate();
  }
}