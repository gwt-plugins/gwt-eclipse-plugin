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

import java.util.ArrayList;
import java.util.List;

/**
 * Provide utility methods for ChangeListeners.
 * 
 * @param <T> Parameterize the type of ChangeListener.
 */
public class BaseChangeListener<T extends ChangeListener> {
  private List<T> listeners = new ArrayList<T>();

  public void addChangeListener(T listener) {
    listeners.add(listener);
  }

  public void fireChangeEvent() {
    for (T listener : listeners) {
      listener.onChange();
    }
  }
}
