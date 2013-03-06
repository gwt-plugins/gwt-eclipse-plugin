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
package com.google.gwt.eclipse.core.modules;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.Signature;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a GWT module.
 */
@SuppressWarnings("restriction")
abstract class AbstractModule implements IModule {

  /**
   * Ensures that all access to the module's underlying DOM model properly
   * updates its internal reference count. Every access to the model should be
   * make through the <code>readModel(IDOMModel)</code> method on a subclass.
   */
  // TODO: use XmlUtilities.ReadOperation instead
  protected abstract class ReadModelOperation {

    public void run() {
      IDOMModel model = null;
      try {
        model = doGetModelForRead();
        readModel(model);
      } catch (IOException e) {
        GWTPluginLog.logError(e);
      } catch (CoreException e) {
        GWTPluginLog.logError(e);
      } finally {
        if (model != null) {
          model.releaseFromRead();
        }
      }
    }

    protected abstract void readModel(IDOMModel model);
  }

  protected static final String CLASS_ATTRIBUTE_NAME = "class";

  protected static final String ENTRY_POINT_TAG_NAME = "entry-point";
  
  protected static final String INHERITS_TAG_NAME = "inherits";

  protected static final String NAME_ATTRIBUTE_NAME = "name";
  
  protected static final String PATH_ATTRIBUTE_NAME = "path";

  protected static final String PUBLIC_PATH_TAG_NAME = "public";

  protected static final String RENAME_TO_ATTRIBUTE = "rename-to";

  protected static final String SOURCE_PATH_TAG_NAME = "source";

  /**
   * Gets a list of attribute values for a particular type of element.
   * 
   * @param doc the XML document
   * @param elementName the type of element to search
   * @param attrName the name of the attribute to get the value of
   * @param defaultValue the default value, if there is no element of the
   *          specified type (e.g., for <source> elements it is "client")
   * @return the list of attribute values
   */
  private static List<String> getElementsAttributes(Document doc,
      String elementName, String attrName, String defaultValue) {
    List<String> attrValues = new ArrayList<String>();

    NodeList elements = doc.getElementsByTagName(elementName);

    if (elements != null && elements.getLength() > 0) {
      for (int i = 0; i < elements.getLength(); i++) {
        Element element = (Element) elements.item(i);
        String attrValue = element.getAttribute(attrName);
        if (attrValue != null) {
          attrValues.add(attrValue);
        }
      }
    }

    if (defaultValue != null && attrValues.size() == 0) {
      attrValues.add(defaultValue);
    }

    return attrValues;
  }

  protected final IStorage storage;

  private String qualifiedName;

  protected AbstractModule(IStorage storage) {
    assert (storage != null);
    this.storage = storage;
  }

  /**
   * Two modules are considered equal iff their qualified names are the same.
   * 
   * Subclasses are expected to obey the same semantics.
   */
  @Override
  public final boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof AbstractModule)) {
      return false;
    }

    return getQualifiedName().equals(((AbstractModule) o).getQualifiedName());
  }

  public String getCompiledName() {
    final String[] compiledName = new String[] {getQualifiedName()};

    new ReadModelOperation() {
      @Override
      public void readModel(IDOMModel model) {
        IDOMDocument doc = model.getDocument();
        Element moduleElement = doc.getDocumentElement();

        String renameTo = moduleElement.getAttribute(RENAME_TO_ATTRIBUTE);
        if (renameTo != null) {
          compiledName[0] = renameTo;
        }
      }
    }.run();

    return compiledName[0];
  }

  public List<String> getEntryPoints() {
    final List<String> ret = new ArrayList<String>();

    new ReadModelOperation() {
      @Override
      public void readModel(IDOMModel model) {
        IDOMDocument doc = model.getDocument();

        // Extract the entry point classes
        ret.addAll(getElementsAttributes(doc, ENTRY_POINT_TAG_NAME,
            CLASS_ATTRIBUTE_NAME, null));
      }
    }.run();

    return ret;
  }

  public Set<IModule> getInheritedModules(final IJavaProject javaProject) {
    final Set<IModule> modules = new HashSet<IModule>();

    new ReadModelOperation() {
      @Override
      protected void readModel(IDOMModel model) {
        IDOMDocument doc = model.getDocument();
        for (String moduleName : getElementsAttributes(doc, INHERITS_TAG_NAME,
            NAME_ATTRIBUTE_NAME, null)) {
          // don't look up any modules in jar files, because this is slllloow
          AbstractModule module = (AbstractModule) ModuleUtils.findModule(
              javaProject, moduleName, false);
          if (module != null) {
            modules.add(module);
          }
        }
      }
    }.run();

    return modules;
  }

  public String getPackageName() {
    return Signature.getQualifier(getQualifiedName());
  }

  public List<IPath> getPublicPaths() {
    final List<IPath> ret = new ArrayList<IPath>();

    new ReadModelOperation() {
      @Override
      public void readModel(IDOMModel model) {
        IDOMDocument doc = model.getDocument();

        List<String> publicPathNames = getElementsAttributes(doc,
            PUBLIC_PATH_TAG_NAME, PATH_ATTRIBUTE_NAME, "public");

        // TODO: if no path attribute, default to . (current directory)

        // Convert the public paths to IPath's (relative to the module location)
        for (String publicPathName : publicPathNames) {
          ret.add(new Path(publicPathName));
        }
      }
    }.run();

    return ret;
  }

  public String getQualifiedName() {
    // Cache the qualified name
    if (qualifiedName == null) {
      qualifiedName = Util.removeFileExtension(storage.getName());

      String modulePckg = doGetPackageName();
      if (modulePckg != null) {
        qualifiedName = modulePckg + "." + qualifiedName;
      }
    }

    return qualifiedName;
  }

  public String getSimpleName() {
    return Signature.getSimpleName(getQualifiedName());
  }

  public List<IPath> getSourcePaths() {
    final List<IPath> ret = new ArrayList<IPath>();

    new ReadModelOperation() {
      @Override
      public void readModel(IDOMModel model) {
        IDOMDocument doc = model.getDocument();

        List<String> sourcePathNames = getElementsAttributes(doc,
            SOURCE_PATH_TAG_NAME, PATH_ATTRIBUTE_NAME, "client");

        // TODO: if no path attribute, default to . (current directory)

        // Convert the source paths to IPath's (relative to the module location)
        for (String sourcePathName : sourcePathNames) {
          ret.add(new Path(sourcePathName));
        }
      }
    }.run();

    return ret;
  }

  @Override
  public int hashCode() {
    return getQualifiedName().hashCode();
  }

  protected abstract IDOMModel doGetModelForRead() throws IOException,
      CoreException;

  protected abstract String doGetPackageName();
  
}
