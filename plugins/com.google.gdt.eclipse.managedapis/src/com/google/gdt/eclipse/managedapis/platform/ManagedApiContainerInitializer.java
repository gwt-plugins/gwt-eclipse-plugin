/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis.platform;

import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiProjectImpl;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * TODO: doc me.
 */
public class ManagedApiContainerInitializer
    extends ClasspathContainerInitializer {

  @Override
  public void initialize(IPath containerPath, IJavaProject project)
      throws CoreException {
    if (ManagedApiPlugin.isManagedApiClasspathContainer(containerPath)) {
      ManagedApiProject managedApiProject = ManagedApiProjectImpl
        .getManagedApiProject(project);
      if (managedApiProject instanceof ManagedApiProjectImpl) {
        ((ManagedApiProjectImpl) managedApiProject)
          .initializeProjectWithManagedApis();
      }
      ManagedApiContainer container = new ManagedApiContainer(
          managedApiProject, containerPath);
      if (container.getManagedApi() != null) {
        JavaCore.setClasspathContainer(containerPath,
            new IJavaProject[] {project}, new IClasspathContainer[] {container},
            null);
        managedApiProject.notifyManagedApisRefreshed(new ManagedApi[] {
            container.getManagedApi()});
      } else {
        // This case exists to handle failures to load the ManagedApi. This
        // typically happens during the import process (File -> Import
        // Project...). The import process calls the Initializer for the
        // container with the API_CONTAINER_PATH_ID before the file system
        // syncs. As a result we can not read the JAR files, descriptors or
        // anything else. As a result, we return null in order to re-evaluate on
        // the next call to query this container (per setClasspathContainer). As
        // a result, we can defer evaluation until the resources become
        // available.
        JavaCore.setClasspathContainer(containerPath,
            new IJavaProject[] {project}, new IClasspathContainer[] {null},
            null);
      }
    }
  }
}
