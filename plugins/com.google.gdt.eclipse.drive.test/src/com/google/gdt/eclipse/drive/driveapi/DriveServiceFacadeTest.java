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
package com.google.gdt.eclipse.drive.driveapi;

import static org.junit.Assert.assertEquals;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.Credential.AccessMethod;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.Sets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.gdt.eclipse.drive.model.AppsScriptProject;
import com.google.gdt.eclipse.drive.model.FolderTree;
import com.google.gdt.eclipse.drive.test.MockAppsScriptExportingTransport;
import com.google.gdt.eclipse.drive.test.MockDriveProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit test for {@link DriveServiceFacade}
 */
@RunWith(JUnit4.class)
public class DriveServiceFacadeTest {
  
  private static final Set<String> LEAFLESS_FOLDER_TITLES =
      MockDriveProvider.getLeaflessFolderTitles();
  
  // TODO(nhcohen): Consider whether connect(), or parts of it, can be made testable by wrapping
  // calls on the login plugin inside a mockable object.
  
  @Test
  public void testGetFolderTree() throws IOException {
    DriveServiceFacade driveService =
        new DriveServiceFacade(
            MockDriveProvider.getMockDrive(),
            new MockAppsScriptExportingTransport(),
            new HttpRequestInitializer(){
              @Override public void initialize(HttpRequest request) {
              }
            });
    FolderTree root = driveService.getFolderTree(MockDriveProvider.getQueryForLeaves());
    assertEquals(FolderTree.DRIVE_ROOT_TITLE, root.getTitle());
    Multimap<String, String> titlesToChildTitles = MockDriveProvider.getTitlesToChildTitles();
    // Allow FolderTree.DRIVE_ROOT_TITLE as an alias for MockDriveProvider.fakeTitle("folder", 0) in
    // child-title lookups:
    titlesToChildTitles.putAll(
        FolderTree.DRIVE_ROOT_TITLE,
        titlesToChildTitles.get(MockDriveProvider.fakeTitle("folder", 0)));
    validateRecursively(root, titlesToChildTitles);
  }
  
  private void validateRecursively(FolderTree node, Multimap<String, String> titlesToChildTitles) {
    Set<String> childTitles = Sets.newHashSet();
    for (FolderTree child : node.getChildren()) {
      validateRecursively(child, titlesToChildTitles);
      childTitles.add(child.getTitle());
    }
    String ownTitle = node.getTitle();
    Set<String> expectedChildTitles = new HashSet<String>(titlesToChildTitles.get(ownTitle));
    expectedChildTitles.removeAll(LEAFLESS_FOLDER_TITLES);
    assertEquals(expectedChildTitles, childTitles);
  }
  
  @Test
  public void testReadProject() throws IOException {
    Credential dummyCredential =
        new Credential(
            new AccessMethod() {
              @Override public void intercept(HttpRequest request, String accessToken) {
              }
              @Override public String getAccessTokenFromRequest(HttpRequest request) {
                return null;
              }          
            });
    dummyCredential.setAccessToken("dummyToken");
    DriveServiceFacade driveService =
        new DriveServiceFacade(
            MockDriveProvider.getMockDrive(),
            new MockAppsScriptExportingTransport(),
            dummyCredential);
    String driveFileId = MockDriveProvider.fakeFileId("leaf", 0);
    AppsScriptProject result = driveService.readProject(driveFileId);
    String scriptName1 =
        MockAppsScriptExportingTransport.getScriptFileName(1) + FileTypes.SCRIPT_FILE_EXTENSION;
    String scriptName2 =
        MockAppsScriptExportingTransport.getScriptFileName(2) + FileTypes.SCRIPT_FILE_EXTENSION;
    assertEquals(driveFileId, result.getDriveFileId());
    assertEquals(ImmutableSet.of(scriptName1, scriptName2), result.getScriptFileNames());
    assertEquals(
        MockAppsScriptExportingTransport.getScriptText(1),
        result.getScriptInfo(scriptName1).getContents());
    assertEquals(
        MockAppsScriptExportingTransport.getScriptText(2),
        result.getScriptInfo(scriptName2).getContents());
  }

}
