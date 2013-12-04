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
package com.google.gdt.eclipse.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdt.eclipse.drive.DrivePlugin.ICommandServiceFactory;
import com.google.gdt.eclipse.drive.DrivePlugin.ILogFactory;
import com.google.gdt.eclipse.drive.driveapi.DriveServiceFacade;
import com.google.gdt.eclipse.drive.images.ImageKeys;
import com.google.gdt.eclipse.drive.natures.AppsScriptNature;
import com.google.gdt.eclipse.drive.preferences.AppsScriptProjectPreferences;
import com.google.gdt.eclipse.drive.preferences.AppsScriptProjectPreferences.IScopeContextFactory;
import com.google.gdt.eclipse.drive.resources.PendingSaveManager;
import com.google.gdt.eclipse.drive.resources.WorkspaceUtils;
import com.google.gdt.eclipse.drive.test.MockDriveClient;
import com.google.gdt.eclipse.drive.test.MockEclipsePreferences;

import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.commands.ICommandService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.prefs.BackingStoreException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Unit test for {@link DrivePlugin}.
 */
@RunWith(JUnit4.class)
public class DrivePluginTest {
  
  @Mock private ILog mockLog;
  @Mock private ICommandService mockCommandService;
  @Mock private ImageRegistry mockImageRegistry;
  
  private List<IStatus> logEntries;
  private Set<String> registeredImageKeys;
  private IExecutionListener executionListener;
  
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doAnswer(
        new Answer<Void>(){
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            IStatus status = (IStatus) invocation.getArguments()[0];
            logEntries.add(status);
            return null;
          }
        })
        .when(mockLog).log(any(IStatus.class));
    doAnswer(
        new Answer<Void>(){
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            IExecutionListener listener = (IExecutionListener) invocation.getArguments()[0];
            executionListener = listener;
            return null;
          }
        })
        .when(mockCommandService).addExecutionListener(any(IExecutionListener.class));
    doAnswer(
        new Answer<Void>(){
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            String key = (String) invocation.getArguments()[0];
            registeredImageKeys.add(key);
            return null;
          }
        })
        .when(mockImageRegistry).put(any(String.class), any(ImageDescriptor.class));
    // Calls on DrivePlugin.getDefault() will return the DrivePlugin constructed below:
    DrivePlugin testPlugin =
        new DrivePlugin(
            new PendingSaveManager(),
            new ILogFactory(){
              @Override public ILog get() {
                return mockLog;
              }
            }, 
            new ICommandServiceFactory(){
              @Override public ICommandService get() {
                return mockCommandService;
              }
            });
    testPlugin.finishStarting();
    logEntries = Lists.newLinkedList();
    registeredImageKeys = Sets.newHashSet();
  }
  
  @Test
  public void testGetDefault() {
    assertNotNull(DrivePlugin.getDefault());
    assertSame(DrivePlugin.getDefault(), DrivePlugin.getDefault()); // reference equality
  }
  
  @Test
  public void testLogging() {
    String message1 = "message one";
    String message2 = "message two";
    String message3 = "message three";
    Throwable throwable1 = new Throwable("throwable 1");
    Throwable throwable2 = new Throwable("throwable 2");
    DrivePlugin.logError(message1, throwable1);
    DrivePlugin.log(IStatus.WARNING, message2, throwable2);
    DrivePlugin.logInfo(message3);
    assertEquals(3, logEntries.size());
    assertEquals(IStatus.ERROR, logEntries.get(0).getSeverity());
    assertEquals(message1, logEntries.get(0).getMessage());
    assertEquals(throwable1, logEntries.get(0).getException());
    assertEquals(IStatus.WARNING, logEntries.get(1).getSeverity());
    assertEquals(message2, logEntries.get(1).getMessage());
    assertEquals(throwable2, logEntries.get(1).getException());
    assertEquals(IStatus.INFO, logEntries.get(2).getSeverity());
    assertEquals(message3, logEntries.get(2).getMessage());
    assertNull(logEntries.get(2).getException());
  }
  
  @Test
  public void testGetPendingSaveManager() {
    DrivePlugin plugin = DrivePlugin.getDefault();
    assertNotNull(plugin.getPendingSaveManager());
    assertSame(plugin.getPendingSaveManager(), plugin.getPendingSaveManager()); // ref. equality
  }
  
  @Test
  public void testInitializeImageRegistry() {
    DrivePlugin plugin = DrivePlugin.getDefault();
    plugin.initializeImageRegistry(mockImageRegistry);
    assertEquals(ImmutableSet.copyOf(ImageKeys.ALL_KEYS), registeredImageKeys);
  }
  
  @Test
  public void testSaveAllInProgress() {
    WorkspaceUtils.setMockProjects();
    DrivePlugin plugin = DrivePlugin.getDefault();
    assertFalse(plugin.saveAllInProgress());
    executionListener.preExecute(IWorkbenchCommandConstants.FILE_SAVE, null);
    assertFalse(plugin.saveAllInProgress());
    executionListener.preExecute(IWorkbenchCommandConstants.FILE_SAVE_ALL, null);
    assertTrue(plugin.saveAllInProgress());
    executionListener.postExecuteSuccess(IWorkbenchCommandConstants.FILE_SAVE, null);
    assertTrue(plugin.saveAllInProgress());
    executionListener.postExecuteSuccess(IWorkbenchCommandConstants.FILE_SAVE_ALL, null);
    assertFalse(plugin.saveAllInProgress());
    executionListener.preExecute(IWorkbenchCommandConstants.FILE_SAVE_ALL, null);
    assertTrue(plugin.saveAllInProgress());
    executionListener.postExecuteFailure(IWorkbenchCommandConstants.FILE_SAVE_ALL, null);
    assertFalse(plugin.saveAllInProgress());
  }
  
  @Test
  public void testNoConcurrentModification() throws CoreException, BackingStoreException {
    // The purpose of this test to confirm that when a save-all operation marks a file as saved, it
    // does not modify the same Iterable<String> of unsaved-file names that is currently being
    // iterated over.
    IFile mockFile1 = Mockito.mock(IFile.class);
    IFile mockFile2 = Mockito.mock(IFile.class);
    setUpMocks(mockFile1, mockFile2);
    PendingSaveManager pendingSaveManager = DrivePlugin.getDefault().getPendingSaveManager();
    pendingSaveManager.markAsUnsaved(mockFile1);
    pendingSaveManager.markAsUnsaved(mockFile2);
    executionListener.preExecute(IWorkbenchCommandConstants.FILE_SAVE_ALL, null);
    executionListener.postExecuteSuccess(IWorkbenchCommandConstants.FILE_SAVE_ALL, null);
  }

  private static void setUpMocks(IFile mockFile1, IFile mockFile2)
      throws CoreException, BackingStoreException {
    String file1Name = "file1.gs";
    String file2Name = "file2.gs";
    final IEclipsePreferences mockPreferences = new MockEclipsePreferences();
    final IScopeContext mockScopeContext = Mockito.mock(IScopeContext.class);
    when(mockScopeContext.getNode(Mockito.<String>anyObject())).thenReturn(mockPreferences);
    AppsScriptProjectPreferences.setMockScopeContextFactory(
        new IScopeContextFactory(){
          @Override public IScopeContext scopeForProject(IProject project) {
            return mockScopeContext;
          }
        });
    IProject mockProject = Mockito.mock(IProject.class);
    when(mockFile1.getName()).thenReturn(file1Name);
    when(mockFile2.getName()).thenReturn(file2Name);
    when(mockFile1.getProject()).thenReturn(mockProject);
    when(mockFile2.getProject()).thenReturn(mockProject);
    when(mockFile1.exists()).thenReturn(true);
    when(mockFile2.exists()).thenReturn(true);
    when(mockFile1.getContents()).thenReturn(utf8InputStreamForString("file 1 contents"));
    when(mockFile2.getContents()).thenReturn(utf8InputStreamForString("file 2 contents"));
    when(mockProject.getName()).thenReturn("mockproject");
    when(mockProject.isOpen()).thenReturn(true);
    when(mockProject.hasNature(AppsScriptNature.NATURE_ID)).thenReturn(true);
    when(mockProject.members()).thenReturn(new IResource[]{mockFile1, mockFile2});
    when(mockProject.getFile(file1Name)).thenReturn(mockFile1);
    when(mockProject.getFile(file2Name)).thenReturn(mockFile2);
    WorkspaceUtils.setMockProjects(mockProject);
    AppsScriptProjectPreferences.writeDriveFileId(mockProject, "mock project Drive file ID");
    AppsScriptProjectPreferences.writeDriveScriptId(mockProject, file1Name, "mock script ID 1");
    AppsScriptProjectPreferences.writeDriveScriptId(mockProject, file2Name, "mock script ID 2");
    DriveServiceFacade.useMockDriveConnection(new MockDriveClient());
  }
  
  private static InputStream utf8InputStreamForString(String s) {
    byte[] asUtf8Bytes = s.getBytes(Charsets.UTF_8);
    return new ByteArrayInputStream(asUtf8Bytes);
  }

}
