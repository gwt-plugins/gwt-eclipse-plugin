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
package com.google.gdt.eclipse.drive.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.gdt.eclipse.drive.preferences.AppsScriptProjectPreferences;
import com.google.gdt.eclipse.drive.preferences.AppsScriptProjectPreferences.IScopeContextFactory;
import com.google.gdt.eclipse.drive.test.MockEclipsePreferences;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.List;

/**
 * Unit test for {@link PendingSaveManager}.
 */
@RunWith(JUnit4.class)
public class PendingSaveManagerTest {
  
  @Mock private IFile mockFile1;
  @Mock private IFile mockFile2;
  @Mock private IProject mockProject1;
  @Mock private IProject mockProject2;
  @Mock private IScopeContext mockContext1;
  @Mock private IScopeContext mockContext2;
  
  private MockEclipsePreferences mockPreferences1;
  private MockEclipsePreferences mockPreferences2;
  
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mockFile1.getName()).thenReturn("file1");
    when(mockFile2.getName()).thenReturn("file2");
    when(mockFile1.getProject()).thenReturn(mockProject1);
    when(mockFile2.getProject()).thenReturn(mockProject2);
    when(mockProject1.getName()).thenReturn("project1");
    when(mockProject2.getName()).thenReturn("project2");
    AppsScriptProjectPreferences.setMockScopeContextFactory(
        new IScopeContextFactory(){
          @Override public IScopeContext scopeForProject(IProject project) {
            if (project == mockProject1) {
              return mockContext1;
            } else if (project == mockProject2) {
              return mockContext2;
            } else {
              return null;
            }
          }
        });
    mockPreferences1 = new MockEclipsePreferences();
    mockPreferences2 = new MockEclipsePreferences();
    when(mockContext1.getNode(any(String.class))).thenReturn(mockPreferences1);
    when(mockContext2.getNode(any(String.class))).thenReturn(mockPreferences2);
  }
  
  @Test
  public void testIsUnsaved() {
    PendingSaveManager psm = new PendingSaveManager();
    assertFalse(psm.isUnsaved(mockFile1));
    psm.markAsUnsaved(mockFile1);
    assertTrue(psm.isUnsaved(mockFile1));
    psm.markAsSaved(mockFile1);
    assertFalse(psm.isUnsaved(mockFile1));
  }
  
  @Test
  public void testAllUnsavedFiles() {
    PendingSaveManager psm = new PendingSaveManager();
    Collection<String> unsavedInProject1AtTime0 = psm.allUnsavedFiles(mockProject1);
    Collection<String> unsavedInProject2AtTime0 = psm.allUnsavedFiles(mockProject2);
    assertTrue(unsavedInProject1AtTime0.isEmpty());
    assertTrue(unsavedInProject2AtTime0.isEmpty());
    Collection<String> unsavedInProject1AtTime0Snapshot =
        ImmutableList.copyOf(unsavedInProject1AtTime0);
    Collection<String> unsavedInProject2AtTime0Snapshot =
        ImmutableList.copyOf(unsavedInProject2AtTime0);
    
    psm.markAsUnsaved(mockFile1);
    Collection<String> unsavedInProject1AtTime1 = psm.allUnsavedFiles(mockProject1);
    Collection<String> unsavedInProject2AtTime1 = psm.allUnsavedFiles(mockProject2);
    assertEquals(multisetOf("file1"), multisetOf(unsavedInProject1AtTime1));
    assertTrue(unsavedInProject2AtTime1.isEmpty());
    // Check that lists returned earlier were not mutated:
    assertEquals(unsavedInProject1AtTime0Snapshot, unsavedInProject1AtTime0);
    assertEquals(unsavedInProject2AtTime0Snapshot, unsavedInProject2AtTime0);
    Collection<String> unsavedInProject1AtTime1Snapshot =
        ImmutableList.copyOf(unsavedInProject1AtTime1);
    Collection<String> unsavedInProject2AtTime1Snapshot =
        ImmutableList.copyOf(unsavedInProject2AtTime1);
    
    psm.markAsUnsaved(mockFile2);
    Collection<String> unsavedInProject1AtTime2 = psm.allUnsavedFiles(mockProject1);
    Collection<String> unsavedInProject2AtTime2 = psm.allUnsavedFiles(mockProject2);
    assertEquals(multisetOf("file1"), multisetOf(unsavedInProject1AtTime2));
    assertEquals(multisetOf("file2"), multisetOf(unsavedInProject2AtTime2));
    // Check that lists returned earlier were not mutated:
    assertEquals(unsavedInProject1AtTime0Snapshot, unsavedInProject1AtTime0);
    assertEquals(unsavedInProject2AtTime0Snapshot, unsavedInProject2AtTime0);
    assertEquals(unsavedInProject1AtTime1Snapshot, unsavedInProject1AtTime1);
    assertEquals(unsavedInProject2AtTime1Snapshot, unsavedInProject2AtTime1);
    Collection<String> unsavedInProject1AtTime2Snapshot =
        ImmutableList.copyOf(unsavedInProject1AtTime2);
    Collection<String> unsavedInProject2AtTime2Snapshot =
        ImmutableList.copyOf(unsavedInProject2AtTime2);
    
    psm.markAsSaved(mockFile1);
    Collection<String> unsavedInProject1AtTime3 = psm.allUnsavedFiles(mockProject1);
    Collection<String> unsavedInProject2AtTime3 = psm.allUnsavedFiles(mockProject2);
    assertTrue(unsavedInProject1AtTime3.isEmpty());
    assertEquals(multisetOf("file2"), multisetOf(unsavedInProject2AtTime3));
    // Check that lists returned earlier were not mutated:
    assertEquals(unsavedInProject1AtTime0Snapshot, unsavedInProject1AtTime0);
    assertEquals(unsavedInProject2AtTime0Snapshot, unsavedInProject2AtTime0);
    assertEquals(unsavedInProject1AtTime1Snapshot, unsavedInProject1AtTime1);
    assertEquals(unsavedInProject2AtTime1Snapshot, unsavedInProject2AtTime1);
    assertEquals(unsavedInProject1AtTime2Snapshot, unsavedInProject1AtTime2);
    assertEquals(unsavedInProject2AtTime2Snapshot, unsavedInProject2AtTime2);
    Collection<String> unsavedInProject1AtTime3Snapshot =
        ImmutableList.copyOf(unsavedInProject1AtTime3);
    Collection<String> unsavedInProject2AtTime3Snapshot =
        ImmutableList.copyOf(unsavedInProject2AtTime3);
    
    psm.markAsSaved(mockFile2);
    Collection<String> unsavedInProject1AtTime4 = psm.allUnsavedFiles(mockProject1);
    Collection<String> unsavedInProject2AtTime4 = psm.allUnsavedFiles(mockProject2);
    assertTrue(unsavedInProject1AtTime4.isEmpty());
    assertTrue(unsavedInProject2AtTime4.isEmpty());
    // Check that lists returned earlier were not mutated:
    assertEquals(unsavedInProject1AtTime0Snapshot, unsavedInProject1AtTime0);
    assertEquals(unsavedInProject2AtTime0Snapshot, unsavedInProject2AtTime0);
    assertEquals(unsavedInProject1AtTime1Snapshot, unsavedInProject1AtTime1);
    assertEquals(unsavedInProject2AtTime1Snapshot, unsavedInProject2AtTime1);
    assertEquals(unsavedInProject1AtTime2Snapshot, unsavedInProject1AtTime2);
    assertEquals(unsavedInProject2AtTime2Snapshot, unsavedInProject2AtTime2);
    assertEquals(unsavedInProject1AtTime3Snapshot, unsavedInProject1AtTime3);
    assertEquals(unsavedInProject2AtTime3Snapshot, unsavedInProject2AtTime3);
  }
  
  @Test
  public void testListenerHandling() {
    PendingSaveManager psm = new PendingSaveManager();
    final List<PendingSaveEvent> eventHistory = Lists.newLinkedList();
    PendingSaveEventListener listener =
        new PendingSaveEventListener(){
          @Override public void onPendingSaveEvent(PendingSaveEvent event) {
            eventHistory.add(event);
          }
        };
    psm.addPendingSaveListener(listener);
    psm.markAsUnsaved(mockFile1);
    psm.markAsSaved(mockFile1);
    psm.removePendingSaveListener(listener);
    psm.markAsUnsaved(mockFile2);
    psm.markAsSaved(mockFile2);
    assertEquals(
        ImmutableList.of(
            new PendingSaveEvent(mockFile1, true), new PendingSaveEvent(mockFile1, false)),
        eventHistory);
  }
  
  private static Multiset<String> multisetOf(String element) {
    return HashMultiset.create(ImmutableList.of(element));
  }
  
  private static Multiset<String> multisetOf(Iterable<String> elements) {
    Multiset<String> result = HashMultiset.create();
    for (String element : elements) {
      result.add(element);
    }
    return result;
  }

}
