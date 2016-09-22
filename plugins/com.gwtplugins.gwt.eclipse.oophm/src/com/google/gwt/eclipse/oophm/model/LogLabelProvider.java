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

import com.google.gwt.eclipse.oophm.model.LabelProviderUtilities.ColumnLabelInfo;
import com.google.gwt.eclipse.oophm.model.LogEntry.Data;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider for the viewer used by the
 * {@link com.google.gwt.eclipse.oophm.views.hierarchical.LogContent} panel.
 * 
 * @param <T>
 */
public class LogLabelProvider<T extends IModelNode> extends ColumnLabelProvider {

  @Override
  public Color getForeground(Object element) {
    LogEntry<?> logEntry = (LogEntry<?>) element;
    Data logData = logEntry.getLogData();
    // Attention level supercedes our level when there is a child element that
    // needs attention
    String logLevel = logData.getAttentionLevel() != null
        ? logData.getAttentionLevel() : logData.getLogLevel();
    ColumnLabelInfo columnLabelInfo = LabelProviderUtilities.getColumnLabelInfoFor(logLevel);
    if (columnLabelInfo != null) {
      return columnLabelInfo.getColor();
    }

    // Default color for the context
    return null;
  }

  @Override
  public Image getImage(Object element) {
    LogEntry<?> logEntry = (LogEntry<?>) element;
    Data logData = logEntry.getLogData();
    ColumnLabelInfo columnLabelInfo = LabelProviderUtilities.getColumnLabelInfoFor(logData.getLogLevel());
    if (columnLabelInfo != null) {
      return columnLabelInfo.getImage();
    }

    // No icon
    return null;
  }

  @Override
  public String getText(Object element) {
    LogEntry<?> logEntry = (LogEntry<?>) element;
    return logEntry.getLogData().getLabel();
  }
}
