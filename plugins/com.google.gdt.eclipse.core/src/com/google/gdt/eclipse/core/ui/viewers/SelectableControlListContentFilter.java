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

/**
 * Define an interface for filtering content
 * (SelectableControlListContentProvider<T>) in a SelectableControlListViewer.
 * 
 * @param <T> The type of element provided by the content provider.
 */
public interface SelectableControlListContentFilter<T> {

  /**
   * Register a listener with this instance.
   */
  void addChangeListener(ChangeListener listener);

  /**
   * Apply the filter to the user. Use this to process elements with the idea
   * that the output will be flatmapped (e.g. null is OK and does not add).
   * 
   * @param element an input entry
   * @return nullable value -- can be the original element or a modified copy
   */
  T apply(T element);
}
