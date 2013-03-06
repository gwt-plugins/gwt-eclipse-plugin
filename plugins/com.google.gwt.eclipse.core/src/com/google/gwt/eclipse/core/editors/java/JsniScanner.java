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
package com.google.gwt.eclipse.core.editors.java;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

import java.util.ArrayList;

/**
 * Scanner that finds the parts of JSNI blocks that need syntax coloring.
 */
public class JsniScanner extends RuleBasedScanner {

  /**
   * Identifies Java references within JSNI blocks. This rule does not need to
   * validate the reference completely, but just do a quick check to verify that
   * it matches the expected format for a Java reference.
   */
  private static class JavaRefRule extends PatternRule {
    // TODO: make this rule smarter so it can detect the proper end of a field
    // reference in all cases. For example, it currently doesn't work on this
    // reference in RichTextAreaImpl (coloring extends to the ( instead of
    // stopping after the "elem"):
    //
    // @com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.focus();

    public JavaRefRule(IToken token) {
      super("@", "(", token, '/', true);
    }

    protected JavaRefRule(String startSequence, String endSequence,
        IToken token, char escapeChar, boolean endOnEOF) {
      super(startSequence, endSequence, token, escapeChar, endOnEOF);
    }

    @Override
    protected boolean endSequenceDetected(ICharacterScanner scanner) {
      int c = 0, readCount = 1;
      while ((c = scanner.read()) != ICharacterScanner.EOF) {
        if (!isValidJavaRefCharacter((char) c)) {
          scanner.unread();
          return true;
        }
        readCount++;
      }

      while (--readCount > 0) {
        scanner.unread();
      }

      return super.endSequenceDetected(scanner);
    }

    private boolean isValidJavaRefCharacter(char c) {
      if (Character.isJavaIdentifierPart(c)) {
        return true;
      }

      if (c == ':' || c == '.') {
        return true;
      }

      // TODO: if Java refs can span multiple lines, need to allow
      // line delimiters also

      return false;
    }
  }

  private static class JsWordDetector implements IWordDetector {

    public boolean isWordPart(char c) {
      return Character.isJavaIdentifierPart(c);
    }

    public boolean isWordStart(char c) {
      return Character.isJavaIdentifierPart(c);
    }
  }

  private static class WhitespaceDetector implements IWhitespaceDetector {

    public boolean isWhitespace(char c) {
      return Character.isWhitespace(c);
    }
  }

  private static final String[] JS_KEYWORDS = {
      "break", "case", "catch", "continue", "default", "delete", "do", "else",
      "false", "finally", "for", "function", "if", "in", "instanceof", "new",
      "null", "return", "switch", "this", "throw", "true", "try", "typeof",
      "var", "void", "while", "with"};

  // TODO: maybe add array of JS ctors, functions (eval, Object, etc.)

  public JsniScanner(IColorManager colorManager) {
    // TODO: get these from GWT preference store instead of Java's

    // TODO: need to refresh colorManager when prefs change
    //
    IToken jsniJavaRef = new Token(new TextAttribute(
        colorManager.getColor(JsniColorConstants.JSNI_JAVA_REF)));

    IToken jsniComment = new Token(new TextAttribute(
        colorManager.getColor(JsniColorConstants.JSNI_COMMENT)));

    IToken jsniKeyword = new Token(new TextAttribute(
        colorManager.getColor(JsniColorConstants.JSNI_KEYWORD)));

    IToken jsniDefault = new Token(new TextAttribute(
        colorManager.getColor(JsniColorConstants.JSNI_DEFAULT)));

    IToken jsniString = new Token(new TextAttribute(
        colorManager.getColor(JsniColorConstants.JSNI_STRING)));

    ArrayList<IRule> rules = new ArrayList<IRule>();

    // Java references
    rules.add(new JavaRefRule(jsniJavaRef));

    // single line comments
    rules.add(new EndOfLineRule("//", jsniComment));

    // JS keywords
    WordRule keywordRules = new WordRule(new JsWordDetector(), jsniDefault);
    for (String keyword : JS_KEYWORDS) {
      keywordRules.addWord(keyword, jsniKeyword);
    }
    rules.add(keywordRules);

    // Strings
    rules.add(new SingleLineRule("\"", "\"", jsniString, '\\', true));
    rules.add(new SingleLineRule("'", "'", jsniString, '\\', true));

    // Add generic whitespace rule.
    rules.add(new WhitespaceRule(new WhitespaceDetector()));

    // convert to array and set them for RuleBasedScanner to use
    setRules(rules.toArray(new IRule[rules.size()]));
  }
}
