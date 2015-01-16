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

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;

/**
* A segment is a self-contained section of a statement.  Only sections that are in the same block
* level as the end of the statement are considered as segments (sections inside parenthesis or
* quotes are ignored).
*
* For example:
* "foo.bar()" has 2 sections: "foo" and "bar()"
* "foo.bar[a(b.y())]" also has 2 sections: "foo" and "bar[]"
* "a.b(foo" only has one section, "foo", since "a.b(" are at a different level.
*
* <p>Each segment is guaranteed to be well-formed, in that calling
* {@code Segment#getValue()} will return either a valid string (with no
* unbalanced parenthesis etc), or the empty string.
*/
final class Segment {
  private static final CharMatcher ALPHANUMERIC =
     CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.anyOf("_$")).precomputed();
  private static final CharMatcher CLOSERS = CharMatcher.anyOf(")]}").precomputed();
  
  private String text = "";
  
  private int tickCounter = 0;
  private int quotesCounter = 0;
  private int parenthesisCounter = 0;
  private int bracketsCounter = 0;
  
  private boolean unbalancedQuotesOrTicks = false;
  
  /**
  * Adds a character to the beginning of the segment.  Characters in a different block level from
  * the beginning of the segment will be ignored.
  */
  void prependText(char c) {
   // Ignore alphanumeric characters inside of quotes, parenthesis, or braces.
   // NOTE: this could be enhanced to ignore any character inside of quotes which does not close
   // the quote, but right now it doesn't.
   if (isValid() || !ALPHANUMERIC.apply(c)) {
     updateCounters(c);
     text = c + text;
   }
  }
  
  /**
  * Updates the internal counters used for checking whether the segment is valid
  */
  private void updateCounters(char c) {
   switch (c) {
     case '(':
       parenthesisCounter--;
       break;
     case ')':
       if (parenthesisCounter >= 0) {
         parenthesisCounter++;
       }
       break;
     case '[':
       bracketsCounter--;
       break;
     case ']':
       if (bracketsCounter >= 0) {
         bracketsCounter++;
       }
       break;
     case '\'':
       tickCounter++;
       break;
     case '\"':
       quotesCounter++;
       break;
     case '\n':
       if (quotesCounter % 2 != 0 || tickCounter % 2 != 0) {
         unbalancedQuotesOrTicks = true;
       }
       break;
   }
  }
  
  /**
  * Returns true if the segment ends in ']', which means the input is of the
  * form: {@code <something>]}. This is potentially referring to an element in an array.
  */
  boolean isArrayElement() {
   return text.endsWith("]");
  }
  
  /**
  * Returns true if the segment starts with whitespace, preceded and followed by an alphanumeric
  * character (an example would be "for x in bar()") or a closing parenthesis/brace/bracket (an 
  * example would be "while(true) foo();").  In that case, the space could act as a
  * delimiter for the segment.
  */
  boolean delimitedBySpace(char lastCharBeforeSegment) {
   int nonWhitespace = CharMatcher.BREAKING_WHITESPACE.negate().indexIn(text);
   int firstAlphanum = ALPHANUMERIC.indexIn(text);
   return nonWhitespace > 0  // has whitespace
       && nonWhitespace == firstAlphanum // has alphanumeric char after space
       && (ALPHANUMERIC.apply(lastCharBeforeSegment) || // has alphanumeric char before space
           CLOSERS.apply(lastCharBeforeSegment)); // or a closing character of some kind
  }
  
  /**
  * Returns true if this segment is an array with no text inside of it (such as "[]").
  */
  boolean isRawArray() {
   return isValid() && isArrayElement() && getValue().isEmpty();
  }
  
  /**
  * Returns true if this segment has evenly-balanced quotes and blocks.  In other words, it has
  * an equal number of opening and closing parenthesis, an equal number of opening and closing
  * array delimiters, and an even number of single and double quotes.
  *
  * <p>Currently we don't handle degenerate cases, such as "([')]'])".
  */
  boolean isValid() {
   return parenthesisCounter == 0
       && bracketsCounter == 0
       && tickCounter % 2 == 0
       && quotesCounter % 2 == 0;
  }
  
  /**
  * Gets the alphanumeric value associated with the segment.  We only consider alphanumeric
  * characters.  This means that, for the segment "abc([])", only "abc" would be returned.
  */
  String getValue() {
   if (isValid()) {
     return ALPHANUMERIC.negate().removeFrom(text);
   } else {
     return "";
   }
  }
  
  /**
  * Returns true if the current segment is invalid and no possible prefix can make it valid,
  * for example "[" is definitely invalid as the brackets cannot balance themselves in any case.
  * This allow optimizations when used in the Autocompleter. 
  */
  public boolean isDefinitelyInvalid() {
   return parenthesisCounter < 0
       || bracketsCounter < 0
       || unbalancedQuotesOrTicks;
  }
  
  @Override
  public String toString() {
   return text;
  }
  
  @Override
  public boolean equals(Object obj) {
   if (obj instanceof Segment) {
     return ((Segment) obj).text.equals(text);
   }
   return false;
  }
  
  @Override
  public int hashCode() {
   return Objects.hashCode(text);
  }
}