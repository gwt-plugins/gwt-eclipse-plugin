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
package com.google.gdt.eclipse.core.validation;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Pairs a set of problems and type dependencies detected by the validation
 * code.
 */
public class ValidationResult {

  private final List<CategorizedProblem> problems;

  private final List<String> typeDependencies;

  public ValidationResult() {
    this.problems = new ArrayList<CategorizedProblem>();
    this.typeDependencies = new ArrayList<String>();
  }

  public ValidationResult(List<CategorizedProblem> problems,
      List<String> typeDependencies) {
    this.problems = new ArrayList<CategorizedProblem>(problems);
    this.typeDependencies = new ArrayList<String>(typeDependencies);
  }

  public void addAllProblems(Collection<? extends CategorizedProblem> problems) {
    this.problems.addAll(problems);
  }

  public void addAllTypeDependencies(
      Collection<String> qualifiedTypeDependencies) {
    for (String typeDep : qualifiedTypeDependencies) {
      addTypeDependency(typeDep);
    }
  }

  public void addProblem(CategorizedProblem problem) {
    if (problem == null) {
      // This occurs when the problem creation method returns null because the
      // problem's severity is Ignore.
      return;
    }
    problems.add(problem);
  }

  public void addTypeDependency(String qualifiedTypeDependency) {
    typeDependencies.add(qualifiedTypeDependency.replace('$', '.'));
  }

  public List<CategorizedProblem> getProblems() {
    return problems;
  }

  public List<String> getTypeDependencies() {
    return typeDependencies;
  }

}