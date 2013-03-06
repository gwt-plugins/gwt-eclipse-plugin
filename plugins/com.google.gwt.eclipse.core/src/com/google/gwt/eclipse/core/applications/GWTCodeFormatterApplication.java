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
package com.google.gwt.eclipse.core.applications;

import com.google.gwt.eclipse.core.editors.java.GWTDocumentSetupParticipant;
import com.google.gwt.eclipse.core.editors.java.JsniFormattingUtil;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.internal.Workbench;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The Eclipse application org.eclipse.jdt.core.JavaCodeFormatter doesn't format
 * JSNI methods correctly. This application invokes GPE's GWT editor formatter,
 * which handles JSNI methods. This code isn't called by GPE itself.
 * 
 * Note that the JSNI formatter relies on the JavaScript being syntactically
 * correct. If the JavaScript is not syntactically correct, then the JavaScript
 * may not be formatted correctly.
 * 
 * Note that because the JavaScript formatter (eventually) has a dependency on
 * the workbench, this application opens a workbench UI window, and then
 * programmatically closes it.
 * 
 * The config files contain the formatter preferences obtained from Eclipse in
 * Window -> Preferences -> (Java|JavaScript) -> Code Style -> Formatter ->
 * Edit... -> Export. They can be left in XML, otherwise the file should be in
 * the form:
 * 
 * <p>
 * <code>
 * #
 * key=value
 * key=value
 * </code>
 * </p>
 * 
 * <p>
 * Usage: eclipse -application
 * com.google.gwt.eclipse.core.formatter.GWTCodeFormatterApplication
 * <javaFormatterConfigFile> <jsFormatterConfigFile> <sourceFile/Folder1>
 * [<sourceFile/Folder2>] ...
 * </p>
 */
@SuppressWarnings("restriction")
public class GWTCodeFormatterApplication implements IApplication {

  private Properties javaConfig;
  private Properties jsConfig;

  public Object start(IApplicationContext context) throws Exception {

    String[] args = (String[]) context.getArguments().get(
        IApplicationContext.APPLICATION_ARGS);

    if (args.length < 3) {
      System.err.println("Usage: eclipse -application com.google.gwt.eclipse.core.formatter.GWTCodeFormatterApplication "
          + "<javaFormatterConfigFile> <jsFormatterConfigFile> <sourceFile1> [<sourceFile2>] ...");
      return IApplication.EXIT_OK;
    }

    javaConfig = getConfig(args[0]);
    jsConfig = getConfig(args[1]);

    // The JavaScriptCore plugin, which the JS formatter depends on, requires
    // the workbench, so start one manually
    startWorkbench();

    for (int i = 2; i < args.length; i++) {
      File f = new File(args[i]);
      format(f);
    }

    return IApplication.EXIT_OK;
  }

  public void stop() {
  }

  @SuppressWarnings("restriction")
  private void doFormat(File file) {
    IDocument doc = new Document();
    try {

      String contents = new String(
          org.eclipse.jdt.internal.compiler.util.Util.getFileCharContent(file,
              null));

      GWTDocumentSetupParticipant.setupGWTPartitioning(doc);

      doc.set(contents);
      TextEdit edit = JsniFormattingUtil.format(doc, javaConfig, jsConfig, null);
      if (edit != null) {
        edit.apply(doc);
      } else {
        System.err.println("Error formatting " + file.getAbsolutePath());
        return;
      }

      // write the file
      final BufferedWriter out = new BufferedWriter(new FileWriter(file));
      try {
        out.write(doc.get());
        out.flush();
      } finally {
        try {
          out.close();
        } catch (IOException e) {
          /* ignore */
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void format(File file) {
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        format(f);
      }
    } else {
      if (file.getName().endsWith(".java")) {
        doFormat(file);
      }
    }
  }

  private Properties getConfig(String configFilename) throws Exception {

    InputStream stream = null;
    File f = new File(configFilename);
    try {
      if (configFilename.endsWith(".xml")) {
        stream = transformXmlToProperties(f);
      } else {
        stream = new BufferedInputStream(new FileInputStream(f));
      }
      final Properties formatterOptions = new Properties();
      formatterOptions.load(stream);
      return formatterOptions;
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException e) {
          /* ignore */
        }
      }
    }
  }

  private void startWorkbench() {
    PlatformUI.createAndRunWorkbench(PlatformUI.createDisplay(),
        new WorkbenchAdvisor() {
          @Override
          public String getInitialWindowPerspectiveId() {
            return null;
          }

          @Override
          public void postStartup() {
            // Kill it when it opens so that the thread is unstuck.
            Workbench.getInstance().close();
          }

        });
  }

  private InputStream transformXmlToProperties(File f) throws IOException, SAXException, ParserConfigurationException {
    
    StringBuilder sb = new StringBuilder((int) f.length());
    sb.append("#\n");
    org.w3c.dom.Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
    NodeList nodes = xml.getElementsByTagName("setting");
    
    for (int i = 0; i < nodes.getLength(); i++) {
      Node n = nodes.item(i);
      String key = n.getAttributes().getNamedItem("id").getNodeValue();
      String value = n.getAttributes().getNamedItem("value").getNodeValue();
      sb.append(key);
      sb.append('=');
      sb.append(value);
      sb.append('\n');
    }
    
    // this copies the string builder twice...
    return new ByteArrayInputStream(sb.toString().getBytes());
  }

}
