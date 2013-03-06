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
package com.google.gwt.eclipse.core.validators.clientbundle;

import com.google.gdt.eclipse.core.validation.ValidationResult;

import org.eclipse.core.runtime.IPath;

import java.util.HashSet;
import java.util.Set;

/**
 * Records possible resource paths for ClientBundle subtypes, in addition to
 * validation errors. Note that the set of paths returned includes *all*
 * possible paths that would match the methods of the ClientBundle (all default
 * extensions are considered, absolute and relative paths are included, etc.).
 */
public class ClientBundleValidationResult extends ValidationResult {

  private final Set<IPath> possibleResourcePaths;

  public ClientBundleValidationResult() {
    super();
    possibleResourcePaths = new HashSet<IPath>();
  }

  public void addAllPossibleResourcePaths(Set<IPath> paths) {
    possibleResourcePaths.addAll(paths);
  }

  public void addPossibleResourcePath(IPath path) {
    possibleResourcePaths.add(path);
  }

  public Set<IPath> getPossibleResourcePaths() {
    return possibleResourcePaths;
  }

}
