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
package com.google.gdt.eclipse.drive.editors.webautocomplete;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

import javax.annotation.Nullable;

/**
 * A peeking iterator which will iterate through the characters in the editor in reverse order.
 * Useful to help us parse the statements to find possible completions for the autocomplete and word
 * completion features.
 */
public class ReverseCharIterator
    extends AbstractIterator<Character>
    implements PeekingIterator<Character> {

  /**
   * A lightweight interface that grants {@link ReverseCharIterator} limited read access to the code
   * editor. All line and character indices are zero-based.
   */
  public interface EditorConnector {

    /**
     * Returns the number of lines in the current file.
     */
    int getNumLines();

    /**
     * Returns the length of the specified line.
     */
    int getLineLength(int line);

    /**
     * Returns the character stored at the specified line and column offset.
     */
    char getCharAt(int line, int ch);
  }

  /**
   * Defines the possible scanning modes used by {@link ReverseCharIterator}. This basically defines
   * what will be the stopping condition when iterating backwards on the code.
   */
  public enum Mode {
    NO_WRAP,
    WRAP,
  }

  /**
   * The iterator is implemented as a state machine the loops backwards over the editor source,
   * outputting characters on certain state transitions.
   */
  private enum State {

    /**
     * Ready to output a character from a source line.
     */
    IN_LINE,

    /**
     * At the beginning of a source line and ready to output a newline.
     */
    AT_START_OF_LINE,

    /**
     * Ready to move to the previous character in the current line.
     */
    MOVE_TO_PREV_CH,

    /**
     * Ready to move to the previous line in the script.
     */
    MOVE_TO_PREV_LINE,

    /**
     * Moved past the beginning of the script and ready to wrap around to the last line in the file,
     * provided that the current search mode is {@link Mode#WRAP}.
     */
    WRAP_AROUND,
  }

  private final EditorConnector editor;
  private final Mode mode;
  @Nullable private final Integer maxLines;

  private final int initialLine;
  private final int initialCh;

  private State state;
  private int line;
  private int ch;
  private int linesVisited;
  private boolean hasWrapped;

  /**
   * Initializes a new {@link ReverseCharIterator} that iterates over characters from the given
   * editor in the given search mode. The maximum number of lines to iterate over may be specified;
   * if no maximum is desired, specify {@code null}. Finally, the editor line and column to begin
   * iterating at must be provided. These line and column indices are zero-based, and the first
   * character yielded by the iterator will be the character <em>before</em> the character at the
   * specified initial position.
   */
  public ReverseCharIterator(
      EditorConnector editor,
      Mode mode,
      @Nullable Integer maxLines,
      int initialLine,
      int initialCh) {
    this.editor = Preconditions.checkNotNull(editor, "editor must be nonnull");
    this.mode = Preconditions.checkNotNull(mode, "mode must be nonnull");
    this.maxLines = checkNonnegative(maxLines, "maxLines");

    this.initialLine = checkNonnegative(initialLine, "initialLine");
    this.initialCh = checkNonnegative(initialCh, "initialCh");

    this.state = State.MOVE_TO_PREV_CH;
    this.line = initialLine;
    this.ch = initialCh;
    this.linesVisited = 0;
    this.hasWrapped = false;
  }

  @Override protected Character computeNext() {
    for (;;) {
      switch (state) {
        case IN_LINE:
          // If we're looking at a character in a line, just yield that character.
          state = State.MOVE_TO_PREV_CH;
          ch--;
          return editor.getCharAt(line, ch);

        case AT_START_OF_LINE:
          // Mark the beginning of each line by yielding a newline character.
          state = State.MOVE_TO_PREV_LINE;
          ++linesVisited;
          return '\n';

        case MOVE_TO_PREV_CH:
          // If we've wrapped around and returned to the initial position, we're done iterating.
          if (hasWrapped && (line < initialLine || line == initialLine && ch <= initialCh)) {
            return endOfData();
          }

          // Otherwise, move to the previous character if possible, or else move to the previous
          // line.
          state = (ch > 0) ? State.IN_LINE : State.AT_START_OF_LINE;
          break;

        case MOVE_TO_PREV_LINE:
          // If we've hit the maximum line count, we're done iterating.
          if (maxLines != null && linesVisited >= maxLines) {
            return endOfData();
          }

          // Otherwise, move to the previous line if possible, or wrap around to the last line in
          // the file.
          if (line > 0) {
            state = State.MOVE_TO_PREV_CH;
            --line;
          } else {
            state = State.WRAP_AROUND;
            line = editor.getNumLines() - 1;
          }
          ch = editor.getLineLength(line);
          break;

        case WRAP_AROUND:
          // If we just wrapped around to the last line in the file and we're searching in "no wrap"
          // mode, we're done iterating.
          if (mode == Mode.NO_WRAP) {
            return endOfData();
          }

          // Otherwise, move to the last character of the last line in the file.
          state = State.MOVE_TO_PREV_CH;
          hasWrapped = true;
          break;
      }
    }
  }

  private static int checkNonnegative(int x, String desc) {
    Preconditions.checkArgument(x >= 0, "%s must be nonnegative: %s", desc, x);
    return x;
  }

  private static Integer checkNonnegative(@Nullable Integer x, String desc) {
    return (x != null) ? checkNonnegative((int) x, desc) : null;
  }
}