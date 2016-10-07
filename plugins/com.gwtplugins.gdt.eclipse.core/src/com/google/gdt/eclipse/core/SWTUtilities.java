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
package com.google.gdt.eclipse.core;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * SWT-related functions.
 */
public final class SWTUtilities {

  /**
   * Returns the number of pixels corresponding to the height of the given
   * number of characters.
   * <p>
   * The required <code>FontMetrics</code> parameter may be created in the
   * following way: <code>
   *  GC gc = new GC(control);
   *  gc.setFont(control.getFont());
   *  fontMetrics = gc.getFontMetrics();
   *  gc.dispose();
   * </code>
   * </p>
   * 
   * Note: This code was taken from org.eclipse.jface.dialogs.Dialog.
   * 
   * @param fontMetrics used in performing the conversion
   * @param chars the number of characters
   * @return the number of pixels
   */
  public static int convertHeightInCharsToPixels(FontMetrics fontMetrics, int chars) {
    return fontMetrics.getHeight() * chars;
  }

  /**
   * Returns the number of pixels corresponding to the width of the given number
   * of characters.
   * <p>
   * The required <code>FontMetrics</code> parameter may be created in the
   * following way: <code>
   *  GC gc = new GC(control);
   *  gc.setFont(control.getFont());
   *  fontMetrics = gc.getFontMetrics();
   *  gc.dispose();
   * </code>
   * </p>
   * 
   * Note: This code was taken from org.eclipse.jface.dialogs.Dialog.
   * 
   * @param fontMetrics used in performing the conversion
   * @param chars the number of characters
   * @return the number of pixels
   */
  public static int convertWidthInCharsToPixels(FontMetrics fontMetrics, int chars) {
    return fontMetrics.getAverageCharWidth() * chars;
  }

  /**
   * Creates a multi-line textbox.
   * 
   * @param parent a composite control which will be the parent of the new
   *          instance (cannot be null)
   * @param style the style of control to construct
   * @param allowTabs set to <code>true</code> to allow \t characters to be
   *          inserted.
   * @return the new textbox
   */
  public static Text createMultilineTextbox(Composite parent, int style, final boolean allowTabs) {
    Text text = new Text(parent, style | SWT.MULTI);
    text.addTraverseListener(new TraverseListener() {
      public void keyTraversed(TraverseEvent e) {
        switch (e.detail) {
          case SWT.TRAVERSE_TAB_NEXT:
            e.doit = !allowTabs;
        }
      }
    });
    return text;
  }

  public static void delay(long waitTimeMillis) {
    Display display = Display.getCurrent();

    // If this is the UI thread, then process input.
    if (display != null) {

      /*
       * We set up a timer on the UI thread that fires after the desired wait
       * time. We do this because we want to make sure that the UI thread wakes
       * up from a display.sleep() call. We set a flag in the runnable so that
       * we can terminate the wait loop.
       */
      final boolean[] hasDeadlineTimerFiredPtr = {false};

      display.timerExec((int) waitTimeMillis, new Runnable() {
        public void run() {

          /*
           * We don't have to worry about putting a lock around the update/read
           * of this variable. It is only accessed by the UI thread, and there
           * is only one UI thread.
           */
          hasDeadlineTimerFiredPtr[0] = true;
        }
      });

      while (!hasDeadlineTimerFiredPtr[0]) {

        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }

      display.update();
    } else {
      try {
        // Otherwise, perform a simple sleep.
        Thread.sleep(waitTimeMillis);
      } catch (InterruptedException e) {
        // Ignored
      }
    }
  }

  /**
   * Retrieves a shell. The active shell is preferred, but failing that, it
   * returns any shell (or null). This method does not have to be called from
   * the UI thread.
   */
  public static Shell getShell() {
    final Shell[] shell = new Shell[1];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        shell[0] = Display.getDefault().getActiveShell();
        if (shell[0] != null) {
          return;
        }

        Shell[] shells = Display.getDefault().getShells();
        if (shells.length > 0) {
          shell[0] = shells[0];
        }
      }
    });

    return shell[0];
  }

  /**
   * Recursively sets enabled on this control and all of its descendants. This
   * is necessary on Windows, since just disabling the parent control will
   * result in the child controls appearing enabled, but not responding to any
   * user interaction.
   */
  public static void setEnabledRecursive(Control control, boolean enabled) {
    control.setEnabled(enabled);
    if (control instanceof Composite) {
      Composite composite = (Composite) control;
      for (Control child : composite.getChildren()) {
        setEnabledRecursive(child, enabled);
      }
    }
  }

  /**
   * @see #setText(Text, String)
   */
  public static void setText(Group control, String contents) {
    if (!control.getText().equals(contents)) {
      control.setText(contents);
    }
  }

  /**
   * Sets the text to the given contents, if it is different from the current
   * contents of the text. This is useful in preventing unnecessary modification
   * callbacks.
   */
  public static void setText(Text control, String contents) {
    if (!control.getText().equals(contents)) {
      control.setText(contents);
    }
  }

  private SWTUtilities() {
    // Not instantiable
  }
}
