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
package com.google.gwt.eclipse.core.uibinder.text;

import org.eclipse.jface.text.DocumentCommand;

/**
 * A simple document command that can be instantiated via a public constructor
 * (unlike {@link DocumentCommand}.)
 */
public class SimpleDocumentCommand extends DocumentCommand {

  /**
   * Copies some fields from one {@link DocumentCommand} to another, but
   * offseting any of the position-based fields.
   * <p>
   * Note: This does not copy commands within the {@link DocumentCommand}.
   * 
   * @param source the command to copy from
   * @param dest the command to copy to
   * @param offset the offset to subtract from the <code>source</code>'s
   *          position-based fields
   */
  public static void copyFields(DocumentCommand source, DocumentCommand dest, int offset) {
    dest.caretOffset = source.caretOffset - offset;
    dest.doit = source.doit;
    dest.length = source.length;
    dest.offset = source.offset - offset;
    dest.shiftsCaret = source.shiftsCaret;
    dest.text = source.text;
  }

  public SimpleDocumentCommand() {
  }
}
