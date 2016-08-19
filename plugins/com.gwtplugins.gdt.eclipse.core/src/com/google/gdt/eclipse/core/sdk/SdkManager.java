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
package com.google.gdt.eclipse.core.sdk;

import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.sdk.SdkSetSerializer.SdkSerializationException;
import com.google.gdt.eclipse.core.sdk.SdkSortComparator.SortBy;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.Collections;
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

  /**
   * Helper class that assists in processing {@link #SdkManager(SdkUpdateEvent)}
   * s.
   */
  public class SdkUpdateEventProcessor {
    private T newDefaultSdk = null;
    private List<T> addedSdkList = new ArrayList<T>();

    /**
     * Iterates through all of the sdkUpdateEvents and checks to see if a new
     * default SDK was chosen. If so, this value is recorded.
     *
     * Also looks for cases where new SDKs were added (which could also
     * correspond to adding a different SDK with the same name after removing
     * it).
     *
     * @param sdkUpdateEvent
     */
    public SdkUpdateEventProcessor(SdkUpdateEvent<T> sdkUpdateEvent) {
      // First, see if we have a new default SDK, and what the names of the
      // added SDKs are
      List<SdkUpdate<T>> sdkUpdates = sdkUpdateEvent.getUpdates();
      for (SdkUpdate<T> sdkUpdate : sdkUpdates) {
        if (sdkUpdate.getType() == SdkUpdate.Type.NEW_DEFAULT) {
          newDefaultSdk = sdkUpdate.getSdk();
        } else if (sdkUpdate.getType() == SdkUpdate.Type.ADDED) {
          addedSdkList.add(sdkUpdate.getSdk());
        }
      }
    }

    /**
     * Given a java project that is using container-based SDKs, determine
     * whether the sdk updates will have caused its SDK to change.
     *
     * If the default SDK has changed and this project is using the default SDK,
     * then the updated SDK is returned.
     *
     * If an SDK has been added with the same name of an existing SDK, and this
     * project is using an SDK with the same name, then the 'new' SDK with the
     * same name is returned.
     *
     * Otherwise, null is returned.
     *
     * @param project the Java project
     */
    public T getUpdatedSdkForProject(IJavaProject project) throws JavaModelException {

      IClasspathEntry entry = ClasspathUtilities.findClasspathEntryContainer(
          project.getRawClasspath(), containerId);

      if (entry == null) {
        return null;
      }

      // There's a new default sdk. Let's see if the project is using a default
      // container.
      if (newDefaultSdk != null) {
        if (SdkClasspathContainer.isDefaultContainerPath(containerId, entry.getPath())) {
          // The project is using a default container, and there's a new
          // defalt sdk. Return it.
          return newDefaultSdk;
        }
      }

      T containerSdk = findSdkForPath(entry.getPath());
      if (containerSdk != null) {
        for (T addedSdk : addedSdkList) {
          if (addedSdk.getName().equals(containerSdk.getName())) {
            /*
             * we found an sdk that was added which has a name that matches a
             * registered sdk that this project is using. return it.
             */
            return addedSdk;
          }
        }
      }

      return null;
    }
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
        SdkSetSerializer.deserialize(backingPreferences, sdkSetCache, sdkFactory);
      } catch (SdkSerializationException e) {
        CorePluginLog.logError(e);
      }
    }

    // Return a copy so that our version is not modified
    return new SdkSet<T>(sdkSetCache);
  }

  public List<T> getSdksSortedList() {
    SdkSet<T> sdkset = getSdks();

    ArrayList<T> sdklist = new ArrayList<T>();
    if (sdkset != null && !sdkset.isEmpty()) {
      sdklist = new ArrayList<T>(sdkset);
    }

    // Sort the sdks list by version
    SdkSortComparator sdkSortComparator = new SdkSortComparator(SortBy.VERSION);
    Collections.sort(sdklist, sdkSortComparator);

    return sdklist;
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

  protected List<SdkUpdate<T>> computeSdkUpdates(SdkSet<T> oldSdkSet, SdkSet<T> newSdkSet) {
    List<SdkUpdate<T>> updates = new ArrayList<SdkUpdate<T>>();

    T oldDefaultSdk = oldSdkSet.getDefault();
    T newDefaultSdk = newSdkSet.getDefault();
    if (oldDefaultSdk != newDefaultSdk) {
      if (oldDefaultSdk == null || oldDefaultSdk != null && !oldDefaultSdk.equals(newDefaultSdk)) {
        // If the new default sdk is null, the new sdk set had better be empty
        assert (newDefaultSdk != null || (newDefaultSdk == null && newSdkSet.isEmpty()));
        updates.add(new SdkUpdate<T>(newDefaultSdk, SdkUpdate.Type.NEW_DEFAULT));
      }
    }

    // An SdkSet is keyed by name, but we want to use full equality comparisons
    // here to deal with cases where names may have been reused.

    List<T> addedSdks = new ArrayList<T>(newSdkSet);

    /*
     * NOTE: Using addedSdks.removeAll(oldSdkSet) will not work as you expect!
     * Under the hood, this calls oldSdkSet.contains(addedSdkItem), which will
     * perform a name-only equality comparison (look at the implementation of
     * SdkSet). If the contains check went the other way
     * (addedSdks.contains(oldSdkSetItem)), we'd be good.
     */
    for (Sdk sdk : oldSdkSet) {
      addedSdks.remove(sdk);
    }
    appendSdkUpdates(addedSdks, SdkUpdate.Type.ADDED, updates);

    List<T> removedSdks = new ArrayList<T>(oldSdkSet);
    // See cautionary note above about not using
    // removedSdks.removeAll(newSdkSet)
    for (Sdk sdk : newSdkSet) {
      removedSdks.remove(sdk);
    }
    appendSdkUpdates(removedSdks, SdkUpdate.Type.REMOVED, updates);

    return updates;
  }

  protected void fireSdkUpdateEvent(List<SdkUpdate<T>> updates) throws CoreException {
    SdkUpdateEvent<T> event = new SdkUpdateEvent<T>(updates);

    for (SdkUpdateListener<T> listener : listeners) {
      if (listener != null) {
        listener.onSdkUpdate(event);
      }
    }
  }
}
