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
package com.google.gwt.eclipse.core.dialogs;

import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Implementation of <code>ICompletionProposal</code> interface for module
 * completions.
 */
public class ModuleCompletionProposal implements ICompletionProposal,
    Comparable<ModuleCompletionProposal> {

  protected final String prefixCompareString;

  private final String displayString;

  private final Image image;

  private final int relevance;

  private int replacementLength;

  private final int replacementOffset;

  private final String newString;

  public ModuleCompletionProposal(String newString, int replacementOffset,
      int replacementLength, Image image, String displayString,
      String prefixCompareString, int relevance) {
    Assert.isNotNull(newString);
    Assert.isNotNull(displayString);
    Assert.isNotNull(prefixCompareString);
    Assert.isTrue(replacementOffset >= 0);
    Assert.isTrue(replacementLength >= 0);

    this.newString = newString;
    this.replacementOffset = replacementOffset;
    this.replacementLength = replacementLength;
    this.image = image;
    this.displayString = displayString;
    this.prefixCompareString = prefixCompareString;
    this.relevance = relevance;
  }

  public void apply(IDocument document) {
    try {
      document.replace(replacementOffset, replacementLength, newString);
    } catch (BadLocationException e) {
      GWTPluginLog.logError(e);
    }
  }

  public int compareTo(ModuleCompletionProposal other) {
    // Higher relevance = higher in list
    int diff = other.relevance - this.relevance;
    if (diff == 0) {
      // Fall back to alphabetic sorting of the display string
      diff = this.displayString.compareToIgnoreCase(other.displayString);
    }

    return diff;
  }

  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    return displayString;
  }

  public Image getImage() {
    return image;
  }

  public int getReplacementLength() {
    return replacementLength;
  }

  public Point getSelection(IDocument document) {
    return new Point(replacementOffset + newString.length(), 0);
  }

  public boolean matches(String input) {
    return prefixCompareString.toLowerCase().startsWith(input.toLowerCase());
  }

  public void setReplacementLength(int replacementLength) {
    this.replacementLength = replacementLength;
  }

  @Override
  public String toString() {
    return this.displayString;
  }

}
