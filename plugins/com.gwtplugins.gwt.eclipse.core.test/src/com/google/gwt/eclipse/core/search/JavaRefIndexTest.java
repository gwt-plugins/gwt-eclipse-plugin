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
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Test cases for the {@link JavaRefIndex} class.
 */
public class JavaRefIndexTest extends AbstractGWTPluginTestCase {

  private static final IPath FILE_1 = new Path("/ProjectA/src/Hello.java");

  private static final IPath FILE_2 = new Path("/ProjectB/src/Second.java");

  private static final String[] REFS_GROUP_1 = new String[] {
      "@com.hello.Hello::sayHi(Ljava/lang/String;)", "@com.hello.Hello::new()",
      "@com.hello.Hello::field1",
      "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)",
      "@com.hello.Greeter::sayHello(Ljava/lang/String;)"};

  private static final String[] REFS_GROUP_2 = new String[] {
      "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)",
      "@com.hello.Greeter::sayHello(Ljava/lang/String;)"};

  private static IndexedJsniJavaRef createJavaRef(String refString, IPath file) {
    JsniJavaRef ref = JsniJavaRef.parse(refString);
    ref.setSource(file);
    IndexedJsniJavaRef indexedRef = new IndexedJsniJavaRef(ref);
    return indexedRef;
  }

  private Map<String, Set<IIndexedJavaRef>> elementIndex;

  private Map<IPath, Set<IIndexedJavaRef>> fileIndex;

  public void testAddFileRefs() {
    Set<IIndexedJavaRef> refs;

    // Add Java refs in two projects
    addRefsToIndex(REFS_GROUP_1, FILE_1);
    addRefsToIndex(REFS_GROUP_2, FILE_2);

    // Check the file index
    assertEquals(2, fileIndex.size());
    refs = fileIndex.get(FILE_1);
    assertEquals(5, refs.size());
    refs = fileIndex.get(FILE_2);
    assertEquals(2, refs.size());

    // Check the element index
    assertEquals(7, elementIndex.size());
    refs = elementIndex.get("::sayHi()");
    assertEquals(3, refs.size());
    refs = elementIndex.get("com.hello.Hello.Inner");
    assertEquals(2, refs.size());
    refs = elementIndex.get("com.hello.Greeter");
    assertEquals(2, refs.size());
    refs = elementIndex.get("::Hello()");
    assertEquals(1, refs.size());
    refs = elementIndex.get("::field1");
    assertEquals(1, refs.size());
    refs = elementIndex.get("com.hello.Hello");
    assertEquals(5, refs.size());
    refs = elementIndex.get("::sayHello()");
    assertEquals(2, refs.size());
  }

  public void testAddRef() throws Exception {
    Set<IIndexedJavaRef> refs;

    // 1. Add one Java ref and check the index
    IIndexedJavaRef ref = addRefToIndex(
        "@com.hello.Hello::sayHi(Ljava/lang/String;)", FILE_1);

    // Check the element index
    assertEquals(2, elementIndex.size());
    assertTrue(elementIndex.containsKey("com.hello.Hello"));
    refs = elementIndex.get("com.hello.Hello");
    assertEquals(1, refs.size());
    assertTrue(refs.contains(ref));
    assertTrue(elementIndex.containsKey("::sayHi()"));
    refs = elementIndex.get("::sayHi()");
    assertEquals(1, refs.size());
    assertTrue(refs.contains(ref));

    // Check the file index
    assertEquals(1, fileIndex.size());
    assertTrue(fileIndex.containsKey(FILE_1));
    refs = fileIndex.get(FILE_1);
    assertEquals(1, refs.size());
    assertTrue(refs.contains(ref));

    // 2. Add the same Java ref again and verify that the index is unchanged
    addRefToIndex("@com.hello.Hello::sayHi(Ljava/lang/String;)", FILE_1);

    // Check the element index
    assertEquals(2, elementIndex.size());
    assertTrue(elementIndex.containsKey("com.hello.Hello"));
    refs = elementIndex.get("com.hello.Hello");
    assertEquals(1, refs.size());
    assertTrue(refs.contains(ref));
    assertTrue(elementIndex.containsKey("::sayHi()"));
    refs = elementIndex.get("::sayHi()");
    assertEquals(1, refs.size());
    assertTrue(refs.contains(ref));

    // Check the file index
    assertEquals(1, fileIndex.size());
    assertTrue(fileIndex.containsKey(FILE_1));
    refs = fileIndex.get(FILE_1);
    assertEquals(1, refs.size());
    assertTrue(refs.contains(ref));

    // 3. Add a Java ref in the same file, referencing the same class
    IIndexedJavaRef ref2 = addRefToIndex("@com.hello.Hello::sayGutenTag()",
        FILE_1);

    // Check the element index
    assertEquals(3, elementIndex.size());
    assertTrue(elementIndex.containsKey("com.hello.Hello"));
    refs = elementIndex.get("com.hello.Hello");
    assertEquals(2, refs.size());
    assertTrue(refs.contains(ref));
    assertTrue(refs.contains(ref2));

    assertTrue(elementIndex.containsKey("::sayHi()"));
    refs = elementIndex.get("::sayHi()");
    assertEquals(1, refs.size());
    assertTrue(refs.contains(ref));

    assertTrue(elementIndex.containsKey("::sayGutenTag()"));
    refs = elementIndex.get("::sayGutenTag()");
    assertEquals(1, refs.size());
    assertTrue(refs.contains(ref2));

    // Check the file index
    assertEquals(1, fileIndex.size());
    assertTrue(fileIndex.containsKey(FILE_1));
    refs = fileIndex.get(FILE_1);
    assertEquals(2, refs.size());
    assertTrue(refs.contains(ref));
  }

  public void testClearAll() {
    addRefsToIndex(REFS_GROUP_1, FILE_1);
    assertTrue(elementIndex.size() > 0);
    assertTrue(fileIndex.size() > 0);

    // Now clear everything
    JavaRefIndex.getInstance().clear();
    assertEquals(0, elementIndex.size());
    assertEquals(0, fileIndex.size());
  }

  public void testClearFile() {
    addRefGroups();

    // Now remove one of the files from the index. This doesn't actually
    // remove the removed file's key from the fileIndex, but it does remove all
    // of the file's refs from the map. It also removes all entries in the
    // elementIndex that were contained in the removed file
    JavaRefIndex.getInstance().clear(FILE_1);
    assertEquals(0, fileIndex.get(FILE_1).size());
    Collection<Set<IIndexedJavaRef>> remainingRefs = elementIndex.values();
    for (Set<IIndexedJavaRef> elementRefs : remainingRefs) {
      for (IIndexedJavaRef elementRef : elementRefs) {
        assertFalse(elementRef.getSource().equals(FILE_1));
      }
    }
  }

  public void testClearProject() {
    addRefGroups();

    // Now clear entries for one of the projects
    JavaRefIndex.getInstance().clear(
        Util.getWorkspaceRoot().getProject("ProjectA"));
    Collection<Set<IIndexedJavaRef>> remainingRefs = elementIndex.values();
    for (Set<IIndexedJavaRef> elementRefs : remainingRefs) {
      for (IIndexedJavaRef elementRef : elementRefs) {
        assertFalse(elementRef.getSource().segment(0).equals("ProjectA"));
      }
    }
  }

  public void testFindElementReferences() {
    addRefGroups();
    Set<IIndexedJavaRef> refs;

    // Search for a field (using case insensitive search and a pattern that
    // differs in case than the actual element name)
    refs = JavaRefIndex.getInstance().findElementReferences("FIELD1",
        IJavaElement.FIELD, false);
    assertEquals(1, refs.size());
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::field1", FILE_1)));

    // Do the same thing, but case sensitive this time
    refs = JavaRefIndex.getInstance().findElementReferences("FIELD1",
        IJavaElement.FIELD, true);
    assertEquals(0, refs.size());

    // Search for a field that does not exist
    refs = JavaRefIndex.getInstance().findElementReferences("badfield",
        IJavaElement.FIELD, false);
    assertEquals(0, refs.size());

    // Search for a method
    refs = JavaRefIndex.getInstance().findElementReferences("sayHello",
        IJavaElement.METHOD, false);
    assertEquals(2, refs.size());
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Greeter::sayHello(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Greeter::sayHello(Ljava/lang/String;)", FILE_2)));

    // Search for a method using the * wildcard (match 0 or more chars)
    refs = JavaRefIndex.getInstance().findElementReferences("sayH*",
        IJavaElement.METHOD, true);
    assertEquals(5, refs.size());
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Greeter::sayHello(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Greeter::sayHello(Ljava/lang/String;)", FILE_2)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_2)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello::sayHi(Ljava/lang/String;)", FILE_1)));

    // Search for a method using the ? wildcard (match exactly 1 char)
    refs = JavaRefIndex.getInstance().findElementReferences("sayH?",
        IJavaElement.METHOD, true);
    assertEquals(3, refs.size());
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_2)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello::sayHi(Ljava/lang/String;)", FILE_1)));

    // Search for a method with a parameter list
    refs = JavaRefIndex.getInstance().findElementReferences("sayHello(String)",
        IJavaElement.METHOD, false);
    assertEquals(2, refs.size());
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Greeter::sayHello(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Greeter::sayHello(Ljava/lang/String;)", FILE_2)));

    // Search for a method with a parameter list and a fully-qualified type name
    refs = JavaRefIndex.getInstance().findElementReferences(
        "com.hello.Greeter.sayHello(java.lang.String)", IJavaElement.METHOD,
        false);
    assertEquals(2, refs.size());
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Greeter::sayHello(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Greeter::sayHello(Ljava/lang/String;)", FILE_2)));

    // Search for a method that does not exist
    refs = JavaRefIndex.getInstance().findElementReferences("badMethod",
        IJavaElement.METHOD, false);
    assertEquals(0, refs.size());

    // Search for a constructor
    refs = JavaRefIndex.getInstance().findElementReferences("Hello()",
        IJavaElement.METHOD, false);
    assertEquals(1, refs.size());
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::Hello()", FILE_1)));

    // Search for a constructor that does not exist
    refs = JavaRefIndex.getInstance().findElementReferences("NotThere()",
        IJavaElement.METHOD, false);
    assertEquals(0, refs.size());

    // Search for a type by its fully-qualified name
    refs = JavaRefIndex.getInstance().findElementReferences("com.hello.Hello",
        IJavaElement.TYPE, false);
    assertEquals(5, refs.size());
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::field1", FILE_1)));
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::Hello()", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello::sayHi(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_2)));

    // Search for a type by the simple (unqualified) name
    refs = JavaRefIndex.getInstance().findElementReferences("Hello",
        IJavaElement.TYPE, false);
    assertEquals(5, refs.size());
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::field1", FILE_1)));
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::Hello()", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello::sayHi(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_2)));
  }

  public void testFindFieldReferences() {
    addRefGroups();
    Set<IIndexedJavaRef> refs;

    // Search for a field that exists
    refs = JavaRefIndex.getInstance().findFieldReferences("field1");
    assertEquals(1, refs.size());
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::field1", FILE_1)));

    // Search for a field that does not exist
    refs = JavaRefIndex.getInstance().findFieldReferences("badfield");
    assertEquals(0, refs.size());
  }

  public void testFindMethodReferences() {
    addRefGroups();
    Set<IIndexedJavaRef> refs;

    // Search for a method that exists
    refs = JavaRefIndex.getInstance().findMethodReferences("sayHi");
    assertEquals(3, refs.size());
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_2)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello::sayHi(Ljava/lang/String;)", FILE_1)));

    // Search for a constructor that exists
    refs = JavaRefIndex.getInstance().findMethodReferences("Hello");
    assertEquals(1, refs.size());
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::Hello()", FILE_1)));

    // Search for a method that does not exist
    refs = JavaRefIndex.getInstance().findMethodReferences("badmethod");
    assertEquals(0, refs.size());
  }

  public void testFindTypeReferences() {
    addRefGroups();
    Set<IIndexedJavaRef> refs;

    // Search for a type that exists
    refs = JavaRefIndex.getInstance().findTypeReferences("com.hello.Hello");
    assertEquals(5, refs.size());
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::field1", FILE_1)));
    assertTrue(refs.contains(createJavaRef("@com.hello.Hello::Hello()", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello::sayHi(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_1)));
    assertTrue(refs.contains(createJavaRef(
        "@com.hello.Hello$Inner::sayHi(Ljava/lang/String;)", FILE_2)));

    // Search for a type that does not exist
    refs = JavaRefIndex.getInstance().findTypeReferences("com.type.not.Here");
    assertEquals(0, refs.size());
  }

  public void testGetIndex() {
    // This will generate a new index if there's not already one there
    JavaRefIndex index = JavaRefIndex.getInstance();

    // This will return the already-created index
    JavaRefIndex index2 = JavaRefIndex.getInstance();

    // Both variables should point to the same singleton index
    assertEquals(index, index2);
  }

  public void testSave() {
    // Add one entry to the index and save
    addRefToIndex("@com.hello.Hello::sayHi()", FILE_1);
    JavaRefIndex.save();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Initialize the index
    JavaRefIndex.getInstance().clear();

    // Cache references to both indices
    elementIndex = getElementIndex();
    fileIndex = getFileIndex();
  }

  private void addRefGroups() {
    // Add Java refs in two projects
    addRefsToIndex(REFS_GROUP_1, FILE_1);
    addRefsToIndex(REFS_GROUP_2, FILE_2);
    assertEquals(2, fileIndex.size());
    assertEquals(7, elementIndex.size());
  }

  private IIndexedJavaRef[] addRefsToIndex(String[] refStrings, IPath file) {
    IIndexedJavaRef[] refs = new IIndexedJavaRef[refStrings.length];
    for (int i = 0; i < refStrings.length; i++) {
      refs[i] = addRefToIndex(refStrings[i], file);
    }

    return refs;
  }

  private IIndexedJavaRef addRefToIndex(String refString, IPath file) {
    IndexedJsniJavaRef indexedRef = createJavaRef(refString, file);
    JavaRefIndex.getInstance().add(indexedRef);
    return indexedRef;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Set<IIndexedJavaRef>> getElementIndex() throws Exception {
    Field elementIndexField = JavaRefIndex.class.getDeclaredField("elementIndex");
    elementIndexField.setAccessible(true);
    return (Map<String, Set<IIndexedJavaRef>>) elementIndexField.get(JavaRefIndex.getInstance());
  }

  @SuppressWarnings("unchecked")
  private Map<IPath, Set<IIndexedJavaRef>> getFileIndex() throws Exception {
    Field fileIndexField = JavaRefIndex.class.getDeclaredField("fileIndex");
    fileIndexField.setAccessible(true);
    return (Map<IPath, Set<IIndexedJavaRef>>) fileIndexField.get(JavaRefIndex.getInstance());
  }

}
