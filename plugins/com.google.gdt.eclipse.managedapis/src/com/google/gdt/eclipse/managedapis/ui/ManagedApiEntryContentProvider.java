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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.ui.viewers.BaseChangeListener;
import com.google.gdt.eclipse.core.ui.viewers.ChangeListener;
import com.google.gdt.eclipse.core.ui.viewers.SelectableControlListContentProvider;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiListing;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * ManagedApiEntryContentProvider serves ManagedApiEntry objects to the viewer.
 */
public class ManagedApiEntryContentProvider extends
    BaseChangeListener<ChangeListener> implements
    SelectableControlListContentProvider<ManagedApiEntry> {

  private Comparator<ManagedApiEntry> comparator;
  private ManagedApiEntry[] elements = new ManagedApiEntry[0];

  public ManagedApiEntryContentProvider() {
    super();
  }

  public ManagedApiEntryContentProvider(Comparator<ManagedApiEntry> comparator) {
    super();
    this.comparator = comparator;
  }

  public ManagedApiEntry[] getElements() {
    return elements;
  }

  public void setComparator(Comparator<ManagedApiEntry> comparator) {
    Comparator<ManagedApiEntry> oldComparator = this.comparator;
    this.comparator = comparator;
    if (oldComparator != comparator) {
      fireChangeEvent();
    }
  }

  public void update(ManagedApiListing listing) {
    List<ManagedApiEntry> entryList = listing.getEntries();
    elements = entryList.toArray(new ManagedApiEntry[entryList.size()]);
    if (comparator != null) {
      Arrays.sort(elements, comparator);
    }
    fireChangeEvent();
  }
}
