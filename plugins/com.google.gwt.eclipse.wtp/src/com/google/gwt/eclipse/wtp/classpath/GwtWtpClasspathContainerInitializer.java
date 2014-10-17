/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gwt.eclipse.wtp.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * An initializer for {@link GwtWtpClasspathContainer}.
 */
public final class GwtWtpClasspathContainerInitializer extends ClasspathContainerInitializer {
  @Override
  public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
    IClasspathContainer sdkClasspathContainer =
        new GwtWtpClasspathContainer(containerPath, project);
    IClasspathContainer[] classpathContainer = new IClasspathContainer[] {sdkClasspathContainer};
    IJavaProject[] javaProject = new IJavaProject[] {project};

    JavaCore.setClasspathContainer(containerPath, javaProject, classpathContainer, null);
  }
}
