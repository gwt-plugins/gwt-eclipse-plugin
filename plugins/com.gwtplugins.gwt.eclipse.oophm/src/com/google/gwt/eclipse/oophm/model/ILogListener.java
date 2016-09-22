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
 * An interface for listening for changes to logs.
 * 
 * TODO: We may need to add methods for removal of log entries (user-initiated
 * through the view), and possibly modification of log entries (parent node
 * expansion on errors).
 * 
 * @param <T>
 */
public interface ILogListener<T extends IModelNode> {

  /**
   * Called whenever a new log entry is added.
   */
  void newLogEntry(LogEntryAddedEvent<T> e);
  
  /**
   * Called whenever entries are removed or marked as undisclosed. 
   */
  void logEntriesRemoved(LogEntriesRemovedEvent<T> e);
  
}
