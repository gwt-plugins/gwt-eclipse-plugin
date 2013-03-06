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
package com.google.gdt.eclipse.core.sdk;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.sdk.SdkSetSerializer.SdkSerializationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a set of {@link Sdk}s and fires off events whenever the set is
 * changed.
 * 
 * @param <T> the type of {@link Sdk} being managed
 */
public class SdkManager<T extends Sdk> {
  /**
   * 
   * @param <T> the type of {@link Sdk} associated with the update
   */
  public static class SdkUpdate<T extends Sdk> {
    /**
     * Type of SDK update.
     */
    public enum Type {
      ADDED, REMOVED, NEW_DEFAULT
    }

    private final T sdk;
    private final Type type;

    public SdkUpdate(T sdk, Type type) {
      this.sdk = sdk;
      this.type = type;
    }

    /**
     * @return the sdk
     */
    public T getSdk() {
      return sdk;
    }

    /**
     * @return the sdk type
     */
    public Type getType() {
      return type;
    }

    @Override
    public String toString() {
      return type.toString() + " " + sdk.getName();
    }
  }

  /**
   * 
   * @param <T> the type of {@link Sdk} associated with this update event
   */
  public static class SdkUpdateEvent<T extends Sdk> {
    private final List<SdkUpdate<T>> updates;

    public SdkUpdateEvent(List<SdkUpdate<T>> updates) {
      assert (updates != null);

      this.updates = updates;
    }

    public List<SdkUpdate<T>> getUpdates() {
      return updates;
    }

    @Override
    public String toString() {
      return updates.toString();
    }
  }
  /**
   * 
   * @param <T> the type of {@link Sdk} associated with this listener
   */
  public interface SdkUpdateListener<T extends Sdk> {
    void onSdkUpdate(SdkUpdateEvent<T> sdkUpdateEvent) throws CoreException;
  }

  private final String containerId;

  private final List<SdkUpdateListener<T>> listeners = new ArrayList<SdkUpdateListener<T>>();

  private final IEclipsePreferences backingPreferences;

  private final SdkFactory<T> sdkFactory;

  private SdkSet<T> sdkSetCache;

  public SdkManager(String containerId, IEclipsePreferences backingPreferences,
      SdkFactory<T> sdkFactory) {
    assert (containerId != null);
    this.containerId = containerId;
    this.backingPreferences = backingPreferences;
    this.sdkFactory = sdkFactory;
  }

  public void addSdkUpdateListener(SdkUpdateListener<T> sdkUpdateListener) {
    listeners.add(sdkUpdateListener);
  }

  public T findSdkForPath(IPath path) {
    if (path.isEmpty()) {
      return null;
    }

    SdkSet<T> sdks = getSdks();
    if (SdkClasspathContainer.isDefaultContainerPath(containerId, path)) {
      // If no specific SDK name was provided return the default
      return sdks.getDefault();
    }

    // Segment 0 is the container ID, segment 1 is the sdk name
    return sdks.findSdk(path.segment(1));
  }

  public SdkSet<T> getSdks() {
    if (sdkSetCache == null) {
      sdkSetCache = new SdkSet<T>();
      try {
        SdkSetSerializer.deserialize(backingPreferences, sdkSetCache,
            sdkFactory);
      } catch (SdkSerializationException e) {
        CorePluginLog.logError(e);
      }
    }

    // Return a copy so that our version is not modified
    return new SdkSet<T>(sdkSetCache);
  }

  public void removeSdkUpdateListener(SdkUpdateListener<T> sdkUpdateListener) {
    listeners.remove(sdkUpdateListener);
  }

  public void setSdks(SdkSet<T> newSdkSet) throws CoreException {
    // Get the old Sdks before saving the new changes
    SdkSet<T> oldSdkSet = getSdks();

    try {
      // Copy their version to avoid conflicts
      sdkSetCache = new SdkSet<T>(newSdkSet);
      SdkSetSerializer.serialize(backingPreferences, newSdkSet);
    } catch (SdkSerializationException e) {
      CorePluginLog.logError(e);
    }

    List<SdkUpdate<T>> updates = computeSdkUpdates(oldSdkSet, newSdkSet);
    fireSdkUpdateEvent(updates);
  }

  protected void appendSdkUpdates(List<T> sdks, SdkUpdate.Type updateType,
      List<SdkUpdate<T>> updates) {
    for (T sdk : sdks) {
      updates.add(new SdkUpdate<T>(sdk, updateType));
    }
  }

  protected List<SdkUpdate<T>> computeSdkUpdates(SdkSet<T> oldSdkSet,
      SdkSet<T> newSdkSet) {
    List<SdkUpdate<T>> updates = new ArrayList<SdkUpdate<T>>();

    T oldDefaultSdk = oldSdkSet.getDefault();
    T newDefaultSdk = newSdkSet.getDefault();
    if (oldDefaultSdk != newDefaultSdk) {
      if (oldDefaultSdk == null || oldDefaultSdk != null
          && !oldDefaultSdk.equals(newDefaultSdk)) {
        // If the new default sdk is null, the new sdk set had better be empty
        assert (newDefaultSdk != null || (newDefaultSdk == null && newSdkSet.isEmpty()));
        updates.add(new SdkUpdate<T>(newDefaultSdk, SdkUpdate.Type.NEW_DEFAULT));
      }
    }

    // An SdkSet is keyed by name, but we want to use full equality comparisons
    // here to deal with cases where names may have been reused.

    List<T> addedSdks = new ArrayList<T>(newSdkSet);
    addedSdks.removeAll(oldSdkSet);
    appendSdkUpdates(addedSdks, SdkUpdate.Type.ADDED, updates);

    List<T> removedSdks = new ArrayList<T>(oldSdkSet);
    removedSdks.removeAll(newSdkSet);
    appendSdkUpdates(removedSdks, SdkUpdate.Type.REMOVED, updates);

    return updates;
  }

  protected void fireSdkUpdateEvent(List<SdkUpdate<T>> updates)
      throws CoreException {
    SdkUpdateEvent<T> event = new SdkUpdateEvent<T>(updates);

    for (SdkUpdateListener<T> listener : listeners) {
      if (listener != null) {
        listener.onSdkUpdate(event);
      }
    }
  }
}

