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
package com.google.gdt.eclipse.gph.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A model representation of a Google Hosted Project.
 */
public class GPHProject {
  private final GPHUser user;
  private final String domain;
  private final String name;
  private final String role;
  private final String scmType;
  private final String summary;
  private final String description;
  private final String htmlLink;
  private final String licenseType;
  private final List<String> repoUrls;
  private final List<String> labels;

  /**
   * Create a new GPHProject.
   * 
   * @param user 
   * @param projectName the name of the project (not <code>null</code>)
   * @param domain the project's domain
   * @param summary 
   * @param description 
   * @param htmlLink 
   * @param repoUrls 
   * @param labels 
   */
  public GPHProject(GPHUser user, String projectName, String domain, String role, String scmType, String summary,
      String description, String htmlLink, String licenseType, List<String> repoUrls, List<String> labels) {
    if (user == null) {
      throw new IllegalArgumentException("user cannot be null");
    }
    if (projectName == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    
    this.user = user;
    this.name = projectName;
    this.domain = domain;
    this.role = role;
    this.scmType = scmType;
    this.summary = summary;
    this.description = description;
    this.htmlLink = htmlLink;
    this.licenseType = licenseType;
    this.repoUrls = Collections.unmodifiableList(new ArrayList<String>(repoUrls));
    if (labels != null) {
      this.labels = Collections.unmodifiableList(new ArrayList<String>(labels));
    } else {
      this.labels = Collections.emptyList();
    }
    
    user.addProject(this);
  }

  /**
   * Get the project's description represented as a String.
   * 
   * @return the project's description (may be <code>null</code>)
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the project's domain represented as a String.
   * 
   * @return the project's domain or <code>null</code> if it is unknown.
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Get the project's html link represented as a String.
   * 
   * @return the project's html link (may be <code>null</code>)
   */
  public String getHtmlLink() {
    return htmlLink;
  }
  
  /**
   * @return the list of project labels
   */
  public List<String> getLabels() {
    return labels;
  }
  
  /**
   * Get the project's license type.
   * 
   * @return the project's license type (may be <code>null</code>)
   */
  public String getLicenseType() {
    return licenseType;
  }
  
  /**
   * Get the project's name represented as a String.
   * 
   * @return the project's name (cannot be <code>null</code>)
   */
  public String getName() {
    return name;
  }

  /**
   * @return the list of repositories for this project
   */
  public List<String> getRepoUrls() {
    return repoUrls;
  }
  
  /**
   * Get the user's role.
   * 
   * @return the user's role name
   */
  public String getRole() {
    return role;
  }

  /**
   * @return the SCM type for this project (i.e., svn, hg, ...)
   */
  public String getScmType() {
    return scmType;
  }
  
  /**
   * @return the SCM type in a human readable string
   */
  public String getScmTypeLabel() {
    String type = getScmType();
    
    if (type == null) {
      return type;
    }
    
    if (type.equals("svn")) {
      return "Subversion";
    }
    
    if (type.equals("hg")) {
      return "Mercurial";
    }
    
    if (type.length() > 1) {
      return type.substring(0, 1).toUpperCase() + type.substring(1);
    }
    
    return type;
  }
  
  /**
   * Get the project's summary represented as a String.
   * 
   * @return the project's summary (may be <code>null</code>)
   */
  public String getSummary() {
    return summary;
  }
  
  /**
   * @return the GPHUser
   */
  public GPHUser getUser() {
    return user;
  }

  @Override
  public String toString() {
    return getName();
  }

}
