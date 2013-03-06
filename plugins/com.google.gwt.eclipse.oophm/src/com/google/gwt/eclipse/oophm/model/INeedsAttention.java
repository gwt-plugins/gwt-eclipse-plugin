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
package com.google.gwt.eclipse.oophm.model;

/**
 * Interface implemented by {@link IModelNode}s that have an attention level.
 */
public interface INeedsAttention {

  /**
   * Set the attention level. The level typically corresponds to one of the
   * {@link com.google.gwt.core.ext.TreeLogger.Type} values. Can be set to
   * <code>null</code>, which has the effect of clearing the attention level for
   * this node.
   * 
   * The attention level will only be set if:
   * 
   * 1) <code>needsAttentionLevel</code> is non-null and the attention level of
   * the node is <code>null</code>
   * 
   * 2) <code>needsAttentionLevel</code> is non-null and the attention level of
   * the node is a lower priority than <code>needsAttentionLevel</code>.
   * 
   * 3) <code>needsAttentionLevel</code> is null and the attention level of the
   * node is non-null
   * 
   * Fires an event to all listeners on the {@link WebAppDebugModel}.
   * 
   * NOTE: As this method fires events, if you're invoking this method from
   * other model classes, make sure that no locks are being held.
   * 
   * @param needsAttentionLevel a string value corresponding to one of the log
   *          levels in {@link com.google.gwt.core.ext.TreeLogger.Type}
   */
  void setNeedsAttentionLevel(String needsAttentionLevel);
}
