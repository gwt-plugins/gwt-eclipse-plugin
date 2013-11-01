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
package com.google.gdt.eclipse.drive.test;

import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;

import org.eclipse.core.internal.preferences.EclipsePreferences;

import java.util.List;
import java.util.Map;

/**
 * An implementation of {@code IEclipsePreferences} using an in-memory hash map to simulate
 * persistently stored preferences. Only String and int preference values are supported.
 */
@SuppressWarnings("restriction") // EclipsePreferences
public class MockEclipsePreferences extends EclipsePreferences {
  private final Map<String, String> simulatedPersistentSettings;
  private final String simulatedAbsolutePath;

  /**
   * Constructs a {@code MockEclipsePreferences} with a default simulated project path whose
   * initial contents are empty.
   */
  public MockEclipsePreferences() {
    this.simulatedPersistentSettings = Maps.newHashMap();
    this.simulatedAbsolutePath = "/project_path";
  }
  
  /**
   * Constructs a {@code MockEclipsePreferences} with a specified simulated project path whose
   * initial contents are copied from a specified {@code Map<String, String>} in which int
   * properties are represented by numeric strings.
   * 
   * @param initialPreferences the specified {@code Map<String, String>}
   * @param simulatedAbsolutePath the specified simulated project path
   */
  public MockEclipsePreferences(
      Map<String, String> initialPreferences, String simulatedAbsolutePath) {
    this.simulatedPersistentSettings = Maps.newHashMap(initialPreferences);
    this.simulatedAbsolutePath = simulatedAbsolutePath;
  }

  @Override public String get(String key, String defaultValue) {
    String result = simulatedPersistentSettings.get(key);
    return result == null ? defaultValue : result;
  }

  @Override public int getInt(String key, int defaultValue) {
    String numericString = simulatedPersistentSettings.get(key);
    if (numericString == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(numericString);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  @Override public String[] keys() {
    List<String> keyList = Lists.newArrayList(simulatedPersistentSettings.keySet());
    String[] result = new String[keyList.size()];
    return keyList.toArray(result);
  }

  @Override public void put(String key, String newValue) {
    simulatedPersistentSettings.put(key, newValue);
  }
  
  @Override public void putInt(String key, int newValue) {
    simulatedPersistentSettings.put(key, Integer.toString(newValue));
  }

  @Override public void remove(String key) {
    simulatedPersistentSettings.remove(key);
  }
  
  @Override public String absolutePath() {
    return simulatedAbsolutePath;
  }

  /**
   * @return a {@code Map<String, String>} representing the contents of this
   * {@code MockEclipsePreferences}, with int values represented by numeric strings.
   */
  public Map<String, String> getStringMap() {
    return simulatedPersistentSettings;
  }
}