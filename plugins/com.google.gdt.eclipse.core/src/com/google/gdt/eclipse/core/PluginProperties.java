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
package com.google.gdt.eclipse.core;

import org.eclipse.core.runtime.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Used to read properties from properties files associated with a plugin. The
 * assumption is that the properties file will have the same name as the plugin
 * class, live in the same package, and have the extension
 * <code>.properties</code>.
 * 
 * It is expected that some of the values in the properties file are meant to be
 * replaced at build time, and that such values have <code>@</code> as the
 * starting and ending characters. Therefore, if any property value has such a
 * format, the property is assumed to be unset.
 */
public class PluginProperties {

  private static boolean isPropertyValueSet(String propValue) {
    if (propValue == null) {
      return false;
    }

    if (propValue.startsWith("@") && propValue.endsWith("@")) {
      return false;
    }

    return true;
  }

  private Properties props;

  /**
   * Create an instance by reading in the properties file associated with the
   * plugin class. If there is a failure while reading the file, then fail
   * silently and output an error message.
   * 
   * @param pluginClass the plugin class for which the associated properties
   *          should be loaded
   */
  public PluginProperties(Class<? extends Plugin> pluginClass) {
    String propsPath = pluginClass.getName().replace('.', '/').concat(
        ".properties");

    props = new Properties();

    try {
      InputStream instream = pluginClass.getClassLoader().getResourceAsStream(
          propsPath);

      if (instream != null) {
        props.load(instream);
      } else {
        System.err.println("Unable to read properties from file " + propsPath
            + ". The " + pluginClass.getName()
            + " plugin may not run correctly.");
      }
    } catch (IOException iox) {
      /*
       * TODO: Once we start exposing property values through accessors off of
       * plugin classes, we'll be able to pass a proper logger. Right now, we
       * can't do this, because this code is called from static blocks in the
       * plugin classes.
       */
      System.err.println("Unable to read properties from file " + propsPath
          + ". The " + pluginClass.getName() + " plugin may not run correctly.");
      iox.printStackTrace();
    }
  }

  /**
   * Gets the value associated with <code>propName</code>. If the property
   * has not been set, or if the property's value starts and ends with the
   * <code>@</code> character (see class documentation for explanation), then
   *        the default value is returned.
   * 
   * @param propName the name of the property
   * @param defaultValue the default value to return in the event that the
   *          property has not been set
   * @return the value associated with the property, or the default value if the
   *         property value has not been set
   */
  public String getProperty(String propName, String defaultValue) {
    String propValue = props.getProperty(propName);
    if (isPropertyValueSet(propValue)) {
      return propValue;
    }
    return defaultValue;
  }
}
