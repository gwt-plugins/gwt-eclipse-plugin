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
package com.google.gdt.eclipse.gph.util;

import java.util.regex.Pattern;

/*
 * Wiki markup help
 * 
 * =Heading1= ==Heading2== ===Heading3===
 * 
 * bold* _italic_ `inline code` escape: `*`
 * 
 * Indent lists 2 spaces: bullet item # numbered list
 * 
 * {{{ verbatim code block }}}
 * 
 * Horizontal rule ----
 * 
 * WikiWordLink [http://domain/page label] http://domain/page
 * 
 * || table || cells ||
 */

/**
 * A utility class for dealing with the GPH wiki syntax.
 * 
 * @see http://code.google.com/p/support/wiki/WikiSyntax
 */
public class WikiUtils {

  private static Pattern HEADERS_PATTERN = Pattern.compile("=+");
  private static Pattern LINK_PATTERN = Pattern.compile("[\\{\\}]*");
  private static Pattern PRAGMA_PATTERN = Pattern.compile("=+");
  private static Pattern XMLTAGS_PATTERN = Pattern.compile("<.*?>");

  /**
   * Convert the given string from wiki format to html. We convert the simple
   * stuff and strip out the complicated stuff.
   * 
   * @param text wiki formatted text
   * @return the equivalent html
   */
  public static String convertToHtml(String text) {
    text = convertInternal(text);

    // Handle the <hr/> separator.
    text = text.replaceAll("----", "<hr/>");

    // Convert end-of-lines to line breaks.
    text = text.replaceAll("\n", "<br/>");

    return text;
  }

  public static String convertToText(String text) {
    text = convertInternal(text);

    // Convert the <hr/> separator to newlines.
    text = text.replaceAll("----", "\n");

    // Remove single leading spaces.
    text = text.replaceAll("\n ", "\n");

    // Converts multiple empty lines to one newline.
    text = text.replaceAll("\r\n", "\n");
    text = text.replaceAll("\n\n\n+", "\n\n");
    
    // Remove any single leading space from the text.
    if (text.startsWith(" ")) {
      text = text.substring(1);
    }

    return text;
  }

  private static String convertInternal(String text) {
    // Remove XML tags.
    text = XMLTAGS_PATTERN.matcher(text).replaceAll("");

    // Remove header syntax "== header here ==".
    text = HEADERS_PATTERN.matcher(text).replaceAll("");

    // Remove any pragmas.
    text = PRAGMA_PATTERN.matcher(text).replaceAll("");

    // Remove any {{{ }}} link patterns.
    text = LINK_PATTERN.matcher(text).replaceAll("");

    return text;
  }

  // private static String bracket(String input, Pattern pattern, String left,
  // String right) {
  // StringBuffer buffer = new StringBuffer();
  //
  // Matcher matcher = pattern.matcher(input);
  //
  // while (matcher.matches()) {
  // matcher.appendReplacement(buffer, left + matcher.group(1) + right);
  // }
  //
  // matcher.appendTail(buffer);
  //
  // return buffer.toString();
  // }

}
