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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Utilities for reading/writing project properties.
 */
public final class PropertiesUtilities {

  /**
   * Returns a list of IPath's deserialized from a raw property value string. If
   * the string is null or 0-length, an empty list is returned;
   */
  public static List<IPath> deserializePaths(String rawPropertyValue) {
    List<IPath> paths = new ArrayList<IPath>();

    if (rawPropertyValue != null && rawPropertyValue.length() > 0) {
      String[] patternStrings = rawPropertyValue.split("\\|");
      for (String patternString : patternStrings) {
        paths.add(new Path(patternString));
      }
    }

    return paths;
  }

  /**
   * Returns a list of strings deserialized from a raw property value string. If
   * the string is null or 0-length, an empty list is returned;
   */
  public static List<String> deserializeStrings(String rawPropertyValue) {
    List<String> strings = new ArrayList<String>();

    if (rawPropertyValue != null && rawPropertyValue.length() > 0) {
      return Arrays.asList(rawPropertyValue.split("\\|"));
    }

    return strings;
  }

  public static String serializePaths(List<IPath> paths) {
    return join(paths, "|");
  }

  /**
   * Serializes a set of strings into one property value. Uses the pipe symbol
   * as a splitter, so the strings must not themselves contain a pipe.
   */
  public static String serializeStrings(List<String> strings) {
    return join(strings, "|");
  }

  // TODO: remove this and defer to the one in StringUtilities
  private static String join(Iterable<?> items, String delimiter) {
    StringBuffer buffer = new StringBuffer();
    Iterator<?> iter = items.iterator();
    if (iter.hasNext()) {
      buffer.append(iter.next().toString());
      while (iter.hasNext()) {
        buffer.append(delimiter);
        buffer.append(iter.next().toString());
      }
    }
    return buffer.toString();
  }

}
