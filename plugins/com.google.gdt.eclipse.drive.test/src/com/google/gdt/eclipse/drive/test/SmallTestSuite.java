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
package com.google.gdt.eclipse.drive.test;

import com.google.gdt.eclipse.drive.DrivePluginTest;
import com.google.gdt.eclipse.drive.driveapi.DriveCacheTest;
import com.google.gdt.eclipse.drive.driveapi.DriveQueriesTest;
import com.google.gdt.eclipse.drive.driveapi.DriveScriptInfoTest;
import com.google.gdt.eclipse.drive.driveapi.DriveServiceFacadeTest;
import com.google.gdt.eclipse.drive.driveapi.DriveWritingExceptionTest;
import com.google.gdt.eclipse.drive.driveapi.FileTypesTest;
import com.google.gdt.eclipse.drive.driveapi.ScriptProjectJsonProcessorTest;
import com.google.gdt.eclipse.drive.editors.ApiDocumentationServiceTest;
import com.google.gdt.eclipse.drive.editors.DelegatingSourceViewerConfigurationTest;
import com.google.gdt.eclipse.drive.editors.HardCodedApiInfoTest;
import com.google.gdt.eclipse.drive.editors.JavaScriptIdentifierNamesTest;
import com.google.gdt.eclipse.drive.editors.WebEditorCompletionProcessorTest;
import com.google.gdt.eclipse.drive.images.NotSavedLabelDecoratorTest;
import com.google.gdt.eclipse.drive.model.AppsScriptProjectTest;
import com.google.gdt.eclipse.drive.model.FolderTreeContentProviderTest;
import com.google.gdt.eclipse.drive.model.FolderTreeLabelProviderTest;
import com.google.gdt.eclipse.drive.model.FolderTreeTest;
import com.google.gdt.eclipse.drive.preferences.AppsScriptProjectPreferencesTest;
import com.google.gdt.eclipse.drive.resources.PendingSaveEventTest;
import com.google.gdt.eclipse.drive.resources.PendingSaveManagerTest;
import com.google.gdt.eclipse.drive.resources.WorkspaceUtilsTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * JUnit4 test suite for small tests
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  ApiDocumentationServiceTest.class,
  AppsScriptProjectPreferencesTest.class,
  AppsScriptProjectTest.class,
  DelegatingSourceViewerConfigurationTest.class,
  DriveCacheTest.class,
  DrivePluginTest.class,
  DriveQueriesTest.class,
  DriveScriptInfoTest.class,
  DriveServiceFacadeTest.class,
  DriveWritingExceptionTest.class,
  FileTypesTest.class,
  FolderTreeTest.class,
  FolderTreeContentProviderTest.class,
  FolderTreeLabelProviderTest.class,
  HardCodedApiInfoTest.class,
  JavaScriptIdentifierNamesTest.class,
  WebEditorCompletionProcessorTest.class,
  NotSavedLabelDecoratorTest.class,
  PendingSaveEventTest.class,
  PendingSaveManagerTest.class,
  ScriptProjectJsonProcessorTest.class,
  WorkspaceUtilsTest.class
})
public class SmallTestSuite { }