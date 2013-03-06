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
package com.google.gwt.eclipse.core.editors.java;

import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.swt.graphics.RGB;

/**
 * Definition of colors used for JSNI syntax highlighting.
 */
public final class JsniColorConstants {

  public static final String JSNI_COMMENT = IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT;

  public static final String JSNI_DEFAULT = IJavaColorConstants.JAVA_DEFAULT;

  public static final RGB JSNI_JAVA_REF = new RGB(0, 128, 128);

  public static final String JSNI_KEYWORD = IJavaColorConstants.JAVA_KEYWORD;

  public static final String JSNI_STRING = IJavaColorConstants.JAVA_STRING;

  public static final RGB WHITE = new RGB(255, 255, 255);

  private JsniColorConstants() {
    // Not instantiable
  }
}
