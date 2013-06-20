/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Holder class for all of the Gson-related serialization classes that we use to
 * read data from the Managed API descriptor file.
 */
public class ManagedApiJsonClasses {

  /**
   * Class used for getting dependency information from descriptor.json.
   */
  public static class ApiDependencies {
    /**
     * Class holding "environments" and "files" members of "dependencies" in the descriptor.json.
     */
    public static class ApiDependency {
      private List<String> environments = new ArrayList<String>();
      private List<File> files = new ArrayList<File>();

      public List<String> getEnvironments() {
        return environments;
      }

      public List<File> getFiles() {
        return files;
      }

    }

    /**
     * Class holding "path" and "type" members of "files" in the descriptor.json.
     */
    public static class File {
      private String path;
      private String type;

      public String getPath() {
        return path;
      }

      public String getType() {
        return type;
      }

    }

    private List<ApiDependency> dependencies = new ArrayList<ApiDependency>();
    private List<File> files = new ArrayList<File>();

    public List<ApiDependency> getDependencies() {
      return dependencies;
    }

    public List<File> getFiles() {
      return files;
    }

    public void setFiles(List<File> files) {
      this.files = files;
    }

  }

  /**
   * Class used for getting name, version, revision and language version from descriptor.json.
   */
  public static class ApiRevision {
    // Variable names as in JSON labels so gson can get their values from JSON
    // file.
    private String name;
    private String revision;
    private String version;
    private String language_version;

    public String getLanguage_version() {
      return language_version;
    }

    public String getName() {
      return name;
    }

    public String getRevision() {
      return revision;
    }

    public String getVersion() {
      return version;
    }

    public void setLanguage_version(String language_version) {
      this.language_version = language_version;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setRevision(String revision) {
      this.revision = revision;
    }

    public void setVersion(String version) {
      this.version = version;
    }
  }

  public static final Gson GSON_CODEC = new Gson();

  /**
   * The filename identifying the resource as an API descriptor.
   * Visible for testing.
   */
  public static final String DESCRIPTOR_FILENAME = "descriptor.json";

}
