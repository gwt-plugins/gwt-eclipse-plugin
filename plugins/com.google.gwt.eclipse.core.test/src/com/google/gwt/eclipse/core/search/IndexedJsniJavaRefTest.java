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

import com.google.gwt.eclipse.core.validators.java.JsniJavaRef;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.core.runtime.Path;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import java.io.StringReader;

/**
 * Test cases for the {@link IndexedJsniJavaRef} class.
 */
public class IndexedJsniJavaRefTest extends AbstractGWTPluginTestCase {

  private static IndexedJsniJavaRef loadRef(String xmlFragment)
      throws WorkbenchException {
    StringReader reader = new StringReader(xmlFragment);
    XMLMemento memento = XMLMemento.createReadRoot(reader);
    return IndexedJsniJavaRef.load(memento);
  }

  public void testLoad() throws WorkbenchException {
    // Test loading from a well-formed XML fragment
    IndexedJsniJavaRef ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.IndexedJsniJavaRef\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">@com.hello.Hello::sayHi(Ljava/lang/String;)</JavaRef>");
    assertNotNull(ref);
    assertEquals(25, ref.getOffset());
    assertEquals("/MyProject/src/com/hello/Hello.java",
        ref.getSource().toString());
    assertEquals("com.hello.Hello", ref.className());
    assertEquals("sayHi", ref.memberName());
    assertEquals("Ljava/lang/String;", ref.paramTypesString());

    // Test fragment missing the Java ref string
    assertNull(loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.IndexedJsniJavaRef\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\"></JavaRef>"));

    // Test fragment with malformed Java ref string
    assertNull(loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.IndexedJsniJavaRef\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">ABC!@#</JavaRef>"));

    // Test fragment missing the source attribute
    assertNull(loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.IndexedJsniJavaRef\" offset=\"25\">@com.hello.Hello::sayHi(Ljava/lang/String;)</JavaRef>"));

    // Test fragment missing the offset attribute
    assertNull(loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.IndexedJsniJavaRef\" source=\"/MyProject/src/com/hello/Hello.java\">@com.hello.Hello::sayHi(Ljava/lang/String;)</JavaRef>"));
  }

  public void testMemberSignature() throws WorkbenchException {
    // Test regular method
    IndexedJsniJavaRef ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.IndexedJsniJavaRef\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">@com.hello.Hello::sayHi()</JavaRef>");
    assertEquals("sayHi()", ref.memberSignature());

    // Test constructor
    ref = loadRef("<JavaRef class=\"com.google.gwt.eclipse.plugin.search.IndexedJsniJavaRef\" offset=\"25\" source=\"/MyProject/src/com/hello/Hello.java\">@com.hello.Hello::new()</JavaRef>");
    assertEquals("Hello()", ref.memberSignature());
  }

  public void testResolve() {
    // The resolve() method just delegates to JsniJavaRef.resolve(IJavaProject),
    // so we're not going to add a duplicate test here.
  }

  public void testSave() {
    XMLMemento memento = XMLMemento.createWriteRoot("JavaRef");

    // Set up a test JsniJavaRef to serialize
    JsniJavaRef ref = JsniJavaRef.parse("@com.hello.Hello::sayHi(Ljava/lang/String;)");
    ref.setOffset(25);
    ref.setSource(new Path("/MyProject/src/com/hello/Hello.java"));
    IndexedJsniJavaRef indexedRef = new IndexedJsniJavaRef(ref);

    indexedRef.save(memento);
    assertEquals("@com.hello.Hello::sayHi(Ljava/lang/String;)",
        memento.getTextData());
    assertEquals(25, memento.getInteger("offset").intValue());
    assertEquals("/MyProject/src/com/hello/Hello.java",
        memento.getString("source"));
  }

}
