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
package com.google.gdt.eclipse.core.ui.controls.support;

import com.google.gdt.eclipse.core.ui.controls.SelectableControlList;
import com.google.gdt.eclipse.core.ui.controls.SelectableControlListUpdateCallback;

import org.eclipse.swt.widgets.Composite;

import java.util.List;

/**
 * NOTE: This code is only for demonstration.
 */
public class SampleControlList extends SelectableControlList<ToolkitControl> {

  public SampleControlList(Composite parent, int style) {
    super(parent, style);
  }

  public void addToolkit(final WBToolkit toolkit) {
    updateContents(new SelectableControlListUpdateCallback() {
      public void execute(Composite scrolledContents) {
        final ToolkitControl toolkitControl = new ToolkitControl(
            scrolledContents, toolkit);
        registerItem(toolkitControl);
        // addSeparator(scrolledContents);
      }
    });
  }

  /**
   * Isolate creation of items
   * 
   * @param parent
   */
  public void addToolkits(final List<WBToolkit> toolkits) {
    updateContents(new SelectableControlListUpdateCallback() {
      public void execute(Composite scrolledContents) {
        for (WBToolkit toolkit : toolkits) {
          final ToolkitControl toolkitControl = new ToolkitControl(
              scrolledContents, toolkit);
          registerItem(toolkitControl);
          addSeparator(scrolledContents);
        }
      }
    });
  }
}
