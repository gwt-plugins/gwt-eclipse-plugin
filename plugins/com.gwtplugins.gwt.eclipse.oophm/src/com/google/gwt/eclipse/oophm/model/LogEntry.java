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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A log entry. Log entries can be nested.
 * 
 * This class is thread-safe.
 * 
 * @param <T> the entity associated with the log entry({@link BrowserTab} or
 *          {@link Server})
 */
public class LogEntry<T extends IModelNode> {

  /**
   * Detailed information associated with a log entry.
   * 
   * TODO: This class is very similar to the LogData protobuf class. Consider
   * some sort of unification.
   */
  public static class Data {

    private static String returnDefaultIfEmpty(String str, String defaultVal) {
      if (str == null || str.length() == 0) {
        return defaultVal;
      }

      return str;
    }

    private String attentionLevel;
    private final String details;
    private final String helpInfoText;
    private final String helpInfoURL;
    private final String label;
    private final String logLevel;
    private final boolean needsAttention;
    private final long timestamp;

    /**
     * Create a new instance.
     * 
     * @param label the label for the log entry
     * @param details any details (such as a stack trace) associated with the
     *          entry
     * @param logLevel the level (ERROR, WARN, etc..) for the log entry
     * @param helpInfoURL a URL that points to helpful information associated
     *          with the log entry
     * @param helpInfoText some helpful text associated with the log entry
     * @param timestamp the time that this entry was created
     */
    public Data(String label, String details, String logLevel,
        String helpInfoURL, String helpInfoText, long timestamp,
        boolean needsAttention) {
      this.label = returnDefaultIfEmpty(label, "Unknown");
      this.details = returnDefaultIfEmpty(details, "");
      this.logLevel = returnDefaultIfEmpty(logLevel, "INFO");
      this.helpInfoText = returnDefaultIfEmpty(helpInfoText, "");
      this.helpInfoURL = returnDefaultIfEmpty(helpInfoURL, "");
      this.timestamp = timestamp;
      this.needsAttention = needsAttention;
    }

    /**
     * Returns the max log level of a child node who needs attention or
     * <code>null</code> if there isn't one.
     */
    public String getAttentionLevel() {
      return attentionLevel;
    }

    public String getDetails() {
      return details;
    }

    public String getHelpInfoText() {
      return helpInfoText;
    }

    public String getHelpInfoURL() {
      return helpInfoURL;
    }

    public String getLabel() {
      return label;
    }

    public String getLogLevel() {
      return logLevel;
    }

    public boolean getNeedsAttention() {
      return needsAttention;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public void setAttentionLevel(String attentionLevel) {
      this.attentionLevel = attentionLevel;
    }
  }

  enum DisclosureFilter {
    DISCLOSED {
      @Override
      public boolean matches(LogEntry<?> logEntry) {
        return logEntry.isDisclosed();
      }
    },
    /**
     * No entries excluded.
     */
    NONE {
      @Override
      public boolean matches(LogEntry<?> logEntry) {
        return true;
      }
    },
    UNDISCLOSED {
      @Override
      public boolean matches(LogEntry<?> logEntry) {
        return !logEntry.isDisclosed();
      }
    };

    public abstract boolean matches(LogEntry<?> logEntry);
  }

  /**
   * A comparator that can be used to compare two different log entries. The log
   * entries must have the same parent, and they must not be children of a root
   * log entry.
   */
  private static final Comparator<LogEntry<?>> NON_ROOT_LOG_ENTRY_COMPARATOR = new Comparator<LogEntry<?>>() {
    public int compare(LogEntry<?> o1, LogEntry<?> o2) {
      if (o1 == null || o2 == null) {
        throw new IllegalArgumentException("Cannot compare null objects.");
      }

      if (o1 == o2) {
        return 0;
      }

      if (o1.getParent() != o2.getParent()) {
        throw new IllegalArgumentException(
            "Cannot compare log entries that have different parents.");
      }

      if ((o1.getParent() == o1.getLog().getRootLogEntry())) {
        throw new IllegalArgumentException(
            "Cannot compare log entries that are direct children of a root log entry.");
      }

      return o1.index - o2.index;
    }
  };

  /**
   * Returns the {@link TreeLogger.Type} enum value corresponding to the
   * <code>treeLoggerTypeName</code> or null if there isn't one.
   */
  public static TreeLogger.Type toTreeLoggerType(String treeLoggerTypeName) {
    assert (treeLoggerTypeName != null);
    try {
      return TreeLogger.Type.valueOf(treeLoggerTypeName);
    } catch (IllegalArgumentException e) {
      // Ignored
    }

    return null;
  }

  private final List<LogEntry<T>> children = new ArrayList<LogEntry<T>>();
  private final ModuleHandle moduleHandle;
  private boolean disclosed = true;
  private final int index;
  private Log<T> log = null;
  private final Data logData;

  private LogEntry<T> parent = null;

  /**
   * Create a log entry.
   * 
   * @param logData detailed information for this log entry
   * @param index the index of the log entry among the parent entry's children
   * @param moduleHandle the handle to the module that created this log entry
   */
  public LogEntry(Data logData, int index, ModuleHandle moduleHandle) {
    this.moduleHandle = moduleHandle;
    this.logData = logData;
    this.index = index;
  }

  /**
   * Add a log entry as a child of this entry. Fires an event to all listeners
   * of the log associated with this entry. May also set the attention level on
   * the entity associated with this entry's {@link Log}, which in turn will
   * cause events to be fired. See
   * {@link INeedsAttention#setNeedsAttentionLevel(String)}.
   */
  public void addChild(LogEntry<T> child) {
    boolean shouldUpdateParents = false;
    boolean childNeedsAttention = false;

    int insertionIndex;

    // Only fires the event once the lock has been released. Holding on to
    // the lock while firing the event may lead to deadlock.
    synchronized (log.instanceLock) {
      child.setLog(log);
      child.setParent(this);

      if (getLog().getRootLogEntry() != this) {
        insertionIndex = insertIntoNonRootEntry(child);
      } else {
        insertionIndex = insertIntoRootEntry(child);
      }

      LogSniffer.log("inserted at index {0,number,#}", insertionIndex);

      if (insertionIndex == -1) {
        return;
      }

      Data childLogData = child.getLogData();
      childNeedsAttention = childLogData.getNeedsAttention();
      LogEntry<T> parent = this;

      while (parent != null) {
        if (!parent.isDisclosed()) {
          // Force the parent to be disclosed since it received a new child
          parent.setDisclosed(true);
          shouldUpdateParents = true;
        }

        if (childNeedsAttention) {
          shouldUpdateParents = true;

          Data parentLogData = parent.getLogData();
          if (parentLogData != null) {
            if (shouldPropageLogLevelToParent(parentLogData, childLogData)) {
              parentLogData.setAttentionLevel(childLogData.getLogLevel());
            }
          } else {
            // Looking at the root
          }
        }

        parent = parent.getParent();
      }
    }

    log.fireNewEntryAdded(insertionIndex, child, childNeedsAttention,
        shouldUpdateParents);

    if (childNeedsAttention) {
      // TODO: Improve type safety
      T entity = log.getEntity();
      if (entity instanceof INeedsAttention) {
        ((INeedsAttention) entity).setNeedsAttentionLevel(child.getLogData().getLogLevel());
      }
    }
  }

  /**
   * Returns all children of this node including undisclosed entries.
   */
  public List<LogEntry<T>> getAllChildren() {
    return getFilteredChildren(DisclosureFilter.NONE);
  }

  /**
   * Returns children that are disclosed by the model.
   */
  public List<LogEntry<T>> getDisclosedChildren() {
    return getFilteredChildren(DisclosureFilter.DISCLOSED);
  }

  /**
   * Return the log associated with this entry.
   */
  public Log<T> getLog() {
    return log;
  }

  /**
   * Return the log entry's data.
   */
  public Data getLogData() {
    return logData;
  }

  /**
   * Return the name of the module that generated this log entry.
   */
  public ModuleHandle getModuleHandle() {
    return moduleHandle;
  }

  /**
   * Return the parent of this entry, or <code>null
   * </code> if this is a top-level log
   * entry.
   */
  public LogEntry<T> getParent() {
    return parent;
  }

  /**
   * Returns the disclosure state of this element.
   */
  public boolean isDisclosed() {
    return disclosed;
  }

  /**
   * Sets the disclosure state of this element.
   */
  public void setDisclosed(boolean disclosed) {
    this.disclosed = disclosed;
  }

  @Override
  public String toString() {
    String logLevel;
    String label;

    if (logData != null) {
      label = logData.getLabel();
      logLevel = logData.getLogLevel();
    } else {
      label = "Root LogEntry";
      logLevel = "Root entry has no log level";
    }

    return String.format("[%1$s] [%3$s] - %2$s", logLevel, label,
        moduleHandle.getName());
  }

  void setLog(Log<T> log) {
    this.log = log;
  }

  void setParent(LogEntry<T> parent) {
    this.parent = parent;
  }

  /**
   * Returns a list of children excluding the elements according to the
   * {@link DisclosureFilter}.
   */
  private List<LogEntry<T>> getFilteredChildren(
      DisclosureFilter disclosureFilter) {
    synchronized (log.instanceLock) {
      List<LogEntry<T>> filteredChildren = new ArrayList<LogEntry<T>>();
      for (LogEntry<T> logEntry : children) {
        if (disclosureFilter.matches(logEntry)) {
          filteredChildren.add(logEntry);
        }
      }

      return filteredChildren;
    }
  }

  /**
   * Insert a new child of a non-root log entry. In order to call this method,
   * this entry must NOT be the root log entry.
   * 
   * The log's instance lock should be held for the duration of this method.
   * 
   * @param newChild the child to insert
   * @return the position where the child was inserted among the entry's
   *         children, or -1 if this entry is already in the set of children
   */
  private int insertIntoNonRootEntry(LogEntry<T> newChild) {
    assert (this != this.getLog().getRootLogEntry());

    /*
     * At the non-root level, the children are all from the same module logger.
     * It is safe to use a binary insertion algorithm that compares the entry
     * indexes.
     */

    int insertionIndex = Collections.binarySearch(children, newChild,
        NON_ROOT_LOG_ENTRY_COMPARATOR);
    if (insertionIndex >= 0) {
      // This child is already in the set of children...
      return -1;
    }

    // Convert the index into the real insertion index and add the child
    insertionIndex = -(insertionIndex + 1);
    children.add(insertionIndex, newChild);

    return insertionIndex;
  }

  /**
   * Insert a new child of a root log entry. In order to call this method, this
   * entry must be the root log entry.
   * 
   * The log's instance lock should be held for the duration of this method.
   * 
   * @param newChild the child to insert
   * @return the position where the child was inserted among the root entry's
   *         children, or -1 if this entry is already in the set of children
   */
  private int insertIntoRootEntry(LogEntry<T> newChild) {
    assert (this == this.getLog().getRootLogEntry());
    assert (newChild.getModuleHandle() != null);

    /*
     * This insertion algorithm differs from the non-root case because the root
     * log entry may have children from different module loggers. As a result,
     * their indexes can only be interpreted in the context of the module that
     * generated them.
     */

    int insertionIndex = children.size();
    for (int i = children.size() - 1; i > -1; i--) {
      LogEntry<T> curChild = children.get(i);
      if (newChild.getModuleHandle() == curChild.getModuleHandle()) {
        if (newChild.index > curChild.index) {
          insertionIndex = i + 1;
          break;
        } else if (newChild.index == curChild.index) {
          // This child is already in the set of children
          insertionIndex = -1;
          break;
        }
      }
    }

    if (insertionIndex != -1) {
      children.add(insertionIndex, newChild);
    }

    return insertionIndex;
  }

  private boolean shouldPropageLogLevelToParent(Data parentLogData,
      Data childLogData) {
    TreeLogger.Type parentLogLevel = toTreeLoggerType(parentLogData.logLevel);
    TreeLogger.Type childLogLevel = toTreeLoggerType(childLogData.logLevel);
    return parentLogLevel == null || childLogLevel == null
        || parentLogLevel.isLowerPriorityThan(childLogLevel);
  }

}
