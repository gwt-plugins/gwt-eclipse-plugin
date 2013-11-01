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
package com.google.gdt.eclipse.drive.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gdt.eclipse.drive.test.MockEclipsePreferences;
import com.google.gdt.eclipse.drive.test.MockResourceRuleFactory;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.prefs.BackingStoreException;

import java.util.Map;
import java.util.Set;

/**
 * Unit test for {@link AppsScriptProjectPreferences}.
 */
@SuppressWarnings("restriction") // org.eclipse.core.internal.resources.Project
@RunWith(JUnit4.class)
public class AppsScriptProjectPreferencesTest {

  private static final String PROJECT_0_FILE_ID = "file ID 0";
  private static final String PROJECT_0_FINGERPRINT = "fingerprint0";
  private static final String PRECENT_ENCODED_FILE_NAME_0 = "file%20name%200%2Egs";
  private static final String UNENCODED_FILE_NAME_0 = "file name 0.gs";
  private static final String FILE_0_IMPORT_NAME = "file name 0";
  private static final String FILE_0_SCRIPT_ID = "script ID 0";
  private static final String FILE_0_DRIVE_TYPE = "Drive type 0";
  
  private Map<String, IEclipsePreferences> projectNamesToSettings;
  
  @Before
  public void setUp() throws BackingStoreException {
    MockitoAnnotations.initMocks(this);
    projectNamesToSettings = Maps.newHashMap();
    AppsScriptProjectPreferences.setMockFactories(
        new MockScopeContextFactory(), new MockResourceRuleFactory());
   }
  
  @Test
  public void testDriveFileIdMethods() throws BackingStoreException {
    IProject project0 = makeProject("version0Prefs", 0);
    IProject project1 = makeProject("one", 1);
    IProject project2 = makeProject("two", 1);
    IProject project3 = makeProject("three", 1);
    String fileId0 = "file ID 0";
    String fileId1 = "file ID 1";
    String fileId2 = "file ID 2";
    AppsScriptProjectPreferences.writeDriveFileId(project0, fileId0);
    AppsScriptProjectPreferences.writeDriveFileId(project1, fileId1);
    AppsScriptProjectPreferences.writeDriveFileId(project2, fileId2);
    assertEquals(fileId0, AppsScriptProjectPreferences.readDriveFileId(project0));
    assertEquals(fileId1, AppsScriptProjectPreferences.readDriveFileId(project1));
    assertEquals(fileId2, AppsScriptProjectPreferences.readDriveFileId(project2));
    assertNull(AppsScriptProjectPreferences.readDriveFileId(project3));    
   }
  
  @Test
  public void testDriveVersionFingerprintMethods() throws BackingStoreException {
    IProject project0 = makeProject("version0Prefs", 0);
    IProject project1 = makeProject("one", 1);
    IProject project2 = makeProject("two", 1);
    IProject project3 = makeProject("three", 1);
    String fingerprint0 = "fingerprint0";
    String fingerprint1 = "fingerprint1";
    String fingerprint2 = "fingerprint2";
    AppsScriptProjectPreferences.writeDriveVersionFingerprint(project0, fingerprint0);
    AppsScriptProjectPreferences.writeDriveVersionFingerprint(project1, fingerprint1);
    AppsScriptProjectPreferences.writeDriveVersionFingerprint(project2, fingerprint2);
    assertEquals(fingerprint0, AppsScriptProjectPreferences.readDriveVersionFingerprint(project0));
    assertEquals(fingerprint1, AppsScriptProjectPreferences.readDriveVersionFingerprint(project1));
    assertEquals(fingerprint2, AppsScriptProjectPreferences.readDriveVersionFingerprint(project2));
    assertNull(AppsScriptProjectPreferences.readDriveVersionFingerprint(project3));    
   }
  
  @Test
  public void testDriveImportNameMethods() throws BackingStoreException {
    IProject project0 = makeProject("version0Prefs", 0);
    IProject project1 = makeProject("one", 1);
    IProject project2 = makeProject("two", 1);
    IProject project3 = makeProject("three", 1);
    String scriptName1 = "script1.gs";
    String importName11 = "script11";
    String importName12 = "script12";
    String scriptName2 = "script2.gs";
    String importName2 = "script2";
    String scriptName3 = "Скрипт3.gs";
    String importName3 = "Скрипт3";
    AppsScriptProjectPreferences.writeDriveImportName(project1, scriptName1, importName11);
    AppsScriptProjectPreferences.writeDriveImportName(project1, scriptName2, importName2);
    AppsScriptProjectPreferences.writeDriveImportName(project2, scriptName1, importName12);
    AppsScriptProjectPreferences.writeDriveImportName(project2, scriptName3, importName3);

    assertEquals(
        FILE_0_IMPORT_NAME,
        AppsScriptProjectPreferences.readDriveImportName(project0, UNENCODED_FILE_NAME_0));
    assertEquals(
        importName11, AppsScriptProjectPreferences.readDriveImportName(project1, scriptName1));
    assertEquals(
        importName2, AppsScriptProjectPreferences.readDriveImportName(project1, scriptName2));
    assertNull(AppsScriptProjectPreferences.readDriveImportName(project1, scriptName3));
    
    assertEquals(
        importName12, AppsScriptProjectPreferences.readDriveImportName(project2, scriptName1));
    assertNull(AppsScriptProjectPreferences.readDriveImportName(project2, scriptName2));
    assertEquals(
        importName3, AppsScriptProjectPreferences.readDriveImportName(project2, scriptName3));
    
    assertNull(AppsScriptProjectPreferences.readDriveImportName(project3, scriptName1));
    
    AppsScriptProjectPreferences.removeDriveImportName(project1, scriptName1);
    assertNull(AppsScriptProjectPreferences.readDriveImportName(project1, scriptName1));
    assertNotNull(AppsScriptProjectPreferences.readDriveImportName(project1, scriptName2));
  }
  
  @Test
  public void testDriveScriptIdMethods() throws BackingStoreException {
    IProject project0 = makeProject("version0Prefs", 0);
    IProject project1 = makeProject("one", 1);
    IProject project2 = makeProject("two", 1);
    IProject project3 = makeProject("three", 1);
    String scriptName1 = "script1.gs";
    String scriptId11 = "script ID 11";
    String scriptId12 = "script ID 12";
    String scriptName2 = "script2.gs";
    String scriptId2 = "script ID 2";
    String scriptName3 = "Скрипт3.gs";
    String scriptId3 = "script ID 3";
    AppsScriptProjectPreferences.writeDriveScriptId(project1, scriptName1, scriptId11);
    AppsScriptProjectPreferences.writeDriveScriptId(project1, scriptName2, scriptId2);
    AppsScriptProjectPreferences.writeDriveScriptId(project2, scriptName1, scriptId12);
    AppsScriptProjectPreferences.writeDriveScriptId(project2, scriptName3, scriptId3);

    assertEquals(
        FILE_0_SCRIPT_ID,
        AppsScriptProjectPreferences.readDriveScriptId(project0, UNENCODED_FILE_NAME_0));
    assertEquals(scriptId11, AppsScriptProjectPreferences.readDriveScriptId(project1, scriptName1));
    assertEquals(scriptId2, AppsScriptProjectPreferences.readDriveScriptId(project1, scriptName2));
    assertNull(AppsScriptProjectPreferences.readDriveScriptId(project1, scriptName3));
    
    assertEquals(scriptId12, AppsScriptProjectPreferences.readDriveScriptId(project2, scriptName1));
    assertNull(AppsScriptProjectPreferences.readDriveScriptId(project2, scriptName2));
    assertEquals(scriptId3, AppsScriptProjectPreferences.readDriveScriptId(project2, scriptName3));
    
    assertNull(AppsScriptProjectPreferences.readDriveScriptId(project3, scriptName1));

    AppsScriptProjectPreferences.removeDriveScriptId(project1, scriptName1);
    assertNull(AppsScriptProjectPreferences.readDriveScriptId(project1, scriptName1));
    assertNotNull(AppsScriptProjectPreferences.readDriveScriptId(project2, scriptName1));
  }
  
  @Test
  public void testDriveTypeMethods() throws BackingStoreException {
    IProject project0 = makeProject("version0Prefs", 0);
    IProject project1 = makeProject("one", 1);
    IProject project2 = makeProject("two", 1);
    IProject project3 = makeProject("three", 1);
    String fileName1 = "script1.gs";
    String driveType11 = "Drive type 11";
    String driveType12 = "Drive type 12";
    String fileName2 = "script2.gs";
    String driveType2 = "Drive type 2";
    String fileName3 = "Скрипт3.gs";
    String driveType3 = "Drive type 3";
    AppsScriptProjectPreferences.writeDriveType(project1, fileName1, driveType11);
    AppsScriptProjectPreferences.writeDriveType(project1, fileName2, driveType2);
    AppsScriptProjectPreferences.writeDriveType(project2, fileName1, driveType12);
    AppsScriptProjectPreferences.writeDriveType(project2, fileName3, driveType3);

    assertEquals(
        FILE_0_DRIVE_TYPE,
        AppsScriptProjectPreferences.readDriveType(project0, UNENCODED_FILE_NAME_0));
    assertEquals(driveType11, AppsScriptProjectPreferences.readDriveType(project1, fileName1));
    assertEquals(driveType2, AppsScriptProjectPreferences.readDriveType(project1, fileName2));
    assertNull(AppsScriptProjectPreferences.readDriveType(project1, fileName3));
    
    assertEquals(driveType12, AppsScriptProjectPreferences.readDriveType(project2, fileName1));
    assertNull(AppsScriptProjectPreferences.readDriveType(project2, fileName2));
    assertEquals(driveType3, AppsScriptProjectPreferences.readDriveType(project2, fileName3));
    
    assertNull(AppsScriptProjectPreferences.readDriveType(project3, fileName1));

    AppsScriptProjectPreferences.removeDriveType(project1, fileName1);
    assertNull(AppsScriptProjectPreferences.readDriveType(project1, fileName1));
    assertNotNull(AppsScriptProjectPreferences.readDriveType(project2, fileName1));
  }
  
  @Test
  public void testUnsavedFileNameMethods() throws BackingStoreException {
    IProject project0 = makeProject("version0Prefs", 0);
    IProject project1 = makeProject("one", 1);
    IProject project2 = makeProject("two", 1);
    IProject project3 = makeProject("three", 1);
    String fileName1 = "script1.gs";
    String fileName2 = "script2.gs";
    String fileName3 = "Скрипт3.gs";
    Set<String> emptySet = ImmutableSet.of();
    assertEquals(emptySet, AppsScriptProjectPreferences.getUnsavedFileNames(project1));
    assertEquals(emptySet, AppsScriptProjectPreferences.getUnsavedFileNames(project2));
    AppsScriptProjectPreferences.addUnsavedFileName(project1, fileName1);
    AppsScriptProjectPreferences.addUnsavedFileName(project2, fileName1);
    assertEquals(
        ImmutableSet.of(UNENCODED_FILE_NAME_0),
        AppsScriptProjectPreferences.getUnsavedFileNames(project0));
    assertEquals(
        ImmutableSet.of(fileName1), AppsScriptProjectPreferences.getUnsavedFileNames(project1));
    assertEquals(
        ImmutableSet.of(fileName1), AppsScriptProjectPreferences.getUnsavedFileNames(project2));
    AppsScriptProjectPreferences.addUnsavedFileName(project1, fileName2);
    AppsScriptProjectPreferences.addUnsavedFileName(project2, fileName3);
    assertEquals(
        ImmutableSet.of(fileName1, fileName2),
        AppsScriptProjectPreferences.getUnsavedFileNames(project1));
    assertEquals(
        ImmutableSet.of(fileName1, fileName3),
        AppsScriptProjectPreferences.getUnsavedFileNames(project2));
    AppsScriptProjectPreferences.removeUnsavedFileName(project1, fileName1);
    AppsScriptProjectPreferences.removeUnsavedFileName(project2, fileName3);
    assertEquals(
        ImmutableSet.of(fileName2), AppsScriptProjectPreferences.getUnsavedFileNames(project1));
    assertEquals(
        ImmutableSet.of(fileName1), AppsScriptProjectPreferences.getUnsavedFileNames(project2));
    AppsScriptProjectPreferences.removeUnsavedFileName(project1, fileName3);
    assertEquals(
        ImmutableSet.of(fileName2), AppsScriptProjectPreferences.getUnsavedFileNames(project1));
    assertEquals(emptySet, AppsScriptProjectPreferences.getUnsavedFileNames(project3));
    AppsScriptProjectPreferences.removeUnsavedFileName(project3, fileName3);
    assertEquals(emptySet, AppsScriptProjectPreferences.getUnsavedFileNames(project3));
  }
  
  @Test
  public void testGetFileNamesWithDocumentIds() throws BackingStoreException {
    IProject project0 = makeProject("version0Prefs", 0);
    IProject project1 = makeProject("one", 1);
    IProject project2 = makeProject("two", 1);
    IProject project3 = makeProject("three", 1);
    String fileName1 = "script1.gs";
    String fileName2 = "script2.gs";
    String fileName3 = "Скрипт3.gs";
    Set<String> emptySet = ImmutableSet.of();
    assertEquals(
        ImmutableSet.of(UNENCODED_FILE_NAME_0),
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project0));
    assertEquals(emptySet, AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project1));
    assertEquals(emptySet, AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project2));
    AppsScriptProjectPreferences.writeDriveScriptId(project1, fileName1, "anything");
    AppsScriptProjectPreferences.writeDriveScriptId(project2, fileName1, "anything");
    assertEquals(
        ImmutableSet.of(fileName1),
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project1));
    assertEquals(
        ImmutableSet.of(fileName1),
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project2));
    AppsScriptProjectPreferences.writeDriveScriptId(project1, fileName2, "anything");
    AppsScriptProjectPreferences.writeDriveScriptId(project2, fileName3, "anything");
    assertEquals(
        ImmutableSet.of(fileName1, fileName2),
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project1));
    assertEquals(
        ImmutableSet.of(fileName1, fileName3),
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project2));
    AppsScriptProjectPreferences.removeDriveScriptId(project1, fileName1);
    AppsScriptProjectPreferences.removeDriveScriptId(project2, fileName3);
    assertEquals(
        ImmutableSet.of(fileName2),
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project1));
    assertEquals(
        ImmutableSet.of(fileName1),
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project2));
    AppsScriptProjectPreferences.removeDriveScriptId(project1, fileName3);
    assertEquals(
        ImmutableSet.of(fileName2),
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project1));
    assertEquals(emptySet, AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project3));
    AppsScriptProjectPreferences.removeDriveScriptId(project3, fileName3);
    assertEquals(emptySet, AppsScriptProjectPreferences.getFileNamesWithDocumentIds(project3));
  }
  
  private IProject makeProject(String name, int preferencesVersion) {
    Map<String, String> initialPreferences = Maps.newHashMap();
    switch (preferencesVersion) {
      case 0:
        // Prepopulate preferences with version 0 preferences, to test for successful conversion
        // to version 1:
        initialPreferences.put(PreferencesConstants.DRIVE_FILE_ID_KEY, PROJECT_0_FILE_ID);
        initialPreferences.put(
            PreferencesConstants.DRIVE_VERSION_FINGERPRINT_KEY, PROJECT_0_FINGERPRINT);
        initialPreferences.put(
            PreferencesConstants.DRIVE_SCRIPT_ID_KEY_PREFIX + PRECENT_ENCODED_FILE_NAME_0,
            FILE_0_SCRIPT_ID);
        initialPreferences.put(
            PreferencesConstants.DRIVE_IMPORT_NAME_KEY_PREFIX + PRECENT_ENCODED_FILE_NAME_0,
            FILE_0_IMPORT_NAME);
        initialPreferences.put(
            PreferencesConstants.DRIVE_TYPE_KEY_PREFIX + PRECENT_ENCODED_FILE_NAME_0,
            FILE_0_DRIVE_TYPE);
        initialPreferences.put(
            PreferencesConstants.UNSAVED_KEY_PREFIX + PRECENT_ENCODED_FILE_NAME_0, "true");
        break;
      default:
        // Create empty current-version preferences:
        initialPreferences.put(
            PreferencesConstants.PREFERENCES_VERSION_KEY, Integer.toString(preferencesVersion));
          
    }
    projectNamesToSettings.put(name, new MockEclipsePreferences(initialPreferences, "/" + name));
    return new Project(new Path(name), null){};
  }
  
  /**
   * An {@link AppsScriptProjectPreferences.IScopeContextFactory} implementation whose
   * {@link #scopeForProject(IProject)} method returns a mock scope context.
   */
  private class MockScopeContextFactory
      implements AppsScriptProjectPreferences.IScopeContextFactory {
    
    /**
     * Returns a dynamically created mock {@link IScopeContext} whose
     * {@link IScopeContext#getNode(String)} method returns the {@code IEclipsePreferences} to
     * which {@code projectNamesToSettings} maps a specified project. 
     * 
     * @param project the specified project
     * @return the mock {@code IScopeContext}
     */
    @Override
    public IScopeContext scopeForProject(IProject project) {
      final IEclipsePreferences projectPreferences = projectNamesToSettings.get(project.getName());
      IScopeContext mockScopeContext = Mockito.mock(IScopeContext.class);
      when(mockScopeContext.getNode(Mockito.<String>anyObject()))
          .thenAnswer(
              new Answer<IEclipsePreferences>(){
                @Override public IEclipsePreferences answer(InvocationOnMock invocation) {
                  return projectPreferences;
                }
              });
      return mockScopeContext;
    }    
  }

}
