/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.gdt.eclipse.drive.driveapi;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.model.AppsScriptProject;
import com.google.gdt.eclipse.drive.model.FolderTree;
import com.google.gdt.eclipse.login.GoogleLogin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;

/**
 * Encapsulates of of this plugin's accesses to the Drive service on behalf of the user, providing
 * the high-level operations required by the plugin. It is expected that the impact of any changes
 * to the Drive service API will be confined to the implementation of this class.
 * 
 * A {@code DriveServiceFacade} may be in a logged-in state or a logged-out state. In the logged-in
 * state, it has an authenticated connection to the Drive service. All methods that require an
 * authenticated connection ensure that the  {@code DriveServiceFacade} is in a logged-in state.
 * This entails forcing the user to log in if the  {@code DriveServiceFacade} was not previously in
 * a logged-in state.
 */
@SuppressWarnings("unused")
public class DriveServiceFacade {
  
  private static final String LOGIN_PROMPT =
      "Accessing Apps Script projects on Google Drive requires authentication.";
  
  private static final String DRIVE_URL_SYSTEM_PROPERTY_NAME = DrivePlugin.PLUGIN_ID + ".driveUrl";
  private static final String API_INFO_URL_SYSTEM_PROPERTY_NAME =
      DrivePlugin.PLUGIN_ID + ".apiInfoUrl";
  
  private static final String DEFAULT_API_INFO_URL = "https://script.google.com/a";

  @VisibleForTesting
  public static final String EXPORT_LINKS_API_PROPERTY_NAME = "exportLinks";
  @VisibleForTesting
  public static final String JSON_MIME_TYPE = "application/json";
  public static final String SCRIPT_PROJECT_JSON_MIME_TYPE =
      "application/vnd.google-apps.script+json";
  
  // The three placeholders correspond to the domain serving the API info
  // (e.g., https://docs.google.com), the Drive file ID for the Apps Script project, and the
  // OAuth token
  private static final String API_INFO_URL_PATTERN =
      "%s/macros/autocomplete/api/resources.js?lib=%s&mid=%s";
  
  private static DriveServiceFacade instance = null;
  
  /**
   * Obtains the singleton {@code DriveServiceFacade} instance, which may be in a logged-in or
   * logged-out state.
   * 
   * @return the singleton instance
   */
  public static DriveServiceFacade get() {
    if (instance == null) {
      instance = new DriveServiceFacade();
    }
    return instance;
  }
  
  @VisibleForTesting
  public static void setMock(DriveServiceFacade mockInstance) {
    instance = mockInstance;
  }
  
  private DriveConnection currentConnection;
  
  private DriveServiceFacade() {
  }
  
  @VisibleForTesting
  public DriveServiceFacade(
      Drive driveClient, HttpTransport transport, HttpRequestInitializer initializer) {
    currentConnection = new DriveConnection(driveClient, "dummyUrl", transport, initializer);
  }

  /**
   * Obtains a {@link FolderTree} containing all the leaf files accessible through this
   * {@code DriveServiceFacade} that satisfy a specified query, and all folders directly or
   * indirectly containing those files.
   * 
   * @param leafQuery
   *    the specified query, typically one of the strings declared in the {@link DriveQueries}
   *    interface
   * @return the {@code FolderTree}
   * @throws IOException
   */
  public FolderTree getFolderTree(String leafQuery) throws IOException {
    DriveConnection connection = ensureConnection();
    DriveCache driveCache = DriveCache.make(connection.getDriveClient(), leafQuery);
    return FolderTree.make(driveCache, false);
  }

  /**
   * Reads a Drive Apps Script project with a specified Drive file ID and creates a corresponding
   * {@link AppsScriptProject} object.  A call on this method results in a login prompt if the user
   * is not already logged in.
   * 
   * @param fileId the specified Drive file ID
   * @return the {@code AppsScriptProject} object
   * @throws IOException
   *     if an error is encountered building or executing a request to read metadata from Drive,
   *     processing the metadata, or reading JSON text from the URL specified in the metadata
   */
  public AppsScriptProject readProject(String fileId) throws IOException {
    DriveConnection connection = ensureConnection();
    File metadata = connection.getDriveClient().files().get(fileId).execute();
    return projectForMetadata(metadata);
  }

  private static String getHttpResponse(
      HttpRequestFactory requestFactory, String url, String description) throws IOException {
    HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(url));
    DrivePlugin.logInfo(
        "About to issue the following HTTP request to obtain " + description + ":\n"
            + HttpRequestUtils.toString(request));
    HttpResponse response = request.execute();
    InputStreamReader responseReader = new InputStreamReader(response.getContent(), Charsets.UTF_8);
    String result = null;
    try {
      result = CharStreams.toString(responseReader);
    } finally {
      responseReader.close();
      DrivePlugin.logInfo(
          result == null ?
              "Unable to read " + description :
              "Read the following " + description + ":\n" + result
          );
    } 
    return result;
  }
  
  /**
   * Reads the Apps Script project with a specified Drive file ID from Drive and returns the
   * corresponding {@link AppsScriptProject}, forcing a login prompt if the user is not already
   * logged in.
   * 
   * @param driveProjectFileId the specified Drive file ID
   * @return the {@code AppsScriptProject}
   * @throws IOException if there is an error reading the project's metadata or contents from Drive
   */
  public AppsScriptProject modelProjectForDriveId(String driveProjectFileId) throws IOException {
    DriveConnection connection = ensureConnection();
    File projectMetadata = connection.getDriveClient().files().get(driveProjectFileId).execute();
    return projectForMetadata(projectMetadata);
  }

  /**
   * Writes a specified {@link AppsScriptProject} back to Drive, replacing the entire contents of
   * the Drive project. It is the responsibility of the caller to check for write-write conflicts
   * before calling this method.
   * 
   * <p>Files in the specified {@code AppsScriptProject} that do not have document IDs are presumed
   * to be files that did not previously exist on Drive, but were created in Eclipse. Drive assigns
   * new document IDs to these files as part of the write. The method returns a map from the names
   * of these new files to {@link DriveScriptInfo} objects containing the new document IDs, so that
   * the new document IDs can be recorded in Eclipse.
   * 
   * <p>A call on this method results in a login prompt if the user is not already logged in.
   * 
   * @param project the model object for the Apps Script project
   * @param filesToSave the Eclipse names of files that should be updated
   * @return
   *     a map from names of files previously unknown to Drive to {@link DriveScriptInfo} objects
   *     containing the new document IDs that Drive has assigned to them
   * @throws DriveWritingException
   *     if the attempt to write to Drive, or to obtain the document IDs for files written for the
   *     first time, fails
   */
  public Map<String, DriveScriptInfo> writeProject(
      AppsScriptProject project, Collection<String> filesToSave)
      throws DriveWritingException {
    DriveConnection connection = ensureConnection();
    String projectFileId = project.getDriveFileId();
    File updatedMetadata;
    Map<String, DriveScriptInfo> newFileInfo;
    try {
      String projectJson = project.getProjectJson(filesToSave);
      ByteArrayContent content =
          new ByteArrayContent(SCRIPT_PROJECT_JSON_MIME_TYPE, projectJson.getBytes(Charsets.UTF_8));
      DrivePlugin.logInfo(
          "Updating Drive project " + projectFileId + " with mime type " + content.getType()
              + " and the following JSON:\n" + projectJson);
      updatedMetadata = 
          connection.getDriveClient().files().update(projectFileId, null, content).execute();
      return mapToNewDriveIdsForFiles(updatedMetadata, project, projectFileId, connection);
    } catch (IOException e) {
      throw new DriveWritingException(e);
    }
  }

  private Map<String, DriveScriptInfo> mapToNewDriveIdsForFiles(
      File updatedDriveMetadata,
      AppsScriptProject writtenEclipseProject,
      String driveProjectFileId,
      DriveConnection connection) throws IOException {
      AppsScriptProject projectWithNewFilesSaved = projectForMetadata(updatedDriveMetadata);
    Map<String, DriveScriptInfo> result = Maps.newHashMap();
    for (String fileName : writtenEclipseProject.getScriptFileNames()) {
      if (writtenEclipseProject.getScriptInfo(fileName).getDocumentId() == null) {
        DriveScriptInfo newInfo = projectWithNewFilesSaved.getScriptInfo(fileName);
        if (newInfo == null) {
          throw new IOException(
              "AppsScriptProject reconstructed from updated Drive metadata has no DriveScriptInfo "
              + "for <" + fileName + ">");
        }
        result.put(fileName, newInfo);
      }
    }
    return result;
  }

  /**
   * Creates a new abstract representation of an Apps Script project, by using the Drive metadata
   * describing that project to read the project contents from Drive. A call on this method results
   * in a login prompt if the user is not already logged in.
   * 
   * @param projectMetadata the Drive metadata
   * @return an {@link AppsScriptProject} object describing the project
   * @throws IOException if there is an error reading the project contents from Drive
   */
  public AppsScriptProject projectForMetadata(File projectMetadata) throws IOException {
    @SuppressWarnings("unchecked")
    // File.get returns a Map<String, String> as an Object
    Map<String, String> mimeTypesToUrls =
        (Map<String, String>) projectMetadata.get(EXPORT_LINKS_API_PROPERTY_NAME);
    String jsonUrlString = mimeTypesToUrls.get(SCRIPT_PROJECT_JSON_MIME_TYPE);
    if (jsonUrlString == null) {
      // This should never happen.
      throw new IOException("No export links with MIME type " + SCRIPT_PROJECT_JSON_MIME_TYPE);
    }
    DriveConnection connection = ensureConnection();
    // We do not simply call driveService.getRequestFactory() because that method is declared final
    // in a superclass of Drive and thus cannot be mocked in tests. The transport and initializer
    // fields of ConnectionInfo exist solely to be used in this statement:
    HttpRequestFactory requestFactory =
        connection.getTransport().createRequestFactory(connection.getInitializer());
    String fileId = projectMetadata.getId();
    
    String importedProjectJson =
        getHttpResponse(requestFactory, jsonUrlString, "JSON for Drive project " + fileId);

    // TODO(nhcohen): Once b/9104082 is resolved, replace the line below with the commented-out code
    // and remove @SuppressWarnings("unused") from the class.
    String apiInfoJsonInScript = "";
//    String urlPrefix= System.getProperty(API_INFO_URL_SYSTEM_PROPERTY_NAME, DEFAULT_API_INFO_URL);
//    String token = ((Credential) connection.getInitializer()).getAccessToken();
//
//    String apiInfoUrlString =
//        String.format(
//            API_INFO_URL_PATTERN,
//            urlPrefix,
//            CharEscapers.escapeUri(fileId),
//            CharEscapers.escapeUri(token));
//    String apiInfoJsonInScript =
//        getHttpResponse(
//            requestFactory, apiInfoUrlString, "API information for Drive project " + fileId);
    return AppsScriptProject.make(fileId, importedProjectJson, apiInfoJsonInScript);
  }

  /**
   * Responds to a notification from the Google login plugin that the user has logged out, by
   * discarding the Drive connection.
   */
  public synchronized void onLogout() {
    currentConnection = null;
  }
  
  /**
   * If there is not a current connection to the Drive service, connects to the Drive service,
   * forcing a login prompt if the user is not already logged in.
   * 
   * @return the {@link Drive} object representing the connection
   */
  public synchronized DriveConnection ensureConnection() {
    if (currentConnection != null) {
      return currentConnection;
    }
    GoogleLogin loginInstance = GoogleLogin.getInstance();
    if (!loginInstance.isLoggedIn()) {
      loginInstance.logIn(LOGIN_PROMPT);
    }
    Credential credential = loginInstance.getCredential();
    HttpTransport transport = new NetHttpTransport();
    Drive.Builder builder = new Drive.Builder(transport, new JacksonFactory(), credential);
    String driveUrl = System.getProperty(DRIVE_URL_SYSTEM_PROPERTY_NAME);
    if (driveUrl != null) {
      builder.setRootUrl(driveUrl);
    }
    Drive driveClient = builder.build();
    DrivePlugin.logInfo("Connected to Drive service at " + driveClient.getRootUrl());
    currentConnection = new DriveConnection(driveClient, driveUrl, transport, credential);
    return currentConnection;
  }
  
  @VisibleForTesting
  public static void useMockDriveConnection(Drive mockDriveClient) {
    instance = new DriveServiceFacade();
    instance.currentConnection = new DriveConnection(mockDriveClient, null, null, null);
  }


  private static class DriveConnection {
    
    private final Drive driveClient;
    private final String hostUrlString;
    private final HttpTransport transport;
    private final HttpRequestInitializer initializer;
    
    public DriveConnection(
        Drive driveClient, String hostUrlString, HttpTransport transport,
        HttpRequestInitializer initializer) {
      this.driveClient = driveClient;
      this.hostUrlString = hostUrlString;
      this.transport = transport;
      this.initializer = initializer;
    }
    
    public Drive getDriveClient() {
      return driveClient;
    }
    
    public String getHostUrlString() {
      return hostUrlString;
    }

    public HttpTransport getTransport() {
      return transport;
    }
    
    public HttpRequestInitializer getInitializer() {
      return initializer;
    }    
  }
}
