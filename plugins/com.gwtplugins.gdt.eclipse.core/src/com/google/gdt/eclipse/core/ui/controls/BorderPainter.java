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
package com.google.gdt.eclipse.core.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

/**
 * Border painter is used to provide an optional seperator between entries in a
 * control list. This class is used as a utility method in SelectableControlList
 * and typically called when children register themselves.
 */
public class BorderPainter implements PaintListener {

  public void paintControl(PaintEvent e) {
    Composite composite = (Composite) e.widget;
    Rectangle bounds = composite.getBounds();
    GC gc = e.gc;
    gc.setLineStyle(SWT.LINE_DOT);
    gc.drawLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y);
  }

}
