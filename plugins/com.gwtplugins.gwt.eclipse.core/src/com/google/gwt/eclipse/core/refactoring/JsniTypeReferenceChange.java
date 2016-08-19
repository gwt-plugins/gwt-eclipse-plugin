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
package com.google.gwt.eclipse.core.refactoring;

import com.google.gwt.eclipse.core.platformproxy.refactoring.IJsniTypeReferenceChange;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import java.lang.reflect.Field;

/**
 * In addition to the extra logic in JsniReferenceChange to recalculate our edit
 * offsets, this class also clears stale JavaRefIndex entries and checks for and
 * corrects for the case where the type we're changing is the same one we're
 * updating references in.
 * 
 * Note that this is the Eclipse 3.5 implementation of the common
 * {@link IJsniTypeReferenceChange} interface.
 */
public class JsniTypeReferenceChange extends JsniReferenceChange implements
    IJsniTypeReferenceChange {

  private static Field getField(String fieldName, Class<?> startingClass) {

    while (startingClass != null) {
      try {
        Field field = startingClass.getDeclaredField(fieldName);
        if (field != null) {
          return field;
        }
      } catch (SecurityException e) {
        // Ignore
      } catch (NoSuchFieldException e) {
        // Ignore
      }
      startingClass = startingClass.getSuperclass();
    }
    return null;
  }

  protected final JsniTypeReferenceChangeHelper helper;

  public JsniTypeReferenceChange(GWTTypeRefactoringSupport refactoringSupport,
      ICompilationUnit cu) {
    super(refactoringSupport, cu);
    helper = new JsniTypeReferenceChangeHelper(this);
  }

  @Override
  public GWTTypeRefactoringSupport getRefactoringSupport() {
    return (GWTTypeRefactoringSupport) refactoringSupport;
  }

  @Override
  public Change perform(IProgressMonitor pm) throws CoreException {
    helper.perform(pm, getCompilationUnit());
    return super.perform(pm);
  }

  public void setCompilationUnit(ICompilationUnit newCu) {
    try {
      Field cuField = getField("fCUnit", getClass());
      if (cuField != null) {
        cuField.setAccessible(true);
        cuField.set(this, newCu);
      }

      Field fileField = getField("fFile", TextFileChange.class);
      if (fileField != null) {
        fileField.setAccessible(true);
        fileField.set(this, newCu.getResource());
      }
    } catch (IllegalArgumentException e) {
      // Ignore
    } catch (IllegalAccessException e) {
      // Ignore
    }
  }
}
