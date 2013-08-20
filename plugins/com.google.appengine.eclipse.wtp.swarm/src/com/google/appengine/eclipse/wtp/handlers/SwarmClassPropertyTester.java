/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.handlers;

import com.google.appengine.eclipse.wtp.swarm.AppEngineSwarmPlugin;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmAnnotationUtils;
import com.google.gdt.eclipse.appengine.swarm.util.SwarmType;
import com.google.gdt.eclipse.core.AdapterUtilities;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import java.util.List;

/**
 * A {@link PropertyTester} which determines if the CU can be used for API generating with given
 * class.
 */
public final class SwarmClassPropertyTester extends PropertyTester {
  private static final String PROPERTY_HAS_TYPES = "hasTypes";

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if (PROPERTY_HAS_TYPES.equals(property)) {
      ICompilationUnit cu = AdapterUtilities.getAdapter(receiver, ICompilationUnit.class);
      if (cu == null) {
        return false;
      }
      try {
        List<IType> entityList = Lists.newArrayList();
        SwarmAnnotationUtils.collectSwarmTypes(entityList, cu);
        if (entityList.isEmpty()) {
          return false;
        }
        if ((SwarmAnnotationUtils.getSwarmType(entityList.get(0)) != SwarmType.ENTITY)
            && (SwarmAnnotationUtils.getSwarmType(entityList.get(0)) != SwarmType.PERSISTENCE_CAPABLE)) {
          return false;
        }
        return true;
      } catch (JavaModelException e) {
        AppEngineSwarmPlugin.logMessage(e);
        return false;
      }
    }
    // unknown property
    return false;
  }
}
