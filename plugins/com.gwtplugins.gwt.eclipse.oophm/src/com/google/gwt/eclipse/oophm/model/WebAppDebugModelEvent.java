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
package com.google.gwt.eclipse.oophm.model;

/**
 * An event that is fired when changes to the {@link WebAppDebugModel} occur.
 * 
 * @param <T> the type of entity that the event applies to
 */
public class WebAppDebugModelEvent<T extends IModelNode> {
  private final T element;

  /**
   * Create a new instance.
   * 
   * @param element the element that changed. The type of entity varies depending
   *          on which {@link IWebAppDebugModelListener} method was invoked.
   */
  public WebAppDebugModelEvent(T element) {
    this.element = element;
  }

  /**
   * Gets the element that changed.
   */
  public T getElement() {
    return element;
  }
}
