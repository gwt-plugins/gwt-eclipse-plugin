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
package com.google.gdt.eclipse.platform.shared.ui;

/**
 * Common interface to the services provided by the <code>PixelConverter</code>
 * class.
 */
public interface IPixelConverter {

  /**
   * Returns the number of pixels corresponding to the height of the given
   * number of characters.
   * 
   * @param chars the number of characters
   * @return the number of pixels
   */
  public int convertHeightInCharsToPixels(int chars);

  /**
   * Returns the number of pixels corresponding to the width of the given number
   * of characters.
   * 
   * @param chars the number of characters
   * @return the number of pixels
   */
  public int convertWidthInCharsToPixels(int chars);
}
