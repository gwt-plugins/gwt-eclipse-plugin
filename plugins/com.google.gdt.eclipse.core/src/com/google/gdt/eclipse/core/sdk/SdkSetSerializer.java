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
package com.google.gdt.eclipse.core.sdk;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * 
 */
public class SdkSetSerializer {
  /**
   * 
   */
  @SuppressWarnings("serial")
  public static class SdkSerializationException extends Exception {
    public SdkSerializationException(String message) {
      super(message);
    }

    public SdkSerializationException(Throwable cause) {
      super(cause);
    }
  }

  private static final String SDK_CONFIGURATION_XML = "sdkSetXml";

  public static <T extends Sdk> void deserialize(
      IEclipsePreferences preferences, SdkSet<T> sdkSet,
      SdkFactory<T> sdkFactory) throws SdkSerializationException {
    byte[] sdkConfigurationXmlBytes = preferences.getByteArray(
        SDK_CONFIGURATION_XML, new byte[0]);
    if (sdkConfigurationXmlBytes.length > 0) {
      // initialize the configurations from the xml
      ByteArrayInputStream byteInputStream = new ByteArrayInputStream(
          sdkConfigurationXmlBytes);
      parseSdkConfigurations(byteInputStream, sdkSet, sdkFactory);
    }
  }

  public static <T extends Sdk> void serialize(IEclipsePreferences preferences,
      SdkSet<T> sdkSet) throws SdkSerializationException {

    try {
      byte[] buffer = sdkSet.toXml().getBytes("UTF-8");
      preferences.putByteArray(SDK_CONFIGURATION_XML, buffer);
      preferences.flush();
    } catch (UnsupportedEncodingException e) {
      throw new SdkSerializationException(e);
    } catch (BackingStoreException e) {
      throw new SdkSerializationException(e);
    }
  }

  private static <T extends Sdk> void parseSdkConfigurations(
      InputStream inputStream, SdkSet<T> sdkSet, SdkFactory<T> sdkFactory)
      throws SdkSerializationException {
    // Wrapper the stream for efficient parsing
    InputStream stream = new BufferedInputStream(inputStream);

    // Do the parsing and obtain the top-level node
    Element config = null;
    try {
      DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      parser.setErrorHandler(new DefaultHandler());
      config = parser.parse(new InputSource(stream)).getDocumentElement();
    } catch (ParserConfigurationException e) {
      throw new SdkSerializationException(e);
    } catch (SAXException e) {
      throw new SdkSerializationException(e);
    } catch (IOException e) {
      throw new SdkSerializationException(e);
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
        throw new SdkSerializationException(e);
      }
    }

    // If the top-level node wasn't what we expected, bail out
    if (!config.getNodeName().equalsIgnoreCase("sdks")) {
      throw new SdkSerializationException("Invalid SdkSet XML");
    }

    String defaultSdkName = config.getAttribute("defaultSdk");
    T defaultSdk = null;

    // Traverse the parsed structure and populate the VMType to VM Map
    NodeList list = config.getChildNodes();
    int length = list.getLength();
    for (int i = 0; i < length; ++i) {
      Node node = list.item(i);
      short type = node.getNodeType();
      if (type == Node.ELEMENT_NODE) {
        Element sdkElement = (Element) node;
        if (sdkElement.getNodeName().equalsIgnoreCase("sdk")) { //$NON-NLS-1$

          String name = sdkElement.getAttribute("name");
          IPath location = new Path(sdkElement.getAttribute("location"));

          T newSdk = sdkFactory.newInstance(name, location);
          sdkSet.add(newSdk);

          if (name.equals(defaultSdkName)) {
            defaultSdk = newSdk;
          }
        }
      }
    }

    sdkSet.setDefault(defaultSdk);
  }
}
