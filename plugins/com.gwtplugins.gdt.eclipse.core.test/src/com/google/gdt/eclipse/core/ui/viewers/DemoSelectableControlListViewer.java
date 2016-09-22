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
package com.google.gdt.eclipse.core.ui.viewers;

import com.google.gdt.eclipse.core.ui.controls.SelectableControlList;
import com.google.gdt.eclipse.core.ui.controls.support.ToolkitControl;
import com.google.gdt.eclipse.core.ui.controls.support.WBToolkit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * NOTE: This code is only for demonstration.
 */
public class DemoSelectableControlListViewer {
  private static ColorRegistry _COLOR_REGISTRY;

  private static Color COLOR_RED;
  private static Color COLOR_WHITE;

  public static void main(String[] args) {
    Display display = new Display();
    COLOR_RED = getRegisteredColor("red", 255, 0, 0);
    COLOR_WHITE = getRegisteredColor("white", 255, 255, 255);

    Shell shell = new Shell(display);
    shell.setText("Demo ControlList");
    shell.setBounds(100, 100, 300, 400);
    FillLayout layout = new FillLayout();
    layout.type = SWT.VERTICAL;
    shell.setLayout(layout);
    createContents(shell);
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }

  private static Control createContents(Composite parent) {
    final Composite body = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    body.setLayout(layout);
    body.setFont(parent.getFont());

    Button contributeWizardEntriesButton = new Button(body, SWT.CHECK);
    contributeWizardEntriesButton.setText("Show uninstalled toolkits as new wizard entries");

    Label separator = new Label(body, SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Text text = new Text(body, SWT.BORDER);
    text.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    Label label = new Label(body, SWT.NONE);
    label.setText("Install additional UI framework toolkits:");
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    label.setForeground(COLOR_RED);

    // SampleControlList.createScolledComposite(body);
    final SelectableControlList<ToolkitControl> ctrlList = new SelectableControlList<ToolkitControl>(
        body, SWT.V_SCROLL | SWT.BORDER);
    ctrlList.setBackground(COLOR_WHITE);
    GridDataFactory.fillDefaults().grab(true, true).hint(100, 100).applyTo(
        ctrlList);

    final SelectableControlListViewer<WBToolkit, ToolkitControl> viewer = new SelectableControlListViewer<WBToolkit, ToolkitControl>(
        ctrlList);
    final WBToolkitProvider contentProvider = new WBToolkitProvider(
        createToolkits());
    viewer.setContentProvider(contentProvider);
    final ToolkitControlFactory controlFactory = new ToolkitControlFactory();
    viewer.setControlFactory(controlFactory);
    viewer.update();

    Composite buttonPanel = new Composite(body, SWT.NONE);
    GridLayout layout2 = new GridLayout(2, false);
    layout2.marginHeight = 0;
    layout2.marginWidth = 0;
    buttonPanel.setLayout(layout2);
    buttonPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

    Button addButton = new Button(buttonPanel, SWT.PUSH);
    addButton.setText("Add...");
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        System.out.println("Add!!");
        List<WBToolkit> tks = createToolkits();
        contentProvider.addToolkit(tks.get((int) (tks.size() * Math.random())));
      }
    });

    Button clearButton = new Button(buttonPanel, SWT.PUSH);
    clearButton.setText("Clear");
    clearButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        contentProvider.clear();
      }
    });

    Button printSelectionsButton = new Button(buttonPanel, SWT.PUSH);
    printSelectionsButton.setText("Print Selections");
    printSelectionsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        System.out.println("Show.");
      }
    });

    ProgressBar progressBar = new ProgressBar(body, SWT.NONE);
    progressBar.setMaximum(100);
    progressBar.setSelection(35);
    progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    progressBar.setVisible(false);

    Dialog.applyDialogFont(body);

    body.layout(true);

    return body;
  }

  private static List<WBToolkit> createToolkits() {
    WBToolkit tk1 = new WBToolkit();
    tk1.setName("Name");
    tk1.setDescription("This is a really detailed description");
    tk1.setProviderDescription("Forge");

    WBToolkit tk2 = new WBToolkit();
    tk2.setName("Name2");
    tk2.setDescription("This is a really detailed description2");
    tk2.setProviderDescription("Forge");

    WBToolkit tk3 = new WBToolkit();
    tk3.setName("Name3");
    tk3.setDescription("This is a really detailed description3");
    tk3.setProviderDescription("Forge");

    WBToolkit tk4 = new WBToolkit();
    tk4.setName("Name4");
    tk4.setDescription("This is a really detailed description4");
    tk4.setProviderDescription("Forge");

    WBToolkit tk5 = new WBToolkit();
    tk5.setName("Name5");
    tk5.setDescription("This is a really detailed description5");
    tk5.setProviderDescription("Forge");

    WBToolkit tk6 = new WBToolkit();
    tk6.setName("Name6");
    tk6.setDescription("This is a really detailed description6. This is a really detailed description6. This is a really detailed description6. This is a really detailed description6.");
    tk6.setProviderDescription("Forge");

    WBToolkit tk7 = new WBToolkit();
    tk7.setName("Name7");
    tk7.setDescription("This is a really detailed description7");
    tk7.setProviderDescription("Forge");

    WBToolkit tk8 = new WBToolkit();
    tk8.setName("Name8");
    tk8.setDescription("This is a really detailed description8");
    tk8.setProviderDescription("Forge");

    WBToolkit tk9 = new WBToolkit();
    tk9.setName("Name9");
    tk9.setDescription("This is a really detailed description9");
    tk9.setProviderDescription("Forge");

    List<WBToolkit> toolkits = Arrays.asList(new WBToolkit[] {
        tk1, tk2, tk3, tk4, tk5, tk6, tk7, tk8, tk9});
    return toolkits;
  }

  private static Color getRegisteredColor(String name, int r, int g, int b) {
    if (_COLOR_REGISTRY == null) {
      _COLOR_REGISTRY = JFaceResources.getColorRegistry();
    }
    if (!_COLOR_REGISTRY.hasValueFor(name)) {
      _COLOR_REGISTRY.put(name, new RGB(r, g, b));
    }
    return _COLOR_REGISTRY.get(name);
  }
}

/**
 * TODO
 */
class ToolkitControlFactory extends BaseSelectableControlListControlFactory
    implements SelectableControlListControlFactory<WBToolkit, ToolkitControl> {

  public ToolkitControl createControl(Composite parent, WBToolkit element) {
    ToolkitControl toolkitControl = new ToolkitControl(parent, element);
    addSeparator(parent);
    return toolkitControl;
  }
}

/**
 * TODO
 */
class WBToolkitProvider extends BaseChangeListener<ChangeListener>
    implements SelectableControlListContentProvider<WBToolkit> {

  private WBToolkit[] elements;

  public WBToolkitProvider(List<WBToolkit> listOfElements) {
    elements = listOfElements.toArray(new WBToolkit[listOfElements.size()]);
  }

  public void addToolkit(WBToolkit wbToolkit) {
    List<WBToolkit> listOfElements = new ArrayList<WBToolkit>(
        elements.length + 1);
    listOfElements.addAll((Collection<? extends WBToolkit>) Arrays.asList(elements));
    listOfElements.add(wbToolkit);
    this.elements = listOfElements.toArray(new WBToolkit[listOfElements.size()]);
    fireChangeEvent();
  }

  public void clear() {
    elements = new WBToolkit[0];
    fireChangeEvent();
  }

  public WBToolkit[] getElements() {
    return elements;
  }

}
