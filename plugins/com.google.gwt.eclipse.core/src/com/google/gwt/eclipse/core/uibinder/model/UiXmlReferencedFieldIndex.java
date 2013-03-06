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
package com.google.gwt.eclipse.core.uibinder.model;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.collections.OneToManyIndex;
import com.google.gdt.eclipse.core.reference.PersistenceException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.IMemento;

import java.util.Set;

/*
 * Synchronize on the instance when atomic operations are required.
 */
/**
 * Records the ui:field references contained within ui.xml templates.
 * <p>
 * This class is thread-safe.
 */
public final class UiXmlReferencedFieldIndex {

  private static final String KEY_FIELD_NAME = "name";

  private static final String KEY_UI_FIELD_REF = "uiField";

  private static final String KEY_UI_XML_PATH = "location";

  public static UiXmlReferencedFieldIndex load(IMemento memento)
      throws PersistenceException {
    UiXmlReferencedFieldIndex index = new UiXmlReferencedFieldIndex();

    boolean hadException = false;

    for (IMemento childMemento : memento.getChildren(KEY_UI_FIELD_REF)) {
      try {
        loadFieldRef(childMemento, index);
      } catch (PersistenceException e) {
        CorePluginLog.logError(e, "Error loading persisted index entry.");
        hadException = true;
      }
    }

    if (hadException) {
      throw new PersistenceException(
          "Error loading all the references, check log for more details.");
    }

    return index;
  }

  private static void loadFieldRef(IMemento refMemento,
      UiXmlReferencedFieldIndex index) throws PersistenceException {
    String uiXmlPath = refMemento.getString(KEY_UI_XML_PATH);
    if (uiXmlPath == null) {
      throw new PersistenceException("Missing key " + KEY_UI_XML_PATH);
    }

    String fieldName = refMemento.getString(KEY_FIELD_NAME);
    if (fieldName == null) {
      throw new PersistenceException("Missing key " + KEY_FIELD_NAME);
    }

    // The references is ok, so add to the index
    index.addFieldReference(new Path(uiXmlPath), fieldName);
  }

  /**
   * Stores the literal ui:field references in a particular UiBinder template,
   * identified by a workspace-relative path ending with ui.xml.
   */
  private final OneToManyIndex<IPath, String> referencedFields = new OneToManyIndex<IPath, String>();

  public void clear(IJavaProject javaProject) {
    synchronized (this) {
      for (IPath uiXmlPath : referencedFields.keys()) {
        IResource uiXmlResource = ResourcesPlugin.getWorkspace().getRoot().findMember(
            uiXmlPath);
        if (uiXmlResource != null) {
          if (uiXmlResource.getProject().equals(javaProject.getProject())) {
            remove(uiXmlPath);
          }
        }
      }
    }
  }

  public boolean hasUiXml(IPath uiXmlPath) {
    return referencedFields.hasKey(uiXmlPath);
  }

  public boolean isFieldReferencedByUiXml(IPath uiXmlPath, String fieldName) {
    return referencedFields.getElements(uiXmlPath).contains(fieldName);
  }

  public void persist(IMemento memento) {
    for (IPath uiXmlPath : getAllUiXmlPaths()) {
      for (String fieldName : getFieldReferences(uiXmlPath)) {
        persistReference(memento.createChild(KEY_UI_FIELD_REF), uiXmlPath,
            fieldName);
      }
    }
  }

  public void putFieldReferencesForUiXml(IPath uiXmlPath, Set<String> fieldNames) {
    referencedFields.putElements(uiXmlPath, fieldNames);
  }

  public void remove(IPath uiXmlPath) {
    referencedFields.removeKey(uiXmlPath);
  }

  private void addFieldReference(IPath uiXmlPath, String fieldName) {
    referencedFields.addElement(uiXmlPath, fieldName);
  }

  private Set<IPath> getAllUiXmlPaths() {
    return referencedFields.keys();
  }

  private Set<String> getFieldReferences(IPath uiXmlPath) {
    return referencedFields.getElements(uiXmlPath);
  }

  private void persistReference(IMemento memento, IPath uiXmlPath,
      String fieldName) {
    memento.putString(KEY_UI_XML_PATH, uiXmlPath.toString());
    memento.putString(KEY_FIELD_NAME, fieldName);
  }

}
