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

import java.util.List;

/**
 * An interface that is used to overlay a tree-like structure over part of the
 * model. This helps to reinforce the parent-child relationships between some of
 * the elements in the model, and it also helps to simplify the implementations
 * of some content providers that use the model.
 */
public interface IModelNode {

  /**
   * Get this node's children, or an empty list if this node has no children.
   */
  List<? extends IModelNode> getChildren();

  /**
   * Returns the unique id of this node. These IDs are monotonically increasing.
   */
  int getId();

  /**
   * Return the name of this node (typically for display purposes).
   */
  String getName();

  /**
   * Return the "needs attention" level for this node, or <code>null</code> if
   * this node does not require attention. The attention level typically
   * corresponds to to one of the log levels in
   * {@link com.google.gwt.core.ext.TreeLogger.Type}
   * 
   * @return the attention level, or <code>null</code> if this node does not
   *         require attention
   */
  String getNeedsAttentionLevel();

  /**
   * Return this node's parent, or <code>null</code> if this node has no parent
   */
  IModelNode getParent();

  /**
   * Returns <code>true</code> if this node is terminated.
   */
  boolean isTerminated();
}
