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
package com.google.gdt.eclipse.maven.enhancement;

import com.google.appengine.eclipse.core.orm.enhancement.EnhancerJob.IEnhancerJobCpFinder;
import com.google.appengine.eclipse.core.orm.enhancement.LaunchUtilities;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.maven.Activator;
import com.google.gdt.eclipse.maven.MavenUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * This class constructs a classpath for the datanucleus enhancer in the Maven
 * context.
 */
public class MavenEnhancerClasspathBuilder implements IEnhancerJobCpFinder {

  public List<String> buildClasspath(IJavaProject javaProject) {

    try {
      if (!MavenUtils.hasMavenNature(javaProject.getProject())) {
        return null;
      }

      List<String> classpath = LaunchUtilities.getDefaultClasspath(javaProject);

      GaeSdk gaeSdk = GaeSdk.findSdkFor(javaProject);
      if (gaeSdk != null) {
        String toolsJarLocation = gaeSdk.getInstallationPath().append(
            GaeSdk.APPENGINE_TOOLS_API_JAR_PATH).toOSString();
        // Add this to the top of the classpath, just in case they have
        // some other version of appengine-api-tools.jar on their build
        // path (they shouldn't though).
        classpath.add(0, toolsJarLocation);
      }

      return classpath;

    } catch (CoreException ce) {
      Activator.getDefault().getLog().log(
          new Status(IStatus.ERROR, Activator.PLUGIN_ID,
              "Unable to generate the enhancer's classpath for project "
                  + javaProject.getElementName(), ce));
      return null;
    }
  }
}
