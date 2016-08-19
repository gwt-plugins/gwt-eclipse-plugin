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
package com.google.gwt.eclipse.oophm.breadcrumbs;

import org.eclipse.swt.graphics.RGB;

/**
 * NOTE: This code was derived from org.eclipse.ui.forms.FormColors in Eclipse
 * 3.4.
 */
final class ColorBlender {
  /**
   * Blends c1 and c2 based in the provided ratio.
   * 
   * @param c1 first color
   * @param c2 second color
   * @param ratio percentage of the first color in the blend (0-100)
   * @return the RGB value of the blended color
   * @since 3.1
   */
  public static RGB blend(RGB c1, RGB c2, int ratio) {
    int r = blend(c1.red, c2.red, ratio);
    int g = blend(c1.green, c2.green, ratio);
    int b = blend(c1.blue, c2.blue, ratio);
    return new RGB(r, g, b);
  }

  /**
   * Blends two primary color components based on the provided ratio.
   * 
   * @param v1 first component (0-255)
   * @param v2 second component (0-255)
   * @param ratio percentage of the first component in the blend
   * @return the blended value (0-255)
   */
  private static int blend(int v1, int v2, int ratio) {
    int b = (ratio * v1 + (100 - ratio) * v2) / 100;
    return Math.min(255, b);
  }
}
