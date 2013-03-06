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
package com.google.gwt.eclipse.core.uibinder;

import com.google.gdt.eclipse.core.ContentDescriberUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.XMLContentDescriber;
import org.eclipse.core.runtime.content.XMLRootElementContentDescriber;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/*
 * Interesting notes from a previous content describer: (1) If VALID is
 * returned, the content type will be assigned to files that are not even in a
 * GWT natured project. (2) There is a side-effect to returning INVALID as well.
 * When a content type's describer deems that a file is INVALID for the
 * particular content type, then it is not even considered to be a viable
 * alternative for the file. The end result of this is that the "Open With.."
 * submenu under the context menu for a file will not contain editors that use
 * the content type described by this class.
 */
/*
 * We check for both root element name and filename. There's the
 * XMLRootElementContentDescriber for root element name checking, but it is
 * final so we cannot inherit from there. Instead, we use composition and
 * delegate to it first.
 */
/**
 * Describes whether the given source is a UiBinder template XML file by
 * checking either a filename suffix of <code>.ui.xml</code> or a root
 * <code>UiBinder</code> element.
 * <p>
 * See {@link XMLRootElementContentDescriber} for executable extension
 * information.
 */
@SuppressWarnings("deprecation")
public class UiBinderXmlContentDescriber extends XMLContentDescriber implements
    IExecutableExtension {

  private final XMLRootElementContentDescriber rootElementContentDescriber;

  public UiBinderXmlContentDescriber() {
    rootElementContentDescriber = new XMLRootElementContentDescriber();
  }

  @Override
  public int describe(InputStream input, IContentDescription description)
      throws IOException {

    if (super.describe(input, description) == INVALID) {
      return INVALID;
    }

    if (rootElementContentDescriber.describe(input, description) == VALID) {
      // The root element is a UiBinder element
      return VALID;
    }

    return describe(ContentDescriberUtilities.resolveFileFromInputStream(input));
  }

  @Override
  public int describe(Reader input, IContentDescription description)
      throws IOException {

    if (super.describe(input, description) == INVALID) {
      return INVALID;
    }

    if (rootElementContentDescriber.describe(input, description) == VALID) {
      // The root element is a UiBinder element
      return VALID;
    }

    return describe(ContentDescriberUtilities.resolveFileFromReader(input));
  }

  /*
   * Declared in IExecutableExtension. Delegate to the
   * XMLRootElementContentDescriber instance since its the reason we implement
   * the IExecutableExtension interface.
   */
  public void setInitializationData(IConfigurationElement config,
      String propertyName, Object data) throws CoreException {
    rootElementContentDescriber.setInitializationData(config, propertyName,
        data);
  }

  private int describe(IFile file) {
    if (file == null) {
      // Cannot be determined
      return INDETERMINATE;
    }

    return file.getName().toLowerCase().endsWith(
        UiBinderConstants.UI_BINDER_XML_EXTENSION) ? VALID : INVALID;
  }
}
