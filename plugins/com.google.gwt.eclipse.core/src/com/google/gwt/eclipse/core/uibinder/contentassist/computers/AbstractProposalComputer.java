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
package com.google.gwt.eclipse.core.uibinder.contentassist.computers;

import com.google.gwt.eclipse.core.uibinder.contentassist.IProposalComputer;

/**
 * An abstract class for proposal computers.
 */
public abstract class AbstractProposalComputer implements IProposalComputer {

  protected final String enteredText;
  protected final int replaceOffset;
  protected final int replaceLength;

  public AbstractProposalComputer(String enteredText, int replaceOffset,
      int replaceLength) {
    this.enteredText = enteredText;
    this.replaceOffset = replaceOffset;
    this.replaceLength = replaceLength;
  }

  public String getEnteredText() {
    return enteredText;
  }

  public int getReplaceLength() {
    return replaceLength;
  }

  public int getReplaceOffset() {
    return replaceOffset;
  }

}
