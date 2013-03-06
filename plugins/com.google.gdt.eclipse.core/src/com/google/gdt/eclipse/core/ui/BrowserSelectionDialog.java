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
package com.google.gdt.eclipse.core.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

import java.util.List;

/**
 * Selects a browser from a list.
 */
@SuppressWarnings("restriction")
public class BrowserSelectionDialog extends ElementListSelectionDialog {

  private static class BrowserLabelProvider implements ILabelProvider {

    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
    }

    public Image getImage(Object element) {
      return null;
    }

    public String getText(Object element) {
      IBrowserDescriptor browser = (IBrowserDescriptor) element;
      return browser.getName();
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }
  }

  private static final int ADD_BROWSER_ID = IDialogConstants.CLIENT_ID + 1;

  /**
   * Opens the dialog for the user to choose a browser.
   * 
   * @param initialSelection optional
   * @return the selected browser, or <code>null</code> if the dialog was
   *         canceled
   */
  public static IBrowserDescriptor chooseBrowser(Shell shell,
      IBrowserDescriptor initialSelection) {

    ElementListSelectionDialog dialog = new BrowserSelectionDialog(shell,
        initialSelection);

    if (dialog.open() == Window.OK) {
      return (IBrowserDescriptor) dialog.getFirstResult();
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  private static List<IBrowserDescriptor> getBrowsers() {
    return BrowserManager.getInstance().getWebBrowsers();
  }

  private BrowserSelectionDialog(Shell parent,
      IBrowserDescriptor initialSelection) {
    super(parent, new BrowserLabelProvider());
    setTitle("Browser Selection");
    setMessage("Choose a browser:");
    List<IBrowserDescriptor> browers = getBrowsers();
    setElements(browers.toArray(new IBrowserDescriptor[browers.size()]));
    setHelpAvailable(false);
    if (initialSelection != null) {
      setInitialSelections(new Object[] {initialSelection});
    }
  }

  @Override
  protected void buttonPressed(int buttonId) {
    super.buttonPressed(buttonId);
    
    if (buttonId == ADD_BROWSER_ID) {
      new AddBrowserDialog(getShell()).open();
      IBrowserDescriptor[] elements = getBrowsers().toArray(
          new IBrowserDescriptor[0]);
      setElements(elements);
      setListElements(elements);
    }
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, ADD_BROWSER_ID, "Add Browser", false);
    super.createButtonsForButtonBar(parent);
  }
}
