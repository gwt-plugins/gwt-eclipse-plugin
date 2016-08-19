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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.eclipse.oophm.LogSniffer;
import com.google.gwt.eclipse.oophm.model.BrowserTab.ModuleHandle;

import java.util.ArrayList;
import java.util.List;

/**
 * A log, for either a {@link BrowserTab} or a {@link Server}. A log consists of
 * a list of log entries.
 * 
 * This class is thread-safe.
 * 
 * @param <T>
 */
public class Log<T extends IModelNode> {

  final Object instanceLock = new Object();
  private final T entity;
  private final List<ILogListener<T>> logListeners = new ArrayList<ILogListener<T>>();
  private final LogEntry<T> rootLogEntry;

  /**
   * Create a new instance of a log.
   * 
   * @param entity The entity associated with this log (i.e. a browser tab or a
   *          server)
   */
  public Log(T entity) {
    this.entity = entity;
    rootLogEntry = new LogEntry<T>(null, 0, new ModuleHandle(
        "Hidden Root Module", "Hidden Root Module Session Key"));
    rootLogEntry.setLog(this);
  }

  /**
   * Add a listener for changes to this log.
   */
  public void addLogListener(ILogListener<T> listener) {
    synchronized (instanceLock) {
      logListeners.add(listener);

      // Log this event
      LogSniffer.log("AddLogListener: {0}", listener.toString());
    }
  }

  /**
   * Get the entity associated with this log.
   */
  public T getEntity() {
    return entity;
  }

  /**
   * Return the most-deeply-nested first child of this log that has an attention
   * level which is greater than or equal to all other entries in the log.
   */
  public LogEntry<T> getFirstDeeplyNestedChildWithMaxAttn() {
    synchronized (instanceLock) {
      return getFirstDeeplyNestedChildWithMaxAttnRecurse(rootLogEntry.getDisclosedChildren());
    }
  }

  /**
   * Returns a list of the {@link ILogListener} instances that are registered
   * with the log.
   */
  public List<ILogListener<T>> getLogListeners() {
    synchronized (instanceLock) {
      return new ArrayList<ILogListener<T>>(logListeners);
    }
  }

  /**
   * Gets the root log entry associated with this log. Entries can be added to
   * this log via the root log entry.
   * 
   * @return the root log entry
   */
  public LogEntry<T> getRootLogEntry() {
    return rootLogEntry;
  }

  /**
   * Returns <code>true</code> if there are any child {@link LogEntry
   * LogEntries} that are disclosed.
   */
  public boolean hasDisclosedLogEntries() {
    /*
     * Note: if any child anywhere down the tree is disclosed the all parents
     * back to the root are disclosed. We only need to check the first set of
     * children.
     */
    return !getRootLogEntry().getDisclosedChildren().isEmpty();
  }

  /**
   * Remove a listener from the list of listeners.
   * 
   * @return <code>true</code> if the listener was successfully removed
   */
  public boolean removeLogListener(ILogListener<T> listener) {
    synchronized (instanceLock) {
      // Log this event
      LogSniffer.log("RemoveLogListener: {0}", listener.toString());

      return logListeners.remove(listener);
    }
  }

  /**
   * Marks all entries in this log as undisclosed. Fires an event to all
   * listeners of this log.
   * 
   * NOTE: This method fires events. If you're invoking this method from other
   * model classes, make sure that no locks are being held.
   */
  public void undiscloseAllLogEntries() {
    List<LogEntry<T>> undisclosedLogEntries;

    synchronized (instanceLock) {
      LogEntry<T> logEntry = getRootLogEntry();
      undisclosedLogEntries = setUndisclosed(logEntry);
    }

    fireEntriesRemoved(undisclosedLogEntries);
  }

  void fireNewEntryAdded(int insertionIndex, LogEntry<T> newEntry,
      boolean needsAttention, boolean parentsChanged) {
    LogEntryAddedEvent<T> e = new LogEntryAddedEvent<T>(insertionIndex,
        newEntry, needsAttention, parentsChanged);
    for (ILogListener<T> listener : getLogListeners()) {
      listener.newLogEntry(e);
    }
  }

  private void fireEntriesRemoved(List<LogEntry<T>> logEntriesRemoved) {
    LogEntriesRemovedEvent<T> e = new LogEntriesRemovedEvent<T>(
        logEntriesRemoved);
    for (ILogListener<T> listener : getLogListeners()) {
      listener.logEntriesRemoved(e);
    }
  }

  private LogEntry<T> getFirstDeeplyNestedChildWithMaxAttnRecurse(
      List<LogEntry<T>> logEntries) {

    LogEntry<T> maxEntryNeedsAttn = null;
    TreeLogger.Type maxEntryAttnLevel = null;

    for (LogEntry<T> child : logEntries) {

      /*
       * Determine the child's attention level.
       * 
       * TODO: It is somewhat confusing that getAttentionLevel() is null for
       * those entries that are marked as needsAttention. Perhaps if an entry
       * needs attention, we could also set it's attention level. That would
       * avoid the need for this conditional below.
       */
      TreeLogger.Type childAttnLevel = null;
      if (child.getLogData().getNeedsAttention()) {
        childAttnLevel = LogEntry.toTreeLoggerType(child.getLogData().getLogLevel());
      } else if (child.getLogData().getAttentionLevel() != null) {
        childAttnLevel = LogEntry.toTreeLoggerType(child.getLogData().getAttentionLevel());
      }

      if (childAttnLevel == null) {
        continue;
      }

      if (maxEntryNeedsAttn == null
          || maxEntryAttnLevel.isLowerPriorityThan(childAttnLevel)) {
        maxEntryNeedsAttn = child;
        maxEntryAttnLevel = childAttnLevel;

        /*
         * TODO: Optimization - once you find a node that has an attention level
         * of ERROR, you can break out of the loop. However, this presumes that
         * we know what the highest attention level is. As it is written right
         * now, the attention levels could be re-ordered and this algorithm
         * would still work.
         */
      }
    }

    if (maxEntryNeedsAttn == null) {
      return null;
    }

    LogEntry<T> childOfMaxEntryNeedsAttn = getFirstDeeplyNestedChildWithMaxAttnRecurse(maxEntryNeedsAttn.getDisclosedChildren());
    if (childOfMaxEntryNeedsAttn == null) {
      return maxEntryNeedsAttn;
    }

    // Always favor the child, since we're going for the deepest nesting
    return childOfMaxEntryNeedsAttn;
  }

  private List<LogEntry<T>> setUndisclosed(LogEntry<T> logEntry) {
    List<LogEntry<T>> logEntriesRemoved = new ArrayList<LogEntry<T>>();
    setUndisclosedRecursive(logEntry, logEntriesRemoved);
    return logEntriesRemoved;
  }

  private void setUndisclosedRecursive(LogEntry<T> logEntry,
      List<LogEntry<T>> logEntriesRemoved) {
    List<LogEntry<T>> disclosedChildren = logEntry.getDisclosedChildren();
    for (LogEntry<T> disclosedChild : disclosedChildren) {
      setUndisclosedRecursive(disclosedChild, logEntriesRemoved);
    }

    // Hide all entries except the root
    if (logEntry != getRootLogEntry()) {
      logEntriesRemoved.add(logEntry);
      logEntry.setDisclosed(false);
    }
  }
}
