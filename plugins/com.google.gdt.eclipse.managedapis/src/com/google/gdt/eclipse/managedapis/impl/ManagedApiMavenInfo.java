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
package com.google.gdt.eclipse.managedapis.impl;

/**
 * Class used for getting the maven members from
 * "https://developers.google.com/resources/api-libraries/info"
 */
public class ManagedApiMavenInfo {

  /**
   * Class that represents "maven" in Managed API info files.
   */
  public static class MavenMembers {
    private String repositoryUrl;
    private String artifactId;
    private String version;
    private String repositoryId;

    public String getArtifactId() {
      return artifactId;
    }

    public String getRepositoryId() {
      return repositoryId;
    }

    public String getRepositoryUrl() {
      return repositoryUrl;
    }

    public String getVersion() {
      return version;
    }
  }

  private MavenMembers maven;

  public MavenMembers getMaven() {
    return maven;
  }
}
