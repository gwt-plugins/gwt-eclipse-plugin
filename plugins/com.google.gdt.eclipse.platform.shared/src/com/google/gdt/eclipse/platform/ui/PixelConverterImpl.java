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
package com.google.gdt.eclipse.platform.ui;

import com.google.gdt.eclipse.platform.shared.ui.IPixelConverter;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;

/**
 * Implementation of the generic {@link IPixelConverter} interface.
 */
public class PixelConverterImpl implements IPixelConverter {

  private PixelConverter pixelConverter;

  public PixelConverterImpl(Control control) {
    pixelConverter = new PixelConverter(control);
  }

  public PixelConverterImpl(Font font) {
    pixelConverter = new PixelConverter(font);
  }

  public int convertHeightInCharsToPixels(int chars) {
    return pixelConverter.convertHeightInCharsToPixels(chars);
  }

  public int convertWidthInCharsToPixels(int chars) {
    return pixelConverter.convertWidthInCharsToPixels(chars);
  }
}
