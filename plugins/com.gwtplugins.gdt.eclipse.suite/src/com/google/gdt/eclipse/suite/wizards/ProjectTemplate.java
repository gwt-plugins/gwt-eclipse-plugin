/*******************************************************************************
 * Copyright 2024 GWT Eclipse Plugin. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.wizards;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaCore;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class loads and parses the template files.
 */
public class ProjectTemplate implements Comparable<ProjectTemplate>{
  private static final String GWT_TEMPLATE_DIR = "GWT_TEMPLATE_DIR";
  private Document config;
  private File dir;

  /**
   * @throws IOException
   * @throws JDOMException
   */
  public ProjectTemplate(File config) throws JDOMException, IOException {
    this.config = parseTemplate(config);
    dir = config.getParentFile();
  }

  /**
   * Get a List of all templates.
   * @return
   * @throws IOException
   */
  public static final List<ProjectTemplate> getTemplates() throws IOException {
    List<ProjectTemplate> templates = new ArrayList<>();
    try {
      Bundle bundle = Platform.getBundle("com.gwtplugins.gdt.eclipse.suite");
      URL url = bundle.getResource("/project_templates");
      url = FileLocator.toFileURL(url);
      File templatesDir = new File(new URI(url.toString()));
      List<ProjectTemplate> list = readTemplates(templatesDir);
      templates.addAll(list);
      //Load custom templates
      String tmp = System.getenv(GWT_TEMPLATE_DIR);
      if(tmp != null)
      {
        list = readTemplates(new File(tmp));
        templates.addAll(list);
      }
    }
    catch(Exception ex) {
      throw new IOException("Unable to read templates" , ex);
    }
    return templates;
  }

  private static List<ProjectTemplate> readTemplates(File templatesDir) throws JDOMException, IOException
  {
    List<ProjectTemplate> templates = new ArrayList<>();
    if(templatesDir.exists())
    {
      File[] dirs = templatesDir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          return pathname.isDirectory();
        }
      });
      for(File dir : dirs)
      {
        File config = new File(dir, "config.xml");
        if(config.exists())
        {
          ProjectTemplate temp = new ProjectTemplate(config);
          templates.add(temp);
        }
      }
    }
    return templates;
  }

  /**
   * @param config
   * @return
   * @throws IOException
   * @throws JDOMException
   */
  private static Document parseTemplate(File config) throws JDOMException, IOException {
    SAXBuilder parser = new SAXBuilder();
    Document doc = parser.build(config);
    doc.getRootElement().getChildren("project");
    return doc;
  }

  /**
   * Returns a list of the project names that are build by this template.
   * @return
   */
  public List<String> getProjectNames(String baseName) {
    List<String> names = new ArrayList<>();
    List<Element> list = config.getRootElement().getChildren("project");
    for(Element el : list)
    {
      String name = el.getAttributeValue("name");
      name = name.replace("_PROJECTNAME_", baseName);
      names.add(name);
    }
    return names;
  }

  /**
   * Creates a copy of the template project into the destination dir.
   * @param project Number of the project to copy.
   * @param dir
   * @throws IOException
   */
  public void copyProject(int projectIndex, File dir) throws IOException {
    Element project = getProjectElement(projectIndex);
    File source = new File(this.dir, project.getAttributeValue("dir"));
    FileUtils.copyDirectory(source, dir);
  }

  private Element getProjectElement(int index)
  {
    return config.getRootElement().getChildren("project").get(index);
  }

  public String getName()
  {
    return config.getRootElement().getChild("name").getTextNormalize();
  }

  /**
   * @param i
   * @return
   */
  public List<String> getNatureIds(int index) {
    Element project = getProjectElement(index);
    List<String> natureIds = new ArrayList<>();
    natureIds.add(JavaCore.NATURE_ID);
    Element natures = project.getChild("natures");
    if(natures != null)
    {
      for(Element nature : project.getChildren("nature"))
      {
        natureIds.add(nature.getAttributeValue("id"));
      }
    }
    return natureIds;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(ProjectTemplate o) {
    return getName().compareTo(o.getName());
  }

}
