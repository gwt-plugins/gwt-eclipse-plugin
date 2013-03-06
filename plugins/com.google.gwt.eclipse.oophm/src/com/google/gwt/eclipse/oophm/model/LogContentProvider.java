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

import com.google.gwt.eclipse.oophm.LogSniffer;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import java.util.List;

/**
 * A content provider for the viewer used by the
 * {@link com.google.gwt.eclipse.oophm.views.hierarchical.LogContent} panel.
 * 
 * @param <T> the entity associated with the log ({@link BrowserTab} or
 *          {@link Server})
 */
public class LogContentProvider<T extends IModelNode> implements
    ITreeContentProvider, ILogListener<T> {

  private static final Object[] NO_ELEMENTS = new Object[0];
  private TreeViewer viewer;

  public void dispose() {
  }

  public Object[] getChildren(Object parentElement) {
    if (parentElement != null) {
      assert (parentElement instanceof LogEntry<?>);

      return ((LogEntry<?>) parentElement).getDisclosedChildren().toArray();
    }

    return NO_ELEMENTS;
  }

  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  public Object getParent(Object element) {
    if (element != null) {
      assert (element instanceof LogEntry<?>);

      return ((LogEntry<?>) element).getParent();
    }

    return null;
  }

  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @SuppressWarnings("unchecked")
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    assert (viewer instanceof TreeViewer);
    assert (oldInput == null || oldInput instanceof LogEntry<?>);
    assert (newInput == null || newInput instanceof LogEntry<?>);

    this.viewer = (TreeViewer) viewer;

    if (oldInput != null) {
      LogEntry<T> oldRootLogEntry = (LogEntry<T>) oldInput;
      oldRootLogEntry.getLog().removeLogListener(this);
    }

    if (newInput != null) {
      LogEntry<T> newRootLogEntry = (LogEntry<T>) newInput;
      newRootLogEntry.getLog().addLogListener(this);
    }
  }

  public void logEntriesRemoved(LogEntriesRemovedEvent<T> e) {
    final List<LogEntry<T>> logEntriesRemoved = e.getLogEntriesRemoved();
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        viewer.remove(logEntriesRemoved.toArray());
      }
    });
  }

  public void newLogEntry(final LogEntryAddedEvent<T> e) {
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        LogEntry<T> logEntry = e.getLogEntry();

        LogEntry<T> parent = logEntry.getParent();
        assert (parent != null);

        LogSniffer.log("{0}.viewer.add({1})",
            LogContentProvider.this.toString(),
            logEntry.getLogData().getLabel());

        /*
         * Insertion will happen in the right place, because we've defined a
         * sorter on this viewer.
         */
        viewer.add(parent, logEntry);

        if (e.parentsChanged()) {

          while (parent != null) {
            // Could specify more specific changes here
            viewer.update(parent, null);
            parent = parent.getParent();
          }
        }

        if (e.needsAttention()) {
          viewer.reveal(logEntry);

          // Set selection to the item that needs attention
          LogEntry<T> firstDeeplyNestedChildWithHighestNeedsAttention = e.getLogEntry().getLog().getFirstDeeplyNestedChildWithMaxAttn();
          if (firstDeeplyNestedChildWithHighestNeedsAttention != null) {
            viewer.setSelection(new StructuredSelection(
                firstDeeplyNestedChildWithHighestNeedsAttention));
          } else {
            // This should never happen
          }
        }
      }
    });
  }
}
