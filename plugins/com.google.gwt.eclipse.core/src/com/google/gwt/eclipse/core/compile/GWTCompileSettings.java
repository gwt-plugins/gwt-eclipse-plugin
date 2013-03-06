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
package com.google.gwt.eclipse.core.compile;

import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.GWTLaunchAttributes;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Stores the settings for a GWT compile.
 */
public class GWTCompileSettings {

  private static final String CDATA_END = "]]>";

  private static final String CDATA_START = "<![CDATA[";

  private static final String ENTRY_POINT_MODULES_TAG = "entry-point-module";
  
  private static final String EXTRA_ARGS_TAG = "extra-args";

  private static final String LOG_LEVEL_TAG = "log-level";

  private static final String OUTPUT_STYLE_TAG = "output-style";

  private static final String ROOT_TAG = "gwt-compile-settings";

  private static final String VM_ARGS_TAG = "vm-args";

  public static GWTCompileSettings deserialize(byte[] bytes, IProject project) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    String s = new String(bytes);
    
    ByteArrayInputStream byteInputStream = new ByteArrayInputStream(bytes);
    InputStream stream = new BufferedInputStream(byteInputStream);

    // Do the parsing and obtain the top-level node
    Element config = null;
    try {
      DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      parser.setErrorHandler(new DefaultHandler());
      config = parser.parse(new InputSource(stream)).getDocumentElement();
    } catch (Exception e) {
      GWTPluginLog.logError(e);
      return null;
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
        GWTPluginLog.logError(e);
      }
    }

    // If the top-level node wasn't what we expected, bail out
    if (!config.getNodeName().equalsIgnoreCase(ROOT_TAG)) {
      return null;
    }

    GWTCompileSettings settings = new GWTCompileSettings();

    // Initialize the settings from the XML
    NodeList nodes = config.getChildNodes();
    int length = nodes.getLength();
    for (int i = 0; i < length; ++i) {
      if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) nodes.item(i);
        String nodeName = element.getNodeName();

        if (nodeName.equalsIgnoreCase(LOG_LEVEL_TAG)) {
          settings.setLogLevel(getElementText(element));
          continue;
        }

        if (nodeName.equalsIgnoreCase(OUTPUT_STYLE_TAG)) {
          settings.setOutputStyle(getElementText(element));
          continue;
        }

        if (nodeName.equalsIgnoreCase(EXTRA_ARGS_TAG)) {
          settings.setExtraArgs(getElementText(element));
          continue;
        }

        if (nodeName.equalsIgnoreCase(VM_ARGS_TAG)) {
          settings.setVmArgs(getElementText(element));
          continue;
        }
        
        if (nodeName.equalsIgnoreCase(ENTRY_POINT_MODULES_TAG)) {
          settings.entryPointModules.add(getElementText(element));
          continue;
        }
      }
    }

    settings.initEntryPointModules();
    
    return settings;
  }

  public static IStatus validateExtraArgs(String extraArgs) {
    if (extraArgs.contains(CDATA_END)) {
      return StatusUtilities.newErrorStatus(
          "Compiler arguments list cannot contain: " + CDATA_END,
          GWTPlugin.PLUGIN_ID);
    }
    return StatusUtilities.OK_STATUS;
  }

  public static IStatus validateVmArgs(String vmArgs) {
    if (vmArgs.contains(CDATA_END)) {
      return StatusUtilities.newErrorStatus(
          "VM arguments list cannot contain: " + CDATA_END, GWTPlugin.PLUGIN_ID);
    }
    return StatusUtilities.OK_STATUS;
  }

  private static String createCDATAElement(String value) {
    if (value.contains(CDATA_END)) {
      throw new IllegalArgumentException("CDATA value cannot contain "
          + CDATA_END);
    }

    return CDATA_START + value + CDATA_END;
  }

  private static String createXmlElement(String tagName, String value) {
    return "<" + tagName + ">" + value + "</" + tagName + ">";
  }

  private static String getElementText(Element element) {
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(0);
      if (child.getNodeType() == Node.TEXT_NODE) {
        return ((Text) child).getNodeValue();
      } else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
        return ((CDATASection) child).getNodeValue();
      }
    }
    return "";
  }

  private List<String> entryPointModules = new ArrayList<String>();

  private String extraArgs = "";

  private String logLevel = (String) GWTLaunchAttributes.LOG_LEVEL.getDefaultValue();
  
  private String outputStyle = (String) GWTLaunchAttributes.OUTPUT_STYLE.getDefaultValue();

  private final IProject project;

  private String vmArgs = "-Xmx512m";
  
  public GWTCompileSettings() {
    this(null);
  }
  
  public GWTCompileSettings(IProject project) {
    this.project = project;
    initEntryPointModules();
  }
  
  @Override
  public boolean equals(Object o) {

    if (o != null && o.getClass().equals(this.getClass())) {
      GWTCompileSettings that = (GWTCompileSettings) o;
      return this.toXml().equals(that.toXml());
    }

    return false;
  }

  public List<String> getEntryPointModules() {
    return entryPointModules;
  }

  public String getExtraArgs() {
    return extraArgs;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public String getOutputStyle() {
    return outputStyle;
  }

  public String getVmArgs() {
    return vmArgs;
  }

  @Override
  public int hashCode() {
    return this.toXml().hashCode();
  }

  /**
   * Entry point modules for compilation are <strong>not</strong> persisted; we
   * simply default to using the project's defined entry point modules instead.
   */
  public void setEntryPointModules(List<String> entryPointModules) {
    this.entryPointModules = entryPointModules;
  }

  public void setExtraArgs(String args) {
    this.extraArgs = args;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public void setOutputStyle(String outputStyle) {
    this.outputStyle = outputStyle;
  }

  public void setVmArgs(String vmArgs) {
    this.vmArgs = vmArgs;
  }

  public byte[] toByteArray() {
    try {
      return toXml().getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      GWTPluginLog.logError(e);
      return new byte[0];
    }
  }

  private void initEntryPointModules() {
    // if the settings don't have any entry point modules, use the ones from
    // the project
    if (project != null && getEntryPointModules().size() == 0) {
      List<String> modules = GWTProjectProperties.getEntryPointModules(project);
      if (modules.size() != 0) {
        setEntryPointModules(modules);
      }
    }
  }

  private boolean shouldSaveEntryPointModules() {
    
    if (entryPointModules == null) {
      return false;
    }
    
    // project will be null if we're doing hashCode or equals, or if something
    // went wrong in toXml, in which case we should err on the safe side
    if (project == null) {
      return true;
    }
    
    // don't persist default settings, so check if the settings we have are the
    // same as the defaults from the project
    List<String> defaultModules = GWTProjectProperties.getDefaultEntryPointModules(project);
    if (defaultModules.size() != entryPointModules.size()) {
      return true;
    } else {
      // check if all the default modules are in this setting's entry point modules
      for (String moduleName : defaultModules) {
        if (!entryPointModules.contains(moduleName)) {
          return false;
        }
      }
    }
    
    return true;
  }

  private String toXml() {
    StringBuilder sb = new StringBuilder();

    sb.append("<" + ROOT_TAG + ">");
    sb.append(createXmlElement(LOG_LEVEL_TAG, logLevel));
    sb.append(createXmlElement(OUTPUT_STYLE_TAG, outputStyle));
    sb.append(createXmlElement(EXTRA_ARGS_TAG, createCDATAElement(extraArgs)));
    sb.append(createXmlElement(VM_ARGS_TAG, createCDATAElement(vmArgs)));
            
    if (shouldSaveEntryPointModules()) {
      for (String moduleName : entryPointModules) {
        sb.append(createXmlElement(ENTRY_POINT_MODULES_TAG, moduleName));
      }
    }

    sb.append("</" + ROOT_TAG + ">");

    return sb.toString();
  }
}
