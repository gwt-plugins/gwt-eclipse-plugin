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
package com.google.gdt.eclipse.core;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Mock class for eclipse preferences. Nothing is saved and only default values
 * are returned.
 */
public class MockEclipsePreferences implements IEclipsePreferences {
  public String absolutePath() {
    return null;
  }

  public void accept(IPreferenceNodeVisitor visitor)
      throws BackingStoreException {
  }

  public void addNodeChangeListener(INodeChangeListener listener) {
  }

  public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
  }

  public String[] childrenNames() throws BackingStoreException {
    return null;
  }

  public void clear() throws BackingStoreException {
  }

  public void flush() throws BackingStoreException {
  }

  public String get(String key, String def) {
    return def;
  }

  public boolean getBoolean(String key, boolean def) {
    return def;
  }

  public byte[] getByteArray(String key, byte[] def) {
    return def;
  }

  public double getDouble(String key, double def) {
    return def;
  }

  public float getFloat(String key, float def) {
    return def;
  }

  public int getInt(String key, int def) {
    return def;
  }

  public long getLong(String key, long def) {
    return def;
  }

  public String[] keys() throws BackingStoreException {
    return null;
  }

  public String name() {
    return null;
  }

  public Preferences node(String path) {
    return null;
  }

  public boolean nodeExists(String pathName) throws BackingStoreException {
    return false;
  }

  public Preferences parent() {
    return null;
  }

  public void put(String key, String value) {
  }

  public void putBoolean(String key, boolean value) {
  }

  public void putByteArray(String key, byte[] value) {
  }

  public void putDouble(String key, double value) {
  }

  public void putFloat(String key, float value) {
  }

  public void putInt(String key, int value) {
  }

  public void putLong(String key, long value) {
  }

  public void remove(String key) {
  }

  public void removeNode() throws BackingStoreException {
  }

  public void removeNodeChangeListener(INodeChangeListener listener) {
  }

  public void removePreferenceChangeListener(IPreferenceChangeListener listener) {
  }

  public void sync() throws BackingStoreException {
  }
}