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
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.core.reference.PersistenceException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMemento;

import java.util.Set;

/*
 * Synchronize on the instance when atomic operations are required.
 */
/**
 * Maps from each UiBinder subtype to its ui.xml template. This index, in
 * conjunction with {@link UiBinderSubtypeToOwnerIndex}, provides the owner
 * class validation visitor with information about the ui:field references in
 * the ui.xml template. It also enables us to automatically re-validate all
 * dependent UiBinder subtypes when a ui.xml file changes.
 * <p>
 * This class is thread-safe.
 */
public class UiBinderSubtypeToUiXmlIndex {

  private static final String KEY_UI_XML_PATH = "uiXmlPath";

  private static final String KEY_UIBINDER_SUBTYPE = "uiBinder";

  private static final String KEY_UIBINDER_SUBTYPE_AND_UI_XML_ENTRY = "entry";

  public static UiBinderSubtypeToUiXmlIndex load(IMemento memento)
      throws PersistenceException {
    UiBinderSubtypeToUiXmlIndex index = new UiBinderSubtypeToUiXmlIndex();

    boolean hadException = false;

    for (IMemento childMemento : memento.getChildren(KEY_UIBINDER_SUBTYPE_AND_UI_XML_ENTRY)) {
      try {
        loadEntry(childMemento, index);
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

  private static void loadEntry(IMemento memento,
      UiBinderSubtypeToUiXmlIndex index) throws PersistenceException {
    String uiXmlPath = memento.getString(KEY_UI_XML_PATH);
    if (uiXmlPath == null) {
      throw new PersistenceException("Missing key " + KEY_UI_XML_PATH);
    }

    String uiBinderSubtypeKey = memento.getString(KEY_UIBINDER_SUBTYPE);
    if (uiBinderSubtypeKey == null) {
      throw new PersistenceException("Missing key " + KEY_UIBINDER_SUBTYPE);
    }

    IJavaElement javaElement = JavaCore.create(uiBinderSubtypeKey);
    if (javaElement == null) {
      throw new PersistenceException("Could not create Java element with key "
          + uiBinderSubtypeKey);
    }

    if (!(javaElement instanceof IType)) {
      throw new PersistenceException("Expecting "
          + javaElement.getElementName() + " to be a type");
    }

    if (!javaElement.getJavaProject().isOpen()) {
      return;
    }

    if (!JavaModelSearch.isValidElement(javaElement)) {
      throw new PersistenceException(
          ((IType) javaElement).getFullyQualifiedName()
              + " is not a valid Java type");
    }

    // Add the subtype + ui.xml pair to the index
    index.setUiXmlPath((IType) javaElement, new Path(uiXmlPath));
  }

  private final OneToManyIndex<IPath, IType> uiXmlPathToOwnerTypes;

  public UiBinderSubtypeToUiXmlIndex() {
    this.uiXmlPathToOwnerTypes = new OneToManyIndex<IPath, IType>();
  }

  public UiBinderSubtypeToUiXmlIndex(UiBinderSubtypeToUiXmlIndex original) {
    this.uiXmlPathToOwnerTypes = new OneToManyIndex<IPath, IType>(
        original.uiXmlPathToOwnerTypes);
  }

  public void clear(IProject project) {
    synchronized (this) {
      for (IType uiBinderType : uiXmlPathToOwnerTypes.elements()) {
        if (uiBinderType.getJavaProject().getProject().equals(project)) {
          removeUiBinderSubtype(uiBinderType);
        }
      }
    }
  }

  /**
   * Returns UiBinder subtypes which are linked to the given ui.xml file.
   */
  public Set<IType> getUiBinderSubtypes(IPath uiXmlPath) {
    return uiXmlPathToOwnerTypes.getElements(uiXmlPath);
  }

  /**
   * Returns the classpath-relative path to the ui.xml file for the given
   * UiBinder subtype, or <code>null</code> if there is no associated ui.xml's.
   */
  public IPath getUiXmlPath(IType uiBinderSubtype) {
    Set<IPath> uiXmls = uiXmlPathToOwnerTypes.getKeys(uiBinderSubtype);
    if (uiXmls.isEmpty()) {
      return null;
    }
    return uiXmls.iterator().next();
  }

  public void persist(IMemento memento) {
    for (IPath uiXmlPath : getAllUiXmlPaths()) {
      for (IType uiBinderSubtype : getUiBinderSubtypes(uiXmlPath)) {
        persistEntry(
            memento.createChild(KEY_UIBINDER_SUBTYPE_AND_UI_XML_ENTRY),
            uiBinderSubtype, uiXmlPath);
      }
    }
  }

  public void removeUiBinderSubtype(IType uiBinderSubtype) {
    uiXmlPathToOwnerTypes.removeElement(uiBinderSubtype);
  }

  public void setUiXmlPath(IType uiBinderSubtype, IPath uiXmlPath) {
    synchronized (this) {
      uiXmlPathToOwnerTypes.removeElement(uiBinderSubtype);
      uiXmlPathToOwnerTypes.addElement(uiXmlPath, uiBinderSubtype);
    }
  }

  private Set<IPath> getAllUiXmlPaths() {
    return uiXmlPathToOwnerTypes.keys();
  }

  private void persistEntry(IMemento memento, IType uiBinderSubtype,
      IPath uiXmlPath) {
    memento.putString(KEY_UIBINDER_SUBTYPE,
        uiBinderSubtype.getHandleIdentifier());
    memento.putString(KEY_UI_XML_PATH, uiXmlPath.toString());
  }

}
