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
import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.core.reference.PersistenceException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMemento;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Maps a UiBinder subtype to one or more owner classes. This class enables us
 * to automatically re-validate all dependent UiBinder owner classes when a
 * UiBinder subtype changes.
 * <p>
 * This class is fully thread-safe.
 */
public class UiBinderSubtypeToOwnerIndex {

  /**
   * Represents a UiBinder subtype and its associated owner class.
   */
  public static final class UiBinderSubtypeAndOwner {

    private static final String KEY_OWNER_TYPE_NAME = "owner";

    private static final String KEY_UIBINDER_SUBTYPE = "uiBinder";

    /**
     * @return an object representing the persisted memento, or null if the
     *         memento is no longer valid
     * @throws PersistenceException
     */
    public static UiBinderSubtypeAndOwner load(IMemento memento)
        throws PersistenceException {
      String ownerTypeName = memento.getString(KEY_OWNER_TYPE_NAME);
      if (ownerTypeName == null) {
        throw new PersistenceException("Missing key " + KEY_OWNER_TYPE_NAME);
      }

      String uiBinderSubtypeKey = memento.getString(KEY_UIBINDER_SUBTYPE);
      if (uiBinderSubtypeKey == null) {
        throw new PersistenceException("Missing key " + KEY_UIBINDER_SUBTYPE);
      }

      IJavaElement javaElement = JavaCore.create(uiBinderSubtypeKey);
      if (javaElement == null) {
        throw new PersistenceException("Could not create type "
            + uiBinderSubtypeKey);
      }

      if (!(javaElement instanceof IType)) {
        throw new PersistenceException("Expecting "
            + javaElement.getElementName() + " to be a type");
      }

      if (!javaElement.getJavaProject().isOpen()) {
        return null;
      }

      if (!JavaModelSearch.isValidElement(javaElement)) {
        throw new PersistenceException(
            ((IType) javaElement).getFullyQualifiedName()
            + " is not a valid Java type");
      }

      return new UiBinderSubtypeAndOwner((IType) javaElement, ownerTypeName);
    }

    private final String ownerTypeName;

    private final IType uiBinderType;

    private UiBinderSubtypeAndOwner(IType uiBinderType, String ownerTypeName) {
      this.uiBinderType = uiBinderType;
      this.ownerTypeName = ownerTypeName;
    }

    public IType findOwnerType() {
      return JavaModelSearch.findType(uiBinderType.getJavaProject(),
          ownerTypeName);
    }

    public String getOwnerTypeName() {
      return ownerTypeName;
    }

    public IType getUiBinderType() {
      return uiBinderType;
    }

    public boolean hasCommonCompilationUnit() {
      IType ownerType = findOwnerType();
      if (ownerType == null) {
        return false;
      }
      return JavaUtilities.equalsWithNullCheck(
          uiBinderType.getCompilationUnit(), ownerType.getCompilationUnit());
    }

    public void persist(IMemento memento) {
      memento.putString(KEY_OWNER_TYPE_NAME, ownerTypeName);
      memento.putString(KEY_UIBINDER_SUBTYPE,
          uiBinderType.getHandleIdentifier());
    }

    @Override
    public String toString() {
      return MessageFormat.format("{0} -> {1}",
          uiBinderType.getFullyQualifiedName(), ownerTypeName);
    }
  }

  private static final String KEY_UIBINDER_SUBTYPE_AND_OWNER_ENTRY = "entry";

  public static UiBinderSubtypeToOwnerIndex load(IMemento memento)
      throws PersistenceException {
    UiBinderSubtypeToOwnerIndex index = new UiBinderSubtypeToOwnerIndex();

    boolean hadException = false;

    for (IMemento childMemento : memento.getChildren(KEY_UIBINDER_SUBTYPE_AND_OWNER_ENTRY)) {
      try {
        UiBinderSubtypeAndOwner subtypeAndOwner = UiBinderSubtypeAndOwner.load(childMemento);
        if (subtypeAndOwner != null) {
          index.setOwnerType(subtypeAndOwner);
        }
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

  /**
   * Synchronized via {@link Collections#synchronizedMap(Map)}, ensure to follow
   * the contract specified by that method.
   */
  private final Map<IType, String> uiBinderToOwnerClass;

  public UiBinderSubtypeToOwnerIndex() {
    this.uiBinderToOwnerClass = Collections.synchronizedMap(new HashMap<IType, String>());
  }

  public UiBinderSubtypeToOwnerIndex(UiBinderSubtypeToOwnerIndex original) {
    synchronized (original.uiBinderToOwnerClass) {
      // Lock on the original since the ctor will iterate over the
      // original's elements
      this.uiBinderToOwnerClass = Collections.synchronizedMap(new HashMap<IType, String>(
          original.uiBinderToOwnerClass));
    }
  }

  public void clear(IProject project) {
    // Copy the set so we don't get a ConcurrentModificationException as we
    // remove
    Set<IType> uiBinderTypes;
    synchronized (uiBinderToOwnerClass) {
      // The ctor for HashSet will iterate over the given key set, so
      // lock
      uiBinderTypes = new HashSet<IType>(uiBinderToOwnerClass.keySet());
    }

    for (IType uiBinderType : uiBinderTypes) {
      if (uiBinderType.getJavaProject().getProject().equals(project)) {
        removeUiBinderType(uiBinderType);
      }
    }
  }

  public boolean containsOwnerType(String typeName) {
    return uiBinderToOwnerClass.containsValue(typeName);
  }

  public boolean containsUiBinderType(IType type) {
    return uiBinderToOwnerClass.containsKey(type);
  }

  /**
   * Returns a new set containing all of the owner qualified type names.
   */
  public Set<String> getAllOwnerTypeNames() {
    synchronized (uiBinderToOwnerClass) {
      // The ctor for HashSet will iterate over the given values, so
      // lock
      return Collections.unmodifiableSet(new HashSet<String>(
          uiBinderToOwnerClass.values()));
    }
  }

  /**
   * Returns a new set containing all of the UiBinder subtypes.
   */
  public Set<IType> getAllUiBinderTypes() {
    synchronized (uiBinderToOwnerClass) {
      /*
       * Need to wrap in a new HashSet since Collections.unmodifiableSet returns
       * a view of the original set, and we do not want to leak out the original
       * key set since iterating over it needs to be synchronized.
       */
      return Collections.unmodifiableSet(new HashSet<IType>(
          uiBinderToOwnerClass.keySet()));
    }
  }

  /**
   * Returns a new set containing the pairs of UiBinder subtypes and owners.
   */
  public Set<UiBinderSubtypeAndOwner> getAllUiBinderTypesAndOwners() {
    Set<UiBinderSubtypeAndOwner> entries = new HashSet<UiBinderSubtypeAndOwner>();
    synchronized (uiBinderToOwnerClass) {
      for (Entry<IType, String> mapEntry : uiBinderToOwnerClass.entrySet()) {
        entries.add(new UiBinderSubtypeAndOwner(mapEntry.getKey(),
            mapEntry.getValue()));
      }
    }
    return entries;
  }

  public String getOwnerTypeName(IType uiBinderType) {
    return uiBinderToOwnerClass.get(uiBinderType);
  }

  /**
   * Returns a new set containing the UiBinder types mapped to the given owner
   * type.
   * 
   * @param ownerType the qualified owner type
   */
  public Set<IType> getUiBinderTypes(String ownerType) {
    Set<IType> uiBinderTypes = new HashSet<IType>();
    synchronized (uiBinderToOwnerClass) {
      for (IType uiBinderType : uiBinderToOwnerClass.keySet()) {
        if (uiBinderToOwnerClass.get(uiBinderType).equals(ownerType)) {
          uiBinderTypes.add(uiBinderType);
        }
      }
    }
    return uiBinderTypes;
  }

  public boolean isOwnerType(String type) {
    return uiBinderToOwnerClass.containsValue(type);
  }

  public void persist(IMemento memento) {
    for (UiBinderSubtypeAndOwner pair : getAllUiBinderTypesAndOwners()) {
      pair.persist(memento.createChild(KEY_UIBINDER_SUBTYPE_AND_OWNER_ENTRY));
    }
  }

  public void removeUiBinderType(IType uiBinderType) {
    uiBinderToOwnerClass.remove(uiBinderType);
  }

  public void setOwnerType(IType uiBinderType, String ownerType) {
    uiBinderToOwnerClass.put(uiBinderType, ownerType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (UiBinderSubtypeAndOwner entry : getAllUiBinderTypesAndOwners()) {
      sb.append(entry.toString());
      sb.append('\n');
    }

    return sb.toString();
  }

  private void setOwnerType(UiBinderSubtypeAndOwner pair) {
    setOwnerType(pair.getUiBinderType(), pair.getOwnerTypeName());
  }

}
