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
package com.google.gwt.eclipse.oophm.breadcrumbs;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

/**
 * A viewer row for the breadcrumb viewer.
 * 
 * NOTE: This code was derived from
 * org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.BreadcrumbViewerRow.
 * 
 * @since 3.4
 */
class BreadcrumbViewerRow extends ViewerRow {

  private Color fForeground;
  private Font fFont;
  private Color fBackground;

  private final BreadcrumbItem fItem;
  private final BreadcrumbViewer fViewer;

  public BreadcrumbViewerRow(BreadcrumbViewer viewer, BreadcrumbItem item) {
    fViewer = viewer;
    fItem = item;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#clone()
   */
  public Object clone() {
    return new BreadcrumbViewerRow(fViewer, fItem);
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getBackground(int)
   */
  public Color getBackground(int columnIndex) {
    return fBackground;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getBounds()
   */
  public Rectangle getBounds() {
    return fItem.getBounds();
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getBounds(int)
   */
  public Rectangle getBounds(int columnIndex) {
    return getBounds();
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getColumnCount()
   */
  public int getColumnCount() {
    return 1;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getControl()
   */
  public Control getControl() {
    return fViewer.getControl();
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getElement()
   */
  public Object getElement() {
    return fItem.getData();
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getFont(int)
   */
  public Font getFont(int columnIndex) {
    return fFont;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getForeground(int)
   */
  public Color getForeground(int columnIndex) {
    return fForeground;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getImage(int)
   */
  public Image getImage(int columnIndex) {
    return fItem.getImage();
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getItem()
   */
  public Widget getItem() {
    return fItem;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getNeighbor(int, boolean)
   */
  public ViewerRow getNeighbor(int direction, boolean sameLevel) {
    return null;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getText(int)
   */
  public String getText(int columnIndex) {
    return fItem.getText();
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#getTreePath()
   */
  public TreePath getTreePath() {
    return new TreePath(new Object[] {getElement()});
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#setBackground(int,
   * org.eclipse.swt.graphics.Color)
   */
  public void setBackground(int columnIndex, Color color) {
    fBackground = color;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#setFont(int,
   * org.eclipse.swt.graphics.Font)
   */
  public void setFont(int columnIndex, Font font) {
    fFont = font;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#setForeground(int,
   * org.eclipse.swt.graphics.Color)
   */
  public void setForeground(int columnIndex, Color color) {
    fForeground = color;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#setImage(int,
   * org.eclipse.swt.graphics.Image)
   */
  public void setImage(int columnIndex, Image image) {
    fItem.setImage(image);
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerRow#setText(int, java.lang.String)
   */
  public void setText(int columnIndex, String text) {
    fItem.setText(text);
  }

}
