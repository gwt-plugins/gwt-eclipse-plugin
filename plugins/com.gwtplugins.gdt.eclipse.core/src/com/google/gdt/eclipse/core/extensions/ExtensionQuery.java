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

import com.google.common.base.Preconditions;
import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns data for a given extension point and attribute name.
 *
 * @param <T> The type for the returned data for the given extension point and
 *          attribute
 */
public class ExtensionQuery<T> {

  /**
   * Structure for holding the data retrieved from a given extension point's
   * attribute.
   *
   * @param <T> The type for the data
   */
  public static class Data<T> {

    private final T extensionPointData;
    private final IConfigurationElement configurationElement;

    /**
     * Create a new instance.
     *
     * @param extensionPointData The data for the extension point
     * @param configurationElement A reference to the extension point
     */
    public Data(T extensionPointData, IConfigurationElement configurationElement) {
      this.extensionPointData = extensionPointData;
      this.configurationElement = configurationElement;
    }

    /**
     * Return the configuration element for the extension point.
     */
    public IConfigurationElement getConfigurationElement() {
      return configurationElement;
    }

    /**
     * Return the data.
     */
    public T getExtensionPointData() {
      return extensionPointData;
    }
  }

  /**
   * Interface for retrieving data from an extension point. The way in which
   * data is retrieved depends on the type of the data.
   *
   * @param <T> The type of data
   */
  protected static interface DataRetriever<T> {

    /**
     * Return the data for the given configuration element (of an extension
     * point) and an attribute name, or <code>null</code> if the data cannot be
     * retrieved/does not exist.
     */
    T getData(IConfigurationElement configurationElement, String attrName);
  }

  protected final String pluginId;
  protected final String extensionPointName;
  protected final String attributeName;

  /**
   * Create a new instance.
   *
   * @param pluginId The id of the plugin providing the extension point.
   * @param extensionPointName the name of the extension point
   * @param attributeName the name of the attribute for which data is to be
   *          retrieved
   */
  public ExtensionQuery(String pluginId, String extensionPointName,
      String attributeName) {
    this.pluginId = extensionPointName.contains(".") ? null : pluginId;
    this.extensionPointName = extensionPointName;
    this.attributeName = attributeName;
  }

  /**
   * Create a new instance.
   *
   * @param pluginId The id of the plugin providing the extension point.
   * @param extensionPointName the name of the extension point
   * @param attributeName the name of the attribute for which data is to be
   *          retrieved
   */
  public ExtensionQuery(String extensionPointName,
      String attributeName) {
    Preconditions.checkArgument(extensionPointName.contains("."), "expected fully-qualified extension point");
    this.pluginId = null;
    this.extensionPointName = extensionPointName;
    this.attributeName = attributeName;
  }
  /**
   * Return the data provided by all available plugins for the given extension
   * point and attribute name.
   */
  @SuppressWarnings("unchecked")
  public List<Data<T>> getData() {
    return getDataImpl(new DataRetriever<T>() {
      @Override
      public T getData(IConfigurationElement configurationElement,
          String attrName) {
        try {
          return (T) configurationElement.createExecutableExtension(attrName);
        } catch (CoreException ce) {
          CorePluginLog.logError(ce);
        }
        return null;
      }
    });
  }

  protected List<Data<T>> getDataImpl(DataRetriever<T> c) {
    List<Data<T>> classes = new ArrayList<Data<T>>();

    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = pluginId == null ? extensionRegistry.getExtensionPoint(extensionPointName) :
      extensionRegistry.getExtensionPoint(pluginId, extensionPointName);
    // try old names: these need to be rewritten
    if(extensionPoint == null && pluginId != null && pluginId.startsWith("com.gwtplugins")) {
      String rewritten = pluginId.replace("com.gwtplugins", "com.google");
      extensionPoint = extensionRegistry.getExtensionPoint(rewritten, extensionPointName);
      if(extensionPoint != null) {
        System.err.println(">>>> OLD EXTENSION POINT REFERENCE: " + pluginId);
        new Throwable(">>>> OLD EXTENSION POINT REFERENCE: " + pluginId + " : " + extensionPointName).printStackTrace();
      }
    }
    if (extensionPoint != null) {
      IExtension[] extensions = extensionPoint.getExtensions();
      for (IExtension extension : extensions) {
        IConfigurationElement[] configurationElements = extension.getConfigurationElements();
        for (IConfigurationElement configurationElement : configurationElements) {
          T value = c.getData(configurationElement, attributeName);
          if (value != null) {
            classes.add(new Data<T>(value, configurationElement));
          }
        }
      }
    }
    return classes;
  }
}
