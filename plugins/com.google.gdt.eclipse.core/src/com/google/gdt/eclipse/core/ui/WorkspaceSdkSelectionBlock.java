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

import com.google.gdt.eclipse.core.sdk.Sdk;

import org.eclipse.swt.widgets.Composite;

/**
 * An Sdk selection block whose default is relative to the workspace.
 * 
 * @param <T> type of sdk
 */
public abstract class WorkspaceSdkSelectionBlock<T extends Sdk> extends
    SdkSelectionBlock<T> {
  public WorkspaceSdkSelectionBlock(Composite parent, int style) {
    super(parent, style);
    updateSdkBlockControls();
  }
}
