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
package com.google.gdt.eclipse.core.extensions;

import org.eclipse.core.runtime.IConfigurationElement;

import java.util.List;

/**
 * Returns string attribute values for a given extension point and attribute
 * name.
 */
public class ExtensionQueryStringAttr extends ExtensionQuery<String> {

  /**
   * Create a new instance.
   * 
   * @param pluginId The id of the plugin providing the extension point.
   * @param extensionPointName the name of the extension point
   * @param attributeName the name of the attribute for which string data is to
   *          be retrieved
   */
  public ExtensionQueryStringAttr(String pluginId, String extensionPointName,
      String attributeName) {
    super(pluginId, extensionPointName, attributeName);
  }

  @Override
  public List<Data<String>> getData() {
    return getDataImpl(new DataRetriever<String>() {
      public String getData(IConfigurationElement configurationElement,
          String attrName) {
        return configurationElement.getAttribute(attrName);
      }
    });
  }
}
