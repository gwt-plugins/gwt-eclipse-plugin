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
package com.google.gdt.eclipse.core.resources;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * Generates source for files needed by web application projects.
 * 
 * TODO: Convert these use to use templates.
 */
@SuppressWarnings("restriction")
public class ProjectResources {

  /**
   * Generate the source for a web.xml file with placeholders for the servlet,
   * servlet-mapping, and welcome-file-list tags.
   */
  public static String createWebXmlSource() {

    StringBuilder sb = new StringBuilder();
    // TODO: insert correct web.xml version number

    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

    sb.append("<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
    sb.append("xmlns=\"http://java.sun.com/xml/ns/javaee\"\n");
    sb.append("xmlns:web=\"http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"\n");
    sb.append("xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee\n");
    sb.append("http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\" version=\"2.5\">\n");
    sb.append("\t<!-- TODO: Add <servlet> tags for each servlet here. -->\n");
    sb.append("\t<!-- TODO: Add <servlet-mapping> tags for each <servlet> here. -->\n");
    sb.append("\t<!-- TODO: Optionally add a <welcome-file-list> tag to display a welcome file. -->\n");
    sb.append("</web-app>\n");
    return sb.toString();
  }

  /**
   * Generate the source for a web.xml file with a servlet tag for the given
   * servletName and servletQualifiedClassName parameters. A servlet-mapping tag
   * is generated which maps this servlet to the <code>/servletName</code> URL.
   * A welcome-file-list is also added, which maps to the
   * <code>/index.html</code> file.
   */
  public static String createWebXmlSource(String servletName,
      String servletPath, String servletQualifiedClassName) {

    StringBuilder sb = new StringBuilder();
    // TODO: insert correct web.xml version number

    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

    sb.append("<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
    sb.append("xmlns=\"http://java.sun.com/xml/ns/javaee\"\n");
    sb.append("xmlns:web=\"http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"\n");
    sb.append("xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee\n");
    sb.append("http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\" version=\"2.5\">\n");
    sb.append("\t<servlet>\n");
    sb.append("\t\t<servlet-name>" + servletName + "</servlet-name>\n");
    sb.append("\t\t<servlet-class>" + servletQualifiedClassName
        + "</servlet-class>\n");
    sb.append("\t</servlet>\n");
    sb.append("\t<servlet-mapping>\n");
    sb.append("\t\t<servlet-name>" + servletName + "</servlet-name>\n");
    sb.append("\t\t<url-pattern>/" + servletPath + "</url-pattern>\n");
    sb.append("\t</servlet-mapping>\n");

    // Use index.html as the welcome file
    sb.append("\t<welcome-file-list>\n");
    sb.append("\t\t<welcome-file>index.html</welcome-file>\n");
    sb.append("\t</welcome-file-list>\n");
    sb.append("</web-app>\n");
    return sb.toString();
  }

  /**
   * Given a java.io.File representing a directory, list all the files
   * underneath that directory matching the given FilenameFilter.
   */
  public static List<File> findFilesInDir(File directory, FilenameFilter filter) {
    Vector<File> files = new Vector<File>();
    File[] entries = directory.listFiles();

    for (File entry : entries) {
      if (filter == null || filter.accept(directory, entry.getName())) {
        files.add(entry);
      }

      if (entry.isDirectory()) {
        files.addAll(findFilesInDir(entry, filter));
      }
    }
    return files;
  }

  /**
   * Given a java.io.File containing Java source, call the Eclipse auto-format
   * code on that source and write it back to disk.
   * 
   * @param file
   * @throws CoreException
   */
  public static void reformatJavaSource(File file) throws CoreException {
    try {
      String generatedSource = textFromFile(file);
      String reformattedSource = reformatJavaSourceAsString(generatedSource);
      if (!reformattedSource.equals(generatedSource)) {
        writeStringToFile(reformattedSource, file);
      }
    } catch (IOException ioe) {
      throw new CoreException(new Status(Status.ERROR, CorePlugin.PLUGIN_ID,
          "IOException while trying to reformat source code in new project"));
    }
  }

  /**
   * Given a String containing the text of a Java source file, return the same
   * Java source, but reformatted by the Eclipse auto-format code, with the
   * user's current Java preferences.
   */
  public static String reformatJavaSourceAsString(String source) {
    TextEdit reformatTextEdit = CodeFormatterUtil.format2(
        CodeFormatter.K_COMPILATION_UNIT, source, 0, (String) null,
        JavaCore.getOptions());
    if (reformatTextEdit != null) {
      Document document = new Document(source);
      try {
        reformatTextEdit.apply(document, TextEdit.NONE);
        source = document.get();
      } catch (BadLocationException ble) {
        CorePluginLog.logError(ble);
      }
    }
    return source;
  }

  /*
   * TODO: These next two methods might be useful somewhere else in the
   * future, but right now, this is the only place where we need to do I/O on
   * java.io.Files instead of Eclipse resources.
   */
  private static String textFromFile(File file) throws IOException {
    char bytes[] = new char[1024];
    int nread;
    StringBuilder builder = new StringBuilder();

    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      while ((nread = reader.read(bytes, 0, 1024)) != -1) {
        char toAppend[] = new char[nread];
        System.arraycopy(bytes, 0, toAppend, 0, nread);
        builder.append(toAppend);
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
    return builder.toString();
  }

  private static void writeStringToFile(String string, File file)
      throws IOException {
    BufferedWriter bw = null;

    try {
      bw = new BufferedWriter(new FileWriter(file));
      bw.write(string);
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  private ProjectResources() {
    // Not instantiable
  }
}
