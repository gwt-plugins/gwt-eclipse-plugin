/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.editors.webautocomplete;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * Singleton holding autocomplete data in the format expected by the {@code Autocompleter}.
 */
public final class AutocompleteEntryHolder {

  private static final AutocompleteEntryHolder INSTANCE = new AutocompleteEntryHolder();

  private final Set<String> typeNames = Sets.newHashSet();

  private final SetMultimap<String, String> topLevelTypeNamesToTypeNames = HashMultimap.create();

  private final SortedSetMultimap<String, AutocompleteEntry> typeNamesToEntries
      = TreeMultimap.create();

  private final SortedSet<AutocompleteEntry> topLevelEntries = Sets.newTreeSet();

  /** Indicates whether the auto-complete data has been already loaded and can be used */
  private boolean isReady = false;

  /** Callbacks to execute when the data is loaded */
  private List<Callback> callbacks = Lists.newArrayList();

  public static AutocompleteEntryHolder getInstance() {
    return INSTANCE;
  }

  /**
   * Adds autocomplete entries from the given bean data proto, binding the top-level type at the
   * specified name.
   */
  public void addBeanData(AutocompleteProto.Bean beanData) {
    Preconditions.checkNotNull(beanData, "beanData must be nonnull");

    boolean isCommon = beanData.getIsCommon();
    AutocompleteProto.Type topLevelType = beanData.getTopLevelType();
    String topLevelTypeName = topLevelType.getName();
    String topLevelTypeDescription = topLevelType.getDescription();

    // Prepopulate the parent-to-child type name mapping with appropriately qualified type names.
    addTypeName(topLevelTypeName, topLevelTypeName, topLevelTypeDescription, isCommon);

    for (AutocompleteProto.Type childTypeData : beanData.getChildTypeList()) {
      addTypeName(
          topLevelTypeName, childTypeData.getName(), childTypeData.getDescription(), isCommon);
    }

    // Add actual type data.
    addTypeData(topLevelTypeName, beanData.getTopLevelType());

    for (AutocompleteProto.Type childTypeData : beanData.getChildTypeList()) {
      addTypeData(topLevelTypeName, childTypeData);
    }
  }

  /**
   * Adds a type name (qualified if necessary) to the type name map, and makes non-common top-level
   * beans available in top-level autocomplete.
   */
  private void addTypeName(
      String topLevelTypeName, String typeName, String description, boolean isCommon) {
    if (!isCommon) {
      if (typeName.equals(topLevelTypeName)) {
        topLevelEntries.add(new FieldAutocompleteEntry(typeName, typeName, description));
      }

      topLevelTypeNamesToTypeNames.put(topLevelTypeName, topLevelTypeName);
    }
  }

  /**
   * Adds autocomplete entries from the given type data proto.
   */
  private void addTypeData(String topLevelTypeName, AutocompleteProto.Type typeData) {
    String typeName = typeData.getName();
    typeNames.add(typeName);
    // A type name gets added to this set even if it has no fields or methods.

    for (AutocompleteProto.Field fieldData : typeData.getFieldList()) {
      addFieldData(typeName, fieldData);
    }

    for (AutocompleteProto.Method methodData : typeData.getMethodList()) {
      addMethodData(typeName, methodData);
    }
  }

  /**
   * Adds autocomplete entries from the specified type data proto.
   */
  private void addFieldData(String typeName, AutocompleteProto.Field fieldData) {
    typeNamesToEntries.put(
        typeName,
        new FieldAutocompleteEntry(
            (fieldData.getKind() == AutocompleteProto.Field.Kind.ENUM_CONSTANT)
                // For enum constants, add a fake parent bean prefix to keep users from drilling
                // down into enum values ad infinitum (http://b/8241597). This prefix won't be shown
                // to users, and, since it begins with a space character, it will never conflict
                // with a legitimate bean identifier.
                ? " __Enum__." + fieldData.getTypeName()
                // For all other fields, use the real type name without modification.
                : fieldData.getTypeName(),
        fieldData.getName(),
        fieldData.getDescription()));
  }

  /**
   * Adds autocomplete entries from the specified method data proto.
   */
  private void addMethodData(String typeName, AutocompleteProto.Method methodData) {
    typeNamesToEntries.put(
        typeName,
        new MethodAutocompleteEntry(
            methodData.getReturnTypeName(),
            methodData.getName(),
            methodData.getParamList(),
            methodData.getDescription()));
  }

  /**
   * Checks if autocomplete entries are available for the specified type name.
   */
  public boolean isTypeName(String typeName) {
    Preconditions.checkNotNull(typeName, "typeName must be nonnull");

    return typeNames.contains(typeName);
  }

  /**
   * Checks if the specified type name is the name through which a top-level bean type is currently
   * exposed.
   */
  public boolean isTopLevelTypeName(String topLevelBinding) {
    Preconditions.checkNotNull(topLevelBinding, "topLevelBinding must be nonnull");

    return topLevelTypeNamesToTypeNames.containsKey(topLevelBinding);
  }

  /**
   * Returns a sorted view of all autocomplete entries associated with the specified type name. If
   * the type name given is {@code null}, then the set of entries for top-level beans will be
   * returned. Also, if no entries are available for the given type name, the empty set will be
   * returned.
   */
  public SortedSet<AutocompleteEntry> getEntriesForTypeName(@Nullable String typeName) {
    return Collections.unmodifiableSortedSet(
        (typeName != null)
        ? typeNamesToEntries.get(typeName)
        : topLevelEntries);
  }

  /**
   * Interface used for callbacks needed when the auto-complete data finishes loading
   */
  public interface Callback {
    public void execute();
  }

  /**
   * Mark that all auto-complete data has been loaded and call all registered callbacks
   */
  public void markAsReady() {
    if (isReady) {
      return;
    }
    isReady = true;
    for (Callback callback : callbacks) {
      callback.execute();
    }
  }

  /**
   * Adds a new callback for when the auto-complete data is ready. If it's already ready, the
   * callback is executed right away.
   * @param callback
   */
  public void addCallback(Callback callback) {
    if (isReady) {
      callback.execute();
    } else {
      callbacks.add(callback);
    }
  }
}