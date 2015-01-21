/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * An extension query with checking for element name. Suitable for extensions having more than one
 * element.
 *
 * @param <T> The type for the returned data for the given extension point and attribute
 */
public class ExtensionQueryWithElement<T> extends ExtensionQuery<T> {

  protected String elementName;

  /**
   * @param pluginId The id of the plugin providing the extension point
   * @param extensionPointName the name of the extension point
   * @param elementName the name of element which attribute data should be retrieved
   * @param attributeName the name of the attribute for which data is to be retrieved
   */
  public ExtensionQueryWithElement(String pluginId, String extensionPointName, String elementName,
      String attributeName) {
    super(pluginId, extensionPointName, attributeName);
    this.elementName = elementName;
  }

  @Override
  protected List<com.google.gdt.eclipse.core.extensions.ExtensionQuery.Data<T>> getDataImpl(
      com.google.gdt.eclipse.core.extensions.ExtensionQuery.DataRetriever<T> c) {
    List<Data<T>> classes = new ArrayList<Data<T>>();

    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(pluginId,
        extensionPointName);
    if (extensionPoint != null) {
      IExtension[] extensions = extensionPoint.getExtensions();
      for (IExtension extension : extensions) {
        IConfigurationElement[] configurationElements = extension.getConfigurationElements();
        for (IConfigurationElement configurationElement : configurationElements) {
          if (configurationElement.getName().equals(elementName)) {
            T value = c.getData(configurationElement, attributeName);
            if (value != null) {
              classes.add(new Data<T>(value, configurationElement));
            }
          }
        }
      }
    }
    return classes;
  }
}
