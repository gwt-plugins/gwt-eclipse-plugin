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

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Stores the workspace references to particular Java elements.
 */
public final class JavaRefIndex {

  private static JavaRefIndex INSTANCE;

  private static final String JAVA_REF_LOAD_METHOD = "load";

  private static final String MEMBER_KEY_PREFIX = "::";

  private static final String METHOD_KEY_SUFFIX = "()";

  private static final String SEARCH_INDEX_FILENAME = "searchIndex";

  private static final String TAG_JAVA_REF = "JavaRef";

  private static final String TAG_JAVA_REF_CLASS = "class";

  private static final String TAG_JAVA_REFS = "JavaRefs";

  public static JavaRefIndex getInstance() {
    // Lazily load the search index
    if (INSTANCE == null) {
      INSTANCE = new JavaRefIndex();
    }
    return INSTANCE;
  }

  public static void save() {
    if (INSTANCE == null) {
      return;
    }
    INSTANCE.saveIndex();
  }

  private static String getElementMemberKey(String memberSignature) {
    /*
     * If the element is a method or ctor, strip out the parameter list before
     * adding to the index (but leave the empty () so we know still know it's
     * not a field ref). If we left the parameters in, we would always have to
     * resolve all parameters of the search element before searching the index.
     * Instead, we'll return matches based just on member name and type, and
     * figure out the final answer by resolving the matches against the search
     * element at the time the search is performed.
     */
    int paren = memberSignature.indexOf('(');
    if (paren > -1) {
      memberSignature = memberSignature.substring(0, paren) + METHOD_KEY_SUFFIX;
    }

    return MEMBER_KEY_PREFIX + memberSignature;
  }

  private static File getIndexFile() {
    // The index file will end up in the directory:
    // <workspace>/.metadata/.plugins/com.google.gwt.eclipse.plugin
    return GWTPlugin.getDefault().getStateLocation().append(
        SEARCH_INDEX_FILENAME).toFile();
  }

  /**
   * Contains all the Java references to a particular Java element (type,
   * method, field, or constructor).
   */
  private final Map<String, Set<IIndexedJavaRef>> elementIndex = new HashMap<String, Set<IIndexedJavaRef>>();

  /**
   * Contains the Java references that are inside a particular file (e.g., .java
   * with JSNI, module XML).
   */
  private final Map<IPath, Set<IIndexedJavaRef>> fileIndex = new HashMap<IPath, Set<IIndexedJavaRef>>();

  private JavaRefIndex() {
    loadIndex();
  }

  // TODO: deep copy the added ref so it can't be modified from the outside?
  public void add(IIndexedJavaRef ref) {
    addToElementIndex(ref);
    addToFileIndex(ref);
  }

  // TODO: deep copy the added ref so it can't be modified from the outside?
  public void add(IPath file, Set<IIndexedJavaRef> refs) {
    /*
     * Update the file index by clearing the original entry and then adding a
     * new one. However, we only add an entry if the file actually contains Java
     * references. This prevents the file index from being polluted with a bunch
     * of keys (one per file in the project) that map to an empty set.
     */
    clear(file);
    if (refs.size() > 0) {
      fileIndex.put(file, refs);
    }

    // Update the Java element index
    for (IIndexedJavaRef ref : refs) {
      addToElementIndex(ref);
    }
  }

  public void clear() {
    elementIndex.clear();
    fileIndex.clear();
  }

  public void clear(IPath file) {
    if (fileIndex.containsKey(file)) {
      // Get the Java refs in this file
      Set<IIndexedJavaRef> fileRefs = fileIndex.get(file);

      // Remove all this file's refs from the element index
      for (IIndexedJavaRef fileRef : fileRefs) {
        removeRefsFromElementIndex(fileRef, file);
      }

      // Finally, remove the file's refs from the file index
      fileRefs.clear();
    }
  }

  public void clear(IProject project) {
    for (IPath file : fileIndex.keySet()) {
      if (project.getName().equals(file.segment(0))) {
        // Remove the file's refs from the index
        clear(file);
      }
    }
  }

  public Set<IIndexedJavaRef> findElementReferences(String pattern,
      int elementType, boolean caseSensitive) {
    boolean simpleTypeNameSearch = false;

    // Type matches can be found for either fully-qualified or simple type names
    if (elementType == IJavaElement.TYPE) {
      if (pattern.lastIndexOf('.') == -1) {
        /*
         * If the pattern has no dots, we assume the search pattern is an
         * unqualified type name, which means we'll need to compare the pattern
         * against the simple names of all the types in the index
         */
        simpleTypeNameSearch = true;
      }
    } else {
      if (elementType == IJavaElement.METHOD) {
        // Strip out the parameter list, if one was specified
        int paren = pattern.indexOf('(');
        if (paren > -1) {
          pattern = pattern.substring(0, paren);
        }

        // Make sure the pattern ends with a () to signify a method
        pattern += METHOD_KEY_SUFFIX;
      }

      /*
       * Remove the type name if it precedes the member name. For pattern
       * searching, we match members by name and ignore their type if specified.
       * This is the same behavior as the default JDT Java Search engine.
       */
      int lastDot = pattern.lastIndexOf('.');
      if (lastDot > -1) {
        pattern = pattern.substring(lastDot + 1);
      }

      // Finally, for member searches we need to generate the right kind of key
      // to search the element index with
      pattern = getElementMemberKey(pattern);
    }

    /*
     * If we don't have any wildcard chars and we're doing a case sensitive
     * search and we're not doing a search for a simple type name, we can
     * perform the search much faster by accessing the element index by a key
     */
    if (caseSensitive && !simpleTypeNameSearch && pattern.indexOf('*') == -1
        && pattern.indexOf('?') == -1) {
      return findElementReferences(pattern);
    }

    // Scan all the element index entries sequentially, since we need to do
    // pattern matching on each one to see if it's a match
    Set<IIndexedJavaRef> refs = new HashSet<IIndexedJavaRef>();
    for (String key : elementIndex.keySet()) {
      String element = key;
      if (simpleTypeNameSearch) {
        // Strip the qualifier off the index element before trying to match
        element = Signature.getSimpleName(element);
      }

      char[] patternChars = pattern.toCharArray();

      if (!caseSensitive) {
        /*
         * Convert the pattern to lower case if we're doing a case-insensitive
         * search. You would think the CharOperation.matched method called below
         * would take care of this, since it takes a caseSensitive parameter,
         * but for some reason it only uses that to convert the characters in
         * the 'name' parameter to lower case.
         */
        patternChars = CharOperation.toLowerCase(patternChars);
      }

      if (CharOperation.match(patternChars, element.toCharArray(),
          caseSensitive)) {
        refs.addAll(elementIndex.get(key));
      }
    }

    return refs;
  }

  public Set<IIndexedJavaRef> findFieldReferences(String fieldName) {
    fieldName = MEMBER_KEY_PREFIX + fieldName;
    return findElementReferences(fieldName);
  }

  public Set<IIndexedJavaRef> findMethodReferences(String methodName) {
    methodName = MEMBER_KEY_PREFIX + methodName + METHOD_KEY_SUFFIX;
    return findElementReferences(methodName);
  }

  public Set<IIndexedJavaRef> findTypeReferences(String qualifiedTypeName) {
    // Normalize type name using dots as the enclosing type separator
    qualifiedTypeName = qualifiedTypeName.replace('$', '.');

    Set<IIndexedJavaRef> refs = elementIndex.get(qualifiedTypeName);
    if (refs != null) {
      /*
       * Return a copy of the set instead of the original. This ensures that the
       * return value doesn't react to later changes to the index. The
       * individual references should also be stable because the IIndexedJavaRef
       * interface doesn't define any setters.
       */
      Set<IIndexedJavaRef> copy = new HashSet<IIndexedJavaRef>();
      copy.addAll(refs);
      return copy;
    }

    return Collections.emptySet();
  }

  /**
   * For testing purposes.
   * 
   * @return the number of unique IIndexedJavaRef's in the index
   */
  public int size() {
    int size = 0;
    for (Entry<IPath, Set<IIndexedJavaRef>> fileIndexEntry : fileIndex.entrySet()) {
      String projectName = fileIndexEntry.getKey().segment(0);
      IProject project = Util.getWorkspaceRoot().getProject(projectName);

      if (project.exists() && project.isOpen()) {
        size += fileIndexEntry.getValue().size();
      }
    }
    return size;
  }

  /**
   * For debugging purposes only.
   */
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer(2048);
    sb.append("File Index (" + fileIndex.size() + " entries):\n");
    for (Entry<IPath, Set<IIndexedJavaRef>> fileIndexEntry : fileIndex.entrySet()) {
      sb.append(fileIndexEntry.getKey().toString());
      sb.append(" => \n");
      for (IIndexedJavaRef ref : fileIndexEntry.getValue()) {
        sb.append(MessageFormat.format("    {0}\n", ref));
      }
    }

    sb.append("\n\nElement Index (" + elementIndex.size() + " entries):\n");

    for (Entry<String, Set<IIndexedJavaRef>> elementIndexEntry : elementIndex.entrySet()) {
      sb.append(elementIndexEntry.getKey());
      sb.append(" => \n");
      for (IIndexedJavaRef ref : elementIndexEntry.getValue()) {
        sb.append(MessageFormat.format("    {0}\n", ref));
      }
    }

    return sb.toString();
  }

  private void addMemberToElementIndex(IIndexedJavaRef ref) {
    addToElementIndex(getElementMemberKey(ref.memberSignature()), ref);
  }

  private void addToElementIndex(IIndexedJavaRef ref) {
    for (String className : getClassNames(ref)) {
      addToElementIndex(className, ref);
    }
    if (ref.getMemberOffset() > -1) {
      addMemberToElementIndex(ref);
    }
  }

  private void addToElementIndex(String elementKey, IIndexedJavaRef ref) {
    // If the element is already indexed, just add this location to the list
    if (elementIndex.containsKey(elementKey)) {
      elementIndex.get(elementKey).add(ref);
    } else {
      // Otherwise, create a new entry for this Java element key
      HashSet<IIndexedJavaRef> refs = new HashSet<IIndexedJavaRef>();
      refs.add(ref);
      elementIndex.put(elementKey, refs);
    }
  }

  private void addToFileIndex(IIndexedJavaRef ref) {
    // If the file is already indexed, just add this JavaRef to the list
    IPath file = ref.getSource();
    if (fileIndex.containsKey(file)) {
      fileIndex.get(file).add(ref);
    } else {
      // Otherwise, create a new entry for this file
      HashSet<IIndexedJavaRef> refs = new HashSet<IIndexedJavaRef>();
      refs.add(ref);
      fileIndex.put(file, refs);
    }
  }

  private Set<IIndexedJavaRef> findElementReferences(String elementKey) {
    Set<IIndexedJavaRef> refs = elementIndex.get(elementKey);
    if (refs != null) {
      /*
       * Return a copy of the set instead of the original. This ensures that the
       * return value doesn't react to later changes to the index. The
       * individual references should also be stable because the IIndexedJavaRef
       * interface doesn't define any setters.
       */
      Set<IIndexedJavaRef> copy = new HashSet<IIndexedJavaRef>();
      copy.addAll(refs);
      return copy;
    }

    return Collections.emptySet();
  }

  /**
   * Returns all classes referenced by this Java ref. This includes the specific
   * type specified in the reference, as well as all declaring types. For
   * example, if the ref's className() is 'com.google.A.B.C', this will return
   * the array: [ com.google.A.B.C, com.google.A.B, com.google.A ]
   */
  private String[] getClassNames(IIndexedJavaRef ref) {
    String innermostClassName = ref.className().replace('$', '.');
    List<String> segments = Arrays.asList(Signature.getSimpleNames(innermostClassName));
    List<String> classNames = new ArrayList<String>();

    // Build the list of class names from innermost to outermost
    for (int i = segments.size() - 1; i >= 0; i--) {
      // If we hit a lower-case segment, assume it's a package fragment, which
      // means there are no remaining outer types to add to the index
      if (Character.isLowerCase(segments.get(i).charAt(0))) {
        break;
      }

      // Add the current class name (fully-qualified) to the list
      String[] classSegments = segments.subList(0, i + 1).toArray(new String[0]);
      String className = Signature.toQualifiedName(classSegments);
      classNames.add(className);
    }

    return classNames.toArray(new String[0]);
  }

  private void loadIndex() {
    FileReader reader = null;
    try {
      try {
        reader = new FileReader(getIndexFile());
        loadIndex(XMLMemento.createReadRoot(reader));
      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    } catch (FileNotFoundException e) {
      // Ignore this ex, which occurs when search index does not yet exist
    } catch (Exception e) {
      GWTPluginLog.logError(e, "Error loading search index");
    }
  }

  private void loadIndex(XMLMemento memento) {
    for (IMemento refNode : memento.getChildren(TAG_JAVA_REF)) {
      IIndexedJavaRef ref = loadJavaRef(refNode);
      if (ref != null) {
        // If we are able to re-instantiate the Java reference object, add it to
        // both of the search indices
        add(ref);
      }
    }
  }

  private IIndexedJavaRef loadJavaRef(IMemento refNode) {
    // Make sure we have the Java reference implementation class name
    String refClassName = refNode.getString(TAG_JAVA_REF_CLASS);
    if (refClassName == null) {
      GWTPluginLog.logError(
          "Missing attribute '{0}' for element '{1}' in search index file",
          TAG_JAVA_REF_CLASS, TAG_JAVA_REF);
      return null;
    }

    try {
      // Load the class responsible for this Java reference
      Class<?> refClass = Class.forName(refClassName);

      // Make sure it implements IIndexedJavaRef
      if (!(IIndexedJavaRef.class.isAssignableFrom(refClass))) {
        GWTPluginLog.logError("{0} does not implement {1}", refClassName,
            IIndexedJavaRef.class.getSimpleName());
        return null;
      }

      // Reflectively invoke the static load method
      Method loadMethod = refClass.getDeclaredMethod(JAVA_REF_LOAD_METHOD,
          IMemento.class);
      Object ref = loadMethod.invoke(null, refNode);

      // Verify that the return value is correct type and not null
      if (!(ref instanceof IIndexedJavaRef)) {
        GWTPluginLog.logError("Return value of {0}.{1} does not return a {2}",
            refClassName, JAVA_REF_LOAD_METHOD,
            IIndexedJavaRef.class.getSimpleName());
        return null;
      }

      // If everything worked, return the Java reference object
      return (IIndexedJavaRef) ref;

    } catch (ClassNotFoundException e) {
      GWTPluginLog.logError("Could not find Java ref type " + refClassName);
    } catch (NoSuchMethodException e) {
      GWTPluginLog.logError(
          "The Java ref type {0} is missing the static method '{1}'",
          refClassName, JAVA_REF_LOAD_METHOD);
    } catch (Exception e) {
      GWTPluginLog.logError(e);
    }

    return null;
  }

  private void removeRefsFromElementIndex(IIndexedJavaRef ref, IPath file) {
    for (String className : getClassNames(ref)) {
      removeRefsFromElementIndex(className, file);
    }
    if (ref.getMemberOffset() > -1) {
      removeRefsFromElementIndex(getElementMemberKey(ref.memberSignature()),
          file);
    }
  }

  private void removeRefsFromElementIndex(String elementKey, IPath file) {
    assert (elementIndex.containsKey(elementKey));
    Set<IIndexedJavaRef> elementRefs = elementIndex.get(elementKey);

    for (Iterator<IIndexedJavaRef> i = elementRefs.iterator(); i.hasNext();) {
      if (i.next().getSource().equals(file)) {
        // Remove refs from the specified file
        i.remove();
      }
    }
  }

  private void saveIndex() {
    XMLMemento memento = XMLMemento.createWriteRoot(TAG_JAVA_REFS);
    saveIndex(memento);

    FileWriter writer = null;
    try {
      try {
        writer = new FileWriter(getIndexFile());
        memento.save(writer);
      } finally {
        if (writer != null) {
          writer.close();
        }
      }
    } catch (IOException e) {
      GWTPluginLog.logError(e, "Error saving search index");
    }
  }

  private void saveIndex(XMLMemento memento) {
    for (Entry<IPath, Set<IIndexedJavaRef>> fileEntry : fileIndex.entrySet()) {
      for (IIndexedJavaRef ref : fileEntry.getValue()) {
        IMemento refNode = memento.createChild(TAG_JAVA_REF);
        /*
         * Embed the Java reference class name into the index. This ends up
         * making the resulting index file larger than it really needs to be
         * (around 100 KB for the index containing gwt-user, gwt-lang, and all
         * the gwt-dev projects), but it still loads in around 50 ms on average
         * on my system, so it doesn't seem to be a bottleneck.
         */
        String refClassName = ref.getClass().getName();
        refNode.putString(TAG_JAVA_REF_CLASS, refClassName);

        // The implementation of IIndexedJavaRef serializes itself
        ref.save(refNode);
      }
    }
  }

}
