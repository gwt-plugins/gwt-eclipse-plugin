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
package com.google.gwt.eclipse.core.refactoring.regionupdater;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An AST matcher that relaxes matching to cope with element renames.
 */
public class RenamedElementAstMatcher extends ASTMatcher {

  /**
   * Computes a regular expression string where each instance of the old name
   * can be fulfilled by either the old name or new name.
   */
  private static String computeRegexString(String originalString, String oldName, String newName) {
    StringBuilder builder = new StringBuilder(originalString.length());
    
    String oldNameOrNewNameRegEx = String.format("(%s)|(%s)",
        Pattern.quote(oldName), Pattern.quote(newName));

    // Note: The original string can only contain the oldName
    int i = originalString.indexOf(oldName);
    if (i >= 0) {
      int prevI = 0;
      while (i >= 0) {
        // Copy this stuff over directly, but make sure it is escaped for regex
        builder.append(Pattern.quote(originalString.substring(prevI, i)));
        // Append the old-or-new-name regex string
        builder.append(oldNameOrNewNameRegEx);

        prevI = i;
        i = originalString.indexOf(oldName, i + oldName.length());
      }
      
      // Tack on the last part we missed above
      builder.append(Pattern.quote(originalString.substring(prevI
          + oldName.length(), originalString.length())));
    }
    
    return builder.toString();
  }

  private static boolean regexMatches(String originalString, String newString,
      String oldName, String newName) {
    Pattern pattern = Pattern.compile(computeRegexString(originalString,
        oldName, newName));
    Matcher matcher = pattern.matcher(newString);
    return matcher.matches();
  }

  private static boolean safeRenamedEquals(String string1, String string2,
      String oldName, String newName) {
    if (string1 == string2) {
      return true;
    }

    if (string1 == null || string2 == null) {
      return false;
    }

    if (string1.equals(string2)) {
      return true;
    }

    // We don't know if string1 or string2 is the original string, try both
    return regexMatches(string1, string2, oldName, newName)
        || regexMatches(string2, string1, oldName, newName);
  }

  private final String oldName;

  private final String newName;

  public RenamedElementAstMatcher(String oldName, String newName) {
    this.oldName = oldName;
    this.newName = newName;
  }

  @Override
  public boolean match(CharacterLiteral node, Object other) {
    if (!(other instanceof CharacterLiteral)) {
      return false;
    }
    CharacterLiteral o = (CharacterLiteral) other;
    return safeRenamedEquals(node.getEscapedValue(), o.getEscapedValue(),
        oldName, newName);
  }

  @Override
  public boolean match(SimpleName node, Object other) {
    if (!(other instanceof SimpleName)) {
      return false;
    }
    SimpleName o = (SimpleName) other;
    return safeRenamedEquals(node.getIdentifier(), o.getIdentifier(), oldName,
        newName);
  }

  @Override
  public boolean match(TagElement node, Object other) {
    if (!(other instanceof TagElement)) {
      return false;
    }
    TagElement o = (TagElement) other;
    return (safeRenamedEquals(node.getTagName(), o.getTagName(), oldName,
        newName) && safeSubtreeListMatch(node.fragments(), o.fragments()));
  }

  @Override
  public boolean match(TextElement node, Object other) {
    if (!(other instanceof TextElement)) {
      return false;
    }
    TextElement o = (TextElement) other;
    return safeRenamedEquals(node.getText(), o.getText(), oldName, newName);
  }
}
