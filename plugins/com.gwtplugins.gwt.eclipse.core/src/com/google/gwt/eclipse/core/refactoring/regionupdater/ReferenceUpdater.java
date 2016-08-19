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
package com.google.gwt.eclipse.core.refactoring.regionupdater;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * Updates references which could have changed from the time of refactoring
 * creation to the time of refactoring performing (since other participants or
 * the processor could have renamed the elements).
 */
public class ReferenceUpdater {

  /**
   * Returns an updated binding key for the given binding key.
   */
  public String getUpdatedBindingKey(String bindingKey) {
    return bindingKey;
  }

  /**
   * Returns an updated compilation unit for the given compilation unit.
   */
  public ICompilationUnit getUpdatedCompilationUnit(
      ICompilationUnit compilationUnit) {
    return compilationUnit;
  }

}
