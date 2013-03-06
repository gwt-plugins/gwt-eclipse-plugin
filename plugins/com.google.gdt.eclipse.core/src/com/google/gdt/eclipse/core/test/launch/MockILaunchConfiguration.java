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
package com.google.gdt.eclipse.core.test.launch;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mock implementation of {@link ILaunchConfiguration} which returns nulls and
 * falses except for:
 * <ul>
 * <li>getName(): returns toString()
 * </ul>
 */
@SuppressWarnings("unchecked")
public class MockILaunchConfiguration implements ILaunchConfiguration {

  public boolean contentsEqual(ILaunchConfiguration configuration) {
    return false;
  }

  public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
    return null;
  }

  public void delete() throws CoreException {
  }

  public boolean exists() {
    return false;
  }

  public Object getAdapter(Class adapter) {
    return null;
  }

  public boolean getAttribute(String attributeName, boolean defaultValue) throws CoreException {
    return false;
  }

  public int getAttribute(String attributeName, int defaultValue) throws CoreException {
    return 0;
  }

  public List getAttribute(String attributeName, List defaultValue) throws CoreException {
    return null;
  }

  public Map getAttribute(String attributeName, Map defaultValue) throws CoreException {
    return null;
  }

  public Set getAttribute(String attributeName, Set defaultValue) throws CoreException {
    return null;
  }

  public String getAttribute(String attributeName, String defaultValue) throws CoreException {
    return null;
  }

  public Map getAttributes() throws CoreException {
    return null;
  }

  public String getCategory() throws CoreException {
    return null;
  }

  public IFile getFile() {
    return null;
  }

  public IPath getLocation() {
    return null;
  }

  public IResource[] getMappedResources() throws CoreException {
    return null;
  }

  public String getMemento() throws CoreException {
    return null;
  }

  public Set getModes() throws CoreException {
    return null;
  }

  public String getName() {
    return toString();
  }

  public ILaunchDelegate getPreferredDelegate(Set modes) throws CoreException {
    return null;
  }

  public ILaunchConfigurationType getType() throws CoreException {
    return new ILaunchConfigurationType() {
      private static final String ID = "MockLaunchConfigurationType";

      public Object getAdapter(Class adapter) {
        return null;
      }

      public String getAttribute(String attributeName) {
        return null;
      }

      public String getCategory() {
        return null;
      }

      public String getContributorName() {
        return null;
      }

      public ILaunchConfigurationDelegate getDelegate() throws CoreException {
        return null;
      }

      public ILaunchConfigurationDelegate getDelegate(String mode)
          throws CoreException {
        return null;
      }

      public ILaunchDelegate[] getDelegates(Set modes) throws CoreException {
        return null;
      }

      public String getIdentifier() {
        return ID;
      }

      public String getName() {
        return null;
      }

      public String getPluginIdentifier() {
        return null;
      }

      public ILaunchDelegate getPreferredDelegate(Set modes)
          throws CoreException {
        return null;
      }

      public String getSourceLocatorId() {
        return null;
      }

      public ISourcePathComputer getSourcePathComputer() {
        return null;
      }

      public Set getSupportedModeCombinations() {
        return null;
      }

      public Set getSupportedModes() {
        return null;
      }

      public boolean isPublic() {
        return false;
      }

      public ILaunchConfigurationWorkingCopy newInstance(IContainer container,
          String name) throws CoreException {
        return null;
      }

      public void setPreferredDelegate(Set modes, ILaunchDelegate delegate)
          throws CoreException {
      }

      public boolean supportsMode(String mode) {
        return false;
      }

      public boolean supportsModeCombination(Set modes) {
        return false;
      }
    };
  }

  public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
    return null;
  }

  public boolean hasAttribute(String attributeName) throws CoreException {
    return false;
  }

  public boolean isLocal() {
    return false;
  }

  public boolean isMigrationCandidate() throws CoreException {
    return false;
  }

  public boolean isReadOnly() {
    return false;
  }

  public boolean isWorkingCopy() {
    return false;
  }

  public ILaunch launch(String mode, IProgressMonitor monitor) throws CoreException {
    return null;
  }

  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build) throws CoreException {
    return null;
  }

  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register)
      throws CoreException {
    return null;
  }

  public void migrate() throws CoreException {
  }

  public boolean supportsMode(String mode) throws CoreException {
    return false;
  }
}
