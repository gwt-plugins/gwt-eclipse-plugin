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
package com.google.gdt.eclipse.core;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;

/**
 * Utility methods for dealing with {@link org.eclipse.core.runtime.IAdaptable} objects.
 */
public class AdapterUtilities {

  /**
   * Returns an instance of the adaptable object with type of adapterType, or
   * null if the given object could not be adapted to that type. See
   * {@link IAdapterManager#getAdapter(Object, Class)}.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getAdapter(Object adaptable,
      Class<? extends T> adapterType) {

    /*
     * Try a simple instanceof check. This also safeguards against a misuse of
     * getAdapter. For example, ISelection is adaptable (there is a
     * SelectionAdapterFactory), so the user may incorrectly try adapting it to
     * IStructuredSelection. However, in this case, the implementation of
     * SelectionAdapterFactory would return null.
     */
    if (adapterType.isInstance(adaptable)) {
      return (T) adaptable;
    }
    
    IAdapterManager adapterManager = Platform.getAdapterManager();
    assert (adapterManager != null);
    return (T) adapterManager.getAdapter(adaptable, adapterType);
  }
}
