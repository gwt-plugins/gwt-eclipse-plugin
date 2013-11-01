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
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.gdt.eclipse.drive.natures.AppsScriptNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

/**
 * Unit test for {@link WorkspaceUtils}.
 */
@RunWith(JUnit4.class)
public class WorkspaceUtilsTest {
  
  @Mock private IProject closedNonAppsScriptProject1;
  @Mock private IProject closedAppsScriptProject1;
  @Mock private IProject openNonAppsScriptProject1;
  @Mock private IProject openAppsScriptProject1;
  @Mock private IProject closedNonAppsScriptProject2;
  @Mock private IProject closedAppsScriptProject2;
  @Mock private IProject openNonAppsScriptProject2;
  @Mock private IProject openAppsScriptProject2;
  
  
  @Test
  public void testAllOpenAppsScriptProjects() throws CoreException {
    MockitoAnnotations.initMocks(this);
    List<IProject> closedProjects =
        ImmutableList.of(
            closedNonAppsScriptProject1, closedNonAppsScriptProject1, closedNonAppsScriptProject2,
            closedAppsScriptProject2);
    List<IProject> openProjects =
        ImmutableList.of(
            openNonAppsScriptProject1, openAppsScriptProject1, openNonAppsScriptProject2,
            openAppsScriptProject2);
    List<IProject> nonAppsScriptProjects =
        ImmutableList.of(
            closedNonAppsScriptProject1, openNonAppsScriptProject1, closedNonAppsScriptProject2,
            openNonAppsScriptProject2);
    List<IProject> appsScriptProjects =
        ImmutableList.of(
            closedAppsScriptProject1, openAppsScriptProject1, closedAppsScriptProject2,
            openAppsScriptProject2);
    for (IProject closedProject : closedProjects) {
      when(closedProject.isOpen()).thenReturn(false);
    }
    for (IProject openProject : openProjects) {
      when(openProject.isOpen()).thenReturn(true);
    }
    for (IProject otherProject : nonAppsScriptProjects) {
      when(otherProject.hasNature(AppsScriptNature.NATURE_ID)).thenReturn(false);
    }
    for (IProject appsScriptProject : appsScriptProjects) {
      when(appsScriptProject.hasNature(AppsScriptNature.NATURE_ID)).thenReturn(true);
    }
    WorkspaceUtils.setMockProjects(
            closedNonAppsScriptProject1, closedAppsScriptProject1, openNonAppsScriptProject1,
            openAppsScriptProject1, closedNonAppsScriptProject2, closedAppsScriptProject2,
            openNonAppsScriptProject2, openAppsScriptProject2);
    
    List<IProject> result = ImmutableList.copyOf(WorkspaceUtils.allOpenAppsScriptProjects());
    assertEquals(2, result.size());
    assertSame(openAppsScriptProject1, result.get(0));
    assertSame(openAppsScriptProject2, result.get(1));
  }

}
