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

import java.util.Collection;
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

  @Override
  public boolean contentsEqual(ILaunchConfiguration configuration) {
    return false;
  }

  @Override
  public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
    return null;
  }

  @Override
  public void delete() throws CoreException {
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public Object getAdapter(Class adapter) {
    return null;
  }

  @Override
  public boolean getAttribute(String attributeName, boolean defaultValue) throws CoreException {
    return false;
  }

  @Override
  public int getAttribute(String attributeName, int defaultValue) throws CoreException {
    return 0;
  }

  @Override
  public List getAttribute(String attributeName, List defaultValue) throws CoreException {
    return null;
  }

  @Override
  public Map getAttribute(String attributeName, Map defaultValue) throws CoreException {
    return null;
  }

  @Override
  public Set getAttribute(String attributeName, Set defaultValue) throws CoreException {
    return null;
  }

  @Override
  public String getAttribute(String attributeName, String defaultValue) throws CoreException {
    return null;
  }

  @Override
  public Map getAttributes() throws CoreException {
    return null;
  }

  @Override
  public String getCategory() throws CoreException {
    return null;
  }

  @Override
  public IFile getFile() {
    return null;
  }

  @Override
  public IPath getLocation() {
    return null;
  }

  @Override
  public IResource[] getMappedResources() throws CoreException {
    return null;
  }

  @Override
  public String getMemento() throws CoreException {
    return null;
  }

  @Override
  public Set getModes() throws CoreException {
    return null;
  }

  @Override
  public String getName() {
    return toString();
  }

  @Override
  public ILaunchDelegate getPreferredDelegate(Set modes) throws CoreException {
    return null;
  }

  @Override
  public ILaunchConfigurationType getType() throws CoreException {
    return new ILaunchConfigurationType() {
      private static final String ID = "MockLaunchConfigurationType";

      @Override
      public Object getAdapter(Class adapter) {
        return null;
      }

      @Override
      public String getAttribute(String attributeName) {
        return null;
      }

      @Override
      public String getCategory() {
        return null;
      }

      @Override
      public String getContributorName() {
        return null;
      }

      @Override
      public ILaunchConfigurationDelegate getDelegate() throws CoreException {
        return null;
      }

      @Override
      public ILaunchConfigurationDelegate getDelegate(String mode)          throws CoreException {
        return null;
      }

      @Override
      public ILaunchDelegate[] getDelegates(Set modes) throws CoreException {
        return null;
      }

      @Override
      public String getIdentifier() {
        return ID;
      }

      @Override
      public String getName() {
        return null;
      }

      @Override
      public String getPluginIdentifier() {
        return null;
      }

      @Override
      public ILaunchDelegate getPreferredDelegate(Set modes)          throws CoreException {
        return null;
      }

      @Override
      public String getSourceLocatorId() {
        return null;
      }

      @Override
      public ISourcePathComputer getSourcePathComputer() {
        return null;
      }

      @Override
      public Set getSupportedModeCombinations() {
        return null;
      }

      @Override
      public Set getSupportedModes() {
        return null;
      }

      @Override
      public boolean isPublic() {
        return false;
      }

      @Override
      public ILaunchConfigurationWorkingCopy newInstance(IContainer container,
          String name) throws CoreException {
        return null;
      }

      @Override
      public void setPreferredDelegate(Set modes, ILaunchDelegate delegate)
          throws CoreException {
      }

      @Override
      public boolean supportsMode(String mode) {
        return false;
      }

      @Override
      public boolean supportsModeCombination(Set modes) {
        return false;
      }

      @Override
      public ILaunchConfiguration[] getPrototypes() throws CoreException {
        // TODO(${user}): Auto-generated method stub
        return null;
      }

      @Override
      public ILaunchConfigurationWorkingCopy newPrototypeInstance(IContainer container, String name)
          throws CoreException {
        // TODO(${user}): Auto-generated method stub
        return null;
      }

      @Override
      public boolean supportsPrototypes() {
        // TODO(${user}): Auto-generated method stub
        return false;
      }

      @Override
      public boolean supportsCommandLine() {
        // TODO(${user}): Auto-generated method stub
        return false;
      }

      @Override
      public boolean supportsOutputMerging() {
        // TODO(${user}): Auto-generated method stub
        return false;
      }
    };
  }

  @Override
  public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
    return null;
  }

  @Override
  public boolean hasAttribute(String attributeName) throws CoreException {
    return false;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public boolean isMigrationCandidate() throws CoreException {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public boolean isWorkingCopy() {
    return false;
  }

  @Override
  public ILaunch launch(String mode, IProgressMonitor monitor) throws CoreException {
    return null;
  }

  @Override
  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build) throws CoreException {
    return null;
  }

  @Override
  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register)
      throws CoreException {
    return null;
  }

  @Override
  public void migrate() throws CoreException {
  }

  @Override
  public boolean supportsMode(String mode) throws CoreException {
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#delete(int)
   */
  @Override
  public void delete(int flag) throws CoreException {
    // TODO(${user}): Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getPrototype()
   */
  @Override
  public ILaunchConfiguration getPrototype() throws CoreException {
    // TODO(${user}): Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#isAttributeModified(java.lang.String)
   */
  @Override
  public boolean isAttributeModified(String attribute) throws CoreException {
    // TODO(${user}): Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#isPrototype()
   */
  @Override
  public boolean isPrototype() {
    // TODO(${user}): Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getPrototypeChildren()
   */
  @Override
  public Collection<ILaunchConfiguration> getPrototypeChildren() throws CoreException {
    // TODO(${user}): Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getKind()
   */
  @Override
  public int getKind() throws CoreException {
    // TODO(${user}): Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#getPrototypeVisibleAttributes()
   */
  @Override
  public Set<String> getPrototypeVisibleAttributes() throws CoreException {
    // TODO(${user}): Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.core.ILaunchConfiguration#setPrototypeAttributeVisibility(java.lang.String, boolean)
   */
  @Override
  public void setPrototypeAttributeVisibility(String attribute, boolean visible) throws CoreException {
    // TODO(${user}): Auto-generated method stub

  }
}
