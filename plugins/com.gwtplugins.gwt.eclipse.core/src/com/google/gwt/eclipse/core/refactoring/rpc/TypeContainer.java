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
package com.google.gwt.eclipse.core.refactoring.rpc;

import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceUtilities;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A container for GWT RPC interface references and relevant data.
 */
class TypeContainer {

  /**
   * Creates and populates a container for the given GWT RPC interface.
   * 
   * @param baseType a GWT RPC interface (e.g. a synchronous or asynchronous
   *          interface)
   * @return a container filled with the given GWT RPC interface and its paired
   *         interface, or null if the given interface is not a GWT RPC
   *         interface
   * @throws JavaModelException if there was a problem finding the paired
   *           interface
   */
  public static TypeContainer createTypeContainer(IType baseType)
      throws JavaModelException {
    boolean isSync = true;
    IType pairedType = null;
    
    if (RemoteServiceUtilities.isSyncInterface(baseType)) {
      isSync = true;
      pairedType = RemoteServiceUtilities.resolveAsyncType(baseType); 
    } else if (RemoteServiceUtilities.isAsyncInterface(baseType)) {
      isSync = false;
      pairedType = RemoteServiceUtilities.resolveSyncType(baseType); 
    }

    return (pairedType != null) ? new TypeContainer(baseType, isSync,
        pairedType) : null; 
  }
  
  private final IType baseType;
  private final boolean isSync;
  private final IType pairedType;

  private TypeContainer(IType baseType, boolean isSync, IType pairedType) {
    this.baseType = baseType;
    this.isSync = isSync;
    this.pairedType = pairedType;
  }

  public IType getBaseType() {
    return baseType;
  }

  public IType getPairedType() {
    return pairedType;
  }

  public boolean isSync() {
    return isSync;
  }
  
}
