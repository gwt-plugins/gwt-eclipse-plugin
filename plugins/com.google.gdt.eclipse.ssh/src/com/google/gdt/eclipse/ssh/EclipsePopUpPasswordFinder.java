/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.ssh;

import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Will make an eclipse plugin style specific popup to retrieve a password from
 * the user
 * 
 */
public class EclipsePopUpPasswordFinder extends InputDialog implements PasswordFinder {

  /**
   * Create a dialog with no title and an "enter password" prompt
   */
  public EclipsePopUpPasswordFinder() {
    super(null, "", "Enter Password", "", null);
  }

  /**
   * Create a dialog with a custom title and prompt
   * 
   * @param title
   * @param prompt
   */
  public EclipsePopUpPasswordFinder(String title, String prompt) {
    super(null, title, prompt, "", null);
  }

  public char[] reqPassword(Resource<?> resource) {
    final AtomicBoolean isOK = new AtomicBoolean(false);
    // display a modal window
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        isOK.set(EclipsePopUpPasswordFinder.this.open() == Window.OK);
      }
    });

    if (isOK.get()) {
      return getValue().toCharArray();
    }
    return null;

  }

  public boolean shouldRetry(Resource<?> arg0) {
    // dont retry on false;
    return false;
  }

  protected int getInputTextStyle() {
    // override the style to be "password" style
    return SWT.PASSWORD | SWT.BORDER | SWT.SINGLE;
  }

}
