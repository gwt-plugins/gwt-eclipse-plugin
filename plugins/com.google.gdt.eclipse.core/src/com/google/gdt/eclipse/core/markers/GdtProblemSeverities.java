/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.core.markers;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.StringUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps a set of problem types to severity levels.
 */
public final class GdtProblemSeverities {

  private static final GdtProblemSeverities INSTANCE = new GdtProblemSeverities();

  public static GdtProblemSeverities getInstance() {
    return INSTANCE;
  }

  private static int parseIntOrReturnNegative(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private final Map<IGdtProblemType, GdtProblemSeverity> severities;

  // Going to use this as a set.
  private final Map<Class<? extends IGdtProblemType>, Class<? extends IGdtProblemType>> problemTypeEnums;

  private GdtProblemSeverities() {
    this.problemTypeEnums = new ConcurrentHashMap<Class<? extends IGdtProblemType>, Class<? extends IGdtProblemType>>();
    this.severities = new ConcurrentHashMap<IGdtProblemType, GdtProblemSeverity>();
    resetToDefaults();
  }

  private GdtProblemSeverities(Map<IGdtProblemType, GdtProblemSeverity> severities) {
    this.problemTypeEnums = new ConcurrentHashMap<Class<? extends IGdtProblemType>, Class<? extends IGdtProblemType>>();
    this.severities = severities;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void addProblemTypeEnums(Class[] problemTypeEnums) {
    for (Class problemTypeEnum : problemTypeEnums) {
      assert (problemTypeEnum.isEnum());
      this.problemTypeEnums.put(problemTypeEnum, problemTypeEnum);
      for (Object problemType : problemTypeEnum.getEnumConstants()) {
        IGdtProblemType gdtProblemType = (IGdtProblemType) problemType;
        severities.put(gdtProblemType, gdtProblemType.getDefaultSeverity());
      }
    }
  }

  public GdtProblemSeverities createWorkingCopy() {
    Map<IGdtProblemType, GdtProblemSeverity> clonedSeverities = new HashMap<IGdtProblemType, GdtProblemSeverity>();
    for (IGdtProblemType problemType : severities.keySet()) {
      clonedSeverities.put(problemType, severities.get(problemType));
    }

    return new GdtProblemSeverities(clonedSeverities);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof GdtProblemSeverities)) {
      return false;
    }

    GdtProblemSeverities that = (GdtProblemSeverities) obj;
    return this.severities.equals(that.severities);
  }

  public Set<IGdtProblemType> getAllProblemTypes() {
    return severities.keySet();
  }

  public GdtProblemSeverity getSeverity(IGdtProblemType problemType) {
    return severities.get(problemType);
  }

  @Override
  public int hashCode() {
    return severities.hashCode();
  }

  public void loadSeverities(String encodedSeverities) {
    if (encodedSeverities.length() == 0) {
      // No customized severity settings
      return;
    }

    // Split apart problem severity settings
    for (String severitySetting : encodedSeverities.split("\\|")) {

      // Split apart a single problem severity setting
      String[] parts = severitySetting.split("=");
      if (parts.length != 2) {
        CorePluginLog.logError("Encoded problem severity string is corrupted: " + severitySetting);
        return;
      }

      // Split apart the enum type name and the problem ID
      String[] parts2 = parts[0].split("#");
      if (parts2.length != 2) {
        CorePluginLog.logError("Encoded problem type name/ID is corrupted: " + parts[0]);
        return;
      }

      // Look up the problem enum type
      String problemTypeEnumTypeName = parts2[0];
      Class<? extends IGdtProblemType> problemTypeEnum = getProblemTypeEnum(problemTypeEnumTypeName);
      if (problemTypeEnumTypeName == null) {
        CorePluginLog.logError("Could not find problem enum type: " + problemTypeEnumTypeName);
        return;
      }

      // Parse the problem ID
      int problemId = parseIntOrReturnNegative(parts2[1]);

      // Get a real IGdtProblemType from the enum
      IGdtProblemType problemType = null;
      for (IGdtProblemType enumProblemType : problemTypeEnum.getEnumConstants()) {
        if (enumProblemType.getProblemId() == problemId) {
          problemType = enumProblemType;
        }
      }
      if (problemType == null) {
        CorePluginLog.logError("Encoded problem type ID is invalid: " + parts2[1]);
        return;
      }

      // Parse the severity ID and get a real GdtProblemSeverity from it
      GdtProblemSeverity severity = GdtProblemSeverity.getSeverity(parseIntOrReturnNegative(parts[1]));
      if (severity == null) {
        CorePluginLog.logError("Encoded problem severity ordinal is invalid: " + parts[1]);
        return;
      }

      // Finally, set the severity
      setSeverity(problemType, severity);
    }
  }

  public void resetToDefaults() {
    severities.clear();

    for (Class<? extends IGdtProblemType> enumType : problemTypeEnums.keySet()) {
      for (IGdtProblemType problemType : enumType.getEnumConstants()) {
        severities.put(problemType, problemType.getDefaultSeverity());
      }
    }
  }

  public void setSeverity(IGdtProblemType problemType, GdtProblemSeverity severity) {
    if (!severities.containsKey(problemType)) {
      throw new IllegalArgumentException("Problem " + problemType.getProblemId() + " not found");
    }
    severities.put(problemType, severity);
  }

  /**
   * Encode the severities as pipe-separated tuples of {@link IGdtProblemType}
   * and {@link GdtProblemSeverity}. Store the problem type by its
   * fully-qualified class name and problem ID. The severity is simply stored as
   * an integer, which we can use to look up the enum constant from the
   * {@link GdtProblemSeverity} type.
   */
  public String toPreferenceString() {
    StringBuilder encodedSeverities = new StringBuilder();

    Iterator<IGdtProblemType> iter = getProblemTypesWithNonDefaultSeverities().iterator();
    while (iter.hasNext()) {
      IGdtProblemType problemType = iter.next();
      encodedSeverities.append(problemType.getClass().getName());
      encodedSeverities.append('#');
      encodedSeverities.append(problemType.getProblemId());
      encodedSeverities.append('=');
      encodedSeverities.append(getSeverity(problemType).getSeverityId());

      if (iter.hasNext()) {
        encodedSeverities.append('|');
      }
    }

    return encodedSeverities.toString();
  }

  @Override
  public String toString() {
    List<String> problemTypes = new ArrayList<String>();

    for (IGdtProblemType type : severities.keySet()) {
      problemTypes.add(type.getDescription() + "=" + severities.get(type).getDisplayName());
    }

    return StringUtilities.join(problemTypes, "\n");
  }

  private Class<? extends IGdtProblemType> getProblemTypeEnum(String qualifiedTypeName) {
    for (Class<? extends IGdtProblemType> problemTypeEnum : problemTypeEnums.keySet()) {
      if (problemTypeEnum.getName().equals(qualifiedTypeName)) {
        return problemTypeEnum;
      }
    }
    return null;
  }

  private Set<IGdtProblemType> getProblemTypesWithNonDefaultSeverities() {
    Set<IGdtProblemType> problemTypes = new HashSet<IGdtProblemType>();
    for (IGdtProblemType problemType : severities.keySet()) {
      if (!getSeverity(problemType).equals(problemType.getDefaultSeverity())) {
        problemTypes.add(problemType);
      }
    }
    return problemTypes;
  }

}
