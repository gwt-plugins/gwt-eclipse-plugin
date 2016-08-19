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
package com.google.gdt.eclipse.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * General-purpose string utilities.
 */
public final class StringUtilities {

  private static final Pattern ALL_WHITESPACE_PATTERN = Pattern.compile("\\s+");

  public static String capitalize(String s) {
    if (s == null || s.length() == 0) {
      return s;
    }

    return s.toUpperCase().charAt(0) + s.substring(1);
  }

  /**
   * Returns a name guaranteed not to be equal to any of provided existing
   * names. This is done by adding an integer suffix to the base name (which
   * itself should not contain any such suffix). For example, if the base name
   * is Foo, and the names Bar and Foo2 are already taken (indicated via the
   * existingNames parameter), then this method would return Foo3.
   */
  public static String computeUniqueName(String[] existingNames, String baseName) {
    Set<String> nameSet = new HashSet<String>(Arrays.asList(existingNames));
    String proposedName = baseName;
    int i = 2; // If Foo exists, the proposed name for another Foo is: Foo2
    while (nameSet.contains(proposedName)) {
      proposedName = baseName + i++;
    }

    return proposedName;
  }

  /**
   * Compares two strings, ignoring any whitespace and optionally ignoring case.
   */
  public static boolean equalsIgnoreWhitespace(String string1, String string2,
      boolean ignoreCase) {
    string1 = ALL_WHITESPACE_PATTERN.matcher(string1).replaceAll("");
    string2 = ALL_WHITESPACE_PATTERN.matcher(string2).replaceAll("");
    return (ignoreCase ? string1.equalsIgnoreCase(string2)
        : string1.equals(string2));
  }

  /**
   * Finds the first non-whitespace character starting at index.
   * 
   * @param s the string to search
   * @param index the start index (inclusive)
   * @return the index of the first non-whitespace character, or the string
   *         length if we reached the end
   */
  public static int findNonwhitespaceCharacter(String s, int index) {
    int sLength = s.length();
    while (index < sLength && Character.isWhitespace(s.charAt(index))) {
      index++;
    }

    return index;
  }

  /**
   * Finds the first string that matches the given string with case ignored.
   * 
   * @return the index, or -1
   */
  public static int indexOfThatIgnoresCase(List<String> list, String s) {
    for (int i = 0; i < list.size(); i++) {
      if (s.equalsIgnoreCase(list.get(i))) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Finds the first string that starts with the given string.
   * 
   * @return the index, or -1
   */
  public static int indexOfThatStartsWith(List<String> list, String startsWith,
      int startIndex) {
    for (int i = startIndex; i < list.size(); i++) {
      String curStr = list.get(i);
      if (curStr != null && curStr.startsWith(startsWith)) {
        return i;
      }
    }

    return -1;
  }

  /**
   * @return if the given string is either null or 0-length
   */
  public static boolean isEmpty(String s) {
    return s == null || s.length() == 0;
  }

  /**
   * Returns {@code true} if the given string is null, empty, or comprises only
   * whitespace characters.
   */
  public static boolean isEmptyOrWhitespace(String string) {
    return isEmpty(string) || ALL_WHITESPACE_PATTERN.matcher(string).matches();
  }

  /**
   * @return true if the string is starts and ends with a '"'
   */
  public static boolean isQuoted(String s) {
    return s.length() >= 2 && s.charAt(0) == '"'
        && s.charAt(s.length() - 1) == '"';
  }

  public static String join(Iterable<?> items, String delimiter) {
    StringBuffer buffer = new StringBuffer();
    Iterator<?> iter = items.iterator();
    if (iter.hasNext()) {
      buffer.append(iter.next().toString());
      while (iter.hasNext()) {
        buffer.append(delimiter);
        buffer.append(iter.next().toString());
      }
    }
    return buffer.toString();
  }

  public static String join(Object[] items, String delimiter) {
    return join(Arrays.asList(items), delimiter);
  }

  /**
   * Note: The policy for quoting MUST match the unquoting policy used by
   * Eclipse's DebugPlugin.ArgumentProcessor.
   * 
   * @return the string surrounded with quotes, with only quotes escaped
   */
  public static String quote(String s) {
    // Escape quotes
    s = s.replaceAll("\"", "\\\\\"");

    return '"' + s + '"';
  }

  /**
   * Forms a string by repeating the character the given number of times.
   */
  public static String repeatCharacter(char c, int repetitions) {
    StringBuilder s = new StringBuilder(repetitions);
    for (int i = 0; i < repetitions; i++) {
      s.append(c);
    }

    return s.toString();
  }

  /**
   * Return a URL encoded string, assuming "UTF-8" encoding for input. If UTF-8
   * is not supported, used default platform encoding.
   * 
   * @param s String to encode.
   * @return URL encoded string.
   */
  @SuppressWarnings("deprecation")
  public static String urlEncode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return URLEncoder.encode(s);
    }
  }

  private StringUtilities() {
    // Not instantiable
  }

}
