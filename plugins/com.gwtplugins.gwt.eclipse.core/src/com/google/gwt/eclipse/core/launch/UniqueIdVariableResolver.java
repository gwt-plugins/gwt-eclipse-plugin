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
package com.google.gwt.eclipse.core.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

/**
 * A variable resolver that resolves to a unique ID for this Eclipse session.
 */
public class UniqueIdVariableResolver implements IDynamicVariableResolver {
  public String resolveValue(IDynamicVariable variable, String argument)
      throws CoreException {
    /*
     * TODO: Generally, a unique ID would work, but the current logic for
     * referencing back to a launch config searches each launch config's
     * command-line args for this string. Having it too short (e.g. based off an
     * AtomicInteger) causes that to fail. Instead of causing churn on that
     * code, we revert back to using nanoTime as the unique ID.
     */
    return String.valueOf(System.nanoTime());
  }
}
