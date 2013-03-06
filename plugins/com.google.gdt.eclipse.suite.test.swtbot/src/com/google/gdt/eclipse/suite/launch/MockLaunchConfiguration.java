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

  TreeMap attributes = new TreeMap();

  public boolean contentsEqual(ILaunchConfiguration configuration) {
    // not used in test
    return false;
  }

  public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
    // not used in test
    return null;
  }

  public void delete() throws CoreException {
    // not used in test

  }

  public boolean exists() {
    // not used in test
    return false;
  }

  public Object getAdapter(Class adapter) {
    // not used in test
    return null;
  }

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

  public List getAttribute(String attributeName, List defaultValue)
      throws CoreException {
    // not used in test
    return null;
  }

  public Map getAttribute(String attributeName, Map defaultValue)
      throws CoreException {
    // not used in test
    return null;
  }

  public Set getAttribute(String attributeName, Set defaultValue)
      throws CoreException {
    // not used in test
    return null;
  }

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

  public Map getAttributes() throws CoreException {
    // not used in test
    return null;
  }

  public String getCategory() throws CoreException {
    // not used in test
    return null;
  }

  public IFile getFile() {
    // not used in test
    return null;
  }

  public IPath getLocation() {
    // not used in test
    return null;
  }

  public IResource[] getMappedResources() throws CoreException {
    // not used in test
    return null;
  }

  public String getMemento() throws CoreException {
    // not used in test
    return null;
  }

  public Set getModes() throws CoreException {
    // not used in test
    return null;
  }

  public String getName() {
    // not used in test
    return null;
  }

  public ILaunchDelegate getPreferredDelegate(Set modes) throws CoreException {
    // not used in test
    return null;
  }

  public ILaunchConfigurationType getType() throws CoreException {
    // not used in test
    return null;
  }

  public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
    // not used in test
    return null;
  }

  public boolean hasAttribute(String attributeName) throws CoreException {
    // not used in test
    return false;
  }

  public boolean isLocal() {
    // not used in test
    return false;
  }

  public boolean isMigrationCandidate() throws CoreException {
    // not used in test
    return false;
  }

  public boolean isReadOnly() {
    // not used in test
    return false;
  }

  public boolean isWorkingCopy() {
    // not used in test
    return false;
  }

  public ILaunch launch(String mode, IProgressMonitor monitor)
      throws CoreException {
    // not used in test
    return null;
  }

  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build)
      throws CoreException {
    // not used in test
    return null;
  }

  public ILaunch launch(String mode, IProgressMonitor monitor, boolean build,
      boolean register) throws CoreException {
    // not used in test
    return null;
  }

  public void migrate() throws CoreException {
    // not used in test

  }

  // this function is for testing convenience
  public void setAttribute(ILaunchConfigurationAttribute attribute, Object value) {
    attributes.put(attribute.getQualifiedName(), value);
  }

  public boolean supportsMode(String mode) throws CoreException {
    // not used in test
    return false;
  }

}
