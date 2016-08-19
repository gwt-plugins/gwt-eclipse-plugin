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
package com.google.gwt.eclipse.core.search;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import java.io.StringReader;

/**
 * Test cases for the {@link JsniJavaRefParamType} class.
 */
public class JsniJavaRefParamTypeTest extends TestCase {

  private static JsniJavaRefParamType loadRef(String xmlFragment)
      throws WorkbenchException {
    StringReader reader = new StringReader(xmlFragment);
    XMLMemento memento = XMLMemento.createReadRoot(reader);
    return JsniJavaRefParamType.load(memento);
  }

  public void testLoad() throws WorkbenchException {
    // Test loading from a well-formed XML fragment
    JsniJavaRefParamType ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>");
    assertNotNull(ref);

    // Test fragment missing the Java ref string
    assertNull(loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\"></JavaRef>"));

    // Test fragment with malformed Java ref string
    assertNull(loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">ABC!@#</JavaRef>"));

    // Test fragment missing the source attribute
    assertNull(loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\">Ljava/lang/String;</JavaRef>"));

    // Test fragment missing the offset attribute
    assertNull(loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>"));
  }

  public void testClassName() throws WorkbenchException {
    JsniJavaRefParamType ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>");
    assertEquals("java.lang.String", ref.className());
  }

  public void testGetClassOffset() throws WorkbenchException {
    JsniJavaRefParamType ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>");
    assertEquals(26, ref.getClassOffset());
  }

  public void testGetMemberOffset() throws WorkbenchException {
    JsniJavaRefParamType ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>");
    assertEquals(-1, ref.getMemberOffset());
  }

  public void testGetOffset() throws WorkbenchException {
    JsniJavaRefParamType ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>");
    assertEquals(25, ref.getOffset());
  }

  public void testGetSource() throws WorkbenchException {
    JsniJavaRefParamType ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>");
    assertEquals(new Path("/MyProject/src/com/hello/Hello.java"),
        ref.getSource());
  }

  public void testMemberName() throws WorkbenchException {
    JsniJavaRefParamType ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>");
    assertNull(ref.memberName());
  }

  public void testMemberSignature() throws WorkbenchException {
    JsniJavaRefParamType ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>");
    assertNull(ref.memberSignature());
  }

  public void testRawClassName() throws WorkbenchException {
    JsniJavaRefParamType ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.JsniJavaRefParamType\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">Ljava/lang/String;</JavaRef>");
    assertEquals("java/lang/String", ref.rawClassName());
  }

  public void testResolve() {
    // The resolve() method just delegates to
    // JavaModelSearch.findType(IJavaProject, String), so we're not going to add
    // a duplicate test here.
  }

  public void testSave() {
    XMLMemento memento = XMLMemento.createWriteRoot("JavaRef");

    // Set up a test JsniJavaRefParamType to serialize
    JsniJavaRefParamType ref = JsniJavaRefParamType.parse(new Path(
        "/MyProject/src/com/hello/Hello.java"), 25, "Ljava/lang/String;");

    ref.save(memento);
    assertEquals("Ljava/lang/String;", memento.getTextData());
    assertEquals(25, memento.getInteger("offset").intValue());
    assertEquals("/MyProject/src/com/hello/Hello.java",
        memento.getString("source"));
  }

}
