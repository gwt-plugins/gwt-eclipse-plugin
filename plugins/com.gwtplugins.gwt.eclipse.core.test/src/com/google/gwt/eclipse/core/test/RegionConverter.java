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
package com.google.gwt.eclipse.core.test;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;

/**
 * Utility class for converting document regions from one platform to another,
 * so the tests will work on both Linux (\n endings) and Windows (\r\n).
 */
public final class RegionConverter {

  public static IRegion convertLinuxRegion(int offset, int length, String text) {
    return convertRegion(offset, length, text, "\n");
  }

  public static IRegion convertMacRegion(int offset, int length, String text) {
    return convertRegion(offset, length, text, "\n");
  }

  public static IRegion convertWindowsRegion(int offset, int length, String text) {
    return convertRegion(offset, length, text, "\r\n");
  }

  private static IRegion convertRegion(int offset, int length, String text,
      String originalDelimiter) {
    try {
      Document document = new Document(text);
      String delimiter = document.getLineDelimiter(0);

      // If the document's line delimiter is the same as that used to originally
      // calculate this offset/length, then just return the original region.
      if (delimiter == null || delimiter.equals(originalDelimiter)) {
        return new Region(offset, length);
      }

      // If we're running a platform other than the one this offset/length was
      // calculated on, we'll need to adjust the values. We start by creating
      // a text selection containing the original region and the text with the
      // original line endings.
      String originalText = text.replaceAll(delimiter, originalDelimiter);
      ITextSelection originalSelection = new TextSelection(new Document(
          originalText), offset, length);

      int delimiterLengthCorrection = originalDelimiter.length()
          - delimiter.length();

      // Adjust the offset by the delimiter length difference for each line
      // that came before it.
      int newOffset = originalSelection.getOffset()
          - (delimiterLengthCorrection * originalSelection.getStartLine());

      // Adjust the length by the delimiter length difference for each line
      // between the start and the end of the original region

      // TODO: account for case where the selection ends with a line break;
      // currently this will not update the length since the selection starts
      // and ends on the same line.
      int regionLineBreaks = originalSelection.getEndLine()
          - originalSelection.getStartLine();
      int newLength = originalSelection.getLength()
          - (delimiterLengthCorrection * regionLineBreaks);

      return new Region(newOffset, newLength);
    } catch (BadLocationException e) {
      return null;
    }
  }

  private RegionConverter() {
    // Not instantiable
  }

}
