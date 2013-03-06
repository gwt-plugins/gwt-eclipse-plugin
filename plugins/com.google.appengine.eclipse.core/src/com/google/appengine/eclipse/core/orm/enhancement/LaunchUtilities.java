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
package com.google.appengine.eclipse.core.orm.enhancement;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Series of utilities used for launching processes.
 */
public class LaunchUtilities implements IJavaLaunchConfigurationConstants {
  /**
   * Utility method to obtain the classpath entries for the given
   * <code>IJavaProject</code>.
   * 
   * @param javaProject The <code>IJavaProject</code> the classpath should be
   *          obtained for
   * @return A <code>List</code> of valid classpath entries as
   *         <code>String</code> values
   * @throws CoreException
   */
  public static List<String> getDefaultClasspath(IJavaProject javaProject)
      throws CoreException {
    List<String> classpath = new ArrayList<String>();

    IRuntimeClasspathEntry outputEntry = JavaRuntime.newDefaultProjectClasspathEntry(javaProject);
    IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry = JavaRuntime.resolveRuntimeClasspathEntry(
        outputEntry, javaProject);
    for (IRuntimeClasspathEntry resolved : resolveRuntimeClasspathEntry) {
      String location = resolved.getLocation();
      if (location != null) {
        classpath.add(location);
      }
    }

    return classpath;
  }
}
