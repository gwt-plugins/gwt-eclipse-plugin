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
package com.google.gwt.eclipse.core.clientbundle;

import com.google.gdt.eclipse.core.ContentDescriberUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.ITextContentDescriber;
import org.eclipse.wst.css.core.internal.contenttype.ContentDescriberForCSS;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Describes <code>.css</code> files that can contain CssResource-specific
 * extensions, such as @if.
 * <p>
 * This first delegates to the regular CSS content describer since if it cannot
 * recognize the CSS file, neither can we (CssResource extensions follow CSS
 * syntax, so the regular CSS describer should not fail when it sees them.)
 */
@SuppressWarnings("restriction")
public class CssResourceContentDescriber implements ITextContentDescriber {

  private final ContentDescriberForCSS regularCssContentDescriber = new ContentDescriberForCSS();
  
  public int describe(InputStream contents, IContentDescription description)
      throws IOException {

    if (regularCssContentDescriber.describe(contents, description) == INVALID) {
      return INVALID;
    }

    return describe(ContentDescriberUtilities.resolveFileFromInputStream(contents));
  }

  public int describe(Reader contents, IContentDescription description)
      throws IOException {

    if (regularCssContentDescriber.describe(contents, description) == INVALID) {
      return INVALID;
    }

    return describe(ContentDescriberUtilities.resolveFileFromReader(contents));
  }

  public QualifiedName[] getSupportedOptions() {
    return regularCssContentDescriber.getSupportedOptions();
  }
  
  private int describe(IFile file) {
    return file != null && GWTNature.isGWTProject(file.getProject()) ? VALID
        : INDETERMINATE;
  }
}
