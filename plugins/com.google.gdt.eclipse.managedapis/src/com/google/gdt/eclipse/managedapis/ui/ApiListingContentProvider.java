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

import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiListing;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.List;

/**
 * Wraps a ManagedApiListing for use in a StructuredViewer.
 */
public class ApiListingContentProvider implements IStructuredContentProvider {

  private ManagedApiListing listing;

  public void dispose() {
    // do any cleanup
  }

  /**
   * Returns the elements to display in the viewer.
   */
  public Object[] getElements(Object inputElementIgnore) {
    List<ManagedApiEntry> entries = listing.getEntries();
    return entries.toArray(new ManagedApiEntry[entries.size()]);
  }

  /**
   * Update the listing.
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof ManagedApiListing) {
      listing = (ManagedApiListing) newInput;
    }
  }
}
