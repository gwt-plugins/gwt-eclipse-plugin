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
 * An event that is fired whenever an entry is added to a {@link Log}.
 * 
 * @param <T> <T> the entity associated with the log event ({@link BrowserTab}
 *          or {@link Server})
 */
public class LogEntryAddedEvent<T extends IModelNode> {

  private final int insertionIndex;
  private final LogEntry<T> logEntry;
  private final boolean needsAttention;
  private final boolean parentsChanged;

  /**
   * Create a new instance.
   * 
   * @param logEntry the new entry that was added to the log
   */
  public LogEntryAddedEvent(int insertionIndex, LogEntry<T> logEntry,
      boolean needsAttention, boolean parentsChanged) {
    this.insertionIndex = insertionIndex;
    this.logEntry = logEntry;
    this.needsAttention = needsAttention;
    this.parentsChanged = parentsChanged;
  }

  /**
   * Return the new entry that was added to the log.
   */
  public LogEntry<T> getLogEntry() {
    return logEntry;
  }

  /**
   * Returns <code>true</code> if the new element requires attention.  This is 
   * a clue to any viewers that the element should receive focus.
   */
  public boolean needsAttention() {
    return needsAttention;
  }

  /**
   * Returns <code>true</code> if the parents of the element were modified as
   * part of adding the new element.  For example, disclosing a previously 
   * undisclosed parent.
   */
  public boolean parentsChanged() {
    return parentsChanged;
  }

  /**
   * Returns the index where the element was added to its parent.  This can
   * be used by content providers to perform intelligent updates of viewers.
   */
  public int getInsertionIndex() {
    return insertionIndex;
  }
}
