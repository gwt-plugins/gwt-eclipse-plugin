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

import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Provides helper methods for Eclipse-related (as a product) tasks.
 */
public class EclipseUtilities {

  private static final Version ECLIPSE_3_5 = new Version("3.5.0");

  public static Version getEclipseVersion() {
    return new Version(
        (String) ResourcesPlugin.getPlugin().getBundle().getHeaders().get(
            Constants.BUNDLE_VERSION));
  }

  public static boolean inEclipse33() {
    Version eclipseVersion = getEclipseVersion();
    return (eclipseVersion.getMajor() == 3 && eclipseVersion.getMinor() == 3);
  }

  public static boolean isAtLeastEclipse35() {
    Version eclipseVersion = getEclipseVersion();
    return (eclipseVersion.compareTo(ECLIPSE_3_5) >= 0);
  }
}
