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
package com.google.gdt.eclipse.core.deploy;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import java.io.OutputStream;

/**
 * 
 */
public interface DeploymentParticipant {
  void predeploy(IJavaProject javaProject, DeploymentSet deploymentSet, IPath warLocation, OutputStream consoleOutputStream,
      IProgressMonitor monitor) throws CoreException;
  
  void deploySucceeded(IJavaProject javaProject, DeploymentSet deploymentSet, IPath warLocation, OutputStream consoleOutputStream,
      IProgressMonitor monitor) throws CoreException;
}
