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

import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.ui.ApiListingItem;
import com.google.gdt.googleapi.core.ApiDirectoryItem;
import com.google.gdt.googleapi.core.ApiDirectoryListingJsonCodec;
import com.google.gdt.googleapi.core.ApiInfoImpl;
import com.google.gson.JsonParseException;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * NOTE: This code is only for demonstration.
 */
public class DemoListingItem {
  public static void main(String[] args) throws JsonParseException,
      FileNotFoundException {
    Display display = new Display();
    Shell shell = new Shell(display);
    GridLayoutFactory.fillDefaults().applyTo(shell);
    Resources rsrc = new LocalResources(shell.getDisplay());

    ApiDirectoryListingJsonCodec codec = new ApiDirectoryListingJsonCodec();
    String sampleData = readFileIntoString(new File(
        "src/com/google/gdt/eclipse/managedapis/SampleSingleAPI.json"));
    ApiDirectoryItem dirEntry = new ApiDirectoryItem((ApiInfoImpl) codec.toApiInfo(
        sampleData, null));
    ManagedApiEntry entry = new ManagedApiEntry(dirEntry, null);
    ApiListingItem item = new ApiListingItem(shell, entry, rsrc);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(item);
    shell.open();

    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }

  private static String readFileIntoString(File source)
      throws FileNotFoundException {
    assert source.exists() : "File not found";
    assert source.canRead() : "Can not read file";

    System.out.println(source.getAbsolutePath());

    return new Scanner(source).useDelimiter("\\Z").next();
  }
}
