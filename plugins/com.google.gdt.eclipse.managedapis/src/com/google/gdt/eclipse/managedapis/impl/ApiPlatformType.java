/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.managedapis.impl;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Defines the platform types for Managed APIs.
 */
public class ApiPlatformType {

  public static final ApiPlatformType ANDROID = new ApiPlatformType("android");
  public static final ApiPlatformType ANDROID_2 = new ApiPlatformType(
      "android", "android2");
  public static final ApiPlatformType ANDROID_3 = new ApiPlatformType(
      "android", "android3");
  public static final ApiPlatformType APPENGINE = new ApiPlatformType(
      "appengine");

  private static final String ANDROID_GENERIC_CLASSPATH_CONTAINER_PREFIX = "Android";
  private static final String ANDROID_2_CLASSPATH_CONTAINER = "Android 2";
  private static final String ANDROID_3_CLASSPATH_CONTAINER = "Android 3";
  private static final String ANDROID_4_CLASSPATH_CONTAINER = "Android 4";

  /**
   * Given an android project, determine the specific version of the Android platform that the
   * project is using (based on its classpath container).
   * 
   * Returns <code>null</code> if <code>androidProject</code> is <code>null</code> or it is not
   * open. Also returns <code>null</code> if the project does not have a Android Classpath
   * Container.
   * 
   * Returns {@link #ANDROID_2} if the classpath container corresponds to "Android 2".
   * 
   * Returns {@link #ANDROID_3} if the classpath container corresponds to "Android 3" or
   * "Android 4".
   * 
   * Returns {@link #ANDROID} if the classpath container has some other version of Android.
   * 
   * @param androidProject
   * @return
   * @throws JavaModelException
   */
  public static ApiPlatformType getAndroidPlatformType(IProject androidProject)
      throws JavaModelException {
    if (androidProject == null || !androidProject.isOpen()) {
      return null;
    }
    IJavaProject androidJavaProject = JavaCore.create(androidProject);
    List<IClasspathEntry> rawClasspathList = new ArrayList<IClasspathEntry>();
    rawClasspathList.addAll(Arrays.asList(androidJavaProject.getRawClasspath()));
    for (IClasspathEntry e : rawClasspathList) {
      if (e.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
        continue;
      }
      IClasspathContainer c = JavaCore.getClasspathContainer(e.getPath(),
          androidJavaProject);
      if (c == null) {
        // cp container could be null
        continue;
      }
      if (c.getDescription().contains(ANDROID_2_CLASSPATH_CONTAINER)) {
        return ApiPlatformType.ANDROID_2;
      } else if (c.getDescription().contains(ANDROID_3_CLASSPATH_CONTAINER)
          || c.getDescription().contains(ANDROID_4_CLASSPATH_CONTAINER)) {
        return ApiPlatformType.ANDROID_3;
      } else if (c.getDescription().contains(
          ANDROID_GENERIC_CLASSPATH_CONTAINER_PREFIX)) {
        /*
         * Used to match any Android classpath container after Android 4+. Note
         * that this would also match the "Android Dependencies" classpath
         * container, but the Android Dependencies container should always be
         * with the Android X.Y classpath container.
         */
        return ApiPlatformType.ANDROID;
      }
    }
    return null;
  }

  private final List<String> idList;

  private ApiPlatformType(String... ids) {
    idList = Collections.unmodifiableList(Arrays.asList(ids));
  }

  /**
   * Returns <code>true</code> if any of the identifiers are an exact match for
   * the identifiers for this platform type. <code>'*'</code> is a special
   * identifier that will match any platform type.
   */
  public boolean matches(List<String> identifiers) {
    for (String id : identifiers) {
      if (matches(id)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns <code>true</code> if the identifier is an exact match for the
   * identifiers for this platform type. <code>'*'</code> is a special
   * identifier that will match any platform type.
   */
  public boolean matches(String identifier) {
    // '*' is a special identifier to indicate that any platform is eligible
    if ("*".equals(identifier)) {
      return true;
    }

    for (String id : idList) {
      if (id.equals(identifier)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the identifiers that correspond to this platform type.
   */
  public List<String> getIdentifiers() {
    return idList;
  }
}
