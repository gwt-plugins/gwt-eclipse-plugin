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
package com.google.gdt.eclipse.core.sdk;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;


/**
 * This class provides a skeletal implementation of the {@link Sdk} interface.
 */
public abstract class AbstractSdk implements Sdk {

  protected static final IClasspathEntry[] NO_ICLASSPATH_ENTRIES = new IClasspathEntry[0];

  private final IPath location;
  private final String name;

  public AbstractSdk(String name, IPath location) {
    this.name = name;
    this.location = location;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof Sdk)) {
      return false;
    }

    Sdk otherSdk = (Sdk) obj;
    // FIXME: If version is dynamic then it could change which would violate the
    // equals contract
    return locationsEqual(otherSdk) && namesEqual(otherSdk) && versionsEqual(otherSdk);
  }

  @Override
  public String getDescription() {
    String version = getVersion();
    if (version == null || version.equals("")) {
      version = "unknown version";
    }

    return getName() + " - " + version;
  }

  @Override
  public IPath getInstallationPath() {
    return location;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + (location == null ? 0 : location.hashCode());
    result = 31 * result + (name == null ? 0 : name.hashCode());
    // FIXME: If version is dynamic then it could change which would violate the
    // hashCode contract
    String version = getVersion();
    result = 31 * result + (version == null ? 0 : version.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return getName() + ", " + getInstallationPath() + ", " + getVersion();
  }

  @Override
  public String toXml() {
    StringBuilder sb = new StringBuilder();
    sb.append("<sdk name=\"");
    sb.append(getName());
    sb.append("\" location=\"");
    sb.append(getInstallationPath().toOSString());
    sb.append("\" version=\"");
    sb.append(getVersion());
    sb.append("\"/>");
    return sb.toString();
  }

  private boolean locationsEqual(Sdk otherSdk) {
    return (getInstallationPath() == null ? otherSdk.getInstallationPath() == null
        : getInstallationPath().equals(otherSdk.getInstallationPath()));
  }

  private boolean namesEqual(Sdk otherSdk) {
    return (getName() == null ? otherSdk.getName() == null : getName().equals(otherSdk.getName()));
  }

  private boolean versionsEqual(Sdk otherSdk) {
    return (getVersion() == null ? otherSdk.getVersion() == null : getVersion().equals(
        otherSdk.getVersion()));
  }

}
