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
import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.StringUtilities;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Utility methods for dealing with {@link Sdk}s.
 */
public class SdkUtils {

  /**
   * Computes the maximum version number for a given number of projects.
   */
  public abstract static class MaxSdkVersionComputer {
    public String computeMaxSdkVersion(IJavaProject[] projects) {
      String maxVersion = null;
      try {
        for (IJavaProject project : projects) {
          if (!project.getProject().isAccessible()) {
            // Skip projects that are not accessible
            continue;
          }

          Sdk sdk = doFindSdk(project);
          if (sdk != null) {
            String currentMaxVersion = maxVersion;
            String version = sdk.getVersion();

            if (isInternal(version)) {
              // Internal SDKs are not updateable.
              continue;
            }

            if (currentMaxVersion == null) {
              currentMaxVersion = version;
            } else if (version != null) {
              try {
                if (SdkUtils.compareVersionStrings(currentMaxVersion, version) < 0) {
                  currentMaxVersion = version;
                }
              } catch (NumberFormatException e) {
                CorePluginLog.logError(e, "Could not compare '"
                    + currentMaxVersion + "' to '" + version
                    + "', ignoring this version");
              }
            }

            if (currentMaxVersion != null) {
              maxVersion = currentMaxVersion;
            }
          }
        }
      } catch (JavaModelException e) {
        // Log the error and ignore it
        CorePluginLog.logError(e);
        maxVersion = null;
      }

      return maxVersion;
    }

    /**
     * Returns the {@link Sdk} that is bound to the {@link IJavaProject} or
     * <code>null</code> if none.
     */
    public abstract Sdk doFindSdk(IJavaProject project)
        throws JavaModelException;
  }

  public static final String INTERNAL_VERSION_PREFIX = "0.0.";
  public static final String INTERNAL_VERSION_SUFFIX = "999";

  /**
   * In some cases, the 3rd component of the version string can contain both
   * digits and non-digits (e.g., 2.1.0M1, 2.1.0-m1); this is incompatible with
   * what GPE expects for a SDK version string.
   */
  public static String cleanupVersion(String versionStr) {
    if (StringUtilities.isEmpty(versionStr)) {
      return versionStr;
    }

    versionStr = toOSGICompatibleVersionString(versionStr);
    
    String[] versionComponents = versionStr.split("\\.");

    if (versionComponents.length != 3) {
      return versionStr;
    }

    String lastComponent = versionComponents[2];

    int nonDigitIndex = -1;
    for (int i = 0, len = lastComponent.length(); i < len; i++) {
      if (!Character.isDigit(lastComponent.charAt(i))) {
        nonDigitIndex = i;
        break;
      }
    }

    if (nonDigitIndex == -1) {
      return versionStr;
    }

    // Substitute zero in for the non-char 3rd component (this really shouldn't
    // happen)
    if (nonDigitIndex == 0) {
      return versionComponents[0] + '.' + versionComponents[1] + ".0." + lastComponent;
    }

    // Add 4th component
    return versionComponents[0] + '.' + versionComponents[1] + '.'
        + lastComponent.substring(0, nonDigitIndex) + '.' + lastComponent.substring(nonDigitIndex);
  }

  /**
   * Compares two version strings for order. Returns a negative integer, zero,
   * or a positive integer if the first argument is less than, equal to, or
   * greater than the second.
   * <p>
   * This method handles version strings that include RC or MS, treating them as
   * MS < RC < final version.
   *
   * @param version1
   * @param version2
   * @return a negative integer, zero, or a positive integer if the first
   *         argument is less than, equal to, or greater than the second
   * @throws NumberFormatException
   * @throws NullPointerException if either version string is null
   */
  public static int compareVersionStrings(String version1, String version2)
      throws NumberFormatException {
    version1 = SdkUtils.cleanupVersion(version1);
    version2 = SdkUtils.cleanupVersion(version2);
    String splitRegex = "[._-]";
    String[] array1 = version1.split(splitRegex);
    String[] array2 = version2.split(splitRegex);

    int n = Math.max(array1.length, array2.length);
    for (int i = 0; i < n; i++) {
      int i1 = parseVersionComponent(array1, i);
      int i2 = parseVersionComponent(array2, i);
      if (i1 != i2) {
        return i1 - i2;
      }
    }
    return 0;
  }

  /**
   * Returns an {@link Sdk} with a matching installation path from the
   * {@link SdkSet} or <code>null</code> if none could be found.
   */
  public static <T extends Sdk> T findSdkForInstallationPath(
      SdkSet<T> sdkSet, IPath installationPath) {
    for (T sdk : sdkSet) {
      if (JavaUtilities.equalsWithNullCheck(sdk.getInstallationPath(), installationPath)) {
        return sdk;
      }
    }

    return null;
  }

  /**
   * Generates a name that is unique to a given {@link SdkSet} based on the name
   * prefix.
   *
   * @param namePrefix name prefix for the generated name
   * @param sdks {@link SdkSet} for within which a unique name is needed
   *
   * @return name that is unique to a given {@link SdkSet} based on the name
   *         prefix
   */
  public static String generateUniqueSdkNameFrom(String namePrefix, SdkSet<? extends Sdk> sdks) {
    String generatedName = namePrefix;
    int i = 0;
    while (true) {
      if (sdks.contains(generatedName)) {
        generatedName = namePrefix + " (" + ++i + ")";
      } else {
        break;
      }
    }

    return generatedName;
  }

  /**
   * Checks whether a given SDK version is internal.
   */
  public static boolean isInternal(String version) {
    return version != null
        && (version.startsWith(INTERNAL_VERSION_PREFIX) || version.endsWith(INTERNAL_VERSION_SUFFIX));
  }

  /**
   * Returns a negative numeric representation of the prerelease version string.
   * This is negative so all prereleases are less than the corresponding
   * release.
   *
   * @param versionString must start with either "rc" or "ms" followed by a
   *        number that is less than 1000
   * @return a negative numeric representation such that r(rc1) < r(rc10) and
   *         r(ms__) < r(rc1)
   * @throws NumberFormatException
   */
  private static int parsePrereleaseVersionString(String versionString)
      throws NumberFormatException {
    String prereleaseType = versionString.substring(0, 2);
    int version = Integer.parseInt(versionString.substring(2));
    // Since it must be negative, (1000 - version) so rc1 < rc10
    int negRepresentation = -(1000 - version);

    if ("ms".equals(prereleaseType)) {
      // rc1 > ms99
      negRepresentation -= 1000;
    }

    return negRepresentation;
  }

  private static int parseVersionComponent(String[] versionComponents, int componentIndex)
      throws NumberFormatException {
    if (componentIndex < versionComponents.length) {
      String component = versionComponents[componentIndex].trim().toLowerCase();
      if (component.length() > 0) {
        if (component.startsWith("rc") || component.startsWith("ms")) {
          return parsePrereleaseVersionString(component);
        } else {
          return Integer.parseInt(component);
        }
      }
    }

    return 0;
  }

  /**
   * Return a string where the modifier is separated from the version number by
   * a '.' instead of a '-'. OSGI strings want a '.' to separate the modifier
   * from the rest of the version number.
   */
  private static String toOSGICompatibleVersionString(String versionString) {
    /*
     * Note this method does try to parse out the version number, to ensure that
     * the replacement only takes place at the start of the qualifier.
     */
    return versionString.replaceFirst("-", ".");
  }



  private SdkUtils() {
  }
}
