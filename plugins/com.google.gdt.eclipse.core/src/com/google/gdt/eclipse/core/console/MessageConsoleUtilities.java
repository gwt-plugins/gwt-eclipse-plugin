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
package com.google.gdt.eclipse.core.console;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

/**
 * Helper methods for dealing with {@link CustomMessageConsole}s.
 */
public class MessageConsoleUtilities {

  /**
   * Returns a {@link CustomMessageConsole} with the given
   * <code>consoleName</code>. If no console by that name exists then one is
   * created. The returned console is cleared; callers of this method can decide
   * when to activate it.
   * 
   * @param consoleName name of the console
   * @param imageDescriptor image descriptor to use
   * @return {@link CustomMessageConsole} with the given
   *         <code>consoleName</code>
   */
  public static CustomMessageConsole getMessageConsole(String consoleName,
      ImageDescriptor imageDescriptor) {
    CustomMessageConsole messageConsole = null;
    IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
    for (IConsole console : consoleManager.getConsoles()) {
      if (console.getName().equals(consoleName)
          && console instanceof CustomMessageConsole) {
        messageConsole = (CustomMessageConsole) console;
        break;
      }
    }

    if (messageConsole == null) {
      messageConsole = new CustomMessageConsole(consoleName, imageDescriptor);
      consoleManager.addConsoles(new IConsole[] {messageConsole});
    } else {
      messageConsole.clearConsole();
    }

    return messageConsole;
  }
}
