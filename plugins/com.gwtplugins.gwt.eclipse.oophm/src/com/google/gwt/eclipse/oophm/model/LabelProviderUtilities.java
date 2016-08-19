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

import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.eclipse.oophm.Activator;
import com.google.gwt.eclipse.oophm.DevModeImages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides the foreground colors and images that correspond to particular log
 * level.
 */
public class LabelProviderUtilities {

  public static class ColumnLabelInfo {
    private final Color color;
    private final Image image;

    public ColumnLabelInfo(int colorId, String imageId) {
      this.color = Display.getCurrent().getSystemColor(colorId);
      this.image = Activator.getDefault().getImage(imageId);
    }

    public Color getColor() {
      return color;
    }

    public Image getImage() {
      return image;
    }
  }

  private static final Map<Type, ColumnLabelInfo> typeToColumnLabelInfos = new HashMap<Type, ColumnLabelInfo>();
  static {
    typeToColumnLabelInfos.put(Type.DEBUG, new ColumnLabelInfo(
        SWT.COLOR_DARK_CYAN, DevModeImages.LOG_ITEM_DEBUG));
    typeToColumnLabelInfos.put(Type.ERROR, new ColumnLabelInfo(SWT.COLOR_RED,
        DevModeImages.LOG_ITEM_ERROR));
    typeToColumnLabelInfos.put(Type.INFO, new ColumnLabelInfo(SWT.COLOR_BLACK,
        DevModeImages.LOG_ITEM_INFO));
    typeToColumnLabelInfos.put(Type.SPAM, new ColumnLabelInfo(
        SWT.COLOR_DARK_GREEN, DevModeImages.LOG_ITEM_SPAM));
    typeToColumnLabelInfos.put(Type.TRACE, new ColumnLabelInfo(
        SWT.COLOR_DARK_GRAY, DevModeImages.LOG_ITEM_TRACE));
    typeToColumnLabelInfos.put(Type.WARN, new ColumnLabelInfo(
        SWT.COLOR_DARK_YELLOW, DevModeImages.LOG_ITEM_WARNING));
  }

  public static ColumnLabelInfo getColumnLabelInfoFor(String treeLoggerLevelName) {
    Type logLevel = LogEntry.toTreeLoggerType(treeLoggerLevelName);
    if (logLevel != null) {
      return typeToColumnLabelInfos.get(logLevel);
    }
    return null;
  }

}
