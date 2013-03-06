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

import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

import java.util.Map;

/**
 * Test cases for the {@link JsniFormattingUtil} class.
 */
@SuppressWarnings("restriction")
public class JsniFormattingUtilTest extends AbstractGWTPluginTestCase {

  private TestClass testClass;

  @SuppressWarnings("unchecked")
  public void testFormat() throws Exception {

    // Use GWT indentation settings
    Map javaPrefs = JavaCore.getDefaultOptions();
    javaPrefs.put(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "2");
    javaPrefs.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
    javaPrefs.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "2");
    javaPrefs.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY, "0");
    
    Map javaScriptPrefs = JavaScriptCore.getDefaultOptions();
    javaScriptPrefs.put(org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "2");
    javaScriptPrefs.put(org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaScriptCore.SPACE);
    javaScriptPrefs.put(org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "2");
    javaScriptPrefs.put(org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY, "0");
    
    // Get the IDocument for the test class
    ICompilationUnit cu = testClass.getCompilationUnit();

    CompilationUnitEditor editor = null;

    try {
      editor = (CompilationUnitEditor) EditorUtility.openInEditor(cu);
      IEditorInput editorInput = editor.getEditorInput();
      IDocument document = editor.getDocumentProvider().getDocument(editorInput);

      // Apply the formatting and test the result
      TextEdit formatEdit = JsniFormattingUtil.format(document, javaPrefs, javaScriptPrefs, null);
      formatEdit.apply(document);
      assertEquals(getFormattedDocument(), document.get());
    } finally {
      if (editor != null) {
        editor.close(false);
      }
    }
  }

  @Override
  protected TestClass[] getTestClasses() {
    testClass = new TestClass(new String[]{
        "package com.hello.client;",
        "",
        "public class FormattingUtilTest {",
        "",
        "  private static native void jsniMethod()/*-{",
        "    var obj = @com.hello.client.FormattingUtilTest::new()();",
        "    var x = 777;",
        "",
        "    if (x == 777) {",
        "     x += 207;",
        "     alert(\"Hello!\");",
        "    }",
        "",
        "   var text = obj.@com.hello.client.FormattingUtilTest::toString()();",
        "  }-*/;",
        "",
        "  private static native void jsniMethodWithNoIndent()/*-{",
        "var obj = @com.hello.client.FormattingUtilTest::new()();",
        "var x = 777;",
        "",
        "if (x == 777) {",
        "  x += 207;",
        "  alert(\"Hello!\");",
        "}",
        "}-*/;",
        "",
        "  private static native void jsniMethodWithExtraIndent()/*-{",
        "        var obj = @com.hello.client.FormattingUtilTest::new()();",
        "        var x = 777;",
        "",
        "        if (x == 777) {",
        "          x += 207;",
        "          alert(\"Hello!\");",
        "        }",
        "",
        "        var text = obj.@com.hello.client.FormattingUtilTest::toString()();",
        "        }-*/;",
        "",
        "  private static native void jsniMethodWithStartTokenOnSeparateLine()",
        "  /*-{",
        "  var obj = @com.hello.client.FormattingUtilTest::new()();",
        "  var x = 777;",
        "    ",
        "  if (x == 777) {",
        "    x += 207;",
        "    alert(\"Hello!\");",
        "  }",
        "",
        "  var text = obj.@com.hello.client.FormattingUtilTest::toString()();",
        "  }-*/;",
        "",
        "  private static native void jsniMethodWithOuterBlankLines()/*-{",
        "",
        "",
        "    var obj = @com.hello.client.FormattingUtilTest::new()();",
        "    var x = 777;",
        "      ",
        "    if (x == 777) {",
        "      x += 207;",
        "      alert(\"Hello!\");",
        "    }",
        "    ",
        "    var text = obj.@com.hello.client.FormattingUtilTest::toString()();",
        "    ",
        "    ",
        "  }-*/;",
        "",
        "  private static native void emptyJsniMethod()/*-{}-*/;",
        "",
        "  private static native void jsniMethodWithSpace() /*-{",
        "    var x = 777;",
        "  }-*/;",
        "",
        "  private static native void jsniMethodWithDeclarationLineThatWraps(Object o,",
        "      String s, int x)/*-{",
        "    var x = 777;",
        "  }-*/;",
        "",
        "}"
        }, "FormattingUtilTest");
    return new TestClass[]{ testClass };
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

  private String getFormattedDocument() {
    return createString(new String[]{
        "package com.hello.client;",
        "",
        "public class FormattingUtilTest {",
        "",
        "  private static native void jsniMethod()/*-{",
        "    var obj = @com.hello.client.FormattingUtilTest::new()();",
        "    var x = 777;",
        "",
        "    if (x == 777) {",
        "      x += 207;",
        "      alert(\"Hello!\");",
        "    }",
        "",
        "    var text = obj.@com.hello.client.FormattingUtilTest::toString()();",
        "  }-*/;",
        "",
        "  private static native void jsniMethodWithNoIndent()/*-{",
        "    var obj = @com.hello.client.FormattingUtilTest::new()();",
        "    var x = 777;",
        "",
        "    if (x == 777) {",
        "      x += 207;",
        "      alert(\"Hello!\");",
        "    }",
        "  }-*/;",
        "",
        "  private static native void jsniMethodWithExtraIndent()/*-{",
        "    var obj = @com.hello.client.FormattingUtilTest::new()();",
        "    var x = 777;",
        "",
        "    if (x == 777) {",
        "      x += 207;",
        "      alert(\"Hello!\");",
        "    }",
        "",
        "    var text = obj.@com.hello.client.FormattingUtilTest::toString()();",
        "  }-*/;",
        "",
        "  private static native void jsniMethodWithStartTokenOnSeparateLine()",
        "  /*-{",
        "    var obj = @com.hello.client.FormattingUtilTest::new()();",
        "    var x = 777;",
        "",
        "    if (x == 777) {",
        "      x += 207;",
        "      alert(\"Hello!\");",
        "    }",
        "",
        "    var text = obj.@com.hello.client.FormattingUtilTest::toString()();",
        "  }-*/;",
        "",
        "  private static native void jsniMethodWithOuterBlankLines()/*-{",
        "",
        "    var obj = @com.hello.client.FormattingUtilTest::new()();",
        "    var x = 777;",
        "",
        "    if (x == 777) {",
        "      x += 207;",
        "      alert(\"Hello!\");",
        "    }",
        "",
        "    var text = obj.@com.hello.client.FormattingUtilTest::toString()();",
        "",
        "  }-*/;",
        "",
        "  private static native void emptyJsniMethod()/*-{}-*/;",
        "",
        "  private static native void jsniMethodWithSpace() /*-{",
        "    var x = 777;",
        "  }-*/;",
        "",
        "  private static native void jsniMethodWithDeclarationLineThatWraps(Object o,",
        "      String s, int x)/*-{",
        "    var x = 777;",
        "  }-*/;",
        "",
        "}"
      });
  }

}
