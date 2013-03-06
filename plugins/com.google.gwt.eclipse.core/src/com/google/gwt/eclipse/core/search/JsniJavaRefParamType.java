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

import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.ui.IMemento;

/**
 * Represents a reference parameter type in a JSNI ref's member signature.
 */
public class JsniJavaRefParamType implements IIndexedJavaRef {

  private static final String TAG_OFFSET = "offset";

  private static final String TAG_SOURCE = "source";

  public static JsniJavaRefParamType load(IMemento memento) {
    // Extract the string representing the reference itself
    String refString = memento.getTextData();
    if (refString == null) {
      return null;
    }

    // Set the source property (the file containing the reference)
    String sourceString = memento.getString(TAG_SOURCE);
    if (sourceString == null) {
      return null;
    }

    // Set the offset property (the location within the containing file)
    Integer offset = memento.getInteger(TAG_OFFSET);
    if (offset == null) {
      return null;
    }

    // Parse the reference string into an actual JsniJavaRefParamType
    return JsniJavaRefParamType.parse(new Path(sourceString),
        offset.intValue(), refString);
  }

  public static JsniJavaRefParamType parse(IPath source, int offset,
      String paramTypeString) {
    // Minimal class type signature is of form "LClassName;"
    if (paramTypeString.length() < 3 || !paramTypeString.endsWith(";")) {
      return null;
    }

    // We only consider class types (no primitives)
    if (Signature.getTypeSignatureKind(paramTypeString) != Signature.CLASS_TYPE_SIGNATURE) {
      return null;
    }

    return new JsniJavaRefParamType(source, offset, paramTypeString);
  }

  private final int offset;

  private final IPath source;

  private final String paramTypeString;

  private final String rawClassName;

  private final String className;

  private JsniJavaRefParamType(IPath source, int offset, String paramTypeString) {
    this.source = source;
    this.offset = offset;
    this.paramTypeString = paramTypeString;

    // Strip off 'L' prefix and ';' suffix
    this.rawClassName = paramTypeString.substring(1,
        paramTypeString.length() - 1);

    // Normalized class name should use dots to separate package segments
    this.className = rawClassName.replace('/', '.');
  }

  public String className() {
    return className;
  }

  public int getClassOffset() {
    // Add one for 'L'
    return offset + 1;
  }

  public int getMemberOffset() {
    return -1;
  }

  public int getOffset() {
    return offset;
  }

  public IPath getSource() {
    return source;
  }

  public String memberName() {
    return null;
  }

  public String memberSignature() {
    return null;
  }

  public String rawClassName() {
    return rawClassName;
  }

  public IJavaElement resolve() {
    IProject project = Util.getProject(getSource());
    if (project == null) {
      return null;
    }

    IJavaProject javaProject = JavaCore.create(project);
    if (javaProject == null) {
      return null;
    }

    // Try to find the type in the project's classpath
    return JavaModelSearch.findType(javaProject, className());
  }

  public void save(IMemento memento) {
    memento.putTextData(paramTypeString);
    memento.putString(TAG_SOURCE, getSource().toString());
    memento.putInteger(TAG_OFFSET, getOffset());
  }

}
