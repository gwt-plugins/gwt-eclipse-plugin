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

import com.google.gwt.eclipse.core.util.Util;
import com.google.gwt.eclipse.core.validators.java.JsniJavaRef;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMemento;

import java.text.MessageFormat;

/**
 * Represents a JSNI Java reference in the search index.
 */
public class IndexedJsniJavaRef extends JsniJavaRef implements IIndexedJavaRef {

  private static final String TAG_OFFSET = "offset";

  private static final String TAG_SOURCE = "source";

  public static IndexedJsniJavaRef load(IMemento memento) {
    // Extract the string representing the reference itself
    String refString = memento.getTextData();
    if (refString == null) {
      return null;
    }

    // Parse the reference string into an actual JsniJavaRef
    JsniJavaRef ref = JsniJavaRef.parse(refString);
    if (ref == null) {
      return null;
    }

    // Set the source property (the file containing the reference)
    String sourceString = memento.getString(TAG_SOURCE);
    if (sourceString == null) {
      return null;
    } else {
      ref.setSource(new Path(sourceString));
    }

    // Set the offset property (the location within the containing file)
    Integer offset = memento.getInteger(TAG_OFFSET);
    if (offset == null) {
      return null;
    } else {
      ref.setOffset(offset.intValue());
    }

    return new IndexedJsniJavaRef(ref);
  }

  public IndexedJsniJavaRef(JsniJavaRef ref) {
    super(ref);
  }

  @Override
  public String memberSignature() {
    /*
     * Add JSNI ctors to the index as they would be defined in Java code (use
     * the name of the class as the member name instead of "new"). That way the
     * search index can remain generic (it shouldn't need to have any knowledge
     * of the specifics of Java references inside JSNI blocks).
     */
    if (isConstructor()) {
      return simpleClassName() + "()";
    }

    return super.memberSignature();
  }

  public String rawClassName() {
    return className();
  }

  public IJavaElement resolve() {
    try {
      // Figure out the containing project based on its source file path

      IProject project = Util.getProject(getSource());
      if (project == null) {
        return null;
      }

      IJavaProject javaProject = JavaCore.create(project);
      if (javaProject == null) {
        return null;
      }

      // Delegate to JsniJavaRef for the actual work of resolving the reference
      return super.resolveJavaElement(javaProject);
    } catch (Exception e) {
      return null;
    }
  }

  public void save(IMemento memento) {
    // We have to make sure to call super.toString() here to use JsniRef's
    // toString(), which provides the literal text of the Java reference)
    memento.putTextData(super.toString());
    memento.putString(TAG_SOURCE, getSource().toString());
    memento.putInteger(TAG_OFFSET, getOffset());
  }

  @Override
  public void setOffset(int offset) {
    // IIndexedJavaRef's should be immutable
    throw new UnsupportedOperationException("Offset property is read-only");
  }

  @Override
  public void setSource(IPath source) {
    // IIndexedJavaRef's should be immutable
    throw new UnsupportedOperationException("Source property is read-only");
  }

  /**
   * This is *not* just for debugging: our super class JsniJavaRef uses the
   * toString() method in its implementation of equals and hashCode. Thus, we
   * must include the location of this reference in the toString return value.
   */
  @Override
  public String toString() {
    String superString = super.toString();
    return MessageFormat.format("{0} @ {1} ({2}, {3})", superString,
        getSource().toString(), String.valueOf(getOffset()),
        String.valueOf(superString.length()));
  }

}
