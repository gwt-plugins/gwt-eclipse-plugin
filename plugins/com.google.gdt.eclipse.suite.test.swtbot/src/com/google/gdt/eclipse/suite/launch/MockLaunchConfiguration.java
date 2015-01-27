/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.launch;

import com.google.gdt.eclipse.core.launch.ILaunchConfigurationAttribute;

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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A Mock LaunchConfiguration used for testing purposes
 */
public class MockLaunchConfiguration implements ILaunchConfiguration {

  TreeMap<String, Object> attributes = new TreeMap<>();

  @Override
  public boolean contentsEqual(ILaunchConfiguration configuration) {
    // not used in test
    return false;
  }

  @Override
  public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public void delete() throws CoreException {
    // not used in test

  }

  @Override
  public boolean exists() {
    // not used in test
    return false;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class adapter) {
    // not used in test
    return null;
  }

  @Override
  public boolean getAttribute(String attributeName, boolean defaultValue)
      throws CoreException {
    Object attr = attributes.get(attributeName);
    if (attr != null) {
      if (attr instanceof Boolean) {
        return ((Boolean) attr).booleanValue();
      } else {
        throw new RuntimeException(
            "Looking for non-boolean value with boolean function");
      }
    }
    return defaultValue;
  }

  @Override
  public int getAttribute(String attributeName, int defaultValue)
      throws CoreException {
    Object attr = attributes.get(attributeName);
    if (attr != null) {
      if (attr instanceof Integer) {
        return ((Integer) attr).intValue();
      }
      throw new RuntimeException(
          "Looking for non-integer value with integer function");
    }
    return defaultValue;
  }

  @Override
  public List<String> getAttribute(String attributeName, List<String> defaultValue)
      throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public Map<String, String> getAttribute(String attributeName, Map<String, String> defaultValue)
      throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public Set<String> getAttribute(String attributeName, Set<String> defaultValue)
      throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public String getAttribute(String attributeName, String defaultValue)
      throws CoreException {
    Object attr = attributes.get(attributeName);
    if (attr != null) {
      if (attr instanceof String) {
        return (String) attr;
      }
      throw new RuntimeException(
          "Looking for non-string value with string function");
    }
    return defaultValue;
  }

  @Override
  public Map<String, Object> getAttributes() throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public String getCategory() throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public IFile getFile() {
    // not used in test
    return null;
  }

  @Deprecated
  @Override
  public IPath getLocation() {
    // not used in test
    return null;
  }

  @Override
  public IResource[] getMappedResources() throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public String getMemento() throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public Set<String> getModes() throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public String getName() {
    // not used in test
    return null;
  }

  @Override
  public ILaunchDelegate getPreferredDelegate(Set<String> modes) throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public ILaunchConfigurationType getType() throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public boolean hasAttribute(String attributeName) throws CoreException {
    // not used in test
    return false;
  }

  @Override
  public boolean isLocal() {
    // not used in test
    return false;
  }

  @Override
  public boolean isMigrationCandidate() throws CoreException {
    // not used in test
    return false;
  }

  @Override
  public boolean isReadOnly() {
    // not used in test
    return false;
  }

  @Override
  public boolean isWorkingCopy() {
    // not used in test
    return false;
  }

  @Override
  public ILaunch launch(String mode, IProgressMonitor monitor)
      throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build)
      throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build,
      boolean register) throws CoreException {
    // not used in test
    return null;
  }

  @Override
  public void migrate() throws CoreException {
    // not used in test

  }

  // this function is for testing convenience
  public void setAttribute(ILaunchConfigurationAttribute attribute, Object value) {
    attributes.put(attribute.getQualifiedName(), value);
  }

  @Override
  public boolean supportsMode(String mode) throws CoreException {
    // not used in test
    return false;
  }

}
