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

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static java.lang.Character.COMBINING_SPACING_MARK;
import static java.lang.Character.CONNECTOR_PUNCTUATION;
import static java.lang.Character.DECIMAL_DIGIT_NUMBER;
import static java.lang.Character.LETTER_NUMBER;
import static java.lang.Character.LOWERCASE_LETTER;
import static java.lang.Character.MODIFIER_LETTER;
import static java.lang.Character.NON_SPACING_MARK;
import static java.lang.Character.OTHER_LETTER;
import static java.lang.Character.TITLECASE_LETTER;
import static java.lang.Character.UPPERCASE_LETTER;

/**
 * Provides static methods and constants useful for analyzing JavaScript identifier names.
 * <i>Identifier names</i> consist of identifiers and reserved words.
 */
public class JavaScriptIdentifierNames {
  
  /**
   * Words that are reserved and have a meaning in ECMAScript 5.1.
   */
  public static final Set<String> KEYWORDS_AND_LITERALS =
      ImmutableSet.of(
          // keywords:
          "break", "case", "catch", "continue", "debugger", "default", "delete", "do", "else",
          "finally", "for", "function", "if", "in", "instanceof", "new", "return", "switch", "this",
          "throw", "try", "typeof", "var", "void", "while", "with",
          // literals:
          "null", "true", "false");
  
  /**
   * Words that are reserved but have no meaning in ECMAScript 5.1 (and therefore should not be
   * offered as completion proposals).
   */
  public static final Set<String> FUTURE_RESERVED_WORDS =
      ImmutableSet.of(
          // reserved:
          "class", "const", "enum", "export", "extends", "import", "super",
          // reserved only in strict mode:
          "implements", "interface", "let", "package", "private", "protected", "public", "static",
          "yield");
  
  /**
   * All identifier names that are reserved, and therefore cannot be used as identifiers, in
   * strict mode.
   */
  public static final Set<String> RESERVED_WORDS =
      ImmutableSet.<String>builder()
          .addAll(KEYWORDS_AND_LITERALS)
          .addAll(FUTURE_RESERVED_WORDS)
          .build();
  
  // We want the next two constants to be of type Set<Integer> rather than Set<Byte> because we test
  // the int result of Character.getType() for membership in these sets. However, the character-type
  // constants like Character.UPPERCASE_LETTER are of type byte, so we prefix each of these
  // constants with a unary + to promote it to int; otherwise ImmutableSet.of would return a
  // Set<Byte>.
  
  private static final Set<Integer> IDENTIFIER_NAME_STARTING_CHARACTER_TYPES =
      ImmutableSet.of(
          +UPPERCASE_LETTER, +LOWERCASE_LETTER, +TITLECASE_LETTER, +MODIFIER_LETTER, +OTHER_LETTER,
          +LETTER_NUMBER);
  
  private static final Set<Integer> IDENTIFIER_NAME_OTHER_CHARACTER_TYPES =
      ImmutableSet.of(
          +NON_SPACING_MARK, +COMBINING_SPACING_MARK, +DECIMAL_DIGIT_NUMBER,
          +CONNECTOR_PUNCTUATION);

  public static boolean isIdentifierNameStartingCharacter(char c) {
    return
        IDENTIFIER_NAME_STARTING_CHARACTER_TYPES.contains(Character.getType(c))
            || c == '_'
            || c == '$';
  }
  
  public static boolean isIdentifierNameCharacter(char c) {
    int charType = Character.getType(c);
    return
        IDENTIFIER_NAME_STARTING_CHARACTER_TYPES.contains(charType)
            || IDENTIFIER_NAME_OTHER_CHARACTER_TYPES.contains(charType)
            || c == '_'
            || c == '$';
  }
  
  private JavaScriptIdentifierNames() { } // prevent instantiation

}
