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
package com.google.gdt.eclipse.gph;

import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.json.JsonHttpParser;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Key;
import com.google.gdt.eclipse.gph.model.GPHProject;
import com.google.gdt.eclipse.gph.model.GPHUser;
import com.google.gdt.eclipse.login.GoogleLogin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * An entry point for access to the GPH project API service.
 */
public class ProjectHostingService {

  /**
   * JSON DTO for project data.
   */
  public static class GPHProjectDTO extends GenericJson {

    /**
     * Long description
     */
    @Key
    public String description;

    /**
     * Host/domain for domain hosting (i.e. codesite projects hosted on domains
     * other than code.google.com such as eclipse labs) (Optional)
     */
    @Key
    public String domain;

    /**
     * Link to the project page
     */
    @Key
    public String htmlLink;

    /**
     * Codesite id
     */
    @Key
    public String id;

    /**
     * Codesite labels
     */
    @Key
    public List<String> labels = new ArrayList<String>();

    /**
     * Public name
     */
    @Key
    public String name;

    /**
     * List of repository URLs
     */
    @Key("repositoryUrls")
    public List<String> repoURLs;

    /**
     * User's role
     */
    @Key
    public String role;

    /**
     * Summary description
     */
    @Key
    public String summary;

    /**
     * String representing the VCS system in use
     */
    @Key("versionControlSystem")
    public String vcsType;

    /**
     * An HTTP URL that points to this project's representation on
     * code.google.com.
     */
    @Key
    public String selfLink;

    @Override
    public String toString() {
      return "GPHProject [id=" + id + ", name=" + name + ", summary=" + summary
          + ", description=" + description + ", htmlLink=" + htmlLink
          + ", labels=" + labels + ", domain=" + domain + ", vcsType="
          + vcsType + ", repoURLs=" + repoURLs + ", role=" + role + "]";
    }
  }

  /**
   * A model representation of a GPH User, corresponding to a JSON object
   * Project Hosting API response.
   */
  public static class GPHUserDTO {

    /**
     * Create a GPH User instance constituted from a JSON object parsed from an
     * HTTP <code>get</code> executed on the given (authenticated) transport and
     * Project Hosting service url.
     * 
     * @param requestFactory the authenticated HttpRequestFactory
     * @param url the project hosting url
     * @return a GPH user object
     * @throws IOException
     */
    public static GPHUserDTO fromTransport(HttpRequestFactory requestFactory,
        GoogleUrl url) throws IOException {
      HttpRequest request = requestFactory.buildGetRequest(url);
      HttpResponse resp = request.execute();
      // System.out.println("[" + resp.parseAsString() + "]");

      JsonHttpParser parser = new JsonHttpParser(new JacksonFactory());
      return parser.parse(resp, GPHUserDTO.class);
    }

    /**
     * A hash for accessing the VCS services (differs from the account
     * password).
     */
    @Key("projectHostingPassword")
    public String hostingPassword;

    /**
     * User's hash identity on code.google.com
     */
    @Key
    public String id;

    /**
     * A list of GPH projects where the user has a defined role.
     */
    @Key
    public List<GPHProjectDTO> projects = new ArrayList<GPHProjectDTO>();

    /**
     * An HTTP URL that points to this user's representation on code.google.com.
     */
    @Key
    public String selfLink;

    @Override
    public String toString() {
      return "GPHUser\nid=" + id + "\nselfLink=" + selfLink
          + "\nhostingPassword=" + hostingPassword + "\nitems=" + projects
          + "]";
    }
  }

  /**
   * URL for the hosting service.
   */
  private static final String PROJECT_HOSTING_URL = "https://www.googleapis.com/projecthosting/v1/users/@me"; //$NON-NLS-1$

  /**
   * Get a list of projects associated with a particular user.
   * 
   * @param requestFactory an HttpRequestFactory object that is been signed with
   *          the users's authentication headers
   * 
   * @return a (possibly empty) list of projects
   */
  public List<GPHProject> getProjects(HttpRequestFactory requestFactory)
      throws HttpResponseException, IOException {
    try {
      return getProjectsImpl(requestFactory);
    } catch (HttpResponseException e) {
      if (e.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
        // If we get a failure trying to execute the request, we should
        // re-authenticate.
        GoogleLogin.getInstance().logOut(false);
      } else {
        // System.out.println("[" + e.response.parseAsString() + "]");

        throw e;
      }
    }

    // ... and try one more time
    return getProjectsImpl(requestFactory);
  }

  /**
   * @throws HttpResponseException if the http request cannot exec
   */
  private List<GPHProject> getProjectsImpl(HttpRequestFactory requestFactory)
      throws IOException {

    GoogleUrl hostingUrl = new GoogleUrl(PROJECT_HOSTING_URL);
    GPHUserDTO userDTO = GPHUserDTO.fromTransport(requestFactory, hostingUrl);

    GPHUser user = new GPHUser(GoogleLogin.getInstance().getEmail(),
        userDTO.id, userDTO.hostingPassword);

    for (GPHProjectDTO projectDTO : userDTO.projects) {
      // TODO: pass the license type into this constructor!
      @SuppressWarnings("unused")
      GPHProject project = new GPHProject(user, projectDTO.name,
          projectDTO.domain, projectDTO.role, projectDTO.vcsType,
          projectDTO.summary, projectDTO.description, projectDTO.htmlLink,
          null, projectDTO.repoURLs, projectDTO.labels);
    }

    return user.getProjects();
  }

}
