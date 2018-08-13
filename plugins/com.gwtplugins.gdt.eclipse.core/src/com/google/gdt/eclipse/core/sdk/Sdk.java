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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;

import java.io.File;

/**
 *
 */
public interface Sdk {

  /**
   * Thrown when there is a problem with an Sdk.
   */
  @SuppressWarnings("serial")
  public static class SdkException extends Exception {
    public SdkException(String shortMessage) {
      super("Invalid SDK: " + shortMessage);
    }

    public SdkException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Returns the set of {@link IClasspathEntry IClasspathEntries} associated with this Sdk.
   *
   * @return the set of {@link IClasspathEntry IClasspathEntries} associated with this Sdk
   *
   */
  IClasspathEntry[] getClasspathEntries();

  /**
   * Returns a description of the Sdk.
   *
   * @return Sdk description
   */
  String getDescription();

  /**
   * Returns the installation path for this Sdk.
   *
   * @return installation path for this Sdk
   */
  IPath getInstallationPath();

  /**
   * Returns the name of this Sdk.
   *
   * @return name of this Sdk
   */
  String getName();

  /**
   * Returns the version string for this Sdk.
   *
   * @return the version
   */
  String getVersion();

  /**
   * Returns the set of files that belong in WEB-INF/lib folder.
   *
   * @return set of files that belong in WEB-INF/lib folder
   */
  File[] getWebAppClasspathFiles(IProject project);

  String toXml();

  IStatus validate();

}
