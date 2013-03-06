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
package com.google.gdt.eclipse.maven.sdk;

import com.google.gdt.eclipse.maven.MavenUtils;
import com.google.gwt.eclipse.core.runtime.GWTRuntime.IProjectBoundSdkFactory;
import com.google.gwt.eclipse.core.runtime.GWTRuntime.ProjectBoundSdk;

import org.eclipse.jdt.core.IJavaProject;

/**
 * A factory for creating project-bound, Maven-based GWT Runtimes.
 */
public class GWTMavenSdkFactory implements IProjectBoundSdkFactory {

  public ProjectBoundSdk newInstance(IJavaProject javaProject) {
    if (!MavenUtils.hasMavenNature(javaProject.getProject())) {
      return null;
    }

    return new GWTMavenRuntime(javaProject);
  }

}
