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
package com.google.gdt.eclipse.managedapis;

import com.google.gdt.eclipse.core.ui.controls.SelectableControlList;
import com.google.gdt.eclipse.core.ui.controls.SelectionChangeListener;
import com.google.gdt.eclipse.core.ui.viewers.BaseChangeListener;
import com.google.gdt.eclipse.core.ui.viewers.BaseSelectableControlListControlFactory;
import com.google.gdt.eclipse.core.ui.viewers.ChangeListener;
import com.google.gdt.eclipse.core.ui.viewers.SelectableControlListContentProvider;
import com.google.gdt.eclipse.core.ui.viewers.SelectableControlListControlFactory;
import com.google.gdt.eclipse.core.ui.viewers.SelectableControlListViewer;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiListing;
import com.google.gdt.eclipse.managedapis.directory.StructuredApiCollection;
import com.google.gdt.eclipse.managedapis.impl.LatestVersionOnlyStructuredApiCollection;
import com.google.gdt.eclipse.managedapis.ui.ApiListingItem;
import com.google.gdt.googleapi.core.ApiDirectoryListing;
import com.google.gdt.googleapi.core.ApiDirectoryListingJsonCodec;
import com.google.gson.JsonParseException;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

/**
 * NOTE: This code is only for demonstration.
 */
public class DemoListingViewer {
  private static ColorRegistry _COLOR_REGISTRY;
  private static Color COLOR_WHITE;

  public static void main(String[] args) throws JsonParseException,
      FileNotFoundException {
    Display display = new Display();
    Shell shell = new Shell(display);
    shell.setText("Demo ListingView");
    shell.setBounds(100, 100, 500, 400);
    shell.setLayout(new FillLayout());

    COLOR_WHITE = getRegisteredColor("white", 255, 255, 255);

    ApiDirectoryListingJsonCodec codec = new ApiDirectoryListingJsonCodec();
    String sampleData = readFileIntoString(new File(
        "src/com/google/gdt/eclipse/managedapis/SampleAPIList.json"));
    ApiDirectoryListing dirListing = codec.toApiDirectoryListing(sampleData,
        null);
    StructuredApiCollection listingResults = new LatestVersionOnlyStructuredApiCollection();
    listingResults.addAll(dirListing.getItems());

    final SelectableControlList<ApiListingItem> ctrlList = new SelectableControlList<ApiListingItem>(
        shell, SWT.V_SCROLL | SWT.BORDER);
    ctrlList.setBackground(COLOR_WHITE);

    final SelectableControlListViewer<ManagedApiEntry, ApiListingItem> viewer = new SelectableControlListViewer<ManagedApiEntry, ApiListingItem>(
        ctrlList);
    final ManagedApiEntryContentProvider contentProvider = new ManagedApiEntryContentProvider(
        listingResults.getListing());
    viewer.setContentProvider(contentProvider);
    ManagedApiEntryControlFactory controlFactory = new ManagedApiEntryControlFactory(
        new LocalResources(shell.getDisplay()));
    viewer.setControlFactory(controlFactory);
    viewer.addSelectionChangeListener(new SelectionChangeListener<ManagedApiEntry>() {

      public void selectionChanged(List<ManagedApiEntry> selection) {
        System.out.println("Selected: ");
        for (ManagedApiEntry entry : selection) {
          System.out.println(entry.getDisplayName());
        }
      }
    });
    viewer.update();

    shell.open();

    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }

  public static String readFileIntoString(File source)
      throws FileNotFoundException {
    assert source.exists() : "File not found";
    assert source.canRead() : "Can not read file";
    System.out.println(source.getAbsolutePath());
    return new Scanner(source).useDelimiter("\\Z").next();
  }

  public static void updateSize(final ScrolledComposite sc1, final Composite c1) {
    c1.layout();
    Point size = c1.computeSize(sc1.getClientArea().width, SWT.DEFAULT, true);
    c1.setSize(size);
    sc1.setMinSize(size);
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

class ManagedApiEntryContentProvider extends BaseChangeListener<ChangeListener>
    implements SelectableControlListContentProvider<ManagedApiEntry> {
  ManagedApiEntry[] elements;

  public ManagedApiEntryContentProvider(ManagedApiListing managedApiListing) {
    if (managedApiListing != null) {
      List<ManagedApiEntry> listOfEntries = managedApiListing.getEntries();
      elements = listOfEntries.toArray(new ManagedApiEntry[listOfEntries.size()]);
    } else {
      elements = new ManagedApiEntry[0];
    }
  }

  public ManagedApiEntry[] getElements() {
    return elements;
  }
}

class ManagedApiEntryControlFactory extends
    BaseSelectableControlListControlFactory implements
    SelectableControlListControlFactory<ManagedApiEntry, ApiListingItem> {

  private Resources resources;

  public ManagedApiEntryControlFactory(Resources resources) {
    this.resources = resources;
  }

  public ApiListingItem createControl(Composite parent, ManagedApiEntry element) {
    ApiListingItem control = new ApiListingItem(parent, element, resources);
    addSeparator(parent);
    return control;
  }

}
