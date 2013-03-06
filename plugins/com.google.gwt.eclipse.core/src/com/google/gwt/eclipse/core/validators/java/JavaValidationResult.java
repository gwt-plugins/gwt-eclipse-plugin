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
package com.google.gwt.eclipse.core.validators.java;

import com.google.gwt.eclipse.core.markers.GWTJavaProblem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Contains the JSNI Java references found and problems reported when validating
 * a Java file in a GWT project.
 * 
 * TODO: override ValidationResult in gdt.core plugin
 */
public class JavaValidationResult {

  private final List<JsniJavaRef> javaRefs = new ArrayList<JsniJavaRef>();

  private final List<GWTJavaProblem> problems = new ArrayList<GWTJavaProblem>();

  public void addAllJavaRefs(Collection<? extends JsniJavaRef> refs) {
    javaRefs.addAll(refs);
  }

  public void addAllProblems(Collection<? extends GWTJavaProblem> problems) {
    for (GWTJavaProblem problem : problems) {
      addProblem(problem);
    }
  }

  public void addJavaRef(JsniJavaRef ref) {
    javaRefs.add(ref);
  }

  public void addProblem(GWTJavaProblem problem) {
    if (problem == null) {
      // This occurs when the problem creation method returns null because the
      // problem's severity is Ignore.
      return;
    }
    problems.add(problem);
  }

  public List<JsniJavaRef> getJavaRefs() {
    return Collections.unmodifiableList(javaRefs);
  }

  public List<GWTJavaProblem> getProblems() {
    return Collections.unmodifiableList(problems);
  }
}