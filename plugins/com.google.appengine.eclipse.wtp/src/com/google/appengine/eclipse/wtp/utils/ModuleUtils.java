/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.utils;

import org.eclipse.wst.server.core.IModule;

/**
 * Utility methods to work with {@link IModule}.
 */
public final class ModuleUtils {
  public static final String MODULETYPE_JST_EAR = "jst.ear";
  public static final String MODULETYPE_JST_WEB = "jst.web";

  /**
   * @return <code>true</code> if <code>module</code> type is not <code>null</code> and equals to
   *         given type ID string.
   */
  public static boolean isModuleType(IModule module, String moduleTypeId) {
    return module.getModuleType() != null && moduleTypeId.equals(module.getModuleType().getId());
  }

  /**
   * Not instantiable.
   */
  private ModuleUtils() {
  }
}
